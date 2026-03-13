import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/theme/app_colors.dart';
import '../../data/repositories/vape_repository.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return FutureBuilder<Map<String, dynamic>>(
      future: _loadStats(ref),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Center(
            child: CircularProgressIndicator(color: AppColors.pastelMint),
          );
        }
        final stats = snapshot.data!;
        return CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Главная',
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                            color: AppColors.textPrimaryDark,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                    const SizedBox(height: 20),
                    Row(
                      children: [
                        Expanded(
                          child: _KpiCard(
                            title: 'Продажи',
                            value: '${stats['salesCount'] ?? 0}',
                            color: KpiCardColor.mint,
                            icon: Icons.receipt,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: _KpiCard(
                            title: 'Выручка',
                            value: _formatMoney(stats['revenue'] as double? ?? 0),
                            color: KpiCardColor.blue,
                            icon: Icons.trending_up,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: _KpiCard(
                            title: 'Прибыль',
                            value: _formatMoney(stats['profit'] as double? ?? 0),
                            color: KpiCardColor.lavender,
                            icon: Icons.wallet,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: _KpiCard(
                            title: 'Чеки',
                            value: '${stats['salesCount'] ?? 0}',
                            color: KpiCardColor.pink,
                            icon: Icons.shopping_bag,
                          ),
                        ),
                      ],
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

  static Future<Map<String, dynamic>> _loadStats(WidgetRef ref) async {
    final repo = ref.read(repositoryProvider);
    final now = DateTime.now();
    final start = DateTime(now.year, now.month, now.day).millisecondsSinceEpoch;
    final end = start + 86400000;
    final profit = await repo.getTotalProfit(start, end);
    final revenue = await repo.getTotalRevenue(start, end);
    final count = await repo.getSalesCountInPeriod(start, end);
    return {
      'profit': profit,
      'revenue': revenue,
      'salesCount': count,
    };
  }

  static String _formatMoney(double v) {
    return NumberFormat('#,##0', 'ru').format(v);
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
