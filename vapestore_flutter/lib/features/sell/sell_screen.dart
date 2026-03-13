import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/product.dart';
import '../../data/models/sale.dart';
import '../../data/repositories/vape_repository.dart';

class SellScreen extends ConsumerStatefulWidget {
  const SellScreen({super.key});

  @override
  ConsumerState<SellScreen> createState() => _SellScreenState();
}

class _SellScreenState extends ConsumerState<SellScreen> {
  List<Product> _products = [];
  List<_CartItem> _cart = [];
  String? _selectedBrand;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final repo = ref.read(repositoryProvider);
    if (_selectedBrand == null) {
      final withStock = await repo.getAllProductsWithStock();
      setState(() {
        _products = withStock;
        _loading = false;
      });
    } else {
      final all = await repo.getProductsByBrand(_selectedBrand!);
      setState(() {
        _products = all.where((p) => p.stock > 0).toList();
        _loading = false;
      });
    }
  }

  void _addToCart(Product p) {
    setState(() {
      final i = _cart.indexWhere((c) => c.product.id == p.id);
      if (i >= 0) {
        _cart[i] = _CartItem(product: p, qty: _cart[i].qty + 1);
      } else {
        _cart.add(_CartItem(product: p, qty: 1));
      }
    });
  }

  Future<void> _checkout() async {
    if (_cart.isEmpty) return;
    final repo = ref.read(repositoryProvider);
    final now = DateTime.now().millisecondsSinceEpoch;
    for (final item in _cart) {
      final revenue = item.product.retailPrice * item.qty;
      final profit = (item.product.retailPrice - item.product.purchasePrice) * item.qty;
      await repo.insertSale(Sale(
        productId: item.product.id,
        date: now,
        revenue: revenue,
        profit: profit,
        quantity: item.qty,
      ));
      for (var i = 0; i < item.qty; i++) {
        await repo.decreaseStock(item.product.id);
      }
    }
    setState(() => _cart = []);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Продажа оформлена')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      appBar: AppBar(
        title: const Text('Продажа', style: TextStyle(color: AppColors.textPrimaryDark)),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: AppColors.pastelMint))
          : Column(
              children: [
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: _products.length,
                    itemBuilder: (_, i) {
                      final p = _products[i];
                      return ListTile(
                        title: Text(
                          '${p.brand} — ${p.flavor}',
                          style: const TextStyle(color: AppColors.textPrimaryDark),
                        ),
                        subtitle: Text(
                          '${p.retailPrice} ₽ · Остаток: ${p.stock}',
                          style: TextStyle(color: AppColors.textSecondaryDark),
                        ),
                        trailing: IconButton(
                          icon: const Icon(Icons.add),
                          onPressed: () => _addToCart(p),
                        ),
                      );
                    },
                  ),
                ),
                if (_cart.isNotEmpty)
                  Container(
                    padding: const EdgeInsets.all(16),
                    color: AppColors.surfaceDark,
                    child: Column(
                      children: [
                        Text(
                          'В корзине: ${_cart.fold<int>(0, (s, c) => s + c.qty)} шт',
                          style: const TextStyle(
                            color: AppColors.textPrimaryDark,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 8),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton(
                            onPressed: _checkout,
                            style: FilledButton.styleFrom(
                              backgroundColor: AppColors.pastelMint,
                              foregroundColor: AppColors.textOnPastel,
                            ),
                            child: const Text('Оформить продажу'),
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
    );
  }
}

class _CartItem {
  final Product product;
  final int qty;
  _CartItem({required this.product, required this.qty});
}
