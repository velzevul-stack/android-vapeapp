import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../data/models/product.dart';
import '../../data/parse_utils.dart';
import '../../utils/category_display.dart';

/// Состояние умных фильтров для поста (без цвета и вкуса)
class PostFilterState {
  final List<String> categoryIds;
  final List<String> brandIds;
  final List<String> strengths;
  final bool inStockOnly;
  final bool noBarcodeOnly;
  final bool reservedOnly;
  final String minPrice;
  final String maxPrice;
  final String manualStrengthInput;

  const PostFilterState({
    this.categoryIds = const [],
    this.brandIds = const [],
    this.strengths = const [],
    this.inStockOnly = false,
    this.noBarcodeOnly = false,
    this.reservedOnly = false,
    this.minPrice = '',
    this.maxPrice = '',
    this.manualStrengthInput = '',
  });

  int get activeCount =>
      (inStockOnly ? 1 : 0) +
      (noBarcodeOnly ? 1 : 0) +
      (reservedOnly ? 1 : 0) +
      (categoryIds.isNotEmpty ? 1 : 0) +
      (brandIds.isNotEmpty ? 1 : 0) +
      (strengths.isNotEmpty ? 1 : 0) +
      (manualStrengthInput.trim().isNotEmpty ? 1 : 0) +
      (minPrice.isNotEmpty ? 1 : 0) +
      (maxPrice.isNotEmpty ? 1 : 0);

  PostFilterState copyWith({
    List<String>? categoryIds,
    List<String>? brandIds,
    List<String>? strengths,
    bool? inStockOnly,
    bool? noBarcodeOnly,
    bool? reservedOnly,
    String? minPrice,
    String? maxPrice,
    String? manualStrengthInput,
  }) {
    return PostFilterState(
      categoryIds: categoryIds ?? this.categoryIds,
      brandIds: brandIds ?? this.brandIds,
      strengths: strengths ?? this.strengths,
      inStockOnly: inStockOnly ?? this.inStockOnly,
      noBarcodeOnly: noBarcodeOnly ?? this.noBarcodeOnly,
      reservedOnly: reservedOnly ?? this.reservedOnly,
      minPrice: minPrice ?? this.minPrice,
      maxPrice: maxPrice ?? this.maxPrice,
      manualStrengthInput: manualStrengthInput ?? this.manualStrengthInput,
    );
  }
}

// Известные категории (для отображения даже если нет товаров в наличии)
final knownCategories = {
  'liquid': ('Жижи', '💧'),
  'disposable': ('Одноразовые', '🔋'),
  'consumable': ('Расходники', '📦'),
  'vape': ('Устройства', '💨'),
  'snus': ('Снюс', '🍃'),
};

class PostFiltersSheet extends StatefulWidget {
  final PostFilterState initialFilters;
  final List<Product> products;
  final Set<int> productIdsWithReservations;
  final List<String> categoryOrder;
  final Map<String, String> categoryDisplayNames;
  final void Function(PostFilterState) onApply;

  const PostFiltersSheet({
    super.key,
    required this.initialFilters,
    required this.products,
    required this.productIdsWithReservations,
    required this.categoryOrder,
    this.categoryDisplayNames = const {},
    required this.onApply,
  });

  @override
  State<PostFiltersSheet> createState() => _PostFiltersSheetState();
}

class _PostFiltersSheetState extends State<PostFiltersSheet> {
  late PostFilterState _filters;
  String _brandSearch = '';
  late TextEditingController _minPriceController;
  late TextEditingController _maxPriceController;
  late TextEditingController _strengthInputController;

  @override
  void initState() {
    super.initState();
    _filters = widget.initialFilters;
    _minPriceController = TextEditingController(text: widget.initialFilters.minPrice);
    _maxPriceController = TextEditingController(text: widget.initialFilters.maxPrice);
    _strengthInputController = TextEditingController(text: widget.initialFilters.manualStrengthInput);
  }

  @override
  void dispose() {
    _minPriceController.dispose();
    _maxPriceController.dispose();
    _strengthInputController.dispose();
    super.dispose();
  }

  List<String> _getAvailableBrands() {
    final selected = _filters.categoryIds;
    if (selected.isEmpty) {
      return widget.products.map((p) => p.brand).toSet().toList()..sort();
    }
    return widget.products
        .where((p) => selected.contains(p.category))
        .map((p) => p.brand)
        .toSet()
        .toList()
      ..sort();
  }

