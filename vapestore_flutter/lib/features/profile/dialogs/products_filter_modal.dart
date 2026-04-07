import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../data/models/product.dart';
import '../../../data/parse_utils.dart';
import '../../../utils/category_display.dart';

class ProductsFilters {
  bool inStockOnly = false;
  bool noBarcodeOnly = false;
  bool reservedOnly = false;
  List<String> selectedCategories = [];
  List<String> selectedBrands = [];
  List<String> selectedStrengths = [];
  String manualStrengthInput = '';
  String minPrice = '';
  String maxPrice = '';

  int get activeCount =>
      (inStockOnly ? 1 : 0) +
      (noBarcodeOnly ? 1 : 0) +
      (reservedOnly ? 1 : 0) +
      (selectedCategories.isNotEmpty ? 1 : 0) +
      (selectedBrands.isNotEmpty ? 1 : 0) +
      (selectedStrengths.isNotEmpty ? 1 : 0) +
      (manualStrengthInput.trim().isNotEmpty ? 1 : 0) +
      (minPrice.isNotEmpty ? 1 : 0) +
      (maxPrice.isNotEmpty ? 1 : 0);

  ProductsFilters copy() {
    return ProductsFilters()
      ..inStockOnly = inStockOnly
      ..noBarcodeOnly = noBarcodeOnly
      ..reservedOnly = reservedOnly
      ..selectedCategories = List<String>.from(selectedCategories)
      ..selectedBrands = List<String>.from(selectedBrands)
      ..selectedStrengths = List<String>.from(selectedStrengths)
      ..manualStrengthInput = manualStrengthInput
      ..minPrice = minPrice
      ..maxPrice = maxPrice;
  }
}

const _categoryIcons = {
  'liquid': Icons.water_drop_outlined,
  'disposable': Icons.battery_charging_full_outlined,
  'consumable': Icons.inventory_2_outlined,
  'vape': Icons.smoking_rooms_outlined,
  'snus': Icons.eco_outlined,
};

(String, IconData) _getCategoryDisplay(String id, Map<String, String> customNames) {
  final name = getCategoryDisplay(id, customNames: customNames).$1;
  final icon = _categoryIcons[id] ?? Icons.category_outlined;
  return (name, icon);
}

/// Модальное окно фильтров товаров: категории → крепость → быстрые фильтры → бренды → цена.
/// Вертикальные плашки для выбора (не chips в кучу). Без фильтра по цвету.
class ProductsFilterModal extends StatefulWidget {
  final ProductsFilters filters;
  final List<String> categories;
  final List<String> brands;
  final List<dynamic>? products;
  final Set<int>? productIdsWithReservations;
  final Map<String, String> categoryDisplayNames;
  final void Function(ProductsFilters) onApply;
  final VoidCallback onDismiss;

  const ProductsFilterModal({
    super.key,
    required this.filters,
    required this.categories,
    required this.brands,
    this.products,
    this.productIdsWithReservations,
    this.categoryDisplayNames = const {},
    required this.onApply,
    required this.onDismiss,
  });

  @override
  State<ProductsFilterModal> createState() => _ProductsFilterModalState();
}

class _ProductsFilterModalState extends State<ProductsFilterModal> {
  late ProductsFilters _local;
  late TextEditingController _minPriceController;
  late TextEditingController _maxPriceController;
  late TextEditingController _strengthInputController;
  String _brandSearch = '';
  bool _brandsExpanded = false;

  @override
  void initState() {
    super.initState();
    _local = widget.filters.copy();
    _minPriceController = TextEditingController(text: widget.filters.minPrice);
    _maxPriceController = TextEditingController(text: widget.filters.maxPrice);
    _strengthInputController = TextEditingController(text: widget.filters.manualStrengthInput);
  }

  @override
  void dispose() {
    _minPriceController.dispose();
    _maxPriceController.dispose();
    _strengthInputController.dispose();
    super.dispose();
  }

  void _reset() {
    setState(() {
      _local = ProductsFilters();
      _minPriceController.text = '';
      _maxPriceController.text = '';
      _strengthInputController.text = '';
      _brandSearch = '';
      _brandsExpanded = false;
    });
    widget.onApply(ProductsFilters());
    widget.onDismiss();
  }

