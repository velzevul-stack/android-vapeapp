import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../app.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/product.dart';
import '../../data/models/reservation.dart';
import '../../data/parse_utils.dart';
import '../../utils/category_display.dart';
import '../../utils/stock_formatter.dart';
import 'post_filters_sheet.dart';

/// Экран "Пост в чат" = Склад (копирование поста по наличию).
/// Форматирование как в старой версии. Без выбора форматов, без Excel, без чекбоксов.
class PostScreen extends ConsumerStatefulWidget {
  const PostScreen({super.key});

  @override
  ConsumerState<PostScreen> createState() => _PostScreenState();
}

class _PostScreenState extends ConsumerState<PostScreen> {
  PostFilterState _filters = const PostFilterState();
  bool _showCategoryNames = true;

  static const _showCategoryNamesKey = 'post_show_category_names';

  @override
  void initState() {
    super.initState();
    _loadShowCategoryNames();
  }

  Future<void> _loadShowCategoryNames() async {
    final prefs = await SharedPreferences.getInstance();
    final val = prefs.getBool(_showCategoryNamesKey);
    if (val != null && mounted) {
      setState(() => _showCategoryNames = val);
    }
  }

  Future<void> _setShowCategoryNames(bool v) async {
    setState(() => _showCategoryNames = v);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_showCategoryNamesKey, v);
  }

  @override
  Widget build(BuildContext context) {
    final productsAsync = ref.watch(productsWithStockProvider);
    final reservationsAsync = ref.watch(activeReservationsProvider);
    final categoryOrderAsync = ref.watch(categoryOrderProvider);

    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 24, 20, 0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Пост в чат',
                      style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                            color: AppColors.textPrimaryDark,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Генерация поста по наличию',
                      style: TextStyle(
                        color: AppColors.textSecondaryDark,
                        fontSize: 14,
                      ),
                    ),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ),
            productsAsync.when(
              loading: () => const SliverFillRemaining(
                child: Center(child: CircularProgressIndicator(color: AppColors.pastelMint)),
              ),
              error: (e, _) => SliverFillRemaining(
                child: Center(child: Text('Ошибка: $e', style: const TextStyle(color: AppColors.textPrimaryDark))),
              ),
              data: (products) {
                final reservedByProduct = reservationsAsync.when(
                  loading: () => const <int, int>{},
                  error: (_, __) => const <int, int>{},
                  data: (reservations) {
                    final m = <int, int>{};
                    for (final Reservation r in reservations) {
                      m[r.productId] = (m[r.productId] ?? 0) + r.quantity;
                    }
                    return m;
                  },
                );
                final productIdsWithReservations = reservedByProduct.keys.toSet();
                final filtered = _applyFilters(products, productIdsWithReservations);
                final categoryOrder = categoryOrderAsync.whenOrNull(data: (d) => d) ?? defaultCategoryOrder;
                final categoryDisplayNames = ref.watch(categoryDisplayNamesProvider).when(
                      data: (d) => d,
                      loading: () => const <String, String>{},
                      error: (_, __) => const <String, String>{},
                    );
                final result = formatStockForCabinet(
                  filtered,
                  reservedByProduct: reservedByProduct,
                  categoryOrder: categoryOrder,
                  categoryDisplayNames: categoryDisplayNames,
                  showCategoryNames: _showCategoryNames,
                );
                return SliverPadding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  sliver: SliverToBoxAdapter(
                    child: _FiltersAndActions(
                      filters: _filters,
                      onFiltersTap: () => _showFiltersSheet(context),
                      onCopyTap: () => _copyToClipboard(result.text),
                      onToggleCategoryNames: () => _setShowCategoryNames(!_showCategoryNames),
                      showCategoryNames: _showCategoryNames,
                      previewText: result.text,
                      totalCount: result.total,
                    ),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  List<Product> _applyFilters(List<Product> products, Set<int> productIdsWithReservations) {
    var list = products;
    if (_filters.inStockOnly) {
      list = list.where((p) => p.stock > 0).toList();
    }
    if (_filters.noBarcodeOnly) {
      list = list.where((p) => p.barcode == null || p.barcode!.isEmpty).toList();
    }
    if (_filters.reservedOnly) {
      list = list.where((p) => productIdsWithReservations.contains(p.id)).toList();
    }
    if (_filters.categoryIds.isNotEmpty) {
      list = list.where((p) => _filters.categoryIds.contains(p.category)).toList();
    }
    if (_filters.brandIds.isNotEmpty) {
      list = list.where((p) => _filters.brandIds.contains(p.brand)).toList();
    }
    if (_filters.strengths.isNotEmpty || _filters.manualStrengthInput.trim().isNotEmpty) {
      final mgSet = <String>{};
      final manual = _filters.manualStrengthInput.trim();
      if (manual.isNotEmpty) {
        final val = int.tryParse(manual);
        if (val != null && val >= 1 && val <= 99) mgSet.add(manual);
      }
      for (final s in _filters.strengths) mgSet.add(s);

      list = list.where((p) {
        // Крепость применяется только к жижам, одноразовым и снюс
        final applies = p.category == 'liquid' || p.category == 'disposable' || p.category == 'snus';
        if (!applies) return true; // остальные категории не фильтруются по крепости
        final mgInProduct = extractStrengthMgValues(p.brand)
          ..addAll(extractStrengthMgValues(p.specification))
          ..addAll(extractStrengthMgValues(p.strength));
        return mgInProduct.any((mg) => mgSet.contains(mg));
      }).toList();
    }
    if (_filters.minPrice.isNotEmpty) {
      final min = double.tryParse(_filters.minPrice.replaceAll(',', '.'));
      if (min != null) list = list.where((p) => p.retailPrice >= min).toList();
    }
    if (_filters.maxPrice.isNotEmpty) {
      final max = double.tryParse(_filters.maxPrice.replaceAll(',', '.'));
      if (max != null) list = list.where((p) => p.retailPrice <= max).toList();
    }
    return list;
  }

  void _copyToClipboard(String text) {
    Clipboard.setData(ClipboardData(text: text));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Текст скопирован в буфер обмена!'),
        backgroundColor: AppColors.pastelMint,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  void _showFiltersSheet(BuildContext context) {
    final productsAsync = ref.read(productsWithStockProvider);
    final reservationsAsync = ref.read(activeReservationsProvider);
    productsAsync.whenData((products) {
      final productIdsWithReservations = reservationsAsync.whenOrNull(
        data: (list) => list.map((r) => r.productId).toSet(),
      ) ?? const <int>{};
      showModalBottomSheet<void>(
        context: context,
        backgroundColor: AppColors.surfaceDark,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        builder: (ctx) => PostFiltersSheet(
          initialFilters: _filters,
          products: products,
          productIdsWithReservations: productIdsWithReservations,
          categoryOrder: ref.read(categoryOrderProvider).whenOrNull(data: (d) => d) ?? defaultCategoryOrder,
          categoryDisplayNames: ref.read(categoryDisplayNamesProvider).whenOrNull(data: (d) => d) ?? const <String, String>{},
          onApply: (f) {
            setState(() => _filters = f);
            Navigator.pop(ctx);
          },
        ),
      );
    });
  }
}

class _FiltersAndActions extends StatelessWidget {
  final PostFilterState filters;
  final VoidCallback onFiltersTap;
  final VoidCallback onCopyTap;
  final VoidCallback onToggleCategoryNames;
  final bool showCategoryNames;
  final String previewText;
  final int totalCount;

  const _FiltersAndActions({
    required this.filters,
    required this.onFiltersTap,
    required this.onCopyTap,
    required this.onToggleCategoryNames,
    required this.showCategoryNames,
    required this.previewText,
    required this.totalCount,
  });

  @override
  Widget build(BuildContext context) {
    final activeCount = filters.activeCount;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Text(
              'Товары',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    color: AppColors.textPrimaryDark,
                    fontWeight: FontWeight.w600,
                  ),
            ),
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: AppColors.surfaceElevatedDark,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                '$totalCount шт.',
                style: TextStyle(
                  color: AppColors.textSecondaryDark,
                  fontSize: 13,
                ),
              ),
            ),
            const Spacer(),
            GestureDetector(
              onTap: onFiltersTap,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: AppColors.surfaceElevatedDark,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.filter_list, size: 18, color: AppColors.pastelMint),
                    if (activeCount > 0) ...[
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: AppColors.pastelMint,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          '$activeCount',
                          style: const TextStyle(
                            color: AppColors.textOnPastel,
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(width: 6),
                    Text('Фильтры', style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 14)),
                  ],
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        OutlinedButton.icon(
          onPressed: onCopyTap,
          icon: const Icon(Icons.copy, size: 20),
          label: const Text('Копировать'),
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.pastelMint,
            side: const BorderSide(color: AppColors.pastelMint),
            padding: const EdgeInsets.symmetric(vertical: 14),
          ),
        ),
        const SizedBox(height: 8),
        TextButton.icon(
          onPressed: onToggleCategoryNames,
          icon: Icon(
            showCategoryNames ? Icons.visibility_off_outlined : Icons.visibility_outlined,
            size: 18,
            color: AppColors.textSecondaryDark,
          ),
          label: Text(
            showCategoryNames ? 'Скрыть категории' : 'Показать категории',
            style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 14),
          ),
        ),
        const SizedBox(height: 16),
        _PreviewCard(previewText: previewText),
        const SizedBox(height: 20),
      ],
    );
  }
}

/// Карточка предпросмотра — текст точно как для копирования
class _PreviewCard extends StatelessWidget {
  final String previewText;

  const _PreviewCard({required this.previewText});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: AppColors.pastelBlue,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Предпросмотр поста',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: AppColors.textOnPastel,
                  fontWeight: FontWeight.w600,
                  fontSize: 14,
                ),
          ),
          const SizedBox(height: 12),
          SelectableText(
            previewText,
            style: const TextStyle(
              color: AppColors.textOnPastel,
              fontSize: 14,
              height: 1.6,
            ),
          ),
        ],
      ),
    );
  }
}
