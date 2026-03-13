class Product {
  final int id;
  final String brand;
  final String flavor;
  final String? barcode;
  final double purchasePrice;
  final double retailPrice;
  final int stock;
  final String category;
  final String strength;
  final String specification;
  final int orderIndex;

  const Product({
    this.id = 0,
    required this.brand,
    required this.flavor,
    this.barcode,
    required this.purchasePrice,
    required this.retailPrice,
    this.stock = 0,
    this.category = 'liquid',
    this.strength = '',
    this.specification = '',
    this.orderIndex = 0,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'brand': brand,
      'flavor': flavor,
      'barcode': barcode,
      'purchasePrice': purchasePrice,
      'retailPrice': retailPrice,
      'stock': stock,
      'category': category,
      'strength': strength,
      'specification': specification,
      'orderIndex': orderIndex,
    };
  }

  Map<String, dynamic> toInsertMap() {
    final m = toMap();
    m.remove('id');
    return m;
  }

  factory Product.fromMap(Map<String, dynamic> map) {
    return Product(
      id: map['id'] as int? ?? 0,
      brand: map['brand'] as String,
      flavor: map['flavor'] as String,
      barcode: map['barcode'] as String?,
      purchasePrice: (map['purchasePrice'] as num).toDouble(),
      retailPrice: (map['retailPrice'] as num).toDouble(),
      stock: map['stock'] as int? ?? 0,
      category: map['category'] as String? ?? 'liquid',
      strength: map['strength'] as String? ?? '',
      specification: map['specification'] as String? ?? '',
      orderIndex: map['orderIndex'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toJson() => toMap();

  factory Product.fromJson(Map<String, dynamic> json) => Product.fromMap(json);

  Product copyWith({
    int? id,
    String? brand,
    String? flavor,
    String? barcode,
    double? purchasePrice,
    double? retailPrice,
    int? stock,
    String? category,
    String? strength,
    String? specification,
    int? orderIndex,
  }) {
    return Product(
      id: id ?? this.id,
      brand: brand ?? this.brand,
      flavor: flavor ?? this.flavor,
      barcode: barcode ?? this.barcode,
      purchasePrice: purchasePrice ?? this.purchasePrice,
      retailPrice: retailPrice ?? this.retailPrice,
      stock: stock ?? this.stock,
      category: category ?? this.category,
      strength: strength ?? this.strength,
      specification: specification ?? this.specification,
      orderIndex: orderIndex ?? this.orderIndex,
    );
  }
}