  List<String> get _availableBrands {
    if (widget.products == null) return widget.brands;
    final prods = widget.products! as List<Product>;
    final cats = _local.selectedCategories;
    var filtered = cats.isEmpty ? prods : prods.where((p) => cats.contains(p.category)).toList();
    if (_local.selectedStrengths.isNotEmpty || _local.manualStrengthInput.trim().isNotEmpty) {
      final mgSet = <String>{};
      final manual = _local.manualStrengthInput.trim();
      if (manual.isNotEmpty) {
        final val = int.tryParse(manual);
        if (val != null && val >= 1 && val <= 99) mgSet.add(manual);
      }
      for (final s in _local.selectedStrengths) mgSet.add(s);
      // Применяем крепость ТОЛЬКО к жижам, одноразовым и снюс
      filtered = filtered.where((p) {
        final applies = p.category == 'liquid' || p.category == 'disposable' || p.category == 'snus';
        if (!applies) return true;
        final mgs = extractStrengthMgValues(p.brand)
          ..addAll(extractStrengthMgValues(p.specification))
          ..addAll(extractStrengthMgValues(p.strength));
        return mgs.any((m) => mgSet.contains(m));
      }).toList();
    }
    return filtered.map((p) => p.brand).toSet().toList()..sort();
  }

  List<String> get _availableStrengths {
    if (widget.products == null) return [];
    final prods = widget.products! as List<Product>;
    final cats = _local.selectedCategories;
    // Показываем крепость только для жижей, одноразовых и снюс
    final hasStrengthCat = cats.isEmpty || cats.any((c) => c == 'liquid' || c == 'disposable' || c == 'snus');
    if (!hasStrengthCat) return [];

    final filtered = cats.isEmpty ? prods : prods.where((p) => cats.contains(p.category)).toList();
    final mgValues = <String>{};
    for (final p in filtered) {
      for (final s in extractStrengthMgValues(p.brand)) mgValues.add(s);
      for (final s in extractStrengthMgValues(p.specification)) mgValues.add(s);
      for (final s in extractStrengthMgValues(p.strength)) mgValues.add(s);
    }
    return mgValues.toList()..sort((a, b) => (int.tryParse(a) ?? 0).compareTo(int.tryParse(b) ?? 0));
  }

  void _toggleCategory(String c) {
    setState(() {
      final list = List<String>.from(_local.selectedCategories);
      if (list.contains(c)) list.remove(c);
      else list.add(c);
      _local.selectedCategories = list;
      if (list.isEmpty) {
        _local.selectedBrands = [];
        _local.selectedStrengths = [];
      } else {
        _local.selectedBrands = _local.selectedBrands
            .where((b) => (widget.products! as List<Product>).any((p) => p.brand == b && list.contains(p.category)))
            .toList();
      }
    });
  }

  void _toggleBrand(String b) {
    setState(() {
      final list = List<String>.from(_local.selectedBrands);
      if (list.contains(b)) list.remove(b);
      else list.add(b);
      _local.selectedBrands = list;
    });
  }

  void _toggleStrength(String s) {
    setState(() {
      final list = List<String>.from(_local.selectedStrengths);
      if (list.contains(s)) list.remove(s);
      else list.add(s);
      _local.selectedStrengths = list;
    });
  }

  void _apply() {
    _local.minPrice = _minPriceController.text.trim();
    _local.maxPrice = _maxPriceController.text.trim();
    _local.manualStrengthInput = _strengthInputController.text.trim();
    widget.onApply(_local);
    widget.onDismiss();
  }

