import 'dart:convert';

import 'package:sqflite/sqflite.dart';

import '../local/database.dart';
import '../models/database_backup.dart';
import '../models/payment_card.dart';
import '../models/product.dart';
import '../models/sale.dart';

class VapeRepository {
  Future<Database> get _db => AppDatabase.instance;

  // Products
  Future<List<Product>> getAllProducts() async {
    final db = await _db;
    final maps = await db.query(
      'products',
      orderBy: "CASE category WHEN 'liquid' THEN 1 WHEN 'disposable' THEN 2 WHEN 'consumable' THEN 3 WHEN 'vape' THEN 4 WHEN 'snus' THEN 5 ELSE 6 END, orderIndex, brand, flavor",
    );
    return maps.map((m) => Product.fromMap(m)).toList();
  }

  Future<List<Product>> getAllProductsWithStock() async {
    final db = await _db;
    final maps = await db.query(
      'products',
      where: 'stock > 0',
      orderBy: "CASE category WHEN 'liquid' THEN 1 WHEN 'disposable' THEN 2 ELSE 6 END, orderIndex, brand, flavor",
    );
    return maps.map((m) => Product.fromMap(m)).toList();
  }

  Future<Product?> getProductByBarcode(String barcode) async {
    final db = await _db;
    final maps = await db.query('products', where: 'barcode = ?', whereArgs: [barcode]);
    return maps.isNotEmpty ? Product.fromMap(maps.first) : null;
  }

  Future<Product?> getProductById(int id) async {
    final db = await _db;
    final maps = await db.query('products', where: 'id = ?', whereArgs: [id]);
    return maps.isNotEmpty ? Product.fromMap(maps.first) : null;
  }

  Future<List<Product>> getProductsByBrand(String brand) async {
    final db = await _db;
    final maps = await db.query('products', where: 'brand = ?', whereArgs: [brand], orderBy: 'orderIndex, flavor');
    return maps.map((m) => Product.fromMap(m)).toList();
  }

  Future<List<String>> getAllBrands() async {
    final db = await _db;
    final maps = await db.rawQuery('SELECT DISTINCT brand FROM products ORDER BY brand');
    return maps.map((m) => m['brand'] as String).toList();
  }

  Future<List<String>> getBrandsWithStock() async {
    final db = await _db;
    final maps = await db.rawQuery('SELECT DISTINCT brand FROM products WHERE stock > 0 ORDER BY brand');
    return maps.map((m) => m['brand'] as String).toList();
  }

  Future<void> insertProduct(Product product) async {
    final db = await _db;
    final maxOrder = await db.rawQuery(
      'SELECT MAX(orderIndex) as m FROM products WHERE category = ?',
      [product.category],
    );
    final orderIndex = ((maxOrder.first['m'] as int?) ?? 0) + 1;
    await db.insert('products', product.copyWith(orderIndex: orderIndex).toInsertMap());
  }

  Future<void> updateProduct(Product product) async {
    final db = await _db;
    await db.update('products', product.toMap(), where: 'id = ?', whereArgs: [product.id]);
  }

  Future<void> deleteProduct(int id) async {
    final db = await _db;
    await db.delete('products', where: 'id = ?', whereArgs: [id]);
  }

  // Sales
  Future<void> insertSale(Sale sale) async {
    final db = await _db;
    await db.insert('sales', sale.toInsertMap());
  }

  Future<List<Sale>> getAllSales() async {
    final db = await _db;
    final maps = await db.query('sales', orderBy: 'date DESC');
    return maps.map((m) => Sale.fromMap(m)).toList();
  }

  Future<List<Sale>> getSalesInPeriod(int start, int end) async {
    final db = await _db;
    final maps = await db.query(
      'sales',
      where: 'date BETWEEN ? AND ? AND isCancelled = 0',
      whereArgs: [start, end],
      orderBy: 'date DESC',
    );
    return maps.map((m) => Sale.fromMap(m)).toList();
  }

  Future<double> getTotalProfit(int start, int end) async {
    final db = await _db;
    final result = await db.rawQuery(
      'SELECT COALESCE(SUM(profit), 0) as s FROM sales WHERE date BETWEEN ? AND ? AND isCancelled = 0',
      [start, end],
    );
    return (result.first['s'] as num?)?.toDouble() ?? 0;
  }

