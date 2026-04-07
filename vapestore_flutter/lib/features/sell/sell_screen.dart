import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dart:convert';

import '../../app.dart';
import '../../core/theme/app_colors.dart';
import '../../shared/widgets/hardware_barcode_listener.dart';
import '../../data/models/debt.dart';
import '../../data/models/payment_card.dart';
import '../../data/models/product.dart';
import '../../data/models/reservation.dart';
import '../../data/models/sale.dart';
import '../../shared/widgets/sale_success_notification.dart';
import '../../shared/widgets/screen_scaffold.dart';
import '../../utils/category_display.dart';
import '../../utils/product_display.dart';

enum _PaymentMethod { cash, card, split }

class SellScreen extends ConsumerStatefulWidget {
  const SellScreen({super.key});

  @override
  ConsumerState<SellScreen> createState() => _SellScreenState();
}

class _SellScreenState extends ConsumerState<SellScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final _searchController = TextEditingController();
  final _discountController = TextEditingController();
  final List<_CartItem> _cart = [];
  double _discount = 0;
  _PaymentMethod _paymentMethod = _PaymentMethod.cash;
  PaymentCard? _selectedCard;
  final _clientNameController = TextEditingController();
  bool _isReservation = false;
  bool _isDebt = false;
  final _splitCashController = TextEditingController();
  final _splitCardController = TextEditingController();
  final _reservationClientController = TextEditingController();
  DateTime _reservationExpiry = DateTime.now().add(const Duration(days: 7));
  SaleSuccessData? _saleSuccessData;
  late final ValueNotifier<String> _barcodeBufferNotifier;
  String? _lastProcessedBarcode;
  DateTime? _lastProcessedAt;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _barcodeBufferNotifier = ValueNotifier('');
  }

  Future<void> _processBarcodeFromScanner(String barcode) async {
    final now = DateTime.now();
    if (_lastProcessedBarcode == barcode &&
        _lastProcessedAt != null &&
        now.difference(_lastProcessedAt!).inMilliseconds < 500) {
      return;
    }
    _lastProcessedBarcode = barcode;
    _lastProcessedAt = now;
    final repo = ref.read(repositoryProvider);
    final product = await repo.getProductByBarcode(barcode, inStockOnly: true);
    if (product != null && mounted) {
      _addToCart(product);
      notifyDataChanged(ref);
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(product == null ? 'Товар не найден' : 'Товар закончился')),
      );
    }
  }

  @override
  void dispose() {
    _barcodeBufferNotifier.dispose();
    _tabController.dispose();
    _searchController.dispose();
    _discountController.dispose();
    _clientNameController.dispose();
    _reservationClientController.dispose();
    _splitCashController.dispose();
    _splitCardController.dispose();
    super.dispose();
  }

  void _addToCart(Product p) {
    setState(() {
      final i = _cart.indexWhere((c) => c.product.id == p.id);
      if (i >= 0) {
        final newQty = (_cart[i].qty + 1).clamp(0, p.stock);
        if (newQty == 0) {
          _cart.removeAt(i);
        } else {
          _cart[i] = _CartItem(product: p, qty: newQty);
        }
      } else {
        _cart.add(_CartItem(product: p, qty: 1));
      }
      _searchController.clear();
    });
  }

  void _updateQuantity(Product p, int qty) {
    setState(() {
      if (qty <= 0) {
        _cart.removeWhere((c) => c.product.id == p.id);
      } else {
        final i = _cart.indexWhere((c) => c.product.id == p.id);
        if (i >= 0) _cart[i] = _CartItem(product: p, qty: qty.clamp(0, p.stock));
      }
    });
  }

  double get _subtotal =>
      _cart.fold(0.0, (s, c) => s + (c.product.retailPrice * c.qty));
  double get _total => (_subtotal - _discount).clamp(0.0, double.infinity);
  double get _splitCashAmount => double.tryParse(_splitCashController.text.replaceAll(',', '.')) ?? 0;
  double get _splitCardAmount => double.tryParse(_splitCardController.text.replaceAll(',', '.')) ?? 0;

  /// Корзина, отсортированная так, чтобы подарочные (purchasePrice==0) шли первыми
  List<_CartItem> get _sortedCart {
    final list = List<_CartItem>.from(_cart);
    list.sort((a, b) {
      final aGift = a.product.purchasePrice == 0;
      final bGift = b.product.purchasePrice == 0;
      if (aGift != bGift) return aGift ? -1 : 1;
      final c = a.product.brand.compareTo(b.product.brand);
      if (c != 0) return c;
      final d = a.product.flavor.compareTo(b.product.flavor);
      if (d != 0) return d;
      return a.product.id.compareTo(b.product.id);
    });
    return list;
  }

  Future<void> _performSell() async {
    if (_cart.isEmpty) return;
    if (_isDebt && _clientNameController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Укажите имя клиента для продажи в долг')),
      );
      return;
    }
    if (_isReservation) {
      final customer = _reservationClientController.text.trim();
      if (customer.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Укажите имя клиента для резерва')),
        );
        return;
      }
      if (_reservationExpiry.millisecondsSinceEpoch <= DateTime.now().millisecondsSinceEpoch) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Дата и время окончания резерва должны быть в будущем')),
        );
        return;
      }
    }
    if (!_isReservation && (_paymentMethod == _PaymentMethod.card || _paymentMethod == _PaymentMethod.split)) {
      final cards = await ref.read(repositoryProvider).getActivePaymentCards();
      if (cards.isNotEmpty && _selectedCard == null) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Выберите карту')),
          );
        }
        return;
      }
    }
    if (!_isReservation && _paymentMethod == _PaymentMethod.split && !_isDebt) {
      final sum = _splitCashAmount + _splitCardAmount;
      if ((sum - _total).abs() > 0.01) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Сумма наличными + картой должна равняться ${_total.toStringAsFixed(0)}')),
          );
        }
        return;
      }
    }

    final repo = ref.read(repositoryProvider);
    final now = DateTime.now().millisecondsSinceEpoch;
    String paymentMethod = 'cash';
    double? cashAmount;
    double? cardAmount;
    int? cardId;

    switch (_paymentMethod) {
      case _PaymentMethod.cash:
        paymentMethod = 'cash';
        break;
      case _PaymentMethod.card:
        paymentMethod = 'card';
        cardId = _selectedCard?.id;
        break;
      case _PaymentMethod.split:
        paymentMethod = 'split';
        cashAmount = _splitCashAmount;
        cardAmount = _splitCardAmount;
        cardId = _selectedCard?.id;
        break;
    }

    if (_isDebt) {
      final itemsJson = jsonEncode(
        _sortedCart.map((c) => {'productId': c.product.id, 'quantity': c.qty}).toList(),
      );
      final totalAmount = _total;
      for (final item in _sortedCart) {
        for (var i = 0; i < item.qty; i++) {
          await repo.decreaseStock(item.product.id);
        }
      }
      final debt = Debt(
        customerName: _clientNameController.text.trim(),
        products: itemsJson,
        date: now,
        totalAmount: totalAmount,
        stockDeducted: true,
      );
      await repo.insertDebt(debt);
    } else if (_isReservation) {
      final customer = _reservationClientController.text.trim();
      final expiryMs = _reservationExpiry.millisecondsSinceEpoch;

      // Check available stock with fresh data from DB (including any new reservations)
      for (final item in _sortedCart) {
        final currentProduct = await repo.getProductById(item.product.id);
        if (currentProduct == null) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Товар не найден в базе данных')),
            );
          }
          return;
        }
        final reservations = await repo.getReservationsForProduct(currentProduct.id);
        final reservedQty = reservations.fold<int>(0, (s, r) => s + r.quantity);
        final available = currentProduct.stock - reservedQty;
        if (available < item.qty) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('Недостаточно товара для резерва (доступно: $available шт.)')),
            );
          }
          return;
        }
      }

      for (final item in _sortedCart) {
        final reservation = Reservation(
          customerName: customer,
          productId: item.product.id,
          quantity: item.qty,
          reservationDate: now,
          expirationDate: expiryMs,
        );
        await repo.insertReservation(reservation);
      }

      setState(() {
        _cart.clear();
        _discount = 0;
        _discountController.clear();
        _clientNameController.clear();
        _reservationClientController.clear();
        _splitCashController.clear();
        _splitCardController.clear();
        _isDebt = false;
        _isReservation = false;
        _saleSuccessData = null;
      });
      if (mounted) FocusManager.instance.primaryFocus?.unfocus();
      notifyDataChanged(ref);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Резерв создан')),
        );
      }
      return;
    } else {
      final effectiveDiscount = _discount.clamp(0.0, _subtotal);
      for (final item in _sortedCart) {
        final revenue = (_subtotal > 0)
            ? (item.product.retailPrice * item.qty) - effectiveDiscount * (item.product.retailPrice * item.qty) / _subtotal
            : (item.product.retailPrice * item.qty);
        final profit = (item.product.retailPrice - item.product.purchasePrice) * item.qty;
        final customerName = _clientNameController.text.trim();
        await repo.insertSale(Sale(
          productId: item.product.id,
          date: now,
          customerName: customerName.isEmpty ? null : customerName,
          revenue: revenue,
          profit: profit,
          quantity: item.qty,
          paymentMethod: paymentMethod,
          cashAmount: cashAmount,
          cardAmount: cardAmount,
          cardId: cardId,
        ));
        for (var i = 0; i < item.qty; i++) {
          await repo.decreaseStock(item.product.id);
        }
      }
    }

    final customerName = _clientNameController.text.trim();
    final effectiveDiscount = _discount.clamp(0.0, _subtotal);
    final successData = SaleSuccessData(
      items: _sortedCart.map((item) {
        final revenue = (_subtotal > 0)
            ? (item.product.retailPrice * item.qty) - effectiveDiscount * (item.product.retailPrice * item.qty) / _subtotal
            : (item.product.retailPrice * item.qty);
        return (product: item.product, qty: item.qty, revenue: revenue);
      }).toList(),
      total: (_subtotal - effectiveDiscount).clamp(0.0, double.infinity),
      paymentMethod: paymentMethod,
      customerName: customerName.isEmpty ? null : customerName,
      cardLabel: _selectedCard?.label,
    );

    setState(() {
      _cart.clear();
      _discount = 0;
      _discountController.clear();
      _clientNameController.clear();
      _reservationClientController.clear();
      _splitCashController.clear();
      _splitCardController.clear();
      _isDebt = false;
      _saleSuccessData = successData;
    });
    if (mounted) FocusManager.instance.primaryFocus?.unfocus();
    notifyDataChanged(ref);
    if (mounted) {
      Future.delayed(const Duration(seconds: 5), () {
        if (mounted) setState(() => _saleSuccessData = null);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final productsAsync = ref.watch(productsWithStockProvider);
    final cardsAsync = ref.watch(activePaymentCardsProvider);

    final isActive = ref.watch(currentNavIndexProvider) == 1;
    return HardwareBarcodeListener(
      bufferNotifier: _barcodeBufferNotifier,
      onBarcode: _processBarcodeFromScanner,
      enabled: isActive,
      child: Stack(
        children: [
          ScreenScaffold(
          title: 'Продажа',
          subtitle: 'Оформление чека',
          actions: IconButton(
            icon: const Icon(Icons.qr_code_scanner, color: AppColors.textSecondaryDark),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Сканер в разработке')),
              );
            },
          ),
          body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: Container(
              decoration: BoxDecoration(
                color: AppColors.surfaceDark,
                borderRadius: BorderRadius.circular(16),
              ),
              child: TabBar(
                controller: _tabController,
                indicator: BoxDecoration(
                  color: AppColors.pastelMint,
                  borderRadius: BorderRadius.circular(12),
                ),
                indicatorSize: TabBarIndicatorSize.tab,
                labelColor: AppColors.textOnPastel,
                unselectedLabelColor: AppColors.textTertiaryDark,
                labelStyle: const TextStyle(fontWeight: FontWeight.w600),
                tabs: const [
                  Tab(text: 'Поиск'),
                  Tab(text: 'Каталог'),
                ],
              ),
            ),
          ),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _SearchTab(
                  productsAsync: productsAsync,
                  searchQuery: _searchController.text,
                  onSearchChanged: (v) => setState(() {}),
                  searchController: _searchController,
                  bufferNotifier: _barcodeBufferNotifier,
                  onAddToCart: _addToCart,
                  ref: ref,
                ),
                _CatalogTab(
                  productsAsync: productsAsync,
                  onAddToCart: _addToCart,
                ),
              ],
            ),
          ),
          if (_cart.isNotEmpty)
            _CartPanel(
              cart: _sortedCart,
              discount: _discount,
              discountController: _discountController,
              onDiscountChanged: (v) => setState(() => _discount = v),
              onQuantityChanged: _updateQuantity,
              paymentMethod: _paymentMethod,
              onPaymentMethodChanged: (v) => setState(() => _paymentMethod = v),
              selectedCard: _selectedCard,
              cards: cardsAsync.whenOrNull(data: (c) => c) ?? [],
              onCardSelected: (c) => setState(() => _selectedCard = c),
              clientNameController: _clientNameController,
              isReservation: _isReservation,
              onReservationChanged: (v) => setState(() {
                _isReservation = v;
                if (v) _isDebt = false;
              }),
              isDebt: _isDebt,
              onDebtChanged: (v) => setState(() {
                _isDebt = v;
                if (v) _isReservation = false;
              }),
              splitCashController: _splitCashController,
              splitCardController: _splitCardController,
              reservationClientController: _reservationClientController,
              reservationExpiry: _reservationExpiry,
              onReservationExpiryChanged: (v) => setState(() => _reservationExpiry = v),
              total: _total,
              onSell: _performSell,
              onClearCart: () => setState(() => _cart.clear()),
            ),
        ],
      ),
        ),
        if (_saleSuccessData != null)
          Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: SaleSuccessNotification(
              data: _saleSuccessData!,
              onDismiss: () => setState(() => _saleSuccessData = null),
            ),
          ),
        ],
      ),
    );
  }
}