  @override
  Widget build(BuildContext context) {
    final maxHeight = MediaQuery.of(context).size.height * 0.85;
    final brands = _availableBrands;
    final filteredBrands = _brandSearch.isEmpty
        ? brands
        : brands.where((b) => b.toLowerCase().contains(_brandSearch.toLowerCase())).toList();
    final strengths = _availableStrengths;
    final hasStrength = strengths.isNotEmpty;

    return Container(
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
      ),
      constraints: BoxConstraints(maxHeight: maxHeight),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Фильтры',
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        color: AppColors.textPrimaryDark,
                        fontWeight: FontWeight.w600,
                      ),
                ),
                TextButton(
                  onPressed: _reset,
                  child: Text(
                    'Сбросить все',
                    style: TextStyle(color: AppColors.pastelMint, fontSize: 14),
                  ),
                ),
              ],
            ),
          ),
          Flexible(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
              children: [
                _SectionTitle('Категории'),
                const SizedBox(height: 8),
                ...widget.categories.map((c) {
                  final (name, icon) = _getCategoryDisplay(c, widget.categoryDisplayNames);
                  final isSelected = _local.selectedCategories.contains(c);
                  return _FilterPlate(
                    label: name,
                    icon: icon,
                    isSelected: isSelected,
                    accentColor: AppColors.pastelMint,
                    onTap: () => _toggleCategory(c),
                  );
                }),
                // Крепость — перед брендами
                if (hasStrength) ...[
                  const SizedBox(height: 20),
                  _SectionTitle('Крепость (mg)'),
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
                  ...strengths.map((s) {
                    final display = '$s mg';
                    final isSelected = _local.selectedStrengths.contains(s);
                    return _FilterPlate(
                      label: display,
                      isSelected: isSelected,
                      accentColor: AppColors.pastelLavender,
                      onTap: () => _toggleStrength(s),
                    );
                  }),
                ],
                // Быстрые фильтры ПЕРЕД брендами
                const SizedBox(height: 20),
                _SectionTitle('Быстрые фильтры'),
                const SizedBox(height: 8),
                _FilterPlate(
                  label: 'Только в наличии',
                  icon: Icons.inventory,
                  isSelected: _local.inStockOnly,
                  accentColor: AppColors.pastelMint,
                  onTap: () => setState(() => _local.inStockOnly = !_local.inStockOnly),
                ),
                const SizedBox(height: 6),
                _FilterPlate(
                  label: 'Без штрихкода',
                  icon: Icons.qr_code_2_outlined,
                  isSelected: _local.noBarcodeOnly,
                  accentColor: AppColors.pastelLavender,
                  onTap: () => setState(() => _local.noBarcodeOnly = !_local.noBarcodeOnly),
                ),
                const SizedBox(height: 6),
                _FilterPlate(
                  label: 'Только резервы',
                  icon: Icons.bookmark_outline,
                  isSelected: _local.reservedOnly,
                  accentColor: AppColors.pastelBlue,
                  onTap: () => setState(() => _local.reservedOnly = !_local.reservedOnly),
                ),
                const SizedBox(height: 20),
                _SectionTitle('Бренды${brands.isEmpty ? '' : ' (${brands.length})'}'),
                if (_local.selectedCategories.isEmpty && _local.selectedStrengths.isEmpty && _local.manualStrengthInput.trim().isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Text(
                      'Выберите категории или крепость',
                      style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 13),
                    ),
                  )
                else if (brands.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Text(
                      'Нет брендов по выбранным фильтрам',
                      style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 13),
                    ),
                  )
                else ...[
                  if (filteredBrands.length > 6) ...[
                    TextField(
                      onChanged: (v) => setState(() => _brandSearch = v.trim()),
                      decoration: InputDecoration(
                        hintText: 'Поиск бренда...',
                        hintStyle: TextStyle(color: AppColors.textTertiaryDark, fontSize: 14),
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
                    const SizedBox(height: 8),
                  ],
                  ...(_brandsExpanded ? filteredBrands : filteredBrands.take(5)).map((b) {
                    final isSelected = _local.selectedBrands.contains(b);
                    return _FilterPlate(
                      label: b,
                      isSelected: isSelected,
                      accentColor: AppColors.pastelBlue,
                      onTap: () => _toggleBrand(b),
                    );
                  }),
                  if (!_brandsExpanded && filteredBrands.length > 5)
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: TextButton.icon(
                        onPressed: () => setState(() => _brandsExpanded = true),
                        icon: Icon(Icons.expand_more, color: AppColors.pastelBlue, size: 20),
                        label: Text(
                          'Показать ещё (${filteredBrands.length - 5})',
                          style: TextStyle(color: AppColors.pastelBlue, fontSize: 14),
                        ),
                      ),
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
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: widget.onDismiss,
                        style: OutlinedButton.styleFrom(
                          foregroundColor: AppColors.textSecondaryDark,
                          side: BorderSide(color: AppColors.borderDark),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                        child: const Text('Отмена'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: FilledButton(
                        onPressed: _apply,
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.pastelMint,
                          foregroundColor: AppColors.textOnPastel,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                        child: const Text('Применить'),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
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
