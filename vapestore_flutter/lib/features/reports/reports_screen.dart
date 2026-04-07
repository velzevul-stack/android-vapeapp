import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart';

import '../../app.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/sale.dart';
import 'dialogs/delete_sale_dialog.dart';
import 'dialogs/edit_sale_dialog.dart';

String _formatChartValue(num v) {
  if (v >= 1000000) return '${(v / 1e6).toStringAsFixed(1)}M';
  if (v >= 1000) return '${(v / 1000).toStringAsFixed(0)}k';
  return NumberFormat('#,##0', 'ru_RU').format(v);
}

/// Экран Отчёты — аналитика и статистика продаж
class ReportsScreen extends ConsumerStatefulWidget {
  const ReportsScreen({super.key});

  @override
  ConsumerState<ReportsScreen> createState() => _ReportsScreenState();
}

class _ReportsScreenState extends ConsumerState<ReportsScreen> with RouteAware {
  bool _showAnalytics = false;
  final Set<int> _expandedDays = {};
  bool _routeSubscribed = false;

  static const _periodLabels = {
    ReportsPeriod.week: 'Неделя',
    ReportsPeriod.month: 'Месяц',
    ReportsPeriod.year: 'Год',
    ReportsPeriod.all: 'Всё',
    ReportsPeriod.custom: 'Произвольный',
  };

