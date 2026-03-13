class Sale {
  final int id;
  final int productId;
  final int date;
  final String? comment;
  final double discount;
  final double revenue;
  final double profit;
  final int quantity;
  final String paymentMethod;
  final double? cashAmount;
  final double? cardAmount;
  final int? cardId;
  final bool isCancelled;
  final int? originalSaleId;
  final String? sourceType;
  final int? sourceId;

  const Sale({
    this.id = 0,
    required this.productId,
    required this.date,
    this.comment,
    this.discount = 0,
    required this.revenue,
    required this.profit,
    this.quantity = 1,
    this.paymentMethod = 'cash',
    this.cashAmount,
    this.cardAmount,
    this.cardId,
    this.isCancelled = false,
    this.originalSaleId,
    this.sourceType,
    this.sourceId,
  });

  Map<String, dynamic> toInsertMap() {
    final m = toMap();
    m.remove('id');
    return m;
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'productId': productId,
      'date': date,
      'comment': comment,
      'discount': discount,
      'revenue': revenue,
      'profit': profit,
      'quantity': quantity,
      'paymentMethod': paymentMethod,
      'cashAmount': cashAmount,
      'cardAmount': cardAmount,
      'cardId': cardId,
      'isCancelled': isCancelled ? 1 : 0,
      'originalSaleId': originalSaleId,
      'sourceType': sourceType,
      'sourceId': sourceId,
    };
  }

  factory Sale.fromMap(Map<String, dynamic> map) {
    return Sale(
      id: map['id'] as int? ?? 0,
      productId: map['productId'] as int,
      date: map['date'] as int,
      comment: map['comment'] as String?,
      discount: (map['discount'] as num?)?.toDouble() ?? 0,
      revenue: (map['revenue'] as num).toDouble(),
      profit: (map['profit'] as num).toDouble(),
      quantity: map['quantity'] as int? ?? 1,
      paymentMethod: map['paymentMethod'] as String? ?? 'cash',
      cashAmount: (map['cashAmount'] as num?)?.toDouble(),
      cardAmount: (map['cardAmount'] as num?)?.toDouble(),
      cardId: map['cardId'] as int?,
      isCancelled: (map['isCancelled'] as int?) == 1,
      originalSaleId: map['originalSaleId'] as int?,
      sourceType: map['sourceType'] as String?,
      sourceId: map['sourceId'] as int?,
    );
  }

  Map<String, dynamic> toJson() => toMap();

  factory Sale.fromJson(Map<String, dynamic> json) => Sale.fromMap(json);
}
