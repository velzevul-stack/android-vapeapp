import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_riverpod/legacy.dart';

import 'core/theme/app_colors.dart';
import 'data/models/debt.dart';
import 'data/models/payment_card.dart';
import 'data/models/product.dart';
import 'data/models/reservation.dart';
import 'data/models/sale.dart';
import 'data/repositories/vape_repository.dart';
import 'utils/category_order_service.dart';
import 'utils/category_display_service.dart';
import 'features/accept/accept_screen.dart';
import 'features/home/home_screen.dart';
import 'features/post/post_screen.dart';
import 'features/profile/profile_screen.dart';
import 'features/sell/sell_screen.dart';

// Global route observer for tracking screen visibility
final RouteObserver<PageRoute<dynamic>> routeObserver = RouteObserver<PageRoute<dynamic>>();

final repositoryProvider = Provider<VapeRepository>((ref) => VapeRepository());

/// Текущий индекс навигации (для отключения сканера на неактивных вкладках)
final currentNavIndexProvider = StateProvider<int>((ref) => 2);

/// Инкрементируется при изменении порядка категорий
final categoryOrderVersionProvider = StateProvider<int>((ref) => 0);

final categoryOrderProvider = FutureProvider<List<String>>((ref) async {
  ref.watch(categoryOrderVersionProvider);
  return CategoryOrderService.getCategoryOrder();
});

/// Инкрементируется при изменении отображаемых названий категорий
final categoryDisplayVersionProvider = StateProvider<int>((ref) => 0);

final categoryDisplayNamesProvider = FutureProvider<Map<String, String>>((ref) async {
  ref.watch(categoryDisplayVersionProvider);
  return CategoryDisplayService.getCustomNames();
});

/// Инкрементируется при любой мутации — триггерит перезагрузку всех зависящих провайдеров
final dataVersionProvider = StateProvider<int>((ref) => 0);

final productsCountProvider = FutureProvider<int>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  final products = await repo.getAllProducts();
  return products.length;
});

final homeStatsProvider = FutureProvider<Map<String, dynamic>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  final start = today.millisecondsSinceEpoch;
  final end = now.millisecondsSinceEpoch;
  final profit = await repo.getTotalProfit(start, end);
  final revenue = await repo.getTotalRevenue(start, end);
  final count = await repo.getSalesCountInPeriod(start, end);
  final quantity = await repo.getTotalQuantityInPeriod(start, end);
  final weekData = await repo.getWeekProfitByDay();
  return {
    'profit': profit,
    'revenue': revenue,
    'salesCount': count,
    'quantity': quantity,
    'weekData': weekData,
  };
});

final todaySummaryProvider = FutureProvider<Map<String, dynamic>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  final start = today.millisecondsSinceEpoch;
  final end = now.millisecondsSinceEpoch;
  return repo.getDaySummary(start, end);
});

final daySalesByCardProvider = FutureProvider.family<List<({String label, int salesCount, double revenue})>, int>((ref, dayStartMs) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getSalesByCardForDay(dayStartMs);
});

final todaySalesByCardProvider = FutureProvider<List<({String label, int salesCount, double revenue})>>((ref) async {
  ref.watch(dataVersionProvider);
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day).millisecondsSinceEpoch;
  return ref.watch(daySalesByCardProvider(today).future);
});

final recentDaysProvider = FutureProvider<List<({int dateMs, int salesCount, double revenue, double profit})>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getSalesByDay(7, onlyDaysWithSales: true);
});

final todayHourlySalesProvider = FutureProvider<List<({int hour, int salesCount, int quantity})>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getSalesByHourToday();
});

enum ReportsPeriod { week, month, year, all, custom }

final reportsPeriodProvider = StateProvider<ReportsPeriod>((ref) => ReportsPeriod.week);

final reportsCustomDateRangeProvider = StateProvider<({DateTime? from, DateTime? to})>((ref) => (from: null, to: null));

