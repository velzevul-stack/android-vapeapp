import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/theme/app_colors.dart';
import '../../../data/models/product.dart';
import '../../../data/repositories/vape_repository.dart';
import '../../../utils/category_display.dart';
import '../../../utils/category_display_service.dart';
import '../../../utils/product_display.dart';

const _categoryIds = ['liquid', 'disposable', 'consumable', 'vape', 'snus'];

Future<void> showCategorySelectionDialog(
  BuildContext context,
  VapeRepository repo,
  void Function(Product product) onProductSelected,
) async {
  final customNames = await CategoryDisplayService.getCustomNames();
  if (!context.mounted) return;
  final category = await showDialog<String>(
    context: context,
    builder: (ctx) => AlertDialog(
      backgroundColor: AppColors.surfaceDark,
      title: Text(
        'Выберите категорию',
        style: TextStyle(color: AppColors.textPrimaryDark),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: ListView.builder(
          shrinkWrap: true,
          itemCount: _categoryIds.length,
          itemBuilder: (_, i) {
            final id = _categoryIds[i];
            final (name, _) = getCategoryDisplay(id, customNames: customNames);
            return ListTile(
              title: Text(name, style: TextStyle(color: AppColors.textPrimaryDark)),
              onTap: () => Navigator.pop(ctx, id),
            );
          },
        ),
      ),
    ),
  );
  if (category == null || !context.mounted) return;

  final brands = await repo.getBrandsByCategory(category);
  if (!context.mounted) return;

  final brand = await showDialog<String>(
    context: context,
    builder: (ctx) => AlertDialog(
      backgroundColor: AppColors.surfaceDark,
      title: Text(
        'Выберите бренд',
        style: TextStyle(color: AppColors.textPrimaryDark),
      ),
      content: SizedBox(
        width: double.maxFinite,
        height: 300,
        child: brands.isEmpty
            ? Center(
                child: Text(
                  'Нет брендов в этой категории',
                  style: TextStyle(color: AppColors.textSecondaryDark),
                ),
              )
            : ListView.builder(
                itemCount: brands.length,
                itemBuilder: (_, i) => ListTile(
                  title: Text(
                    brands[i],
                    style: TextStyle(color: AppColors.textPrimaryDark),
                  ),
                  onTap: () => Navigator.pop(ctx, brands[i]),
                ),
              ),
      ),
    ),
  );
  if (brand == null || !context.mounted) return;

  final products = await repo.getProductsByBrand(brand);
  if (!context.mounted) return;

  final product = await showDialog<Product>(
    context: context,
    builder: (ctx) => AlertDialog(
      backgroundColor: AppColors.surfaceDark,
      title: Text(
        'Выберите товар',
        style: TextStyle(color: AppColors.textPrimaryDark),
      ),
      content: SizedBox(
        width: double.maxFinite,
        height: 300,
        child: products.isEmpty
            ? Center(
                child: Text(
                  'Нет товаров для этого бренда',
                  style: TextStyle(color: AppColors.textSecondaryDark),
                ),
              )
            : ListView.builder(
                itemCount: products.length,
                itemBuilder: (_, i) {
                  final p = products[i];
                  return ListTile(
                    title: Text(
                      productDisplayName(p),
                      style: TextStyle(color: AppColors.textPrimaryDark),
                    ),
                    subtitle: Text(
                      '${productDisplaySubtitle(p) != null && productDisplaySubtitle(p)!.isNotEmpty ? '${productDisplaySubtitle(p)} · ' : ''}${p.retailPrice} · ${p.stock} шт.',
                      style: TextStyle(color: AppColors.textSecondaryDark),
                    ),
                    onTap: () => Navigator.pop(ctx, p),
                  );
                },
              ),
      ),
    ),
  );
  if (product != null) {
    onProductSelected(product);
  }
}