  @override
  void initState() {
    super.initState();
    // Forces refresh when screen becomes visible
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _refreshAllData();
    });
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_routeSubscribed) return;
    final route = ModalRoute.of(context);
    if (route is PageRoute) {
      routeObserver.subscribe(this, route);
      _routeSubscribed = true;
    }
  }

  @override
  void dispose() {
    routeObserver.unsubscribe(this);
    super.dispose();
  }

  @override
  void didPopNext() {
    // This is called when returning to this screen from another screen
    _refreshAllData();
  }

  void _refreshAllData() {
    // Сброс кэша отчётов и всех провайдеров, завязанных на dataVersion (в т.ч. family по дням)
    ref.invalidate(reportsDaysProvider);
    ref.invalidate(reportsPaymentTypesProvider);
    ref.invalidate(reportsByCardProvider);
    ref.invalidate(reportsProductsPopularityProvider);
    ref.invalidate(reportsBrandsPopularityProvider);
    ref.read(dataVersionProvider.notifier).state++;
  }

  @override
  Widget build(BuildContext context) {
    final selectedPeriod = ref.watch(reportsPeriodProvider);
    final customRange = ref.watch(reportsCustomDateRangeProvider);

    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            _buildHeader(),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildPeriodSection(selectedPeriod, customRange),
                    const SizedBox(height: 24),
                    _buildSummaryCard(ref),
                    const SizedBox(height: 24),
                    _buildAnalyticsToggle(),
                    if (_showAnalytics) _buildAnalyticsCharts(ref),
                    const SizedBox(height: 24),
                    _buildProductsPopularityButton(ref),
                    const SizedBox(height: 12),
                    _buildBrandsPopularityButton(ref),
                    const SizedBox(height: 24),
                    _buildDaysSectionTitle(),
                    const SizedBox(height: 12),
                  ],
                ),
              ),
            ),
            _buildDaysList(ref),
            const SliverToBoxAdapter(child: SizedBox(height: 40)),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 24, 20, 0),
        child: Row(
          children: [
            IconButton(
              icon: const Icon(Icons.arrow_back_ios, size: 20, color: AppColors.textPrimaryDark),
              onPressed: () => Navigator.pop(context),
            ),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Отчёты',
                    style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                          color: AppColors.textPrimaryDark,
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Аналитика и статистика продаж',
                    style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 14),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPeriodSection(ReportsPeriod selectedPeriod, ({DateTime? from, DateTime? to}) customRange) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Период',
          style: Theme.of(context).textTheme.titleSmall?.copyWith(
                color: AppColors.textPrimaryDark,
                fontWeight: FontWeight.w600,
              ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: ReportsPeriod.values.map((p) {
            final label = _periodLabels[p]!;
            final isSelected = p == selectedPeriod;
            return FilterChip(
              label: Text(label),
              selected: isSelected,
              onSelected: (_) {
                ref.read(reportsPeriodProvider.notifier).state = p;
              },
              backgroundColor: AppColors.surfaceElevatedDark,
              selectedColor: AppColors.pastelMint.withValues(alpha: 0.3),
              side: BorderSide(
                color: isSelected ? AppColors.pastelMint : AppColors.borderDark,
              ),
            );
          }).toList(),
        ),
        if (selectedPeriod == ReportsPeriod.custom) ...[
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _DateField(
                  label: 'От',
                  date: customRange.from,
                  onTap: () async {
                    final picked = await showDatePicker(
                      context: context,
                      initialDate: customRange.from ?? DateTime.now(),
                      firstDate: DateTime(2020),
                      lastDate: DateTime.now(),
                    );
                    if (picked != null) {
                      ref.read(reportsCustomDateRangeProvider.notifier).state = (
                        from: picked,
                        to: customRange.to ?? picked,
                      );
                    }
                  },
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: _DateField(
                  label: 'До',
                  date: customRange.to,
                  onTap: () async {
                    final picked = await showDatePicker(
                      context: context,
                      initialDate: customRange.to ?? customRange.from ?? DateTime.now(),
                      firstDate: customRange.from ?? DateTime(2020),
                      lastDate: DateTime.now(),
                    );
                    if (picked != null) {
                      ref.read(reportsCustomDateRangeProvider.notifier).state = (
                        from: customRange.from ?? picked,
                        to: picked,
                      );
                    }
                  },
                ),
              ),
            ],
          ),
        ],
      ],
    );
  }

  Widget _buildSummaryCard(WidgetRef ref) {
    final daysAsync = ref.watch(reportsDaysProvider);
    return daysAsync.when(
      loading: () => const SizedBox.shrink(),
      error: (_, __) => const SizedBox.shrink(),
      data: (days) {
        final totalRevenue = days.fold<double>(0, (s, d) => s + d.revenue);
        final totalProfit = days.fold<double>(0, (s, d) => s + d.profit);
        final fmt = NumberFormat('#,##0', 'ru_RU');
        return Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: AppColors.surfaceDark,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: AppColors.pastelMint.withValues(alpha: 0.3)),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Column(
                children: [
                  Text('Выручка', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                  Text(fmt.format(totalRevenue), style: const TextStyle(color: AppColors.pastelMint, fontWeight: FontWeight.bold, fontSize: 18)),
                ],
              ),
              Container(width: 1, height: 40, color: AppColors.borderDark),
              Column(
                children: [
                  Text('Прибыль', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                  Text(fmt.format(totalProfit), style: const TextStyle(color: AppColors.pastelBlue, fontWeight: FontWeight.bold, fontSize: 18)),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildAnalyticsToggle() {
    return GestureDetector(
      onTap: () => setState(() => _showAnalytics = !_showAnalytics),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        decoration: BoxDecoration(
          color: AppColors.surfaceDark,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: AppColors.pastelLavender.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(Icons.trending_up, color: AppColors.pastelLavender, size: 20),
                ),
                const SizedBox(width: 12),
                Text(
                  'Аналитика и графики',
                  style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 15),
                ),
              ],
            ),
            Icon(
              _showAnalytics ? Icons.expand_less : Icons.expand_more,
              color: AppColors.textSecondaryDark,
              size: 24,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAnalyticsCharts(WidgetRef ref) {
    final daysAsync = ref.watch(reportsDaysProvider);
    final paymentTypesAsync = ref.watch(reportsPaymentTypesProvider);
    final byCardAsync = ref.watch(reportsByCardProvider);

    return Padding(
      padding: const EdgeInsets.only(top: 16),
      child: daysAsync.when(
        loading: () => const SizedBox.shrink(),
        error: (_, __) => const SizedBox.shrink(),
        data: (days) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (days.isNotEmpty) ...[
                _RevenueProfitCharts(days: days),
                const SizedBox(height: 20),
                _SalesCountBarChart(days: days),
              ],
              const SizedBox(height: 20),
              paymentTypesAsync.when(
                loading: () => const SizedBox.shrink(),
                error: (_, __) => const SizedBox.shrink(),
                data: (sources) {
                  if (sources.isEmpty) return const SizedBox.shrink();
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _PaymentTypesPieChart(sources: sources),
                      const SizedBox(height: 20),
                    ],
                  );
                },
              ),
              byCardAsync.when(
                loading: () => const SizedBox.shrink(),
                error: (_, __) => const SizedBox.shrink(),
                data: (sources) {
                  if (sources.isEmpty) return const SizedBox.shrink();
                  return _SalesByCardPieChart(sources: sources);
                },
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildDaysSectionTitle() {
    return Text(
      'Продажи по дням',
      style: Theme.of(context).textTheme.titleSmall?.copyWith(
            color: AppColors.textPrimaryDark,
            fontWeight: FontWeight.w600,
          ),
    );
  }

  Widget _buildDaysList(WidgetRef ref) {
    final daysAsync = ref.watch(reportsDaysProvider);
    return daysAsync.when(
      loading: () => SliverToBoxAdapter(
        child: Padding(
          padding: const EdgeInsets.all(40),
          child: Center(child: CircularProgressIndicator(color: AppColors.pastelMint)),
        ),
      ),
      error: (e, _) => SliverToBoxAdapter(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Text('Ошибка: $e', style: const TextStyle(color: AppColors.textPrimaryDark)),
        ),
      ),
      data: (days) {
        final cardsAsync = ref.watch(activePaymentCardsProvider);
        final cardMap = cardsAsync.whenOrNull(data: (cards) => {for (final c in cards) c.id: c.label}) ?? {};
        return SliverList(
          delegate: SliverChildBuilderDelegate(
            (context, i) {
              final d = days[i];
              final isExpanded = _expandedDays.contains(d.dateMs);
              return _DayCard(
                dateMs: d.dateMs,
                salesCount: d.salesCount,
                revenue: d.revenue,
                profit: d.profit,
                isExpanded: isExpanded,
                onTap: () {
                  setState(() {
                    if (isExpanded) {
                      _expandedDays.remove(d.dateMs);
                    } else {
                      _expandedDays.add(d.dateMs);
                    }
                  });
                },
                formatDayLabel: _formatDayLabel,
                salesAsync: ref.watch(salesForDayWithProductsProvider(d.dateMs)),
                paymentSummaryAsync: ref.watch(dayPaymentSummaryProvider(d.dateMs)),
                byCardAsync: ref.watch(daySalesByCardProvider(d.dateMs)),
                cardLabels: cardMap,
                onEditSale: (sale, displayName, displaySubtitle) => _showEditSaleDialog(context, ref, sale, displayName, displaySubtitle),
                onDeleteSale: (sale) => _showDeleteSaleDialog(context, ref, sale),
              );
            },
            childCount: days.length,
          ),
        );
      },
    );
  }

  Widget _buildProductsPopularityButton(WidgetRef ref) {
    return GestureDetector(
      onTap: () => _showProductsPopularitySheet(context, ref),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        decoration: BoxDecoration(
          color: AppColors.surfaceDark,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: AppColors.pastelLavender.withValues(alpha: 0.3)),
        ),
        child: Row(
          children: [
            Icon(Icons.star, color: AppColors.pastelLavender, size: 24),
            const SizedBox(width: 12),
            Text(
              'Товары по популярности',
              style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 15),
            ),
            const Spacer(),
            Icon(Icons.chevron_right, color: AppColors.textTertiaryDark),
          ],
        ),
      ),
    );
  }

  Future<void> _showProductsPopularitySheet(BuildContext context, WidgetRef ref) {
    return showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.backgroundDark,
      isScrollControlled: true,
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.7,
        maxChildSize: 0.95,
        minChildSize: 0.5,
        expand: false,
        builder: (ctx, scrollController) => _ProductsPopularitySheet(controller: scrollController),
      ),
    );
  }

  Widget _buildBrandsPopularityButton(WidgetRef ref) {
    return GestureDetector(
      onTap: () => _showBrandsPopularitySheet(context, ref),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        decoration: BoxDecoration(
          color: AppColors.surfaceDark,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: AppColors.pastelLavender.withValues(alpha: 0.3)),
        ),
        child: Row(
          children: [
            Icon(Icons.business, color: AppColors.pastelLavender, size: 24),
            const SizedBox(width: 12),
            Text(
              'Бренды по популярности',
              style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 15),
            ),
            const Spacer(),
            Icon(Icons.chevron_right, color: AppColors.textTertiaryDark),
          ],
        ),
      ),
    );
  }

  Future<void> _showBrandsPopularitySheet(BuildContext context, WidgetRef ref) {
    return showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.backgroundDark,
      isScrollControlled: true,
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.7,
        maxChildSize: 0.95,
        minChildSize: 0.5,
        expand: false,
        builder: (ctx, scrollController) => _BrandsPopularitySheet(controller: scrollController),
      ),
    );
  }

  String _formatDayLabel(DateTime date) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final d = DateTime(date.year, date.month, date.day);
    final diff = today.difference(d).inDays;
    if (diff == 0) return 'Сегодня';
    if (diff == 1) return 'Вчера';
    if (diff == 2) return 'Позавчера';
    final dayOfWeek = DateFormat('E', 'ru_RU').format(date);
    final dayMonth = DateFormat('d MMM', 'ru_RU').format(date);
    return '$dayOfWeek, $dayMonth';
  }

  Future<void> _showEditSaleDialog(BuildContext context, WidgetRef ref, Sale sale, String displayName, String displaySubtitle) async {
    final products = await ref.read(allProductsProvider.future);
    final cards = await ref.read(activePaymentCardsProvider.future);
    if (!context.mounted) return;
    await showDialog(
      context: context,
      builder: (ctx) => EditSaleDialog(
        sale: sale,
        productName: displaySubtitle.isNotEmpty ? '$displayName — $displaySubtitle' : displayName,
        products: products,
        cards: cards,
        onDismiss: () => Navigator.pop(ctx),
        onSave: (_) async {},
        isReservation: sale.sourceType == 'reservation',
      ),
    );
    return;
  }

  void _showDeleteSaleDialog(BuildContext context, WidgetRef ref, Sale sale) {
    showDialog(
      context: context,
      builder: (ctx) => DeleteSaleDialog(
        sale: sale,
        onDismiss: () => Navigator.pop(ctx),
        onConfirm: () async {
          final success = await ref.read(repositoryProvider).cancelSale(sale.id);
          if (!success) throw Exception('Не удалось удалить');
          notifyDataChanged(ref);
        },
      ),
    );
  }
}

