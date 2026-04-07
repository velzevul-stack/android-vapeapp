import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/product.dart';
import '../../data/parse_utils.dart';
import 'dialogs/brands_manager_modal.dart';
import 'dialogs/category_order_modal.dart';
import 'dialogs/edit_brand_dialog.dart';
import 'dialogs/edit_product_dialog.dart';
import '../../utils/category_display.dart';
import 'dialogs/products_filter_modal.dart';
const _categoryIcons = {
  'liquid': Icons.water_drop_outlined,
  'disposable': Icons.battery_charging_full_outlined,
  'consumable': Icons.inventory_2_outlined,
  'vape': Icons.smoking_rooms_outlined,
  'snus': Icons.eco_outlined,
};

class ProductsScreen extends ConsumerStatefulWidget {
  const ProductsScreen({super.key});

  @override
  ConsumerState<ProductsScreen> createState() => _ProductsScreenState();
}

class _ProductsScreenState extends ConsumerState<ProductsScreen> {
  final _searchController = TextEditingController();
  final _scrollController = ScrollController();
  String _searchQuery = '';
  ProductsFilters _filters = ProductsFilters();
  final Set<String> _expandedCategories = {'liquid', 'disposable'};
  final Set<String> _expandedBrands = {};

  @override
  void initState() {
    super.initState();
    _searchController.addListener(() {
      setState(() => _searchQuery = _searchController.text.trim().toLowerCase());
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  List<Product> _applyFilters(List<Product> products, Set<int> productIdsWithReservations) {
    var list = products;
    if (_searchQuery.isNotEmpty) {
      final looksLikeBarcode = RegExp(r'^\d+$').hasMatch(_searchQuery) && _searchQuery.length >= 8;
      list = list.where((p) {
        if (looksLikeBarcode && p.barcode != null && p.barcode!.contains(_searchQuery)) return true;
        return p.brand.toLowerCase().contains(_searchQuery) ||
            p.flavor.toLowerCase().contains(_searchQuery) ||
            (p.specification.isNotEmpty && p.specification.toLowerCase().contains(_searchQuery)) ||
            (p.barcode != null && p.barcode!.contains(_searchQuery));
      }).toList();
    }
    if (_filters.inStockOnly) {
      list = list.where((p) => p.stock > 0).toList();
    }
    if (_filters.noBarcodeOnly) {
      list = list.where((p) => p.barcode == null || p.barcode!.isEmpty).toList();
    }
    if (_filters.reservedOnly) {
      list = list.where((p) => productIdsWithReservations.contains(p.id)).toList();
    }
    if (_filters.selectedCategories.isNotEmpty) {
      list = list.where((p) => _filters.selectedCategories.contains(p.category)).toList();
    }
    if (_filters.selectedBrands.isNotEmpty) {
      list = list.where((p) => _filters.selectedBrands.contains(p.brand)).toList();
    }
    if (_filters.selectedStrengths.isNotEmpty || _filters.manualStrengthInput.trim().isNotEmpty) {
      final mgSet = <String>{};
      final manual = _filters.manualStrengthInput.trim();
      if (manual.isNotEmpty) {
        final val = int.tryParse(manual);
        if (val != null && val >= 1 && val <= 99) mgSet.add(manual);
      }
      for (final s in _filters.selectedStrengths) mgSet.add(s);

      list = list.where((p) {
        // Крепость только для жиж, одноразок и снюса
        final applies = p.category == 'liquid' || p.category == 'disposable' || p.category == 'snus';
        if (!applies) return true; // все остальные категории проходят фильтр
        final mgs = extractStrengthMgValues(p.brand)
          ..addAll(extractStrengthMgValues(p.specification))
          ..addAll(extractStrengthMgValues(p.strength));
        return mgs.any((m) => mgSet.contains(m));
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

  Future<void> _updateStock(Product product, int delta) async {
    if (delta == 0) return;
    final repo = ref.read(repositoryProvider);
    if (delta > 0) {
      await repo.increaseStock(product.id);
    } else {
      if (product.stock <= 0) return;
      await repo.decreaseStock(product.id);
    }
    notifyDataChanged(ref);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(delta > 0 ? '+1 шт · ${product.brand} ${product.flavor}' : '−1 шт · ${product.brand} ${product.flavor}'),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  Future<void> _rejectProduct(Product product) async {
    if (product.stock <= 0) return;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surfaceDark,
        title: const Text('Забраковать?', style: TextStyle(color: AppColors.textPrimaryDark)),
        content: Text(
          'Списать 1 шт. ${product.brand} ${product.flavor} без прибыли',
          style: const TextStyle(color: AppColors.textSecondaryDark),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Отмена')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.pastelPink),
            child: const Text('Забраковать'),
          ),
        ],
      ),
    );
    if (ok == true) await _updateStock(product, -1);
  }

  Future<void> _editProduct(Product product) async {
    await showDialog<void>(
      context: context,
        builder: (ctx) => EditProductDialog(
        product: product,
        onDismiss: () => Navigator.pop(ctx),
        onSave: (_) async {},
      ),
    );
  }

  Future<void> _editBrand(String brand, {Product? sampleProduct}) async {
    await showDialog<void>(
      context: context,
      builder: (ctx) => EditBrandDialog(
        currentBrand: brand,
        category: sampleProduct?.category,
        strength: sampleProduct?.strength.isNotEmpty == true ? sampleProduct!.strength : null,
        retailPrice: sampleProduct?.retailPrice,
        purchasePrice: sampleProduct?.purchasePrice,
      ),
    );
  }

  void _showFilterModal(List<Product> allProducts) {
    final categoryOrder = ref.read(categoryOrderProvider).whenOrNull(data: (d) => d) ?? defaultCategoryOrder;
    final categories = List<String>.from(categoryOrder);
    final brands = allProducts.map((p) => p.brand).toSet().toList()..sort();
    final customNames = ref.read(categoryDisplayNamesProvider).whenOrNull(data: (d) => d) ?? const <String, String>{};
    final reservationsAsync = ref.watch(activeReservationsProvider);
    final productIdsWithReservations = reservationsAsync.whenOrNull(
      data: (list) => list.map((r) => r.productId).toSet(),
    ) ?? <int>{};

    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (ctx) => ProductsFilterModal(
        filters: _filters,
        categories: categories,
        brands: brands,
        products: allProducts,
        productIdsWithReservations: productIdsWithReservations,
        categoryDisplayNames: customNames,
        onApply: (f) {
          setState(() => _filters = f);
        },
        onDismiss: () => Navigator.pop(ctx),
      ),
    );
  }

  void _showCategoryOrderModal() {
    showDialog(
      context: context,
      builder: (ctx) => const CategoryOrderModal(),
    );
  }

  void _showBrandsManager(List<Product> allProducts) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (ctx) => BrandsManagerModal(
        allProducts: allProducts,
        onDismiss: () => Navigator.pop(ctx),
        onSaved: () => Navigator.pop(ctx),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final productsAsync = ref.watch(allProductsProvider);
    final reservationsAsync = ref.watch(activeReservationsProvider);
    final categoryOrder = ref.watch(categoryOrderProvider).whenOrNull(data: (d) => d) ?? defaultCategoryOrder;
    final customNames = ref.watch(categoryDisplayNamesProvider).when(
          data: (d) => d,
          loading: () => const <String, String>{},
          error: (_, __) => const <String, String>{},
        );
    final productIdsWithReservations = reservationsAsync.whenOrNull(
      data: (list) => list.map((r) => r.productId).toSet(),
    ) ?? <int>{};

    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
              child: Row(
                children: [
                  InkWell(
                    onTap: () => Navigator.pop(context),
                    borderRadius: BorderRadius.circular(12),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.arrow_back_ios, size: 20, color: AppColors.textPrimaryDark),
                          const SizedBox(width: 8),
                          Text('Назад', style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w500, fontSize: 16)),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  TextField(
                    controller: _searchController,
                    decoration: InputDecoration(
                      hintText: 'Поиск по бренду, вкусу...',
                      hintStyle: TextStyle(color: AppColors.textTertiaryDark, fontSize: 16),
                      prefixIcon: Icon(Icons.search, size: 24, color: AppColors.textSecondaryDark),
                      filled: true,
                      fillColor: AppColors.surfaceElevatedDark,
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                    ),
                    style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 16),
                  ),
                  const SizedBox(height: 16),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: [
                      InkWell(
                        onTap: _showCategoryOrderModal,
                        borderRadius: BorderRadius.circular(14),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceElevatedDark,
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: AppColors.borderDark.withValues(alpha: 0.5)),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.sort_outlined, size: 22, color: AppColors.textSecondaryDark),
                              const SizedBox(width: 10),
                              Text('Категории', style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 15, fontWeight: FontWeight.w500)),
                            ],
                          ),
                        ),
                      ),
                      InkWell(
                        onTap: () => productsAsync.whenOrNull(data: _showBrandsManager),
                        borderRadius: BorderRadius.circular(14),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceElevatedDark,
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: AppColors.borderDark.withValues(alpha: 0.5)),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.local_offer_outlined, size: 22, color: AppColors.textSecondaryDark),
                              const SizedBox(width: 10),
                              Text('Бренды', style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 15, fontWeight: FontWeight.w500)),
                            ],
                          ),
                        ),
                      ),
                      InkWell(
                        onTap: () => productsAsync.whenOrNull(data: _showFilterModal),
                        borderRadius: BorderRadius.circular(14),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceElevatedDark,
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: AppColors.borderDark.withValues(alpha: 0.5)),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Badge(
                                isLabelVisible: _filters.activeCount > 0,
                                label: Text('${_filters.activeCount}', style: const TextStyle(fontSize: 11)),
                                child: Icon(Icons.filter_list, size: 22, color: AppColors.textSecondaryDark),
                              ),
                              const SizedBox(width: 10),
                              Text('Фильтры', style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 15, fontWeight: FontWeight.w500)),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        _categoryChip(null),
                        ...categoryOrder.map((c) => _categoryChip(c)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: productsAsync.when(
                loading: () => productsAsync.hasValue
                    ? _buildProductList(productsAsync.value!, productIdsWithReservations, categoryOrder, customNames)
                    : const Center(child: CircularProgressIndicator(color: AppColors.pastelMint)),
                error: (e, _) => Center(child: Text('Ошибка: $e', style: TextStyle(color: AppColors.textPrimaryDark))),
                data: (allProducts) => _buildProductList(allProducts, productIdsWithReservations, categoryOrder, customNames),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildProductList(List<Product> allProducts, Set<int> productIdsWithReservations, List<String> categoryOrder, Map<String, String> customNames) {
    final filtered = _applyFilters(allProducts, productIdsWithReservations);
    if (filtered.isEmpty) {
      return Center(
        child: Text(
          _searchQuery.isNotEmpty || _filters.activeCount > 0 ? 'Ничего не найдено' : 'Нет товаров',
          style: TextStyle(color: AppColors.textSecondaryDark),
        ),
      );
    }

    final byCategory = <String, List<Product>>{};
    for (final p in filtered) {
      byCategory.putIfAbsent(p.category, () => []).add(p);
    }
    final categories = categoryOrder.where((c) => byCategory.containsKey(c)).toList();

    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
      itemCount: categories.length,
      itemBuilder: (context, ci) {
        final cat = categories[ci];
        final items = byCategory[cat]!..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));
        final byBrand = <String, List<Product>>{};
        final brandMinOrder = <String, int>{};
        for (final p in items) {
          byBrand.putIfAbsent(p.brand, () => []).add(p);
          final cur = brandMinOrder[p.brand];
          if (cur == null || p.orderIndex < cur) brandMinOrder[p.brand] = p.orderIndex;
        }
        final brands = byBrand.keys.toList()..sort((a, b) => (brandMinOrder[a] ?? 0).compareTo(brandMinOrder[b] ?? 0));
        final isExpanded = _expandedCategories.contains(cat);

        return Container(
          margin: const EdgeInsets.only(bottom: 12),
          decoration: BoxDecoration(
            color: AppColors.surfaceDark,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.borderDark.withValues(alpha: 0.5)),
          ),
          child: Column(
            children: [
              InkWell(
                onTap: () {
                  setState(() {
                    if (isExpanded) _expandedCategories.remove(cat);
                    else _expandedCategories.add(cat);
                  });
                },
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                  child: Row(
                    children: [
                      Icon(_categoryIcons[cat] ?? Icons.category_outlined, size: 24, color: AppColors.textSecondaryDark),
                      const SizedBox(width: 12),
                      Text(
                        getCategoryDisplay(cat, customNames: customNames).$1,
                        style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 17),
                      ),
                      const Spacer(),
                      Icon(isExpanded ? Icons.expand_less : Icons.expand_more, color: AppColors.textSecondaryDark, size: 24),
                    ],
                  ),
                ),
              ),
              if (isExpanded)
                ...brands.map((brand) {
                  final brandProducts = byBrand[brand]!..sort((a, b) => a.flavor.compareTo(b.flavor));
                  final key = '$cat-$brand';
                  final brandExpanded = _expandedBrands.contains(key);

                  return Column(
                    children: [
                      InkWell(
                        onTap: () {
                          setState(() {
                            if (brandExpanded) _expandedBrands.remove(key);
                            else _expandedBrands.add(key);
                          });
                        },
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  brand,
                                  style: TextStyle(color: AppColors.textSecondaryDark, fontWeight: FontWeight.w500, fontSize: 14),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              IconButton(
                                icon: Icon(Icons.edit_outlined, size: 22, color: AppColors.textTertiaryDark),
                                onPressed: () => _editBrand(brand, sampleProduct: brandProducts.first),
                                style: IconButton.styleFrom(minimumSize: const Size(44, 44)),
                                tooltip: 'Редактировать бренд',
                              ),
                              Text(
                                '${brandProducts.length}',
                                style: TextStyle(color: AppColors.textTertiaryDark, fontSize: 12),
                              ),
                              const SizedBox(width: 4),
                              Icon(brandExpanded ? Icons.expand_less : Icons.expand_more, color: AppColors.textSecondaryDark, size: 18),
                            ],
                          ),
                        ),
                      ),
                      if (brandExpanded)
                        ...brandProducts.map((p) => _ProductRow(
                              product: p,
                              onEdit: () => _editProduct(p),
                              onPlus: () => _updateStock(p, 1),
                              onMinus: () => _rejectProduct(p),
                            )),
                    ],
                  );
                }),
            ],
          ),
        );
      },
    );
  }

  Widget _categoryChip(String? category) {
    final selected = category == null
        ? _filters.selectedCategories.isEmpty
        : _filters.selectedCategories.contains(category);
    final customNames = ref.watch(categoryDisplayNamesProvider).when(
          data: (d) => d,
          loading: () => const <String, String>{},
          error: (_, __) => const <String, String>{},
        );
    final label = category == null ? 'Все' : getCategoryDisplay(category, customNames: customNames).$1;

    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: FilterChip(
        label: Text(label, style: const TextStyle(fontSize: 14)),
        selected: selected,
        onSelected: (_) => setState(() {
          if (category == null) {
            _filters.selectedCategories = [];
          } else {
            final list = List<String>.from(_filters.selectedCategories);
            if (list.contains(category)) {
              list.remove(category);
            } else {
              list.add(category);
            }
            _filters.selectedCategories = list;
          }
        }),
        backgroundColor: AppColors.surfaceElevatedDark,
        selectedColor: AppColors.pastelMint.withValues(alpha: 0.3),
        checkmarkColor: AppColors.pastelMint,
        labelStyle: TextStyle(color: selected ? AppColors.pastelMint : AppColors.textPrimaryDark, fontSize: 14),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      ),
    );
  }
}

