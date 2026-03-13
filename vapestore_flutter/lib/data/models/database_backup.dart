import 'payment_card.dart';
import 'product.dart';
import 'sale.dart';

/// JSON backup format from Android VapeStoreApp DatabaseExporter
class DatabaseBackup {
  final int version;
  final int exportDate;
  final List<Product> products;
  final List<Sale> sales;
  final List<PaymentCard>? paymentCards;

  const DatabaseBackup({
    required this.version,
    required this.exportDate,
    required this.products,
    required this.sales,
    this.paymentCards,
  });

  factory DatabaseBackup.fromJson(Map<String, dynamic> json) {
    return DatabaseBackup(
      version: json['version'] as int? ?? 1,
      exportDate: json['exportDate'] as int? ?? 0,
      products: (json['products'] as List<dynamic>?)
              ?.map((e) => Product.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      sales: (json['sales'] as List<dynamic>?)
              ?.map((e) => Sale.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      paymentCards: (json['paymentCards'] as List<dynamic>?)
          ?.map((e) => PaymentCard.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