class _DateField extends StatelessWidget {
  final String label;
  final DateTime? date;
  final VoidCallback onTap;

  const _DateField({required this.label, required this.date, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final fmt = DateFormat('dd.MM.yyyy', 'ru_RU');
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
        const SizedBox(height: 6),
        GestureDetector(
          onTap: onTap,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.surfaceElevatedDark,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Row(
              children: [
                Icon(Icons.calendar_today, size: 18, color: AppColors.textSecondaryDark),
                const SizedBox(width: 10),
                Text(
                  date != null ? fmt.format(date!) : 'Выберите',
                  style: TextStyle(color: date != null ? AppColors.textPrimaryDark : AppColors.textTertiaryDark, fontSize: 14),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _DayCard extends StatelessWidget {
  final int dateMs;
  final int salesCount;
  final double revenue;
  final double profit;
  final bool isExpanded;
  final VoidCallback onTap;
  final String Function(DateTime) formatDayLabel;
  final AsyncValue<List<({Sale sale, String displayName, String displaySubtitle})>> salesAsync;
  final AsyncValue<({double cash, double card})> paymentSummaryAsync;
  final AsyncValue<List<({String label, int salesCount, double revenue})>> byCardAsync;
  final Map<int, String> cardLabels;
  final void Function(Sale sale, String displayName, String displaySubtitle) onEditSale;
  final void Function(Sale sale) onDeleteSale;

  const _DayCard({
    required this.dateMs,
    required this.salesCount,
    required this.revenue,
    required this.profit,
    required this.isExpanded,
    required this.onTap,
    required this.formatDayLabel,
    required this.salesAsync,
    required this.paymentSummaryAsync,
    required this.byCardAsync,
    required this.cardLabels,
    required this.onEditSale,
    required this.onDeleteSale,
  });

  @override
  Widget build(BuildContext context) {
    final date = DateTime.fromMillisecondsSinceEpoch(dateMs);
    final label = formatDayLabel(date);
    final fmt = NumberFormat('#,##0', 'ru_RU');

    final paymentSummary = paymentSummaryAsync.whenOrNull(
      data: (s) => s,
    );
    final byCard = byCardAsync.whenOrNull(
          data: (s) => s,
        ) ??
        const <({String label, int salesCount, double revenue})>[];

    return Container(
      margin: const EdgeInsets.fromLTRB(20, 0, 20, 12),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.borderDark),
      ),
      child: Column(
        children: [
          GestureDetector(
            onTap: onTap,
            behavior: HitTestBehavior.opaque,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          label,
                          style: const TextStyle(
                            color: AppColors.textPrimaryDark,
                            fontWeight: FontWeight.w600,
                            fontSize: 15,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '$salesCount продаж • Выручка: ${fmt.format(revenue)}',
                          style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 13),
                        ),
                        if (paymentSummary != null && (paymentSummary.cash > 0 || paymentSummary.card > 0)) ...[
                          const SizedBox(height: 6),
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: AppColors.pastelTeal.withValues(alpha: 0.25),
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(color: AppColors.pastelTeal.withValues(alpha: 0.4)),
                                ),
                                child: Text(
                                  'Наличные: ${fmt.format(paymentSummary.cash)}',
                                  style: TextStyle(color: AppColors.pastelTeal, fontWeight: FontWeight.w600, fontSize: 14),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: AppColors.pastelBlue.withValues(alpha: 0.25),
                                  borderRadius: BorderRadius.circular(8),
                                  border: Border.all(color: AppColors.pastelBlue.withValues(alpha: 0.4)),
                                ),
                                child: Text(
                                  'Карта: ${fmt.format(paymentSummary.card)}',
                                  style: TextStyle(color: AppColors.pastelBlue, fontWeight: FontWeight.w600, fontSize: 14),
                                ),
                              ),
                            ],
                          ),
                        ],
                        const SizedBox(height: 2),
                        Text(
                          'Прибыль: ${fmt.format(profit)}',
                          style: TextStyle(color: AppColors.pastelBlue, fontSize: 13),
                        ),
                      ],
                    ),
                  ),
                  Icon(
                    isExpanded ? Icons.expand_less : Icons.chevron_right,
                    color: AppColors.textTertiaryDark,
                    size: 24,
                  ),
                ],
              ),
            ),
          ),
          if (isExpanded)
            salesAsync.when(
              loading: () => Padding(
                padding: const EdgeInsets.all(16),
                child: Center(
                  child: SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(color: AppColors.pastelMint, strokeWidth: 2),
                  ),
                ),
              ),
              error: (e, _) => Padding(
                padding: const EdgeInsets.all(16),
                child: Text('Ошибка: $e', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 13)),
              ),
              data: (items) {
                if (items.isEmpty) {
                  return Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text('Нет детализации', style: TextStyle(color: AppColors.textTertiaryDark, fontSize: 13)),
                  );
                }
                return Container(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                  decoration: BoxDecoration(
                    color: AppColors.backgroundDark,
                    borderRadius: const BorderRadius.only(bottomLeft: Radius.circular(20), bottomRight: Radius.circular(20)),
                  ),
                  child: Column(
                    children: [
                      if (byCard.isNotEmpty) ...[
                        const SizedBox(height: 12),
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceDark,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: AppColors.borderDark.withValues(alpha: 0.6)),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Продажи по картам',
                                style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 13),
                              ),
                              const SizedBox(height: 8),
                              ...byCard.map((e) => Padding(
                                    padding: const EdgeInsets.symmetric(vertical: 3),
                                    child: Row(
                                      children: [
                                        Expanded(
                                          child: Text(
                                            e.label,
                                            style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12),
                                            maxLines: 1,
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        ),
                                        Text(
                                          '${e.salesCount} • ${fmt.format(e.revenue)}',
                                          style: const TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 12),
                                        ),
                                      ],
                                    ),
                                  )),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),
                      ],
                      ...items.map((item) => _SaleTile(
                            sale: item.sale,
                            displayName: item.displayName,
                            displaySubtitle: item.displaySubtitle,
                            cardLabels: cardLabels,
                            onEdit: (sale, displayName, displaySubtitle) => onEditSale(sale, displayName, displaySubtitle),
                            onDelete: (sale) => onDeleteSale(sale),
                          )),
                    ],
                  ),
                );
              },
            ),
        ],
      ),
    );
  }
}

