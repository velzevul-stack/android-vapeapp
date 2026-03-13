import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/product.dart';
import '../../data/models/sale.dart';
import '../../data/repositories/vape_repository.dart';

class CabinetScreen extends ConsumerStatefulWidget {
  const CabinetScreen({super.key});

  @override
  ConsumerState<CabinetScreen> createState() => _CabinetScreenState();
}

class _CabinetScreenState extends ConsumerState<CabinetScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      appBar: AppBar(
        title: const Text('Кабинет', style: TextStyle(color: AppColors.textPrimaryDark)),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: 'Склад'),
            Tab(text: 'Продажи'),
            Tab(text: 'Товары'),
            Tab(text: 'Управление'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _StockTab(),
          _SalesTab(),
          _ProductsTab(),
          const Center(child: Text('Управление продажами', style: TextStyle(color: AppColors.textSecondaryDark))),
        ],
      ),
    );
  }
}

class _StockTab extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return FutureBuilder<List<Product>>(
      future: ref.read(repositoryProvider).getAllProductsWithStock(),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator(color: AppColors.pastelMint));
        }
        final products = snapshot.data!;
        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: products.length,
          itemBuilder: (_, i) {
            final p = products[i];
            return ListTile(
              title: Text('${p.brand} ${p.flavor}', style: const TextStyle(color: AppColors.textPrimaryDark)),
              subtitle: Text('${p.stock} шт', style: TextStyle(color: AppColors.textSecondaryDark)),
            );
          },
        );
      },
    );
  }
}

class _SalesTab extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return FutureBuilder<List<Sale>>(
      future: ref.read(repositoryProvider).getAllSales(),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator(color: AppColors.pastelMint));
        }
        final sales = snapshot.data!;
        if (sales.isEmpty) {
          return const Center(child: Text('Нет продаж', style: TextStyle(color: AppColors.textSecondaryDark)));
        }
        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: sales.length,
          itemBuilder: (_, i) {
            final s = sales[i];
            final dateStr = DateTime.fromMillisecondsSinceEpoch(s.date).toString().substring(0, 16);
            return ListTile(
              title: Text('${s.revenue.toStringAsFixed(0)} ₽', style: const TextStyle(color: AppColors.textPrimaryDark)),
              subtitle: Text('${s.quantity} шт · $dateStr', style: const TextStyle(color: AppColors.textSecondaryDark)),
            );
          },
        );
      },
    );
  }
}

class _ProductsTab extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return FutureBuilder<List<Product>>(
      future: ref.read(repositoryProvider).getAllProducts(),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator(color: AppColors.pastelMint));
        }
        final products = snapshot.data!;
        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: products.length,
          itemBuilder: (_, i) {
            final p = products[i];
            return ListTile(
              title: Text('${p.brand} ${p.flavor}', style: const TextStyle(color: AppColors.textPrimaryDark)),
              subtitle: Text('${p.retailPrice} ₽ · ${p.stock} шт', style: TextStyle(color: AppColors.textSecondaryDark)),
            );
          },
        );
      },
    );
  }
}