  List<String> _getAvailableStrengths() {
    final selected = _filters.categoryIds;
    // Показываем крепость только для жижей, одноразовых и снюс
    final needStrength = selected.isEmpty ||
        selected.any((c) => c == 'liquid' || c == 'disposable' || c == 'snus');
    if (!needStrength) return [];

    final products = selected.isEmpty
        ? widget.products
        : widget.products.where((p) => selected.contains(p.category)).toList();

    final mgValues = <String>{};
    for (final p in products) {
      for (final s in extractStrengthMgValues(p.brand)) mgValues.add(s);
      for (final s in extractStrengthMgValues(p.specification)) mgValues.add(s);
      for (final s in extractStrengthMgValues(p.strength)) mgValues.add(s);
      // Не берем из flavor для крепости
    }
    return mgValues.toList()..sort((a, b) => (int.tryParse(a) ?? 0).compareTo(int.tryParse(b) ?? 0));
  }

  /// Категории из продуктов с наличием + известные (на случай пустого склада)
  List<({String id, String name, String emoji, int count})> _getAvailableCategories() {
    final byCat = <String, int>{};
    for (final p in widget.products) {
      if (p.stock > 0) {
        byCat[p.category] = (byCat[p.category] ?? 0) + 1;
      }
    }
    final allIds = {
      ...byCat.keys,
      ...knownCategories.keys,
    };
    final list = allIds.map((id) {
      final (name, emoji) = getCategoryDisplay(id, customNames: widget.categoryDisplayNames);
      return (id: id, name: name, emoji: emoji, count: byCat[id] ?? 0);
    }).where((c) => c.count > 0).toList();
    list.sort((a, b) {
      final ai = widget.categoryOrder.indexOf(a.id);
      final bi = widget.categoryOrder.indexOf(b.id);
      if (ai >= 0 && bi >= 0) return ai.compareTo(bi);
      if (ai >= 0) return -1;
      if (bi >= 0) return 1;
      return a.name.compareTo(b.name);
    });
    return list;
  }

  void _toggleCategory(String id) {
    setState(() {
      final list = List<String>.from(_filters.categoryIds);
      if (list.contains(id)) {
        list.remove(id);
      } else {
        list.add(id);
      }
      final newCategories = list;
      final validBrands = newCategories.isEmpty
          ? _filters.brandIds
          : _filters.brandIds.where((b) => widget.products
              .any((p) => p.brand == b && newCategories.contains(p.category))).toList();
      _filters = _filters.copyWith(
        categoryIds: newCategories,
        brandIds: validBrands,
      );
    });
  }

  void _selectAllCategories(List<String> ids) {
    setState(() => _filters = _filters.copyWith(categoryIds: ids));
  }

  void _clearCategories() {
    setState(() => _filters = _filters.copyWith(
      categoryIds: [],
      brandIds: [],
      strengths: [],
    ));
  }

  void _toggleBrand(String id) {
    setState(() {
      final list = List<String>.from(_filters.brandIds);
      if (list.contains(id)) {
        list.remove(id);
      } else {
        list.add(id);
      }
      _filters = _filters.copyWith(brandIds: list);
    });
  }

  void _toggleStrength(String s) {
    setState(() {
      final list = List<String>.from(_filters.strengths);
      if (list.contains(s)) {
        list.remove(s);
      } else {
        list.add(s);
      }
      _filters = _filters.copyWith(strengths: list);
    });
  }

  void _reset() {
    setState(() {
      _filters = const PostFilterState();
      _minPriceController.text = '';
      _maxPriceController.text = '';
      _strengthInputController.text = '';
      _brandSearch = '';
    });
  }