class _SaleTile extends StatelessWidget {
  final Sale sale;
  final String displayName;
  final String displaySubtitle;
  final Map<int, String> cardLabels;
  final void Function(Sale sale, String displayName, String displaySubtitle)? onEdit;
  final void Function(Sale sale)? onDelete;

  const _SaleTile({
    required this.sale,
    required this.displayName,
    required this.displaySubtitle,
    this.cardLabels = const {},
    this.onEdit,
    this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final time = DateTime.fromMillisecondsSinceEpoch(sale.date);
    final timeStr = DateFormat('HH:mm', 'ru_RU').format(time);
    final fmt = NumberFormat('#,##0', 'ru_RU');
    final paymentLabel = _paymentLabel(sale);

    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.borderDark.withValues(alpha: 0.5)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(timeStr, style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 13, fontWeight: FontWeight.w500)),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: _paymentChipColor(paymentLabel).withValues(alpha: 0.25),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: _paymentChipColor(paymentLabel).withValues(alpha: 0.5)),
                  ),
                  child: Text(
                    paymentLabel,
                    style: TextStyle(
                      color: _paymentChipColor(paymentLabel),
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                const SizedBox(height: 4),
                if (displayName.isNotEmpty)
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(displayName, style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12, fontWeight: FontWeight.w500), maxLines: 1, overflow: TextOverflow.ellipsis),
                      if (displaySubtitle.isNotEmpty)
                        Text(displaySubtitle, style: TextStyle(color: AppColors.textTertiaryDark, fontSize: 11), maxLines: 1, overflow: TextOverflow.ellipsis),
                    ],
                  ),
                if (sale.comment != null && sale.comment!.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(sale.comment!, style: TextStyle(color: AppColors.textTertiaryDark, fontSize: 11), overflow: TextOverflow.ellipsis),
                  ),
              ],
            ),
          ),
          Text(fmt.format(sale.revenue), style: const TextStyle(color: AppColors.pastelMint, fontWeight: FontWeight.w600)),
          if (onEdit != null || onDelete != null) ...[
            const SizedBox(width: 4),
            if (onEdit != null && sale.sourceType != 'debt')
              IconButton(
                icon: Icon(Icons.edit_outlined, size: 18, color: AppColors.textSecondaryDark),
                onPressed: () => onEdit!(sale, displayName, displaySubtitle),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
              ),
            if (onDelete != null)
              IconButton(
                icon: Icon(Icons.delete_outline, size: 18, color: AppColors.pastelPink),
                onPressed: () => onDelete!(sale),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
              ),
          ],
        ],
      ),
    );
  }

  String _paymentLabel(Sale s) {
    String methodLabel() {
      if (s.cashAmount != null && s.cashAmount! > 0 && (s.cardAmount == null || s.cardAmount == 0)) return 'Наличные';
      if (s.cardAmount != null && s.cardAmount! > 0) {
        if (s.cardId != null && cardLabels.containsKey(s.cardId)) return cardLabels[s.cardId]!;
        return 'Карта';
      }
      switch (s.paymentMethod) {
        case 'cash': return 'Наличные';
        case 'card':
          if (s.cardId != null && cardLabels.containsKey(s.cardId)) return cardLabels[s.cardId]!;
          return 'Карта';
        case 'split': return 'Сплит';
        default: return 'Наличные';
      }
    }
    if (s.sourceType == 'debt') {
      if (s.paymentMethod == 'debt') return 'В долг';
      return 'В долг (${methodLabel()})';
    }
    return methodLabel();
  }

  static Color _paymentChipColor(String label) {
    if (label == 'Наличные') return AppColors.pastelTeal;
    if (label == 'Карта') return AppColors.pastelBlue;
    return AppColors.pastelMint;
  }
}