/// Диапазон дат для отчётов: (startMs, endMs)
final reportsDateRangeProvider = Provider<({int startMs, int endMs})>((ref) {
  ref.watch(reportsPeriodProvider);
  final custom = ref.watch(reportsCustomDateRangeProvider);
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  final endMs = now.millisecondsSinceEpoch;

  final period = ref.watch(reportsPeriodProvider);
  if (period == ReportsPeriod.custom) {
    final from = custom.from ?? today;
    final to = custom.to ?? today;
    final start = DateTime(from.year, from.month, from.day).millisecondsSinceEpoch;
    final end = DateTime(to.year, to.month, to.day).add(const Duration(days: 1)).millisecondsSinceEpoch - 1;
    return (startMs: start, endMs: end);
  }

  final days = switch (period) {
    ReportsPeriod.week => 7,
    ReportsPeriod.month => 30,
    ReportsPeriod.year => 365,
    ReportsPeriod.all => 3650,
    ReportsPeriod.custom => 7,
  };
  final startMs = today.subtract(Duration(days: days - 1)).millisecondsSinceEpoch;
  return (startMs: startMs, endMs: endMs);
});

final reportsDaysProvider = FutureProvider<List<({int dateMs, int salesCount, double revenue, double profit})>>((ref) async {
  ref.watch(dataVersionProvider);
  final range = ref.watch(reportsDateRangeProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getSalesByDateRange(startMs: range.startMs, endMs: range.endMs, onlyDaysWithSales: true);
});

final reportsPaymentTypesProvider = FutureProvider<List<({String label, double revenue})>>((ref) async {
  ref.watch(dataVersionProvider);
  final range = ref.watch(reportsDateRangeProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getRevenueByPaymentTypeForPeriod(range.startMs, range.endMs);
});

final reportsByCardProvider = FutureProvider<List<({String label, double revenue})>>((ref) async {
  ref.watch(dataVersionProvider);
  final range = ref.watch(reportsDateRangeProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getRevenueByCardForPeriod(range.startMs, range.endMs);
});

final reportsProductsPopularityProvider = FutureProvider<List<({int productId, String displayName, String? displaySubtitle, int quantity, double revenue})>>((ref) async {
  ref.watch(dataVersionProvider);
  final range = ref.watch(reportsDateRangeProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getProductsByPopularityForPeriod(range.startMs, range.endMs);
});

final reportsBrandsPopularityProvider = FutureProvider<List<({String brand, int quantity, double revenue})>>((ref) async {
  ref.watch(dataVersionProvider);
  final range = ref.watch(reportsDateRangeProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getBrandsByPopularityForPeriod(range.startMs, range.endMs);
});

final salesForDayProvider = FutureProvider.family<List<Sale>, int>((ref, dayStartMs) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getSalesForDay(dayStartMs);
});

final salesForDayWithProductsProvider = FutureProvider.family<List<({Sale sale, String displayName, String displaySubtitle})>, int>((ref, dayStartMs) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getSalesForDayWithProducts(dayStartMs);
});

final dayPaymentSummaryProvider = FutureProvider.family<({double cash, double card}), int>((ref, dayMs) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getDayPaymentSummary(dayMs);
});

final todayRecentSalesProvider = FutureProvider<List<({Sale sale, String displayName, String displaySubtitle})>>((ref) async {
  ref.watch(dataVersionProvider);
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  final dayStartMs = today.millisecondsSinceEpoch;
  final repo = ref.watch(repositoryProvider);
  final all = await repo.getSalesForDayWithProducts(dayStartMs);
  return all.take(5).toList();
});

final productsWithStockProvider = FutureProvider<List<Product>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  final allProducts = await repo.getAllProducts();
  final reservedMap = await repo.getReservedQuantityByProduct();

  // В UI отдаем доступный остаток (stock - reserved), а не сырой складской stock.
  return allProducts
      .map((product) {
        final reserved = reservedMap[product.id] ?? 0;
        final available = product.stock - reserved;
        return product.copyWith(stock: available);
      })
      .where((product) => product.stock > 0)
      .toList();
});

/// Map: productId -> reserved quantity (active reservations only)
final reservedQuantitiesProvider = FutureProvider<Map<int, int>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getReservedQuantityByProduct();
});

final allProductsProvider = FutureProvider<List<Product>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getAllProducts();
});

final allSalesProvider = FutureProvider<List<Sale>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getAllSales();
});

final activeDebtsProvider = FutureProvider<List<Debt>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getAllActiveDebts();
});

final activeReservationsProvider = FutureProvider<List<Reservation>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getAllActiveReservations();
});