  @override
  Widget build(BuildContext context) {
    final categories = _getAvailableCategories();
    final brands = _getAvailableBrands();
    final strengths = _getAvailableStrengths();
    final hasStrength = strengths.isNotEmpty;

    return DraggableScrollableSheet(
      initialChildSize: 0.65,
      maxChildSize: 0.92,
      minChildSize: 0.4,
      expand: false,
      builder: (context, scrollController) {
        return Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 12),
              Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.textTertiaryDark,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(20),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Фильтры для поста',
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            color: AppColors.textPrimaryDark,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                    TextButton(
                      onPressed: _reset,
                      child: Text(
                        'Сбросить',
                        style: TextStyle(color: AppColors.pastelMint, fontSize: 14),
                      ),
                    ),
                  ],
                ),
              ),
              Flexible(
                child: ListView(
                  controller: scrollController,
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  children: [
                    _SectionTitle('Категории (${categories.length})'),
                    const SizedBox(height: 8),
                    if (categories.isNotEmpty) ...[
                      Row(
                        children: [
                          TextButton(
                            onPressed: () => _selectAllCategories(categories.map((c) => c.id).toList()),
                            child: Text('Выбрать все', style: TextStyle(color: AppColors.pastelMint, fontSize: 12)),
                          ),
                          TextButton(
                            onPressed: _clearCategories,
                            child: Text('Очистить', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: categories.map((c) {
                          final isSelected = _filters.categoryIds.contains(c.id);
                          final label = c.count > 0 ? '${c.emoji} ${c.name} (${c.count})' : '${c.emoji} ${c.name}';
                          return FilterChip(
                            label: Text(label),
                            selected: isSelected,
                            onSelected: (_) => _toggleCategory(c.id),
                            backgroundColor: AppColors.surfaceElevatedDark,
                            selectedColor: AppColors.pastelMint.withValues(alpha: 0.3),
                            side: BorderSide(
                              color: isSelected ? AppColors.pastelMint : AppColors.borderDark,
                            ),
                          );
                        }).toList(),
                      ),
                    ] else
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        child: Text(
                          'Нет товаров в наличии',
                          style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 13),
                        ),
                      ),
                    // КреPOСТЬ ПЕРЕД брендами
                    if (hasStrength) ...[
                      const SizedBox(height: 20),
                      _SectionTitle('Крепость (mg)${_filters.strengths.isEmpty && _strengthInputController.text.trim().isEmpty ? '' : ' (${_filters.strengths.length}${_strengthInputController.text.trim().isNotEmpty ? '+1' : ''})'}'),
                      const SizedBox(height: 8),
                      TextField(
                        controller: _strengthInputController,
                        onChanged: (_) => setState(() {}),
                        decoration: InputDecoration(
                          labelText: 'Ввести вручную (например 20, 50)',
                          labelStyle: TextStyle(color: AppColors.textSecondaryDark),
                          hintText: '20',
                          hintStyle: TextStyle(color: AppColors.textTertiaryDark),
                          filled: true,
                          fillColor: AppColors.surfaceElevatedDark,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: BorderSide.none,
                          ),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                        ),
                        style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 16),
                        keyboardType: TextInputType.number,
                        cursorColor: AppColors.pastelMint,
                      ),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: strengths.map((s) {
                          final display = '$s mg';
                          final isSelected = _filters.strengths.contains(s);
                          return FilterChip(
                            label: Text(display),
                            selected: isSelected,
                            onSelected: (_) => _toggleStrength(s),
                            backgroundColor: AppColors.surfaceElevatedDark,
                            selectedColor: AppColors.pastelLavender.withValues(alpha: 0.3),
                            side: BorderSide(
                              color: isSelected ? AppColors.pastelLavender : AppColors.borderDark,
                            ),
                          );
                        }).toList(),
                      ),
                    ],
                    // Быстрые фильтры ПЕРЕД брендами
                    const SizedBox(height: 20),
                    _SectionTitle('Быстрые фильтры'),
                    const SizedBox(height: 8),
                    _FilterPlate(
                      label: 'Только в наличии',
                      icon: Icons.inventory,
                      isSelected: _filters.inStockOnly,
                      accentColor: AppColors.pastelMint,
                      onTap: () => setState(() => _filters = _filters.copyWith(inStockOnly: !_filters.inStockOnly)),
                    ),
                    const SizedBox(height: 6),
                    _FilterPlate(
                      label: 'Без штрихкода',
                      icon: Icons.qr_code_2_outlined,
                      isSelected: _filters.noBarcodeOnly,
                      accentColor: AppColors.pastelLavender,
                      onTap: () => setState(() => _filters = _filters.copyWith(noBarcodeOnly: !_filters.noBarcodeOnly)),
                    ),
                    const SizedBox(height: 6),
                    _FilterPlate(
                      label: 'Только резервы',
                      icon: Icons.bookmark_outline,
                      isSelected: _filters.reservedOnly,
                      accentColor: AppColors.pastelBlue,
                      onTap: () => setState(() => _filters = _filters.copyWith(reservedOnly: !_filters.reservedOnly)),
                    ),
                    const SizedBox(height: 20),
                    _SectionTitle('Бренды${brands.isEmpty ? '' : ' (${brands.length})'}'),
                    const SizedBox(height: 8),
                    if (brands.isEmpty)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        child: Text(
                          'Выберите категории с товарами',
                          style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 13),
                        ),
                      )
                    else ...[
                      if (brands.length > 8)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: TextField(
                            onChanged: (v) => setState(() => _brandSearch = v.trim().toLowerCase()),
                            decoration: InputDecoration(
                              hintText: 'Поиск бренда...',
                              hintStyle: TextStyle(color: AppColors.textTertiaryDark),
                              prefixIcon: Icon(Icons.search, color: AppColors.textTertiaryDark, size: 20),
                              filled: true,
                              fillColor: AppColors.surfaceElevatedDark,
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(12),
                                borderSide: BorderSide.none,
                              ),
                              contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                            ),
                            style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 14),
                          ),
                        ),
                      Builder(
                        builder: (_) {
                          final filtered = _brandSearch.isEmpty
                              ? brands
                              : brands.where((b) => b.toLowerCase().contains(_brandSearch)).toList();
                          return Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: filtered.map((b) {
                              final isSelected = _filters.brandIds.contains(b);
                              return FilterChip(
                                label: Text(b),
                                selected: isSelected,
                                onSelected: (_) => _toggleBrand(b),
                                backgroundColor: AppColors.surfaceElevatedDark,
                                selectedColor: AppColors.pastelBlue.withValues(alpha: 0.3),
                                side: BorderSide(
                                  color: isSelected ? AppColors.pastelBlue : AppColors.borderDark,
                                ),
                              );
                            }).toList(),
                          );
                        },
                      ),
                    ],
                    const SizedBox(height: 20),
                    _SectionTitle('Цена'),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _minPriceController,
                            onChanged: (_) => setState(() {}),
                            decoration: InputDecoration(
                              labelText: 'От',
                              labelStyle: TextStyle(color: AppColors.textSecondaryDark),
                              hintText: '0',
                              hintStyle: TextStyle(color: AppColors.textTertiaryDark),
                              filled: true,
                              fillColor: AppColors.surfaceElevatedDark,
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(12),
                                borderSide: BorderSide.none,
                              ),
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                            ),
                            style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 16),
                            keyboardType: const TextInputType.numberWithOptions(decimal: true),
                            cursorColor: AppColors.pastelMint,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: TextField(
                            controller: _maxPriceController,
                            onChanged: (_) => setState(() {}),
                            decoration: InputDecoration(
                              labelText: 'До',
                              labelStyle: TextStyle(color: AppColors.textSecondaryDark),
                              hintText: '9999',
                              hintStyle: TextStyle(color: AppColors.textTertiaryDark),
                              filled: true,
                              fillColor: AppColors.surfaceElevatedDark,
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(12),
                                borderSide: BorderSide.none,
                              ),
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                            ),
                            style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 16),
                            keyboardType: const TextInputType.numberWithOptions(decimal: true),
                            cursorColor: AppColors.pastelMint,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
              SafeArea(
                top: false,
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: () {
                        final f = _filters.copyWith(
                          minPrice: _minPriceController.text.trim(),
                          maxPrice: _maxPriceController.text.trim(),
                          manualStrengthInput: _strengthInputController.text.trim(),
                        );
                        widget.onApply(f);
                      },
                      style: FilledButton.styleFrom(
                        backgroundColor: AppColors.pastelMint,
                        foregroundColor: AppColors.textOnPastel,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                      ),
                      child: const Text('Применить'),
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;

  const _SectionTitle(this.title);

  @override
  Widget build(BuildContext context) {
    return Text(
      title,
      style: Theme.of(context).textTheme.titleSmall?.copyWith(
            color: AppColors.textPrimaryDark,
            fontWeight: FontWeight.w600,
          ),
    );
  }
}

class _FilterPlate extends StatelessWidget {
  final String label;
  final IconData? icon;
  final bool isSelected;
  final Color accentColor;
  final VoidCallback onTap;

  const _FilterPlate({
    required this.label,
    this.icon,
    required this.isSelected,
    required this.accentColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            decoration: BoxDecoration(
              color: isSelected ? accentColor.withValues(alpha: 0.25) : AppColors.surfaceElevatedDark,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: isSelected ? accentColor : AppColors.borderDark,
                width: isSelected ? 2 : 1,
              ),
            ),
            child: Row(
              children: [
                if (icon != null) ...[
                  Icon(icon, size: 22, color: isSelected ? accentColor : AppColors.textSecondaryDark),
                  const SizedBox(width: 12),
                ],
                Expanded(
                  child: Text(
                    label,
                    style: TextStyle(
                      color: isSelected ? accentColor : AppColors.textPrimaryDark,
                      fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
                      fontSize: 15,
                    ),
                  ),
                ),
                if (isSelected)
                  Icon(Icons.check_circle, color: accentColor, size: 22),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