  Future<double> getTotalRevenue(int start, int end) async {
    final db = await _db;
    final result = await db.rawQuery(
      'SELECT COALESCE(SUM(revenue), 0) as s FROM sales WHERE date BETWEEN ? AND ? AND isCancelled = 0',
      [start, end],
    );
    return (result.first['s'] as num?)?.toDouble() ?? 0;
  }

  Future<int> getSalesCountInPeriod(int start, int end) async {
    final db = await _db;
    final result = await db.rawQuery(
      'SELECT COUNT(*) as c FROM sales WHERE date BETWEEN ? AND ? AND isCancelled = 0',
      [start, end],
    );
    return result.first['c'] as int? ?? 0;
  }

  Future<int> getTotalQuantityInPeriod(int start, int end) async {
    final db = await _db;
    final result = await db.rawQuery(
      'SELECT COALESCE(SUM(quantity), 0) as s FROM sales WHERE date BETWEEN ? AND ? AND isCancelled = 0',
      [start, end],
    );
    return (result.first['s'] as num?)?.toInt() ?? 0;
  }

  /// Возвращает прибыль по дням за последние 7 дней
  /// Формат: список пар (название дня, прибыль)
  Future<List<({String dayName, double profit})>> getWeekProfitByDay() async {
    final db = await _db;
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    
    final dayNames = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
    final result = <({String dayName, double profit})>[];
    
    // Получаем продажи за последние 7 дней
    final weekStart = today.subtract(const Duration(days: 6));
    final weekStartMs = weekStart.millisecondsSinceEpoch;
    final weekEndMs = now.millisecondsSinceEpoch;
    
    final sales = await getSalesInPeriod(weekStartMs, weekEndMs);
    
    // Группируем по дням
    final Map<int, double> dayProfits = {};
    for (final sale in sales) {
      final saleDate = DateTime.fromMillisecondsSinceEpoch(sale.date);
      final dayStart = DateTime(saleDate.year, saleDate.month, saleDate.day);
      final dayMs = dayStart.millisecondsSinceEpoch;
      dayProfits[dayMs] = (dayProfits[dayMs] ?? 0) + sale.profit;
    }
    
    // Формируем список за последние 7 дней
    for (int i = 0; i < 7; i++) {
      final day = weekStart.add(Duration(days: i));
      final dayMs = day.millisecondsSinceEpoch;
      final profit = dayProfits[dayMs] ?? 0.0;
      final weekday = (day.weekday + 5) % 7; // Понедельник = 0
      result.add((dayName: dayNames[weekday], profit: profit));
    }
    
    return result;
  }

  Future<void> increaseStock(int productId) async {
    final p = await getProductById(productId);
    if (p != null) await updateProduct(p.copyWith(stock: p.stock + 1));
  }

  Future<void> decreaseStock(int productId) async {
    final p = await getProductById(productId);
    if (p != null && p.stock > 0) await updateProduct(p.copyWith(stock: p.stock - 1));
  }

  // Import backup
  Future<void> importBackup(DatabaseBackup backup) async {
    final db = await _db;
    await db.delete('sales');
    await db.delete('products');
    await db.delete('payment_cards');
    for (final p in backup.products) {
      await db.insert('products', p.toMap());
    }
    for (final s in backup.sales) {
      await db.insert('sales', s.toMap());
    }
    for (final c in backup.paymentCards ?? []) {
      await db.insert('payment_cards', c.toMap());
    }
  }

  // Export
  Future<String> exportToJson() async {
    final products = await getAllProducts();
    final sales = await getAllSales();
    final cards = await getAllPaymentCards();
    final backup = {
      'version': 3,
      'exportDate': DateTime.now().millisecondsSinceEpoch,
      'products': products.map((e) => e.toJson()).toList(),
      'sales': sales.where((s) => !s.isCancelled).map((e) => e.toJson()).toList(),
      'paymentCards': cards.map((e) => e.toJson()).toList(),
    };
    return const JsonEncoder.withIndent('  ').convert(backup);
  }

  Future<List<PaymentCard>> getAllPaymentCards() async {
    final db = await _db;
    final maps = await db.query('payment_cards');
    return maps.map((m) => PaymentCard.fromMap(m)).toList();
  }
}