Future<void> showGiftProductDialog(
  BuildContext context,
  VapeRepository repo,
  void Function(Product product) onProductSelected,
) async {
  final products = await repo.getAllProducts();
  if (!context.mounted) return;

  String searchQuery = '';
  int? selectedProductId;

  await showDialog(
    context: context,
    builder: (ctx) => StatefulBuilder(
      builder: (context, setDialogState) {
        final filtered = searchQuery.isEmpty
            ? products
            : products.where((p) {
                return p.brand.toLowerCase().contains(searchQuery.toLowerCase()) ||
                    p.flavor.toLowerCase().contains(searchQuery.toLowerCase());
              }).toList();

        return AlertDialog(
          backgroundColor: AppColors.surfaceDark,
          title: Row(
            children: [
              Icon(Icons.card_giftcard, color: AppColors.pastelMint),
              const SizedBox(width: 8),
              Text(
                'Подарочный товар',
                style: TextStyle(color: AppColors.textPrimaryDark),
              ),
            ],
          ),
          content: SizedBox(
            width: double.maxFinite,
            height: 400,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  decoration: const InputDecoration(
                    labelText: 'Поиск товара',
                    hintText: 'Бренд или вкус',
                  ),
                  onChanged: (v) => setDialogState(() => searchQuery = v),
                ),
                const SizedBox(height: 12),
                Expanded(
                  child: ListView.builder(
                    itemCount: filtered.length,
                    itemBuilder: (_, i) {
                      final p = filtered[i];
                      final selected = selectedProductId == p.id;
                      return ListTile(
                        title: Text(
                          productDisplayName(p),
                          style: TextStyle(
                            color: AppColors.textPrimaryDark,
                            fontWeight: selected ? FontWeight.w600 : null,
                          ),
                        ),
                        subtitle: Text(
                          '${productDisplaySubtitle(p) != null && productDisplaySubtitle(p)!.isNotEmpty ? '${productDisplaySubtitle(p)} · ' : ''}${p.retailPrice}',
                          style: TextStyle(color: AppColors.textSecondaryDark),
                        ),
                        trailing: selected
                            ? Icon(Icons.check_circle, color: AppColors.pastelMint)
                            : null,
                        onTap: () => setDialogState(() => selectedProductId = p.id),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Отмена'),
            ),
            FilledButton(
              onPressed: selectedProductId == null
                  ? null
                  : () async {
                      final p = products.firstWhere((x) => x.id == selectedProductId);
                      Navigator.pop(ctx);
                      final newProduct = p.copyWith(purchasePrice: 0, stock: 1, id: 0);
                      final newId = await repo.insertProduct(newProduct);
                      final inserted = await repo.getProductById(newId);
                      if (inserted != null) onProductSelected(inserted);
                    },
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.pastelMint,
                foregroundColor: AppColors.textOnPastel,
              ),
              child: const Text('Добавить'),
            ),
          ],
        );
      },
    ),
  );
}

Future<void> showAddProductDialog(
  BuildContext context,
  VapeRepository repo,
  String? initialBarcode,
  void Function(Product product) onProductAdded,
) async {
  await showDialog(
    context: context,
    builder: (ctx) => _AddProductDialogContent(
      repo: repo,
      initialBarcode: initialBarcode ?? '',
      onProductAdded: onProductAdded,
    ),
  );
}

class _AddProductDialogContent extends StatefulWidget {
  final VapeRepository repo;
  final String initialBarcode;
  final void Function(Product product) onProductAdded;

  const _AddProductDialogContent({
    required this.repo,
    required this.initialBarcode,
    required this.onProductAdded,
  });

  @override
  State<_AddProductDialogContent> createState() => _AddProductDialogContentState();
}

class _AddProductDialogContentState extends State<_AddProductDialogContent> {
  late String category;
  late TextEditingController _brandController;
  late TextEditingController _strengthController;
  late TextEditingController _flavorController;
  late TextEditingController _specController;
  late TextEditingController _barcodeController;
  late TextEditingController _purchaseController;
  late TextEditingController _retailController;
  Map<String, String> _customCategoryNames = const {};

  @override
  void initState() {
    super.initState();
    category = 'liquid';
    _brandController = TextEditingController();
    _strengthController = TextEditingController();
    _flavorController = TextEditingController();
    _specController = TextEditingController();
    _barcodeController = TextEditingController(text: widget.initialBarcode);
    _purchaseController = TextEditingController();
    _retailController = TextEditingController();

    CategoryDisplayService.getCustomNames().then((value) {
      if (!mounted) return;
      setState(() => _customCategoryNames = value);
    });
  }

  @override
  void dispose() {
    _brandController.dispose();
    _strengthController.dispose();
    _flavorController.dispose();
    _specController.dispose();
    _barcodeController.dispose();
    _purchaseController.dispose();
    _retailController.dispose();
    super.dispose();
  }

  void _onCategoryChanged(String id) {
    setState(() {
      category = id;
      _brandController.clear();
      _flavorController.clear();
      _specController.clear();
    });
  }

  Future<void> _showBrandPicker(BuildContext context) async {
    final brands = await widget.repo.getBrandsByCategory(category);
    if (!context.mounted) return;
    final (categoryName, _) = getCategoryDisplay(category, customNames: _customCategoryNames);
    final selected = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surfaceDark,
        title: Text(
          'Выберите бренд ($categoryName)',
          style: TextStyle(color: AppColors.textPrimaryDark),
        ),
        content: SizedBox(
          width: double.maxFinite,
          height: 300,
          child: brands.isEmpty
              ? Center(
                  child: Text(
                    'Нет брендов в этой категории',
                    style: TextStyle(color: AppColors.textSecondaryDark),
                  ),
                )
              : ListView.builder(
                  itemCount: brands.length,
                  itemBuilder: (_, i) => ListTile(
                    title: Text(
                      brands[i],
                      style: TextStyle(color: AppColors.textPrimaryDark),
                    ),
                    onTap: () => Navigator.pop(ctx, brands[i]),
                  ),
                ),
        ),
      ),
    );
    if (selected != null && mounted) {
      _brandController.text = selected;
    }
  }

  Future<void> _submit() async {
    final brand = _brandController.text.trim();
    if (brand.isEmpty) return;
    if ((category == 'liquid' || category == 'disposable' || category == 'snus') &&
        _flavorController.text.trim().isEmpty) return;
    if (category == 'vape' && _specController.text.trim().isEmpty) return;

    final purchasePrice = double.tryParse(_purchaseController.text.replaceAll(',', '.')) ?? 0;
    final retailPrice = double.tryParse(_retailController.text.replaceAll(',', '.')) ?? 0;
    final barcode = _barcodeController.text.trim();
    final strength = _strengthController.text.trim();
    final flavor = _flavorController.text.trim();
    final specification = _specController.text.trim();

    final finalBrand = category == 'liquid' && strength.isNotEmpty ? '$brand ${strength}mg' : brand;
    final finalFlavor = (category == 'liquid' || category == 'disposable' || category == 'snus')
        ? flavor
        : (category == 'vape' ? specification : brand);
    final finalSpec = category == 'vape' ? (specification.isEmpty ? 'Стандарт' : specification) : '';

    final product = Product(
      brand: finalBrand,
      flavor: finalFlavor,
      barcode: barcode.length == 13 ? barcode : null,
      purchasePrice: purchasePrice,
      retailPrice: retailPrice,
      stock: 1,
      category: category,
      strength: (category == 'liquid' || category == 'snus') ? strength : '',
      specification: finalSpec,
    );
    await widget.repo.insertProduct(product);

    if (mounted) {
      Navigator.pop(context);
      final products = await widget.repo.getAllProducts();
      final added = products.firstWhere(
        (p) => p.brand == finalBrand && p.flavor == finalFlavor,
        orElse: () => products.last,
      );
      widget.onProductAdded(added);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.surfaceDark,
      title: Text(
        'Добавление нового товара',
        style: TextStyle(color: AppColors.textPrimaryDark),
      ),
      content: SingleChildScrollView(
        child: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('1. Категория:', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
              const SizedBox(height: 4),
              Wrap(
                spacing: 8,
                runSpacing: 4,
                children: _categoryIds.map((id) {
                  final (name, _) = getCategoryDisplay(id, customNames: _customCategoryNames);
                  return FilterChip(
                    label: Text(name),
                    selected: category == id,
                    onSelected: (_) => _onCategoryChanged(id),
                  );
                }).toList(),
              ),
              const SizedBox(height: 12),
              Text('2. Бренд:', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
              const SizedBox(height: 4),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _brandController,
                      decoration: const InputDecoration(hintText: 'Название бренда'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton(
                    onPressed: () => _showBrandPicker(context),
                    icon: const Icon(Icons.search),
                    tooltip: 'Выбрать из списка',
                    style: IconButton.styleFrom(
                      backgroundColor: AppColors.surfaceElevatedDark,
                      foregroundColor: AppColors.textSecondaryDark,
                    ),
                  ),
                ],
              ),
              if (category == 'liquid') ...[
                const SizedBox(height: 12),
                Text('3. Крепость (mg):', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                const SizedBox(height: 4),
                TextField(
                  controller: _strengthController,
                  decoration: const InputDecoration(hintText: '50'),
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
              ],
              if (category == 'snus') ...[
                const SizedBox(height: 12),
                Text('3. Крепость (mg):', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                const SizedBox(height: 4),
                TextField(
                  controller: _strengthController,
                  decoration: const InputDecoration(hintText: '150'),
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
              ],
              if (category == 'liquid' || category == 'disposable' || category == 'snus') ...[
                const SizedBox(height: 12),
                Text('${category == 'liquid' ? '4' : (category == 'snus' ? '4' : '3')}. Вкус:', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                const SizedBox(height: 4),
                TextField(
                  controller: _flavorController,
                  decoration: const InputDecoration(hintText: 'Вкус'),
                ),
              ],
              if (category == 'vape') ...[
                const SizedBox(height: 12),
                Text('3. Цвет:', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                const SizedBox(height: 4),
                TextField(
                  controller: _specController,
                  decoration: const InputDecoration(hintText: 'PURPLE, PASTEL CRYSTAL'),
                ),
              ],
              const SizedBox(height: 12),
              Text('Штрих-код:', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
              const SizedBox(height: 4),
              TextField(
                controller: _barcodeController,
                decoration: const InputDecoration(hintText: '13 цифр'),
                keyboardType: TextInputType.number,
                inputFormatters: [
                  FilteringTextInputFormatter.digitsOnly,
                  LengthLimitingTextInputFormatter(13),
                ],
              ),
              const SizedBox(height: 12),
              Text('Цены:', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _purchaseController,
                      decoration: const InputDecoration(labelText: 'Закупка'),
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextField(
                      controller: _retailController,
                      decoration: const InputDecoration(labelText: 'Розница'),
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Отмена'),
        ),
        FilledButton(
          onPressed: _submit,
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.pastelMint,
            foregroundColor: AppColors.textOnPastel,
          ),
          child: const Text('Добавить'),
        ),
      ],
    );
  }
}
