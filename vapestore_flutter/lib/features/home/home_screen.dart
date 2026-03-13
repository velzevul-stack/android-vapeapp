import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app.dart';
import '../../core/theme/app_colors.dart';
import '../../data/repositories/vape_repository.dart';

class HomeScreen extends ConsumerStatefulWidget {
  final Function(int)? onNavigateToIndex;
  
  const HomeScreen({super.key, this.onNavigateToIndex});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  Future<Map<String, dynamic>>? _statsFuture;

  @override
  void initState() {
    super.initState();
    _statsFuture = _loadStats(ref);
  }

  void _refresh() {
    setState(() {
      _statsFuture = _loadStats(ref);
    });
  }

  Future<Map<String, dynamic>> _loadStats(WidgetRef ref) async {
    final repo = ref.read(repositoryProvider);
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
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Map<String, dynamic>>(
      future: _statsFuture,
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Center(
            child: CircularProgressIndicator(color: AppColors.pastelMint),
          );
        }
        final stats = snapshot.data!;
        final weekData = stats['weekData'] as List<({String dayName, double profit})>;
        
        return CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Добро пожаловать',
                              style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                                    color: AppColors.textPrimaryDark,
                                    fontWeight: FontWeight.w600,
                                  ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'VapeStore Dashboard',
                              style: TextStyle(
                                color: AppColors.textSecondaryDark,
                                fontSize: 14,
                              ),
                            ),
                          ],
                        ),
                        IconButton(
                          onPressed: _refresh,
                          icon: const Icon(
                            Icons.refresh,
                            color: AppColors.textSecondaryDark,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 20),
                    Row(
                      children: [
                        Expanded(
                          child: _KpiCard(
                            title: 'Продано сегодня',
                            value: '${stats['quantity'] ?? 0}',
                            color: KpiCardColor.mint,
                            icon: Icons.shopping_bag,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: _KpiCard(
                            title: 'Чеков сегодня',
                            value: '${stats['salesCount'] ?? 0}',
                            color: KpiCardColor.blue,
                            icon: Icons.receipt,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: _KpiCard(
                            title: 'Выручка',
                            value: _formatMoney(stats['revenue'] as double? ?? 0),
                            color: KpiCardColor.lavender,
                            icon: Icons.trending_up,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: _KpiCard(
                            title: 'Прибыль',
                            value: _formatMoney(stats['profit'] as double? ?? 0),
                            color: KpiCardColor.pink,
                            icon: Icons.wallet,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),
                    _ChartCard(weekData: weekData),
                    const SizedBox(height: 24),
                    _QuickActionsCard(
                      onNavigate: (route) {
                        final index = _getRouteIndex(route);
                        if (index != null && widget.onNavigateToIndex != null) {
                          widget.onNavigateToIndex!(index);
                        }
                      },
                    ),
                  ],
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  int? _getRouteIndex(String route) {
    const routes = ['/accept', '/sell', '/', '/cabinet', '/management'];
    final index = routes.indexOf(route);
    return index >= 0 ? index : null;
  }

  static String _formatMoney(double v) {
    if (v >= 1000000) {
      return '${(v / 1000000).toStringAsFixed(1)}M';
    } else if (v >= 1000) {
      return '${(v / 1000).toStringAsFixed(0)}k';
    }
    return NumberFormat('#,##0', 'ru').format(v);
  }
}

class _ChartCard extends StatelessWidget {
  final List<({String dayName, double profit})> weekData;

  const _ChartCard({required this.weekData});

  @override
  Widget build(BuildContext context) {
    final maxProfit = weekData.isEmpty
        ? 1.0
        : (weekData.map((e) => e.profit).reduce((a, b) => a > b ? a : b)).clamp(1.0, double.infinity);
    final totalProfit = weekData.fold<double>(0, (sum, e) => sum + e.profit);

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Прибыль за неделю',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: AppColors.textPrimaryDark,
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 8),
          Text(
            _HomeScreenState._formatMoney(totalProfit),
            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  color: AppColors.textPrimaryDark,
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 24),
          SizedBox(
            height: 120,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: weekData.asMap().entries.map((entry) {
                final index = entry.key;
                final day = entry.value;
                final barHeight = maxProfit > 0 ? (day.profit / maxProfit * 80).clamp(4.0, 80.0) : 4.0;
                final isMax = day.profit >= maxProfit && day.profit > 0;
                final colors = [
                  AppColors.pastelMint,
                  AppColors.pastelBlue,
                  AppColors.pastelLavender,
                  AppColors.pastelPink,
                  AppColors.pastelMint,
                  AppColors.pastelTeal,
                  AppColors.pastelBlue,
                ];
                final barColor = isMax ? AppColors.pastelTeal : colors[index % colors.length];

                return Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Container(
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        width: 24,
                        height: barHeight,
                        decoration: BoxDecoration(
                          color: barColor,
                          borderRadius: const BorderRadius.only(
                            topLeft: Radius.circular(8),
                            topRight: Radius.circular(8),
                          ),
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        day.dayName,
                        style: TextStyle(
                          color: AppColors.textSecondaryDark,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Row(
                children: [
                  Container(
                    width: 8,
                    height: 8,
                    decoration: BoxDecoration(
                      color: AppColors.pastelMint,
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Обычный день',
                    style: TextStyle(
                      color: AppColors.textSecondaryDark,
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
              const SizedBox(width: 16),
              Row(
                children: [
                  Container(
                    width: 8,
                    height: 8,
                    decoration: BoxDecoration(
                      color: AppColors.pastelTeal,
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Лучший день',
                    style: TextStyle(
                      color: AppColors.textSecondaryDark,
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _QuickActionsCard extends StatelessWidget {
  final Function(String) onNavigate;

  const _QuickActionsCard({required this.onNavigate});

  @override
  Widget build(BuildContext context) {
    final actions = [
      ('Новая продажа', AppColors.pastelMint, '/sell'),
      ('Приемка товара', AppColors.pastelBlue, '/accept'),
      ('Отчеты', AppColors.pastelLavender, '/cabinet'),
      ('Инвентаризация', AppColors.pastelPink, '/cabinet'),
    ];

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Быстрые действия',
            style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  color: AppColors.textPrimaryDark,
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 16),
          ...actions.chunked(2).map((chunk) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Row(
                  children: chunk.map((action) {
                    final (label, accentColor, route) = action;
                    return Expanded(
                      child: GestureDetector(
                        onTap: () => onNavigate(route),
                        child: Container(
                          margin: EdgeInsets.only(
                            right: chunk.indexOf(action) == 0 ? 12 : 0,
                          ),
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceElevatedDark,
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Row(
                            children: [
                              Container(
                                width: 3,
                                height: 20,
                                decoration: BoxDecoration(
                                  color: accentColor,
                                  borderRadius: BorderRadius.circular(2),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Text(
                                  label,
                                  style: TextStyle(
                                    color: AppColors.textPrimaryDark,
                                    fontSize: 14,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
              )),
        ],
      ),
    );
  }
}

extension ListExtension<T> on List<T> {
  List<List<T>> chunked(int size) {
    final result = <List<T>>[];
    for (var i = 0; i < length; i += size) {
      result.add(sublist(i, (i + size > length) ? length : i + size));
    }
    return result;
  }
}

class _KpiCard extends StatelessWidget {
  final String title;
  final String value;
  final KpiCardColor color;
  final IconData icon;

  const _KpiCard({
    required this.title,
    required this.value,
    required this.color,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: color.bg,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                title,
                style: TextStyle(
                  color: color.textColor.withValues(alpha: 0.7),
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                ),
              ),
              Icon(icon, color: color.textColor.withValues(alpha: 0.4), size: 20),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            value,
            style: TextStyle(
              color: color.textColor,
              fontSize: 24,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