class _ProductRow extends StatelessWidget {
  final Product product;
  final VoidCallback onEdit;
  final VoidCallback onPlus;
  final VoidCallback onMinus;

  const _ProductRow({
    required this.product,
    required this.onEdit,
    required this.onPlus,
    required this.onMinus,
  });

  @override
  Widget build(BuildContext context) {
    final stockColor = product.stock == 0
        ? AppColors.pastelPink
        : product.stock <= 2
            ? AppColors.pastelLavender
            : AppColors.textSecondaryDark;

    return InkWell(
      onTap: onEdit,
      child: Container(
        padding: const EdgeInsets.fromLTRB(36, 14, 16, 14),
        decoration: BoxDecoration(
          color: AppColors.backgroundDark.withValues(alpha: 0.5),
          border: Border(top: BorderSide(color: AppColors.borderDark.withValues(alpha: 0.3))),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    product.brand,
                    style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w500, fontSize: 15),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  if (product.flavor.isNotEmpty && product.flavor != product.brand)
                    Padding(
                      padding: const EdgeInsets.only(top: 2),
                      child: Text(
                        product.flavor,
                        style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 14),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  if (product.barcode == null || product.barcode!.isEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Text('Без штрихкода', style: TextStyle(color: AppColors.pastelPink, fontSize: 12)),
                    ),
                ],
              ),
            ),
            IconButton(
              icon: Icon(Icons.edit_outlined, size: 22, color: AppColors.textSecondaryDark),
              onPressed: onEdit,
              style: IconButton.styleFrom(minimumSize: const Size(44, 44)),
              tooltip: 'Редактировать',
            ),
            IconButton(
              icon: Icon(Icons.remove_circle_outline, size: 24, color: AppColors.pastelPink),
              onPressed: product.stock > 0 ? onMinus : null,
              style: IconButton.styleFrom(minimumSize: const Size(44, 44)),
              tooltip: 'Забраковать',
            ),
            SizedBox(
              width: 36,
              child: Text(
                '${product.stock}',
                textAlign: TextAlign.center,
                style: TextStyle(color: stockColor, fontWeight: FontWeight.w600, fontSize: 16),
              ),
            ),
            IconButton(
              icon: Icon(Icons.add_circle_outline, size: 24, color: AppColors.pastelMint),
              onPressed: onPlus,
              style: IconButton.styleFrom(minimumSize: const Size(44, 44)),
              tooltip: 'Добавить',
            ),
          ],
        ),
      ),
    );
  }
}
