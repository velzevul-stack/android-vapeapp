import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../app.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/payment_card.dart';
import '../../../data/models/product.dart';
import '../../../data/models/sale.dart';
import '../../../utils/product_display.dart';

enum _PaymentMethod { cash, card, split }

/// Диалог редактирования продажи
class EditSaleDialog extends ConsumerStatefulWidget {
  final Sale sale;
  final String productName;
  final List<Product> products;
  final List<PaymentCard> cards;
  final VoidCallback onDismiss;
  final Future<void> Function(Sale updated) onSave;
  final bool isReservation;

  const EditSaleDialog({
    super.key,
    required this.sale,
    required this.productName,
    required this.products,
    required this.cards,
    required this.onDismiss,
    required this.onSave,
    this.isReservation = false,
  });

  @override
  ConsumerState<EditSaleDialog> createState() => _EditSaleDialogState();
}

class _EditSaleDialogState extends ConsumerState<EditSaleDialog> {
  late Product _selectedProduct;
  late int _quantity;
  late double _discount;
  late _PaymentMethod _paymentMethod;
  PaymentCard? _selectedCard;
  late String _comment;
  late DateTime _saleDate;
  final _discountController = TextEditingController();
  final _commentController = TextEditingController();
  final _splitCashController = TextEditingController();
  final _splitCardController = TextEditingController();
  final _searchController = TextEditingController();
  bool _loading = false;

  List<Product> get _filteredProducts {
    final q = _searchController.text.trim().toLowerCase();
    if (q.isEmpty) return widget.products.take(50).toList();
    return widget.products
        .where((p) =>
            p.brand.toLowerCase().contains(q) ||
            p.flavor.toLowerCase().contains(q) ||
            (p.specification.isNotEmpty && p.specification.toLowerCase().contains(q)))
        .take(50)
        .toList();
  }

  double get _total => _selectedProduct.retailPrice * _quantity - _discount;
  double get _maxDiscount => _selectedProduct.retailPrice * _quantity;

  @override
  void initState() {
    super.initState();
    Product p;
    if (widget.products.any((x) => x.id == widget.sale.productId)) {
      p = widget.products.firstWhere((x) => x.id == widget.sale.productId);
    } else if (widget.products.isNotEmpty) {
      p = widget.products.first;
    } else {
      p = Product(id: widget.sale.productId, brand: 'Товар', flavor: '#${widget.sale.productId}', purchasePrice: 0, retailPrice: 0);
    }
    _selectedProduct = p;
    _quantity = widget.sale.quantity;
    _discount = widget.sale.discount;
    _discountController.text = _discount.toStringAsFixed(0);
    _comment = widget.sale.comment ?? '';
    _commentController.text = _comment;
    _saleDate = DateTime.fromMillisecondsSinceEpoch(widget.sale.date);

    if (widget.sale.paymentMethod == 'card') {
      _paymentMethod = _PaymentMethod.card;
      final match = widget.cards.where((c) => c.id == widget.sale.cardId);
      _selectedCard = match.isEmpty ? (widget.cards.isNotEmpty ? widget.cards.first : null) : match.first;
    } else if (widget.sale.paymentMethod == 'split') {
      _paymentMethod = _PaymentMethod.split;
      final match = widget.cards.where((c) => c.id == widget.sale.cardId);
      _selectedCard = match.isEmpty ? (widget.cards.isNotEmpty ? widget.cards.first : null) : match.first;
      _splitCashController.text = (widget.sale.cashAmount ?? 0).toStringAsFixed(0);
      _splitCardController.text = (widget.sale.cardAmount ?? 0).toStringAsFixed(0);
    } else {
      _paymentMethod = _PaymentMethod.cash;
    }
  }