final activePaymentCardsProvider = FutureProvider<List<PaymentCard>>((ref) async {
  ref.watch(dataVersionProvider);
  final repo = ref.watch(repositoryProvider);
  return repo.getActivePaymentCards();
});

/// Вызвать после любой мутации (продажа, приёмка, добавление товара)
void notifyDataChanged(WidgetRef ref) {
  ref.read(dataVersionProvider.notifier).state++;
}

/// Вызвать после изменения порядка категорий
void notifyCategoryOrderChanged(WidgetRef ref) {
  ref.read(categoryOrderVersionProvider.notifier).state++;
}

/// Вызвать после изменения отображаемых названий категорий
void notifyCategoryDisplayChanged(WidgetRef ref) {
  ref.read(categoryDisplayVersionProvider.notifier).state++;
}

class AppShell extends ConsumerStatefulWidget {
  const AppShell({super.key});

  @override
  ConsumerState<AppShell> createState() => _AppShellState();
}

class _AppShellState extends ConsumerState<AppShell> {
  Timer? _reservationsExpiryTimer;

  static const _navItems = [
    (route: '/accept', label: 'Приёмка', icon: Icons.inventory_2_outlined),
    (route: '/sell', label: 'Продажа', icon: Icons.shopping_cart_outlined),
    (route: '/', label: 'Главная', icon: Icons.home_outlined),
    (route: '/post', label: 'Пост', icon: Icons.message_outlined),
    (route: '/profile', label: 'Профиль', icon: Icons.person_outline),
  ];

  int get _currentIndex => ref.watch(currentNavIndexProvider);

  @override
  void initState() {
    super.initState();
    // Автоснятие просроченных резервов: при старте и далее по ближайшему expirationDate.
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await _runExpiredReservationsCleanup();
      await _scheduleNextReservationExpiryCheck();
    });
  }

  Future<void> _runExpiredReservationsCleanup() async {
    try {
      final repo = ref.read(repositoryProvider);
      final expiredCount = await repo.returnExpiredReservations();
      if (expiredCount > 0) {
        notifyDataChanged(ref);
      }
    } catch (_) {
      // Автоснятие не должно ломать приложение.
    }
  }

  Future<void> _scheduleNextReservationExpiryCheck() async {
    try {
      _reservationsExpiryTimer?.cancel();
      _reservationsExpiryTimer = null;

      final repo = ref.read(repositoryProvider);
      final active = await repo.getAllActiveReservations();
      if (active.isEmpty) return;

      final now = DateTime.now().millisecondsSinceEpoch;
      final nextExpiration = active
          .map((r) => r.expirationDate)
          .reduce((a, b) => a < b ? a : b);

      // Если уже просрочено — чистим сразу и перепланируем.
      if (nextExpiration <= now) {
        await _runExpiredReservationsCleanup();
        await _scheduleNextReservationExpiryCheck();
        return;
      }

      // Ставим таймер прямо на момент истечения (+ небольшой буфер).
      final delayMs = (nextExpiration - now) + 250;
      _reservationsExpiryTimer = Timer(Duration(milliseconds: delayMs), () async {
        if (!mounted) return;
        await _runExpiredReservationsCleanup();
        if (!mounted) return;
        await _scheduleNextReservationExpiryCheck();
      });
    } catch (_) {
      // На случай ошибки оставляем приложение рабочим.
    }
  }

  @override
  void dispose() {
    _reservationsExpiryTimer?.cancel();
    _reservationsExpiryTimer = null;
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<int>(currentNavIndexProvider, (prev, next) {
      if (next == 2 && prev != 2) {
        notifyDataChanged(ref);
      }
    });

    return Scaffold(
          backgroundColor: AppColors.backgroundDark,
          body: IndexedStack(
            index: _currentIndex,
            children: [
              const AcceptScreen(),
              const SellScreen(),
              HomeScreen(
                onNavigateToIndex: (index) {
                  ref.read(currentNavIndexProvider.notifier).state = index;
                },
              ),
              const PostScreen(),
              const ProfileScreen(),
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
                      onTap: () => ref.read(currentNavIndexProvider.notifier).state = i,
                    ),
                  ),
                ),
              ),
            ),
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