/// Scrollable bar chart — как на главной: горизонтальный скролл, узкие столбцы, без наложения
class _ScrollableBarChart extends StatelessWidget {
  final String title;
  final List<double> values;
  final List<String> labels;
  final double maxVal;
  final Color color;

  const _ScrollableBarChart({
    required this.title,
    required this.values,
    required this.labels,
    required this.maxVal,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    final maxY = maxVal.clamp(1.0, double.infinity);
    const barWidth = 52.0;
    final labelStep = values.length > 30
        ? 3
        : values.length > 14
            ? 2
            : 1;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 14)),
          const SizedBox(height: 16),
          SizedBox(
            height: 140,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                SizedBox(
                  width: 36,
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(_formatChartValue(maxY), style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 9)),
                      if (maxY > 1) Text(_formatChartValue(maxY / 2), style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 9)),
                      Text('0', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 9)),
                    ],
                  ),
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      mainAxisSize: MainAxisSize.min,
                      children: List.generate(values.length, (i) {
                        final val = values[i];
                        final barH = maxY > 0 ? (val / maxY * 70).clamp(4.0, 70.0) : 4.0;
                        final showLabel = labelStep <= 1 || (i % labelStep == 0);
                        return Tooltip(
                          message: '${labels[i]}: ${_formatChartValue(val)}',
                          child: SizedBox(
                            width: barWidth,
                            child: Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 4),
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.end,
                                children: [
                                  Container(
                                    width: 20,
                                    height: barH,
                                    decoration: BoxDecoration(
                                      color: color,
                                      borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
                                    ),
                                  ),
                                  const SizedBox(height: 6),
                                  Text(
                                    showLabel ? labels[i] : '',
                                    style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 10),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    textAlign: TextAlign.center,
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                      }),
                    ),
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

class _RevenueProfitCharts extends StatelessWidget {
  final List<({int dateMs, int salesCount, double revenue, double profit})> days;

  const _RevenueProfitCharts({required this.days});

  @override
  Widget build(BuildContext context) {
    final maxRevenue = days.isEmpty ? 1.0 : days.map((d) => d.revenue).reduce((a, b) => a > b ? a : b);
    final maxProfit = days.isEmpty ? 1.0 : days.map((d) => d.profit).reduce((a, b) => a > b ? a : b);
    final dayLabels = days.map((d) => _chartDayLabel(d.dateMs)).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _ScrollableBarChart(
          title: 'Выручка',
          values: days.map((d) => d.revenue).toList(),
          labels: dayLabels,
          maxVal: maxRevenue * 1.1,
          color: AppColors.pastelMint,
        ),
        const SizedBox(height: 16),
        _ScrollableBarChart(
          title: 'Прибыль',
          values: days.map((d) => d.profit).toList(),
          labels: dayLabels,
          maxVal: maxProfit * 1.1,
          color: AppColors.pastelBlue,
        ),
      ],
    );
  }

  static String _chartDayLabel(int dateMs) {
    final dt = DateTime.fromMillisecondsSinceEpoch(dateMs);
    return '${dt.day} ${DateFormat('MMM', 'ru_RU').format(dt)}';
  }
}