class _SearchTab extends ConsumerStatefulWidget {
  final AsyncValue<List<Product>> productsAsync;
  final String searchQuery;
  final ValueChanged<String> onSearchChanged;
  final TextEditingController searchController;
  final ValueNotifier<String> bufferNotifier;
  final ValueChanged<Product> onAddToCart;
  final WidgetRef ref;

  const _SearchTab({
    required this.productsAsync,
    required this.searchQuery,
    required this.onSearchChanged,
    required this.searchController,
    required this.bufferNotifier,
    required this.onAddToCart,
    required this.ref,
  });

  @override
  ConsumerState<_SearchTab> createState() => _SearchTabState();
}

class _SearchTabState extends ConsumerState<_SearchTab> {
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
          child: Container(
            decoration: BoxDecoration(
              color: AppColors.surfaceDark,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.borderDark),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
                  child: Text(
                    'Готов к сканированию (без клавиатуры)',
                    style: TextStyle(
                      color: AppColors.textSecondaryDark,
                      fontSize: 12,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                  child: ValueListenableBuilder<String>(
                    valueListenable: widget.bufferNotifier,
                    builder: (_, buffer, __) {
                      final display = buffer.isEmpty ? 'Наведите сканер' : buffer;
                      return SizedBox(
                        height: 48,
                        child: Center(
                          child: Text(
                            display,
                            style: TextStyle(
                              color: buffer.isEmpty
                                  ? AppColors.textTertiaryDark
                                  : AppColors.textPrimaryDark,
                              fontSize: 14,
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: TextField(
            controller: widget.searchController,
            onChanged: widget.onSearchChanged,
            decoration: InputDecoration(
              hintText: 'Поиск (мин. 2 символа)...',
              prefixIcon: const Icon(Icons.search, color: AppColors.textTertiaryDark),
              suffixIcon: widget.searchController.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.close, color: AppColors.textTertiaryDark),
                      onPressed: () {
                        widget.searchController.clear();
                        widget.onSearchChanged('');
                      },
                    )
                  : null,
              filled: true,
              fillColor: AppColors.surfaceDark,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(18),
                borderSide: BorderSide.none,
              ),
            ),
            style: const TextStyle(color: AppColors.textPrimaryDark),
          ),
        ),
        Expanded(
          child: widget.productsAsync.when(
            loading: () => const Center(child: CircularProgressIndicator(color: AppColors.pastelMint)),
            error: (e, _) => Center(child: Text('Ошибка: $e', style: const TextStyle(color: AppColors.textPrimaryDark))),
            data: (products) {
              final raw = widget.searchController.text.trim();
              final q = raw.toLowerCase();
              final isBarcodeSearch = raw.length >= 3 && RegExp(r'^\d+$').hasMatch(raw);
              if (!isBarcodeSearch && q.length < 2) {
                return Center(
                  child: Text(
                    'Введите минимум 2 символа для поиска',
                    style: TextStyle(color: AppColors.textTertiaryDark, fontSize: 14),
                  ),
                );
              }
              final filtered = products
                  .where((p) =>
                      p.brand.toLowerCase().contains(q) ||
                      p.flavor.toLowerCase().contains(q) ||
                      (p.specification.isNotEmpty && p.specification.toLowerCase().contains(q)) ||
                      (p.barcode != null && p.barcode!.contains(raw)))
                  .toList();
              if (filtered.isEmpty) {
                return Center(
                  child: Text(
                    'Ничего не найдено',
                    style: TextStyle(color: AppColors.textTertiaryDark, fontSize: 14),
                  ),
                );
              }
              return GridView.builder(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 12,
                  childAspectRatio: 0.85,
                ),
                itemCount: filtered.length,
                itemBuilder: (context, i) {
                  final p = filtered[i];
                  return _ProductCard(
                    product: p,
                    onTap: () => widget.onAddToCart(p),
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

class _CatalogTab extends ConsumerStatefulWidget {
  final AsyncValue<List<Product>> productsAsync;
  final ValueChanged<Product> onAddToCart;

  const _CatalogTab({
    required this.productsAsync,
    required this.onAddToCart,
  });

  @override
  ConsumerState<_CatalogTab> createState() => _CatalogTabState();
}

class _CatalogTabState extends ConsumerState<_CatalogTab> {
  final Set<String> _expandedCategories = {};
  final Set<String> _expandedBrands = {};

  @override
  Widget build(BuildContext context) {
    final customNames = ref.watch(categoryDisplayNamesProvider).when(
          data: (d) => d,
          loading: () => const <String, String>{},
          error: (_, __) => const <String, String>{},
        );

    return widget.productsAsync.when(
      loading: () => const Center(child: CircularProgressIndicator(color: AppColors.pastelMint)),
      error: (e, _) => Center(child: Text('Ошибка: $e', style: const TextStyle(color: AppColors.textPrimaryDark))),
      data: (products) {
        final byCategory = <String, Map<String, List<Product>>>{};
        for (final p in products) {
          byCategory
              .putIfAbsent(p.category, () => {})
              .putIfAbsent(p.brand, () => [])
              .add(p);
        }
        return ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
          children: byCategory.entries.map((catEntry) {
            final catName = getCategoryDisplay(catEntry.key, customNames: customNames).$1;
            final expanded = _expandedCategories.contains(catEntry.key);
            return Column(
              children: [
                InkWell(
                  onTap: () => setState(() {
                    if (expanded) {
                      _expandedCategories.remove(catEntry.key);
                    } else {
                      _expandedCategories.add(catEntry.key);
                    }
                  }),
                  borderRadius: BorderRadius.circular(18),
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceDark,
                      borderRadius: BorderRadius.circular(18),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            catName,
                            style: const TextStyle(
                              color: AppColors.textPrimaryDark,
                              fontWeight: FontWeight.w600,
                              fontSize: 16,
                            ),
                          ),
                        ),
                        Icon(
                          expanded ? Icons.expand_less : Icons.expand_more,
                          color: AppColors.textTertiaryDark,
                        ),
                      ],
                    ),
                  ),
                ),
                if (expanded) ...[
                  ...catEntry.value.entries.map((brandEntry) {
                    final brandExpanded = _expandedBrands.contains('${catEntry.key}_${brandEntry.key}');
                    return Padding(
                      padding: const EdgeInsets.only(left: 16, top: 8),
                      child: Column(
                        children: [
                          InkWell(
                            onTap: () => setState(() {
                              final k = '${catEntry.key}_${brandEntry.key}';
                              if (brandExpanded) {
                                _expandedBrands.remove(k);
                              } else {
                                _expandedBrands.add(k);
                              }
                            }),
                            borderRadius: BorderRadius.circular(12),
                            child: Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: AppColors.surfaceElevatedDark,
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: Row(
                                children: [
                                  Expanded(
                                    child: Text(
                                      brandEntry.key,
                                      style: const TextStyle(
                                        color: AppColors.textPrimaryDark,
                                        fontWeight: FontWeight.w500,
                                      ),
                                    ),
                                  ),
                                  Icon(
                                    brandExpanded ? Icons.expand_less : Icons.expand_more,
                                    color: AppColors.textTertiaryDark,
                                    size: 20,
                                  ),
                                ],
                              ),
                            ),
                          ),
                          if (brandExpanded)
                            ...brandEntry.value.map((p) => ListTile(
                                  title: Text(
                                    productDisplayName(p),
                                    style: const TextStyle(color: AppColors.textPrimaryDark, fontSize: 14),
                                  ),
                                  subtitle: Text(
                                    '${productDisplaySubtitle(p) != null && productDisplaySubtitle(p)!.isNotEmpty ? '${productDisplaySubtitle(p)} · ' : ''}${p.retailPrice} · ${p.stock} шт',
                                    style: const TextStyle(color: AppColors.textSecondaryDark, fontSize: 12),
                                  ),
                                  trailing: IconButton(
                                    icon: const Icon(Icons.add_circle, color: AppColors.pastelMint),
                                    onPressed: () => widget.onAddToCart(p),
                                  ),
                                )),
                        ],
                      ),
                    );
                  }),
                ],
                const SizedBox(height: 8),
              ],
            );
          }).toList(),
        );
      },
    );
  }
}

class _ProductCard extends StatelessWidget {
  final Product product;
  final VoidCallback onTap;

  const _ProductCard({required this.product, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.surfaceDark,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    productDisplayName(product),
                    style: const TextStyle(
                      color: AppColors.textPrimaryDark,
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                    ),
                  ),
                  if (productDisplaySubtitle(product) != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      productDisplaySubtitle(product)!,
                      style: const TextStyle(
                        color: AppColors.textTertiaryDark,
                        fontSize: 12,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '${product.retailPrice.toStringAsFixed(0)}',
                    style: const TextStyle(
                      color: AppColors.pastelMint,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  Text(
                    '${product.stock} шт',
                    style: const TextStyle(color: AppColors.textTertiaryDark, fontSize: 12),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CartPanel extends StatelessWidget {
  final List<_CartItem> cart;
  final double discount;
  final TextEditingController discountController;
  final ValueChanged<double> onDiscountChanged;
  final void Function(Product p, int qty) onQuantityChanged;
  final _PaymentMethod paymentMethod;
  final ValueChanged<_PaymentMethod> onPaymentMethodChanged;
  final PaymentCard? selectedCard;
  final List<PaymentCard> cards;
  final ValueChanged<PaymentCard?> onCardSelected;
  final TextEditingController clientNameController;
  final bool isReservation;
  final ValueChanged<bool> onReservationChanged;
  final bool isDebt;
  final ValueChanged<bool> onDebtChanged;
  final TextEditingController splitCashController;
  final TextEditingController splitCardController;
  final TextEditingController reservationClientController;
  final DateTime reservationExpiry;
  final ValueChanged<DateTime> onReservationExpiryChanged;
  final double total;
  final Future<void> Function() onSell;
  final VoidCallback onClearCart;

  const _CartPanel({
    required this.cart,
    required this.discount,
    required this.discountController,
    required this.onDiscountChanged,
    required this.onQuantityChanged,
    required this.paymentMethod,
    required this.onPaymentMethodChanged,
    required this.selectedCard,
    required this.cards,
    required this.onCardSelected,
    required this.clientNameController,
    required this.isReservation,
    required this.onReservationChanged,
    required this.isDebt,
    required this.onDebtChanged,
    required this.splitCashController,
    required this.splitCardController,
    required this.reservationClientController,
    required this.reservationExpiry,
    required this.onReservationExpiryChanged,
    required this.total,
    required this.onSell,
    required this.onClearCart,
  });

  @override
  Widget build(BuildContext context) {
    final maxHeight = MediaQuery.of(context).size.height * 0.6;

    Future<void> pickReservationExpiry() async {
      final initial = reservationExpiry;
      final date = await showDatePicker(
        context: context,
        initialDate: initial,
        firstDate: DateTime.now(),
        lastDate: DateTime.now().add(const Duration(days: 3650)),
      );
      if (date == null) return;
      final time = await showTimePicker(
        context: context,
        initialTime: TimeOfDay.fromDateTime(initial),
      );
      if (time == null) return;
      onReservationExpiryChanged(DateTime(date.year, date.month, date.day, time.hour, time.minute));
    }

    final expiryText = '${reservationExpiry.day.toString().padLeft(2, '0')}.'
        '${reservationExpiry.month.toString().padLeft(2, '0')}.'
        '${reservationExpiry.year} '
        '${reservationExpiry.hour.toString().padLeft(2, '0')}:'
        '${reservationExpiry.minute.toString().padLeft(2, '0')}';

    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeOutCubic,
      child: Container(
        margin: const EdgeInsets.all(16),
        constraints: BoxConstraints(maxHeight: maxHeight),
        decoration: BoxDecoration(
          color: AppColors.surfaceDark,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: AppColors.borderDark),
        ),
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Оформление',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: AppColors.textPrimaryDark,
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                  IconButton(
                    onPressed: onClearCart,
                    icon: const Icon(Icons.close, color: AppColors.textTertiaryDark),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              ...cart.map((item) => _CartItemRow(
                    item: item,
                    onQuantityChanged: onQuantityChanged,
                  )),
              const SizedBox(height: 16),
              TextField(
                controller: discountController,
                onChanged: (v) {
                  final parsed = double.tryParse(v.replaceAll(',', '.')) ?? 0;
                  final subtotal = cart.fold(0.0, (s, c) => s + (c.product.retailPrice * c.qty));
                  final clamped = parsed.clamp(0.0, subtotal);
                  if (clamped != parsed) {
                    WidgetsBinding.instance.addPostFrameCallback((_) {
                      discountController.text = clamped == clamped.toInt()
                          ? clamped.toInt().toString()
                          : clamped.toStringAsFixed(2);
                    });
                  }
                  onDiscountChanged(clamped);
                },
                decoration: InputDecoration(
                  labelText: 'Скидка',
                  labelStyle: const TextStyle(color: AppColors.textSecondaryDark),
                  filled: true,
                  fillColor: AppColors.surfaceElevatedDark,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide(color: AppColors.borderDark),
                  ),
                ),
                style: const TextStyle(color: AppColors.textPrimaryDark),
                keyboardType: TextInputType.number,
              ),
              const SizedBox(height: 16),
              Text(
                'Способ оплаты',
                style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  _PaymentChip(
                    label: 'Наличные',
                    icon: Icons.account_balance_wallet_outlined,
                    selected: paymentMethod == _PaymentMethod.cash,
                    onTap: () => onPaymentMethodChanged(_PaymentMethod.cash),
                  ),
                  const SizedBox(width: 8),
                  _PaymentChip(
                    label: 'Карта',
                    icon: Icons.credit_card,
                    selected: paymentMethod == _PaymentMethod.card,
                    onTap: () => onPaymentMethodChanged(_PaymentMethod.card),
                  ),
                  const SizedBox(width: 8),
                  _PaymentChip(
                    label: 'Сплит',
                    icon: Icons.account_balance,
                    selected: paymentMethod == _PaymentMethod.split,
                    onTap: () => onPaymentMethodChanged(_PaymentMethod.split),
                  ),
                ],
              ),
              if (paymentMethod == _PaymentMethod.card && cards.isNotEmpty) ...[
                const SizedBox(height: 12),
                DropdownButtonFormField<PaymentCard>(
                  value: selectedCard != null && cards.any((c) => c.id == selectedCard!.id)
                      ? cards.firstWhere((c) => c.id == selectedCard!.id)
                      : null,
                  hint: const Text('Выберите карту', style: TextStyle(color: AppColors.textSecondaryDark)),
                  decoration: InputDecoration(
                    labelText: 'Карта',
                    filled: true,
                    fillColor: AppColors.surfaceElevatedDark,
                  ),
                  dropdownColor: AppColors.surfaceDark,
                  items: cards
                      .map((c) => DropdownMenuItem(
                            value: c,
                            child: Text(c.label, style: const TextStyle(color: AppColors.textPrimaryDark)),
                          ))
                      .toList(),
                  onChanged: (c) => onCardSelected(c),
                ),
              ],
              if (paymentMethod == _PaymentMethod.split) ...[
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: splitCashController,
                        decoration: InputDecoration(
                          labelText: 'Наличные',
                          filled: true,
                          fillColor: AppColors.surfaceElevatedDark,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: BorderSide(color: AppColors.borderDark),
                          ),
                        ),
                        style: const TextStyle(color: AppColors.textPrimaryDark),
                        keyboardType: TextInputType.number,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextField(
                        controller: splitCardController,
                        decoration: InputDecoration(
                          labelText: 'Карта',
                          filled: true,
                          fillColor: AppColors.surfaceElevatedDark,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: BorderSide(color: AppColors.borderDark),
                          ),
                        ),
                        style: const TextStyle(color: AppColors.textPrimaryDark),
                        keyboardType: TextInputType.number,
                      ),
                    ),
                  ],
                ),
                if (cards.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  DropdownButtonFormField<PaymentCard>(
                    value: selectedCard != null && cards.any((c) => c.id == selectedCard!.id)
                        ? cards.firstWhere((c) => c.id == selectedCard!.id)
                        : null,
                    hint: const Text('Выберите карту', style: TextStyle(color: AppColors.textSecondaryDark)),
                    decoration: InputDecoration(
                      labelText: 'Карта',
                      filled: true,
                      fillColor: AppColors.surfaceElevatedDark,
                    ),
                    dropdownColor: AppColors.surfaceDark,
                    items: cards
                        .map((c) => DropdownMenuItem(
                              value: c,
                              child: Text(c.label, style: const TextStyle(color: AppColors.textPrimaryDark)),
                            ))
                        .toList(),
                    onChanged: (c) => onCardSelected(c),
                  ),
                ],
              ],
              const SizedBox(height: 12),
              if (!isReservation)
                TextField(
                  controller: clientNameController,
                  decoration: InputDecoration(
                    labelText: isDebt ? 'Имя клиента*' : 'Имя клиента (необязательно)',
                    hintText: 'Введите имя клиента',
                    filled: true,
                    fillColor: AppColors.surfaceElevatedDark,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide(color: AppColors.borderDark),
                    ),
                  ),
                  style: const TextStyle(color: AppColors.textPrimaryDark),
                ),
              const SizedBox(height: 12),
              CheckboxListTile(
                value: isReservation,
                onChanged: (v) => onReservationChanged(v ?? false),
                title: const Text(
                  'Резерв (не учитывать в выручке)',
                  style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 14),
                ),
                activeColor: AppColors.pastelMint,
              ),
              if (isReservation) ...[
                const SizedBox(height: 8),
                TextField(
                  controller: reservationClientController,
                  decoration: InputDecoration(
                    labelText: 'Имя для резерва*',
                    hintText: 'Введите имя клиента',
                    filled: true,
                    fillColor: AppColors.surfaceElevatedDark,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide(color: AppColors.borderDark),
                    ),
                  ),
                  style: const TextStyle(color: AppColors.textPrimaryDark),
                ),
                const SizedBox(height: 12),
                InkWell(
                  onTap: pickReservationExpiry,
                  borderRadius: BorderRadius.circular(12),
                  child: Container(
                    padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 12),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceElevatedDark,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppColors.borderDark),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Дата и время окончания резерва*',
                                style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                expiryText,
                                style: const TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600),
                              ),
                            ],
                          ),
                        ),
                        const Icon(Icons.calendar_month, color: AppColors.textTertiaryDark),
                      ],
                    ),
                  ),
                ),
              ],
              CheckboxListTile(
                value: isDebt,
                onChanged: (v) => onDebtChanged(v ?? false),
                title: const Text(
                  'В долг',
                  style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 14),
                ),
                activeColor: AppColors.pastelMint,
              ),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Итого:', style: TextStyle(color: AppColors.textSecondaryDark)),
                  Text(
                    '${total.toStringAsFixed(0)}',
                    style: const TextStyle(
                      color: AppColors.pastelMint,
                      fontSize: 24,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: () async {
                  try {
                    await onSell();
                  } catch (e) {
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('Ошибка: $e')),
                      );
                    }
                  }
                },
                icon: const Icon(Icons.check, size: 20),
                label: const Text('Продать'),
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.pastelMint,
                  foregroundColor: AppColors.textOnPastel,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CartItemRow extends StatelessWidget {
  final _CartItem item;
  final void Function(Product p, int qty) onQuantityChanged;

  const _CartItemRow({required this.item, required this.onQuantityChanged});

  @override
  Widget build(BuildContext context) {
    final p = item.product;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  productDisplayName(p),
                  style: const TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600),
                ),
                if (productDisplaySubtitle(p) != null)
                  Text(
                    productDisplaySubtitle(p)!,
                    style: const TextStyle(color: AppColors.textSecondaryDark, fontSize: 12),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                Text(
                  '${p.retailPrice} × ${item.qty}',
                  style: const TextStyle(color: AppColors.textSecondaryDark, fontSize: 12),
                ),
              ],
            ),
          ),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              IconButton(
                onPressed: item.qty > 1
                    ? () => onQuantityChanged(p, item.qty - 1)
                    : () => onQuantityChanged(p, 0),
                icon: const Icon(Icons.remove_circle_outline, color: AppColors.textSecondaryDark),
              ),
              Text(
                '${item.qty}',
                style: const TextStyle(
                  color: AppColors.textPrimaryDark,
                  fontWeight: FontWeight.w600,
                  fontSize: 16,
                ),
              ),
              IconButton(
                onPressed: item.qty < p.stock
                    ? () => onQuantityChanged(p, item.qty + 1)
                    : null,
                icon: const Icon(Icons.add_circle_outline, color: AppColors.pastelMint),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _PaymentChip extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  const _PaymentChip({
    required this.label,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Material(
        color: selected ? AppColors.pastelMint : AppColors.surfaceElevatedDark,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(14),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: Column(
              children: [
                Icon(icon, size: 20, color: selected ? AppColors.textOnPastel : AppColors.textTertiaryDark),
                const SizedBox(height: 4),
                Text(
                  label,
                  style: TextStyle(
                    color: selected ? AppColors.textOnPastel : AppColors.textTertiaryDark,
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _CartItem {
  final Product product;
  final int qty;
  _CartItem({required this.product, required this.qty});
}
