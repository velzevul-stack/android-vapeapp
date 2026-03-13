import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/theme/app_colors.dart';
import 'data/repositories/vape_repository.dart';
import 'features/accept/accept_screen.dart';
import 'features/cabinet/cabinet_screen.dart';
import 'features/home/home_screen.dart';
import 'features/import/import_screen.dart';
import 'features/management/management_screen.dart';
import 'features/sell/sell_screen.dart';

final repositoryProvider = Provider<VapeRepository>((ref) => VapeRepository());

final productsCountProvider = FutureProvider<int>((ref) async {
  final repo = ref.watch(repositoryProvider);
  final products = await repo.getAllProducts();
  return products.length;
});

class AppShell extends ConsumerStatefulWidget {
  const AppShell({super.key});

  @override
  ConsumerState<AppShell> createState() => _AppShellState();
}

class _AppShellState extends ConsumerState<AppShell> {
  static const _navItems = [
    (route: '/accept', label: 'Приёмка', icon: Icons.inventory_2_outlined),
    (route: '/sell', label: 'Продажа', icon: Icons.shopping_cart_outlined),
    (route: '/', label: 'Главная', icon: Icons.home_outlined),
    (route: '/cabinet', label: 'Кабинет', icon: Icons.folder_outlined),
    (route: '/management', label: 'Управление', icon: Icons.settings_outlined),
  ];

  int _currentIndex = 2; // Home

  @override
  Widget build(BuildContext context) {
    return AsyncValue.when(
      data: (count) {
        if (count == 0) {
          return const ImportScreen();
        }
        return Scaffold(
          backgroundColor: AppColors.backgroundDark,
          body: IndexedStack(
            index: _currentIndex,
            children: const [
              AcceptScreen(),
              SellScreen(),
              HomeScreen(),
              CabinetScreen(),
              ManagementScreen(),
            ],
          ),
          bottomNavigationBar: Container(
            color: AppColors.surfaceDark,
            child: SafeArea(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: List.generate(
                    _navItems.length,
                    (i) => _NavItem(
                      icon: _navItems[i].icon,
                      label: _navItems[i].label,
                      isSelected: _currentIndex == i,
                      onTap: () => setState(() => _currentIndex = i),
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      },
      loading: () => const Scaffold(
        backgroundColor: AppColors.backgroundDark,
        body: Center(
          child: CircularProgressIndicator(color: AppColors.pastelMint),
        ),
      ),
      error: (e, _) => Scaffold(
        body: Center(child: Text('Ошибка: $e')),
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool isSelected;
  final VoidCallback onTap;

  const _NavItem({
    required this.icon,
    required this.label,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            icon,
            size: 24,
            color: isSelected ? AppColors.pastelMint : AppColors.textTertiaryDark,
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 12,
              color: isSelected ? AppColors.textPrimaryDark : AppColors.textTertiaryDark,
              fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}