class _SalesCountBarChart extends StatelessWidget {
  final List<({int dateMs, int salesCount, double revenue, double profit})> days;

  const _SalesCountBarChart({required this.days});

  @override
  Widget build(BuildContext context) {
    final maxVal = days.isEmpty ? 1.0 : days.map((d) => d.salesCount.toDouble()).reduce((a, b) => a > b ? a : b);
    final dayLabels = days.map((d) {
      final dt = DateTime.fromMillisecondsSinceEpoch(d.dateMs);
      return '${dt.day} ${DateFormat('MMM', 'ru_RU').format(dt)}';
    }).toList();

    return _ScrollableBarChart(
      title: 'Продаж в день',
      values: days.map((d) => d.salesCount.toDouble()).toList(),
      labels: dayLabels,
      maxVal: maxVal * 1.2,
      color: AppColors.pastelLavender,
    );
  }
}

class _PaymentTypesPieChart extends StatelessWidget {
  final List<({String label, double revenue})> sources;

  const _PaymentTypesPieChart({required this.sources});

  static const _colors = [
    AppColors.pastelMint,
    AppColors.pastelBlue,
    AppColors.pastelLavender,
    AppColors.pastelPink,
    AppColors.pastelTeal,
  ];

  @override
  Widget build(BuildContext context) {
    final total = sources.fold<double>(0, (s, e) => s + e.revenue);
    if (total <= 0) return const SizedBox.shrink();

    final sections = sources.asMap().entries.map((e) {
      final i = e.key;
      final s = e.value;
      final pct = (s.revenue / total * 100).toStringAsFixed(0);
      return PieChartSectionData(
        value: s.revenue,
        title: '$pct%',
        color: _colors[i % _colors.length],
        radius: 60,
        titleStyle: const TextStyle(color: AppColors.textOnPastel, fontSize: 12, fontWeight: FontWeight.w600),
      );
    }).toList();

    final fmt = NumberFormat('#,##0', 'ru_RU');
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Типы оплаты', style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 14)),
          const SizedBox(height: 16),
          SizedBox(
            height: 200,
            child: Row(
              children: [
                Expanded(
                  flex: 2,
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      PieChart(
                        PieChartData(
                          sections: sections,
                          sectionsSpace: 2,
                          centerSpaceRadius: 40,
                        ),
                      ),
                      Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(fmt.format(total), style: TextStyle(color: AppColors.pastelMint, fontWeight: FontWeight.bold, fontSize: 14)),
                        ],
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: sources.asMap().entries.map((e) {
                      final i = e.key;
                      final s = e.value;
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 2),
                        child: Row(
                          children: [
                            Container(
                              width: 10,
                              height: 10,
                              decoration: BoxDecoration(color: _colors[i % _colors.length], borderRadius: BorderRadius.circular(2)),
                            ),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                s.label,
                                style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 11),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      );
                    }).toList(),
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

class _SalesByCardPieChart extends StatelessWidget {
  final List<({String label, double revenue})> sources;

  const _SalesByCardPieChart({required this.sources});

  static const _colors = [
    AppColors.pastelMint,
    AppColors.pastelBlue,
    AppColors.pastelLavender,
    AppColors.pastelPink,
    AppColors.pastelTeal,
  ];

  @override
  Widget build(BuildContext context) {
    final total = sources.fold<double>(0, (s, e) => s + e.revenue);
    if (total <= 0) return const SizedBox.shrink();

    final fmt = NumberFormat('#,##0', 'ru_RU');
    final sections = sources.asMap().entries.map((e) {
      final i = e.key;
      final s = e.value;
      return PieChartSectionData(
        value: s.revenue,
        title: fmt.format(s.revenue),
        color: _colors[i % _colors.length],
        radius: 60,
        titleStyle: const TextStyle(color: AppColors.textOnPastel, fontSize: 11, fontWeight: FontWeight.w600),
      );
    }).toList();

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Продажи по картам', style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 14)),
          const SizedBox(height: 16),
          SizedBox(
            height: 200,
            child: Row(
              children: [
                Expanded(
                  flex: 2,
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      PieChart(
                        PieChartData(
                          sections: sections,
                          sectionsSpace: 2,
                          centerSpaceRadius: 40,
                        ),
                      ),
                      Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(fmt.format(total), style: TextStyle(color: AppColors.pastelMint, fontWeight: FontWeight.bold, fontSize: 14)),
                        ],
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: sources.asMap().entries.map((e) {
                      final i = e.key;
                      final s = e.value;
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 4),
                        child: Row(
                          children: [
                            Container(
                              width: 10,
                              height: 10,
                              decoration: BoxDecoration(color: _colors[i % _colors.length], borderRadius: BorderRadius.circular(2)),
                            ),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                s.label,
                                style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            Text(fmt.format(s.revenue), style: TextStyle(color: AppColors.pastelMint, fontSize: 11)),
                          ],
                        ),
                      );
                    }).toList(),
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

class _ProductsPopularitySheet extends ConsumerWidget {
  final ScrollController controller;

  const _ProductsPopularitySheet({required this.controller});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final productsAsync = ref.watch(reportsProductsPopularityProvider);
    final fmt = NumberFormat('#,##0', 'ru_RU');

    return Column(
      children: [
        const SizedBox(height: 12),
        Container(
          width: 40,
          height: 4,
          decoration: BoxDecoration(color: AppColors.textTertiaryDark, borderRadius: BorderRadius.circular(2)),
        ),
        Padding(
          padding: const EdgeInsets.all(20),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Товары по популярности',
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      color: AppColors.textPrimaryDark,
                      fontWeight: FontWeight.w600,
                    ),
              ),
              IconButton(
                icon: const Icon(Icons.close, color: AppColors.textSecondaryDark),
                onPressed: () => Navigator.pop(context),
              ),
            ],
          ),
        ),
        Expanded(
          child: productsAsync.when(
            loading: () => const Center(child: CircularProgressIndicator(color: AppColors.pastelMint)),
            error: (e, _) => Center(child: Text('Ошибка: $e', style: const TextStyle(color: AppColors.textPrimaryDark))),
            data: (products) {
              final list = products.take(50).toList();
              if (list.isEmpty) {
                return Center(
                  child: Text('Нет данных за период', style: TextStyle(color: AppColors.textSecondaryDark)),
                );
              }
              return ListView.builder(
                controller: controller,
                padding: const EdgeInsets.symmetric(horizontal: 20),
                itemCount: list.length,
                itemBuilder: (context, i) {
                  final p = list[i];
                  return Container(
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceDark,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 32,
                          height: 32,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            color: AppColors.pastelMint.withValues(alpha: 0.2),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            '${i + 1}',
                            style: const TextStyle(color: AppColors.pastelMint, fontWeight: FontWeight.w600, fontSize: 14),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(p.displayName, style: const TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w500, fontSize: 14), overflow: TextOverflow.ellipsis),
                              if (p.displaySubtitle != null && p.displaySubtitle!.isNotEmpty)
                                Text(p.displaySubtitle!, style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12), overflow: TextOverflow.ellipsis),
                              Text('${p.quantity} шт', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                            ],
                          ),
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(fmt.format(p.revenue), style: const TextStyle(color: AppColors.pastelMint, fontWeight: FontWeight.w600)),
                            Text('выручка', style: TextStyle(color: AppColors.textTertiaryDark, fontSize: 11)),
                          ],
                        ),
                      ],
                    ),
                  );
                },
              );
            },
          ),
        ),
      ],
    );
  }
}