  @override
  void dispose() {
    _discountController.dispose();
    _commentController.dispose();
    _splitCashController.dispose();
    _splitCardController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final discountVal = double.tryParse(_discountController.text.replaceAll(',', '.')) ?? 0;
    if (discountVal < 0 || discountVal > _maxDiscount) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Скидка от 0 до ${_maxDiscount.toStringAsFixed(0)}')),
      );
      return;
    }

    if (_paymentMethod == _PaymentMethod.card || _paymentMethod == _PaymentMethod.split) {
      if (widget.cards.isNotEmpty && _selectedCard == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Выберите карту')),
        );
        return;
      }
    }

    double? cashAmount;
    double? cardAmount;
    if (_paymentMethod == _PaymentMethod.split) {
      cashAmount = double.tryParse(_splitCashController.text.replaceAll(',', '.')) ?? 0;
      cardAmount = double.tryParse(_splitCardController.text.replaceAll(',', '.')) ?? 0;
      if ((cashAmount + cardAmount - _total).abs() > 0.01) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Сумма наличными + картой = ${_total.toStringAsFixed(0)}')),
        );
        return;
      }
    }

    final reserved = (await ref.read(repositoryProvider).getReservationsForProduct(_selectedProduct.id))
        .fold<int>(0, (s, r) => s + r.quantity);
    if (!mounted) return;
    if (_selectedProduct.id != widget.sale.productId && _selectedProduct.stock - reserved < _quantity) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Недостаточно товара на складе')),
      );
      return;
    }

    setState(() => _loading = true);
    try {
      final success = await ref.read(repositoryProvider).correctSale(
        originalSaleId: widget.sale.id,
        newProductId: _selectedProduct.id,
        newQuantity: _quantity,
        newDiscount: discountVal,
        newPaymentMethod: _paymentMethod == _PaymentMethod.cash
            ? 'cash'
            : _paymentMethod == _PaymentMethod.card
                ? 'card'
                : 'split',
        newDate: _saleDate.millisecondsSinceEpoch,
        comment: _commentController.text.trim().isEmpty ? null : _commentController.text.trim(),
        cashAmount: cashAmount,
        cardAmount: cardAmount,
        cardId: _selectedCard?.id,
      );
      if (success && mounted) {
        widget.onDismiss();
        notifyDataChanged(ref);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Продажа исправлена')),
        );
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Ошибка при сохранении')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final fmt = NumberFormat('#,##0', 'ru_RU');
    return AlertDialog(
      backgroundColor: AppColors.surfaceDark,
      title: Text(
        widget.isReservation
            ? 'Редактировать продажу резерва #${widget.sale.id}'
            : 'Редактировать продажу #${widget.sale.id}',
        style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 16),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                margin: const EdgeInsets.only(bottom: 12),
                decoration: BoxDecoration(
                  color: AppColors.pastelMint.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.pastelMint.withValues(alpha: 0.25)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      productDisplayName(_selectedProduct),
                      style: const TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600, fontSize: 14),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    if (productDisplaySubtitle(_selectedProduct) != null) ...[
                      const SizedBox(height: 2),
                      Text(
                        productDisplaySubtitle(_selectedProduct)!,
                        style: const TextStyle(color: AppColors.textSecondaryDark, fontSize: 13),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                    const SizedBox(height: 4),
                    Text(
                      '${_selectedProduct.retailPrice.toStringAsFixed(0)} × $_quantity',
                      style: const TextStyle(color: AppColors.pastelMint, fontSize: 13, fontWeight: FontWeight.w500),
                    ),
                  ],
                ),
              ),
              if (widget.isReservation) ...[
                const SizedBox.shrink(),
              ] else ...[
                Text('Товар', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12, fontWeight: FontWeight.w500)),
                const SizedBox(height: 4),
                TextField(
                  controller: _searchController,
                  onChanged: (_) => setState(() {}),
                  decoration: InputDecoration(
                    hintText: 'Поиск по бренду, вкусу...',
                    hintStyle: TextStyle(color: AppColors.textTertiaryDark),
                    prefixIcon: Icon(Icons.search, size: 20, color: AppColors.textSecondaryDark),
                    filled: true,
                    fillColor: AppColors.surfaceElevatedDark,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                  ),
                  style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 14),
                ),
                const SizedBox(height: 8),
                ConstrainedBox(
                  constraints: const BoxConstraints(maxHeight: 120),
                  child: ListView.builder(
                    shrinkWrap: true,
                    itemCount: _filteredProducts.length,
                    itemBuilder: (context, i) {
                      final p = _filteredProducts[i];
                      final selected = _selectedProduct.id == p.id;
                      return ListTile(
                        dense: true,
                        title: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(productDisplayName(p),
                                style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 13, fontWeight: FontWeight.w500),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis),
                            if (productDisplaySubtitle(p) != null && productDisplaySubtitle(p)!.isNotEmpty)
                              Text(productDisplaySubtitle(p)!,
                                  style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis),
                          ],
                        ),
                        subtitle: Text('${fmt.format(p.retailPrice)} • ${p.stock} шт',
                            style: TextStyle(color: AppColors.textTertiaryDark, fontSize: 11)),
                        selected: selected,
                        selectedTileColor: AppColors.pastelMint.withValues(alpha: 0.2),
                        onTap: () => setState(() => _selectedProduct = p),
                      );
                    },
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Text('Количество', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
                    const Spacer(),
                    Row(
                      children: [
                        IconButton(
                          icon: Icon(Icons.remove, size: 20, color: _quantity > 1 ? AppColors.pastelMint : AppColors.textTertiaryDark),
                          onPressed: _quantity > 1 ? () => setState(() => _quantity--) : null,
                        ),
                        Text('$_quantity', style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600)),
                        IconButton(
                          icon: Icon(Icons.add, size: 20, color: AppColors.pastelMint),
                          onPressed: () => setState(() => _quantity++),
                        ),
                      ],
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              TextField(
                controller: _discountController,
                onChanged: (_) => setState(() {}),
                decoration: InputDecoration(
                  labelText: 'Скидка',
                  labelStyle: TextStyle(color: AppColors.textSecondaryDark),
                  hintText: 'Макс ${_maxDiscount.toStringAsFixed(0)}',
                  filled: true,
                  fillColor: AppColors.surfaceElevatedDark,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                ),
                keyboardType: TextInputType.number,
                style: TextStyle(color: AppColors.textPrimaryDark),
              ),
              const SizedBox(height: 12),
              Text('Дата и время', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
              const SizedBox(height: 4),
              OutlinedButton.icon(
                onPressed: () async {
                  final date = await showDatePicker(
                    context: context,
                    initialDate: _saleDate,
                    firstDate: DateTime(2020),
                    lastDate: DateTime.now().add(const Duration(days: 1)),
                  );
                  if (!context.mounted || date == null) return;
                  final time = await showTimePicker(
                    context: context,
                    initialTime: TimeOfDay.fromDateTime(_saleDate),
                  );
                  if (!context.mounted) return;
                  if (time != null) {
                    setState(() => _saleDate = DateTime(date.year, date.month, date.day, time.hour, time.minute));
                  }
                },
                icon: Icon(Icons.calendar_today, size: 18, color: AppColors.pastelMint),
                label: Text(DateFormat('dd.MM.yyyy HH:mm', 'ru_RU').format(_saleDate),
                    style: TextStyle(color: AppColors.textPrimaryDark)),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.textPrimaryDark,
                  side: BorderSide(color: AppColors.borderDark),
                ),
              ),
              const SizedBox(height: 16),
              Text('Способ оплаты', style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 12)),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _chip('Наличные', _PaymentMethod.cash),
                  _chip('Карта', _PaymentMethod.card),
                  _chip('Сплит', _PaymentMethod.split),
                ],
              ),
              if (_paymentMethod == _PaymentMethod.card || _paymentMethod == _PaymentMethod.split) ...[
                const SizedBox(height: 8),
                DropdownButtonFormField<PaymentCard>(
                  initialValue: _selectedCard,
                  decoration: InputDecoration(
                    filled: true,
                    fillColor: AppColors.surfaceElevatedDark,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                  ),
                  dropdownColor: AppColors.surfaceDark,
                  hint: Text('Карта', style: TextStyle(color: AppColors.textSecondaryDark)),
                  items: widget.cards
                      .map((c) => DropdownMenuItem(value: c, child: Text(c.label, style: TextStyle(color: AppColors.textPrimaryDark))))
                      .toList(),
                  onChanged: (v) => setState(() => _selectedCard = v),
                ),
              ],
              if (_paymentMethod == _PaymentMethod.split) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _splitCashController,
                        decoration: InputDecoration(
                          labelText: 'Наличными',
                          filled: true,
                          fillColor: AppColors.surfaceElevatedDark,
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        keyboardType: TextInputType.number,
                        style: TextStyle(color: AppColors.textPrimaryDark),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: _splitCardController,
                        decoration: InputDecoration(
                          labelText: 'Картой',
                          filled: true,
                          fillColor: AppColors.surfaceElevatedDark,
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        keyboardType: TextInputType.number,
                        style: TextStyle(color: AppColors.textPrimaryDark),
                      ),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              TextField(
                controller: _commentController,
                decoration: InputDecoration(
                  labelText: 'Комментарий',
                  filled: true,
                  fillColor: AppColors.surfaceElevatedDark,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                ),
                style: TextStyle(color: AppColors.textPrimaryDark),
                maxLines: 2,
              ),
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.pastelMint.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Итого', style: TextStyle(color: AppColors.textSecondaryDark, fontWeight: FontWeight.w500)),
                    Text('${fmt.format(_total)}', style: TextStyle(color: AppColors.pastelMint, fontWeight: FontWeight.bold, fontSize: 16)),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _loading ? null : widget.onDismiss,
          child: Text('Отмена', style: TextStyle(color: AppColors.textSecondaryDark)),
        ),
        FilledButton(
          onPressed: _loading ? null : _save,
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.pastelMint,
            foregroundColor: AppColors.textOnPastel,
          ),
          child: _loading
              ? SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.textOnPastel),
                )
              : const Text('Сохранить'),
        ),
      ],
    );
  }

  Widget _chip(String label, _PaymentMethod m) {
    final selected = _paymentMethod == m;
    return FilterChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) => setState(() => _paymentMethod = m),
      backgroundColor: AppColors.surfaceElevatedDark,
      selectedColor: AppColors.pastelMint.withValues(alpha: 0.3),
      side: BorderSide(color: selected ? AppColors.pastelMint : AppColors.borderDark),
    );
  }
}