class _BrandsPopularitySheet extends ConsumerWidget {
  final ScrollController controller;

  const _BrandsPopularitySheet({required this.controller});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final brandsAsync = ref.watch(reportsBrandsPopularityProvider);
    final fmt = NumberFormat('#,##0', 'ru_RU');

    return Column(
      children: [
        const SizedBox(height: 12),
        Container(
          width: 40,
          height: 4,
          decoration: BoxDecoration(color: AppColors.textTertiaryDark, borderRadius: BorderRadius.circular(2)),
        ),
        Padding(
          padding: const EdgeInsets.all(20),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Бренды по популярности',
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      color: AppColors.textPrimaryDark,
                      fontWeight: FontWeight.w600,
                    ),
              ),
              IconButton(
                icon: const Icon(Icons.close, color: AppColors.textSecondaryDark),
                onPressed: () => Navigator.pop(context),
              ),
            ],
          ),
        ),
        Expanded(
          child: brandsAsync.when(
            loading: () => const Center(child: CircularProgressIndicator(color: AppColors.pastelMint)),
            error: (e, _) => Center(child: Text('Ошибка: $e', style: const TextStyle(color: AppColors.textPrimaryDark))),
            data: (brands) {
              final list = brands.take(50).toList();
              if (list.isEmpty) {
                return Center(
                  child: Text('Нет данных за период', style: TextStyle(color: AppColors.textSecondaryDark)),
                );
              }
              return ListView.builder(
                controller: controller,
                padding: const EdgeInsets.symmetric(horizontal: 20),
                itemCount: list.length,
                itemBuilder: (context, i) {
                  final b = list[i];
                  return Container(
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceDark,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 32,
                          height: 32,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            color: AppColors.pastelLavender.withValues(alpha: 0.2),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            '${i + 1}',
                            style: const TextStyle(color: AppColors.pastelLavender, fontWeight: FontWeight.w600, fontSize: 14),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(b.brand, style: const TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w500, fontSize: 14), overflow: TextOverflow.ellipsis),
                              Text('${b.quantity} шт', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                            ],
                          ),
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(fmt.format(b.revenue), style: const TextStyle(color: AppColors.pastelMint, fontWeight: FontWeight.w600)),
                            Text('выручка', style: TextStyle(color: AppColors.textTertiaryDark, fontSize: 11)),
                          ],
                        ),
                      ],
                    ),
                  );
                },
              );
            },
          ),
        ),
      ],
    );
  }
}
