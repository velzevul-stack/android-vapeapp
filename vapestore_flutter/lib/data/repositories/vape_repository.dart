import 'dart:convert';
import 'dart:developer' as developer;

import 'package:sqflite/sqflite.dart';

import '../local/database.dart';
import '../parse_utils.dart';
import '../models/database_backup.dart';
import '../models/debt.dart';
import '../../utils/category_display_service.dart';
import '../../utils/category_order_service.dart';
import '../models/payment_card.dart';
import '../models/product.dart';
import '../models/reservation.dart';
import '../models/sale.dart';
import '../../utils/product_display.dart';

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
      orderBy: "CASE category WHEN 'liquid' THEN 1 WHEN 'disposable' THEN 2 ELSE 6 END, orderIndex, brand, flavor, purchasePrice ASC",
    );
    return maps.map((m) => Product.fromMap(m)).toList();
  }

  /// При нескольких товарах с одним штрихкодом возвращает подарочный (закупка 0) первым.
  /// [inStockOnly] — для продажи: только товары в наличии; для приёмки: любой.
  Future<Product?> getProductByBarcode(String barcode, {bool inStockOnly = false}) async {
    final db = await _db;
    final where = inStockOnly ? 'barcode = ? AND stock > 0' : 'barcode = ?';
    final maps = await db.query(
      'products',
      where: where,
      whereArgs: [barcode],
      orderBy: 'purchasePrice ASC',
    );
    if (maps.isEmpty) return null;
    final product = Product.fromMap(maps.first);
    if (!inStockOnly) return product;
    // Check available stock considering active reservations
    final reservations = await getReservationsForProduct(product.id);
    final reservedQty = reservations.fold<int>(0, (sum, r) => sum + r.quantity);
    final available = product.stock - reservedQty;
    return available > 0 ? product.copyWith(stock: available) : null;
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

  Future<List<String>> getBrandsByCategory(String category) async {
    final db = await _db;
    final maps = await db.rawQuery(
      'SELECT DISTINCT brand FROM products WHERE category = ? ORDER BY brand',
      [category],
    );
    return maps.map((m) => m['brand'] as String).toList();
  }

  Future<int> insertProduct(Product product) async {
    final db = await _db;
    final maxOrder = await db.rawQuery(
      'SELECT MAX(orderIndex) as m FROM products WHERE category = ?',
      [product.category],
    );
    final orderIndex = parseInt(maxOrder.first['m'], 0) + 1;
    return await db.insert('products', product.copyWith(orderIndex: orderIndex).toInsertMap());
  }

  Future<void> updateProduct(Product product) async {
    final db = await _db;
    await db.update('products', product.toMap(), where: 'id = ?', whereArgs: [product.id]);
  }

  /// Очистить весь склад (обнулить stock у всех товаров)
  Future<void> clearAllStock() async {
    final db = await _db;
    await db.execute('UPDATE products SET stock = 0');
  }

  /// Переименовать бренд во всех товарах
  Future<void> renameBrand(String oldBrand, String newBrand) async {
    final trimmed = newBrand.trim();
    if (trimmed.isEmpty || trimmed == oldBrand) return;
    final db = await _db;
    await db.update('products', {'brand': trimmed}, where: 'brand = ?', whereArgs: [oldBrand]);
  }

  /// Присвоить цену(ы) всем товарам бренда
  Future<void> updateBrandPrices(String brand, double retailPrice, [double? purchasePrice]) async {
    final db = await _db;
    final updates = <String, Object>{'retailPrice': retailPrice};
    if (purchasePrice != null) updates['purchasePrice'] = purchasePrice;
    await db.update('products', updates, where: 'brand = ?', whereArgs: [brand]);
  }

  /// Обновить категорию и/или крепость всех товаров бренда
  Future<void> updateBrandMeta(String brand, {String? category, String? strength}) async {
    final db = await _db;
    if (category != null) await db.update('products', {'category': category}, where: 'brand = ?', whereArgs: [brand]);
    if (strength != null) await db.update('products', {'strength': strength}, where: 'brand = ?', whereArgs: [brand]);
  }

  /// Удалить бренд и все его товары
  Future<void> deleteBrand(String brand) async {
    final db = await _db;
    await db.delete('products', where: 'brand = ?', whereArgs: [brand]);
  }

  /// Поменять порядок двух брендов в категории (определяет порядок в посте)
  Future<void> swapBrandOrder(String category, String brandA, String brandB) async {
    final productsA = await getProductsByBrand(brandA);
    final productsB = await getProductsByBrand(brandB);
    final inCategoryA = productsA.where((p) => p.category == category).toList()..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));
    final inCategoryB = productsB.where((p) => p.category == category).toList()..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));
    if (inCategoryA.isEmpty || inCategoryB.isEmpty) return;
    final indexesA = inCategoryA.map((p) => p.orderIndex).toList();
    final indexesB = inCategoryB.map((p) => p.orderIndex).toList();
    const tempOffset = 1000000;
    final db = await _db;
    for (var i = 0; i < inCategoryA.length; i++) {
      await db.update('products', {'orderIndex': tempOffset + i}, where: 'id = ?', whereArgs: [inCategoryA[i].id]);
    }
    for (var i = 0; i < inCategoryB.length; i++) {
      await db.update('products', {'orderIndex': indexesA[i % indexesA.length]}, where: 'id = ?', whereArgs: [inCategoryB[i].id]);
    }
    for (var i = 0; i < inCategoryA.length; i++) {
      await db.update('products', {'orderIndex': indexesB[i % indexesB.length]}, where: 'id = ?', whereArgs: [inCategoryA[i].id]);
    }
  }

  /// Бренды категории в порядке orderIndex (для отображения и поста)
  Future<List<String>> getBrandsByCategoryOrdered(String category) async {
    final db = await _db;
    final maps = await db.rawQuery(
      'SELECT brand, MIN(orderIndex) as minOrder FROM products WHERE category = ? GROUP BY brand ORDER BY minOrder, brand',
      [category],
    );
    return maps.map((m) => m['brand'] as String).toList();
  }

  /// Установить порядок брендов в категории
  Future<void> setBrandOrder(String category, List<String> orderedBrands) async {
    if (orderedBrands.isEmpty) return;
    final db = await _db;
    var baseIndex = 0;
    for (final brand in orderedBrands) {
      final products = await getProductsByBrand(brand);
      final inCat = products.where((p) => p.category == category).toList()..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));
      for (var i = 0; i < inCat.length; i++) {
        await db.update('products', {'orderIndex': baseIndex + i}, where: 'id = ?', whereArgs: [inCat[i].id]);
      }
      baseIndex += inCat.length;
    }
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

  Future<Sale?> getSaleById(int saleId) async {
    final db = await _db;
    final maps = await db.query('sales', where: 'id = ?', whereArgs: [saleId]);
    return maps.isNotEmpty ? Sale.fromMap(maps.first) : null;
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
    return parseInt(result.first['c'], 0);
  }

  Future<int> getTotalQuantityInPeriod(int start, int end) async {
    final db = await _db;
    final result = await db.rawQuery(
      'SELECT COALESCE(SUM(quantity), 0) as s FROM sales WHERE date BETWEEN ? AND ? AND isCancelled = 0',
      [start, end],
    );
    return (result.first['s'] as num?)?.toInt() ?? 0;
  }

  /// Сводка за день: продажи, наличные, карта, в долг
  Future<Map<String, dynamic>> getDaySummary(int start, int end) async {
    final sales = await getSalesInPeriod(start, end);
    var cash = 0.0;
    var card = 0.0;
    var debt = 0.0;
    for (final s in sales) {
      if (s.sourceType == 'debt') {
        if (s.cashAmount != null && s.cashAmount! > 0) cash += s.cashAmount!;
        if (s.cardAmount != null && s.cardAmount! > 0) card += s.cardAmount!;
        if ((s.cashAmount == null || s.cashAmount == 0) && (s.cardAmount == null || s.cardAmount == 0)) {
          switch (s.paymentMethod) {
            case 'cash':
              cash += s.revenue;
              break;
            case 'card':
              card += s.revenue;
              break;
            default:
              debt += s.revenue;
          }
        }
      } else if (s.cashAmount != null || s.cardAmount != null) {
        cash += s.cashAmount ?? 0;
        card += s.cardAmount ?? 0;
      } else {
        switch (s.paymentMethod) {
          case 'cash':
            cash += s.revenue;
            break;
          case 'card':
            card += s.revenue;
            break;
          default:
            cash += s.revenue;
        }
      }
    }
    final count = sales.length;
    final quantity = sales.fold<int>(0, (sum, s) => sum + s.quantity);
    final revenue = sales.fold<double>(0, (sum, s) => sum + s.revenue);
    return {
      'salesCount': count,
      'quantity': quantity,
      'revenue': revenue,
      'cash': cash,
      'card': card,
      'debt': debt,
    };
  }

  /// Продажи по дням (последние N дней) для раздела «Последние дни»
  /// onlyDaysWithSales: true — только дни, в которые были продажи; сегодня сверху
  Future<List<({int dateMs, int salesCount, double revenue, double profit})>> getSalesByDay(int days, {bool onlyDaysWithSales = true}) async {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final startMs = today.subtract(Duration(days: days - 1)).millisecondsSinceEpoch;
    final endMs = now.millisecondsSinceEpoch;
    final sales = await getSalesInPeriod(startMs, endMs);

    final byDay = <int, ({int count, double revenue, double profit})>{};
    for (final s in sales) {
      final d = DateTime.fromMillisecondsSinceEpoch(s.date);
      final dayMs = DateTime(d.year, d.month, d.day).millisecondsSinceEpoch;
      final current = byDay[dayMs] ?? (count: 0, revenue: 0.0, profit: 0.0);
      byDay[dayMs] = (
        count: current.count + 1,
        revenue: current.revenue + s.revenue,
        profit: current.profit + s.profit,
      );
    }

    final result = byDay.entries
        .where((e) => !onlyDaysWithSales || e.value.count > 0)
        .map((e) => (dateMs: e.key, salesCount: e.value.count, revenue: e.value.revenue, profit: e.value.profit))
        .toList();
    result.sort((a, b) => b.dateMs.compareTo(a.dateMs)); // сегодня сверху
    return result;
  }

  /// Продажи по дням в диапазоне — только дни с продажами, сортировка по дате (новые первые)
  Future<List<({int dateMs, int salesCount, double revenue, double profit})>> getSalesByDateRange({
    required int startMs,
    required int endMs,
    bool onlyDaysWithSales = true,
  }) async {
    final sales = await getSalesInPeriod(startMs, endMs);
    final byDay = <int, ({int count, double revenue, double profit})>{};
    for (final s in sales) {
      final d = DateTime.fromMillisecondsSinceEpoch(s.date);
      final dayMs = DateTime(d.year, d.month, d.day).millisecondsSinceEpoch;
      final current = byDay[dayMs] ?? (count: 0, revenue: 0.0, profit: 0.0);
      byDay[dayMs] = (
        count: current.count + 1,
        revenue: current.revenue + s.revenue,
        profit: current.profit + s.profit,
      );
    }
    final result = byDay.entries
        .where((e) => !onlyDaysWithSales || e.value.count > 0)
        .map((e) => (dateMs: e.key, salesCount: e.value.count, revenue: e.value.revenue, profit: e.value.profit))
        .toList();
    result.sort((a, b) => b.dateMs.compareTo(a.dateMs));
    return result;
  }

  /// Типы оплаты за период: В долг, Карта, Наличные, Сплит (для отдельного графика)
  Future<List<({String label, double revenue})>> getRevenueByPaymentTypeForPeriod(int startMs, int endMs) async {
    final sales = await getSalesInPeriod(startMs, endMs);
    var cash = 0.0;
    var card = 0.0;
    var debt = 0.0;
    var split = 0.0;

    for (final s in sales) {
      if (s.sourceType == 'debt') {
        final hasCash = s.cashAmount != null && s.cashAmount! > 0;
        final hasCard = s.cardAmount != null && s.cardAmount! > 0;
        if (hasCash && hasCard) {
          split += s.revenue;
        } else if (hasCash) {
          cash += s.cashAmount!;
        } else if (hasCard) {
          card += s.cardAmount!;
        } else {
          switch (s.paymentMethod) {
            case 'cash':
              cash += s.revenue;
              break;
            case 'card':
              card += s.revenue;
              break;
            default:
              debt += s.revenue;
          }
        }
      } else if (s.cashAmount != null && s.cashAmount! > 0 && s.cardAmount != null && s.cardAmount! > 0) {
        split += s.revenue;
      } else if (s.cashAmount != null && s.cashAmount! > 0) {
        cash += s.cashAmount!;
      } else if (s.cardAmount != null && s.cardAmount! > 0) {
        card += s.cardAmount!;
      } else {
        switch (s.paymentMethod) {
          case 'card':
            card += s.revenue;
            break;
          case 'cash':
            cash += s.revenue;
            break;
          default:
            cash += s.revenue;
        }
      }
    }

    final result = <({String label, double revenue})>[];
    if (debt > 0) result.add((label: 'В долг', revenue: debt));
    if (card > 0) result.add((label: 'Карта', revenue: card));
    if (cash > 0) result.add((label: 'Наличные', revenue: cash));
    if (split > 0) result.add((label: 'Сплит', revenue: split));
    return result;
  }

  /// Продажи по картам за период (только карточные платежи, для круговой диаграммы)
  Future<List<({String label, double revenue})>> getRevenueByCardForPeriod(int startMs, int endMs) async {
    final sales = await getSalesInPeriod(startMs, endMs);
    final cards = await getActivePaymentCards();
    final cardMap = {for (final c in cards) c.id: c.label};
    final byCard = <String, double>{};

    for (final s in sales) {
      final cardAmt = s.cardAmount ?? (s.paymentMethod == 'card' ? s.revenue : 0.0);
      if (cardAmt <= 0) continue;
      final label = s.cardId != null && cardMap.containsKey(s.cardId) ? cardMap[s.cardId]! : 'Карта';
      byCard[label] = (byCard[label] ?? 0) + cardAmt;
    }

    return byCard.entries.map((e) => (label: e.key, revenue: e.value)).toList();
  }

  /// Продажи по картам за один день: (карта -> кол-во чеков, сумма)
  Future<List<({String label, int salesCount, double revenue})>> getSalesByCardForDay(int dayStartMs) async {
    final sales = await getSalesForDay(dayStartMs);
    final cards = await getActivePaymentCards();
    final cardMap = {for (final c in cards) c.id: c.label};

    final byCard = <String, ({int count, double revenue})>{};
    for (final s in sales) {
      final cardAmt = s.cardAmount ?? (s.paymentMethod == 'card' ? s.revenue : 0.0);
      if (cardAmt <= 0) continue;
      final label = (s.cardId != null && cardMap.containsKey(s.cardId)) ? cardMap[s.cardId]! : 'Карта';
      final cur = byCard[label] ?? (count: 0, revenue: 0.0);
      byCard[label] = (count: cur.count + 1, revenue: cur.revenue + cardAmt);
    }

    final result = byCard.entries.map((e) => (label: e.key, salesCount: e.value.count, revenue: e.value.revenue)).toList();
    result.sort((a, b) => b.revenue.compareTo(a.revenue));
    return result;
  }

  /// Товары по популярности (кол-во продаж) за период
  Future<List<({int productId, String displayName, String? displaySubtitle, int quantity, double revenue})>> getProductsByPopularityForPeriod(int startMs, int endMs) async {
    final sales = await getSalesInPeriod(startMs, endMs);
    final byProduct = <int, ({int quantity, double revenue})>{};
    for (final s in sales) {
      final cur = byProduct[s.productId] ?? (quantity: 0, revenue: 0.0);
      byProduct[s.productId] = (quantity: cur.quantity + s.quantity, revenue: cur.revenue + s.revenue);
    }
    final products = await getAllProducts();
    final productMap = {for (final p in products) p.id: p};
    final result = byProduct.entries.map((e) {
      final p = productMap[e.key];
      final displayName = p != null ? productDisplayName(p) : 'Неизвестный товар';
      final displaySubtitle = p != null ? productDisplaySubtitle(p) : null;
      return (productId: e.key, displayName: displayName, displaySubtitle: displaySubtitle, quantity: e.value.quantity, revenue: e.value.revenue);
    }).toList();
    result.sort((a, b) => b.quantity.compareTo(a.quantity));
    return result;
  }

  /// Бренды по популярности (кол-во продаж) за период
  Future<List<({String brand, int quantity, double revenue})>> getBrandsByPopularityForPeriod(int startMs, int endMs) async {
    final sales = await getSalesInPeriod(startMs, endMs);
    final products = await getAllProducts();
    final productMap = {for (final p in products) p.id: p};
    final byBrand = <String, ({int quantity, double revenue})>{};
    for (final s in sales) {
      final p = productMap[s.productId];
      final brand = p != null ? p.brand : 'Неизвестный';
      final cur = byBrand[brand] ?? (quantity: 0, revenue: 0.0);
      byBrand[brand] = (quantity: cur.quantity + s.quantity, revenue: cur.revenue + s.revenue);
    }
    final result = byBrand.entries.map((e) => (brand: e.key, quantity: e.value.quantity, revenue: e.value.revenue)).toList();
    result.sort((a, b) => b.quantity.compareTo(a.quantity));
    return result;
  }

  /// Сводка по оплате за день: наличные и карта
  Future<({double cash, double card})> getDayPaymentSummary(int dayStartMs) async {
    final sales = await getSalesForDay(dayStartMs);
    var cash = 0.0;
    var card = 0.0;
    for (final s in sales) {
      if (s.cashAmount != null && s.cashAmount! > 0) cash += s.cashAmount!;
      if (s.cardAmount != null && s.cardAmount! > 0) card += s.cardAmount!;
      if (s.cashAmount == null && s.cardAmount == null) {
        if (s.paymentMethod == 'card') {
          card += s.revenue;
        } else if (s.paymentMethod == 'cash') {
          cash += s.revenue;
        }
      }
    }
    return (cash: cash, card: card);
  }

  /// Продажи за день (для детализации при раскрытии)
  Future<List<Sale>> getSalesForDay(int dayStartMs) async {
    final dayEnd = DateTime.fromMillisecondsSinceEpoch(dayStartMs).add(const Duration(days: 1));
    final endMs = dayEnd.millisecondsSinceEpoch - 1;
    return getSalesInPeriod(dayStartMs, endMs);
  }

  /// Продажи за день с названиями товаров (displayName, displaySubtitle — по категории)
  Future<List<({Sale sale, String displayName, String displaySubtitle})>> getSalesForDayWithProducts(int dayStartMs) async {
    final sales = await getSalesForDay(dayStartMs);
    final result = <({Sale sale, String displayName, String displaySubtitle})>[];

    for (final sale in sales) {
      String displayName = '';
      String displaySubtitle = '';

      if (sale.productId == 0) {
        // Для продаж с productId 0 проверяем sourceType
        if (sale.sourceType == 'debt' && sale.sourceId != null) {
          final debt = await getDebtById(sale.sourceId!);
          if (debt != null) {
            try {
              final items = (jsonDecode(debt.products) as List<dynamic>)
                  .map((e) => e as Map)
                  .map((m) => {
                        'productId': m['productId'] as int,
                        'quantity': m['quantity'] as int,
                      })
                  .toList();
              final parts = <String>[];
              for (final item in items) {
                final p = await getProductById(item['productId'] as int);
                if (p != null) {
                  final name = productDisplayName(p);
                  final sub = productDisplaySubtitle(p);
                  final full = sub != null ? '$name — $sub' : name;
                  parts.add('$full × ${item['quantity']}');
                }
              }
              displayName = parts.isEmpty ? 'Долг #${sale.sourceId}' : parts.join(', ');
              displaySubtitle = '';
            } catch (_) {
              displayName = 'Долг #${sale.sourceId}';
              displaySubtitle = '';
            }
          } else {
            displayName = 'Долг #${sale.sourceId}';
            displaySubtitle = '';
          }
        } else if (sale.sourceType == 'reservation' && sale.sourceId != null) {
          final reservation = await getReservationById(sale.sourceId!);
          if (reservation != null) {
            final p = await getProductById(reservation.productId);
            if (p != null) {
              displayName = productDisplayName(p);
              displaySubtitle = productDisplaySubtitle(p) ?? '';
            } else {
              displayName = 'Резерв #${sale.sourceId}';
              displaySubtitle = '';
            }
          } else {
            displayName = 'Резерв #${sale.sourceId}';
            displaySubtitle = '';
          }
        } else {
          // productId 0 без sourceType — пытаемся избежать "ID 0"
          displayName = 'Продажа';
          displaySubtitle = '';
        }
      } else {
        final p = await getProductById(sale.productId);
        if (p != null) {
          displayName = productDisplayName(p);
          displaySubtitle = productDisplaySubtitle(p) ?? '';
        } else {
          displayName = 'Неизвестный товар (ID ${sale.productId})';
          displaySubtitle = '';
        }
      }

      result.add((sale: sale, displayName: displayName, displaySubtitle: displaySubtitle));
    }

    return result;
  }

  /// Продажи и чеки по часам за сегодня (0–23)
  Future<List<({int hour, int salesCount, int quantity})>> getSalesByHourToday() async {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final start = today.millisecondsSinceEpoch;
    final end = now.millisecondsSinceEpoch;
    final sales = await getSalesInPeriod(start, end);

    final byHour = List.generate(24, (i) => (hour: i, salesCount: 0, quantity: 0));
    for (final s in sales) {
      final d = DateTime.fromMillisecondsSinceEpoch(s.date);
      final h = d.hour;
      byHour[h] = (
        hour: h,
        salesCount: byHour[h].salesCount + 1,
        quantity: byHour[h].quantity + s.quantity,
      );
    }
    return byHour;
  }

  /// Возвращает прибыль по дням за последние 7 дней
  /// Формат: список пар (название дня, прибыль)
  Future<List<({String dayName, double profit})>> getWeekProfitByDay() async {
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

  /// Отмена продажи: возврат товара на склад (или восстановление долга/резерва), isCancelled=1
  Future<bool> cancelSale(int saleId) async {
    final sale = await getSaleById(saleId);
    if (sale == null || sale.isCancelled) return false;

    final db = await _db;

    if (sale.sourceType == 'debt') {
      if (sale.sourceId != null) {
        final debt = await getDebtById(sale.sourceId!);
        if (debt != null) {
          await db.update(
            'debts',
            {'isPaid': 0, 'paidAmount': 0},
            where: 'id = ?',
            whereArgs: [sale.sourceId],
          );
          if (debt.stockDeducted) {
            try {
              final items = (jsonDecode(debt.products) as List<dynamic>)
                  .map((e) => e as Map)
                  .toList();
              for (final item in items) {
                final productId = item['productId'] as int;
                final quantity = item['quantity'] as int;
                if (productId > 0) {
                  for (int i = 0; i < quantity; i++) {
                    await increaseStock(productId);
                  }
                }
              }
              await db.update(
                'debts',
                {'stockDeducted': 0},
                where: 'id = ?',
                whereArgs: [sale.sourceId],
              );
            } catch (_) {}
          }
        }
      }
    } else if (sale.sourceType == 'reservation') {
      if (sale.sourceId != null && sale.productId > 0) {
        final reservation = await getReservationById(sale.sourceId!);
        if (reservation != null) {
          await db.update(
            'reservations',
            {'isSold': 0},
            where: 'id = ?',
            whereArgs: [sale.sourceId],
          );
          final p = await getProductById(sale.productId);
          if (p != null) {
            await updateProduct(p.copyWith(stock: p.stock + sale.quantity));
          }
        }
      }
    } else {
      if (sale.productId > 0) {
        final p = await getProductById(sale.productId);
        if (p != null) {
          await updateProduct(p.copyWith(stock: p.stock + sale.quantity));
        }
      }
    }

    await db.update('sales', {'isCancelled': 1}, where: 'id = ?', whereArgs: [saleId]);
    return true;
  }

  /// Исправление продажи: возврат оригинала на склад, новая продажа, отмена оригинала
  Future<bool> correctSale({
    required int originalSaleId,
    int? newProductId,
    int? newQuantity,
    double? newDiscount,
    String? newPaymentMethod,
    int? newDate,
    String? comment,
    double? cashAmount,
    double? cardAmount,
    int? cardId,
  }) async {
    final originalSale = await getSaleById(originalSaleId);
    if (originalSale == null || originalSale.isCancelled) return false;

    final isDebt = originalSale.sourceType == 'debt';
    final isReservation = originalSale.sourceType == 'reservation';

    final finalProductId = (isDebt || isReservation) ? originalSale.productId : (newProductId ?? originalSale.productId);
    final product = await getProductById(finalProductId);
    if (product == null && !isDebt) return false;

    final finalQuantity = (isDebt || isReservation) ? originalSale.quantity : (newQuantity ?? originalSale.quantity);
    final finalDate = newDate ?? originalSale.date;

    if (isDebt) {
      final db = await _db;
      await db.update('sales', {'isCancelled': 1}, where: 'id = ?', whereArgs: [originalSaleId]);
      final correctedSale = Sale(
        productId: originalSale.productId,
        date: finalDate,
        customerName: originalSale.customerName,
        comment: comment ?? originalSale.comment,
        discount: originalSale.discount,
        revenue: originalSale.revenue,
        profit: originalSale.profit,
        quantity: originalSale.quantity,
        paymentMethod: originalSale.paymentMethod,
        cashAmount: originalSale.cashAmount,
        cardAmount: originalSale.cardAmount,
        cardId: originalSale.cardId,
        originalSaleId: originalSaleId,
        sourceType: originalSale.sourceType,
        sourceId: originalSale.sourceId,
      );
      await db.insert('sales', correctedSale.toInsertMap());
      return true;
    }

    if (isReservation) {
      final finalDiscount = newDiscount ?? originalSale.discount;
      final finalPaymentMethod = newPaymentMethod ?? originalSale.paymentMethod;
      final finalCash = finalPaymentMethod == 'split' ? (cashAmount ?? originalSale.cashAmount) : null;
      final finalCard = finalPaymentMethod == 'split' ? (cardAmount ?? originalSale.cardAmount) : null;
      final finalCardId = (finalPaymentMethod == 'card' || finalPaymentMethod == 'split') ? (cardId ?? originalSale.cardId) : null;

      final revenue = product!.retailPrice * finalQuantity - finalDiscount;
      final profit = (product.retailPrice - product.purchasePrice) * finalQuantity - finalDiscount;

      final db = await _db;
      await db.update('sales', {'isCancelled': 1}, where: 'id = ?', whereArgs: [originalSaleId]);
      final correctedSale = Sale(
        productId: originalSale.productId,
        date: finalDate,
        customerName: originalSale.customerName,
        comment: comment ?? originalSale.comment,
        discount: finalDiscount,
        revenue: revenue,
        profit: profit,
        quantity: originalSale.quantity,
        paymentMethod: finalPaymentMethod,
        cashAmount: finalCash,
        cardAmount: finalCard,
        cardId: finalCardId,
        originalSaleId: originalSaleId,
        sourceType: originalSale.sourceType,
        sourceId: originalSale.sourceId,
      );
      await db.insert('sales', correctedSale.toInsertMap());
      return true;
    }

    if (product == null) return false;

    final finalDiscount = newDiscount ?? originalSale.discount;
    final finalPaymentMethod = newPaymentMethod ?? originalSale.paymentMethod;

    final revenue = product.retailPrice * finalQuantity - finalDiscount;
    final profit = (product.retailPrice - product.purchasePrice) * finalQuantity - finalDiscount;

    final originalProduct = await getProductById(originalSale.productId);

    final isSameProduct = product.id == originalSale.productId;
    final effectiveStock = isSameProduct
        ? (originalProduct?.stock ?? 0) + originalSale.quantity - finalQuantity
        : product.stock;
    final reservedQty = (await getReservationsForProduct(product.id)).fold<int>(0, (s, r) => s + r.quantity);
    final available = isSameProduct
        ? effectiveStock - reservedQty
        : product.stock + (isSameProduct ? originalSale.quantity : 0) - reservedQty;
    if (!isSameProduct && available < finalQuantity) return false;
    if (isSameProduct && effectiveStock - reservedQty < 0) return false;

    if (originalProduct != null && !isSameProduct) {
      await updateProduct(originalProduct.copyWith(stock: originalProduct.stock + originalSale.quantity));
    }

    final finalCash = finalPaymentMethod == 'split' ? (cashAmount ?? originalSale.cashAmount) : null;
    final finalCard = finalPaymentMethod == 'split' ? (cardAmount ?? originalSale.cardAmount) : null;
    final finalCardId = (finalPaymentMethod == 'card' || finalPaymentMethod == 'split') ? (cardId ?? originalSale.cardId) : null;

    if (isSameProduct) {
      await updateProduct(product.copyWith(stock: effectiveStock));
    } else {
      await updateProduct(product.copyWith(stock: product.stock - finalQuantity));
    }

    final correctedSale = Sale(
      productId: finalProductId,
      date: finalDate,
      customerName: originalSale.customerName,
      comment: comment ?? 'Исправление продажи #$originalSaleId',
      discount: finalDiscount,
      revenue: revenue,
      profit: profit,
      quantity: finalQuantity,
      paymentMethod: finalPaymentMethod,
      cashAmount: finalCash,
      cardAmount: finalCard,
      cardId: finalCardId,
      originalSaleId: originalSaleId,
    );

    final db = await _db;
    await db.update('sales', {'isCancelled': 1}, where: 'id = ?', whereArgs: [originalSaleId]);
    await db.insert('sales', correctedSale.toInsertMap());

    return true;
  }

  /// Нормализует штрих-код: только цифры, ровно 13. Возвращает null если невалидный.
  static String? _normalizeBarcode13(String? barcode) {
    if (barcode == null || barcode.isEmpty) return null;
    final digits = barcode.replaceAll(RegExp(r'\D'), '');
    if (digits.length != 13) return null;
    return digits;
  }

  // Import backup (совместим с форматом экспорта из старого Android VapeStoreApp)
  /// Товары с валидным 13-значным штрих-кодом, уже существующие в БД, пропускаются (без дублей).
  Future<void> importBackup(DatabaseBackup backup) async {
    final db = await _db;
    await db.delete('sales');
    await db.delete('payment_cards');
    await db.delete('debts');
    await db.delete('reservations');
    // products НЕ удаляем — мержим с дедупликацией по штрих-коду
    final idMap = <int, int>{}; // backupProductId -> ourProductId
    for (var p in backup.products) {
      // Нормализация vape: если specification пусто, а в flavor лежит цвет — копируем
      if (p.category == 'vape') {
        final spec = p.specification.trim();
        final flav = p.flavor.trim();
        final brand = p.brand.trim();
        if (spec.isEmpty && flav.isNotEmpty && flav.toLowerCase() != brand.toLowerCase()) {
          p = p.copyWith(specification: flav);
        } else if (spec.isNotEmpty && (flav.isEmpty || flav.toLowerCase() == brand.toLowerCase())) {
          p = p.copyWith(flavor: spec);
        }
      }
      // Нормализация consumable: если flavor == brand, оставляем как есть (display-логика обработает)
      final normalized = _normalizeBarcode13(p.barcode);
      if (normalized != null) {
        final existing = await getProductByBarcode(normalized);
        if (existing != null) {
          idMap[p.id] = existing.id;
          continue;
        }
      }
      final rowId = await db.insert('products', p.toInsertMap());
      idMap[p.id] = rowId;
    }
    for (final s in backup.sales) {
      final ourProductId = idMap[s.productId];
      if (ourProductId == null) continue;
      await db.insert('sales', Sale(
        productId: ourProductId,
        date: s.date,
        customerName: s.customerName,
        comment: s.comment,
        discount: s.discount,
        revenue: s.revenue,
        profit: s.profit,
        quantity: s.quantity,
        paymentMethod: s.paymentMethod,
        cashAmount: s.cashAmount,
        cardAmount: s.cardAmount,
        cardId: s.cardId,
        isCancelled: s.isCancelled,
        originalSaleId: s.originalSaleId,
        sourceType: s.sourceType,
        sourceId: s.sourceId,
      ).toInsertMap());
    }
    for (final c in backup.paymentCards ?? []) {
      await db.insert('payment_cards', c.toMap());
    }
    for (final d in backup.debts ?? []) {
      final remapped = _remapDebtProductIds(d.products, idMap);
      if (remapped == null) continue;
      await db.insert('debts', Debt(
        customerName: d.customerName,
        products: remapped,
        date: d.date,
        totalAmount: d.totalAmount,
        paidAmount: d.paidAmount,
        isPaid: d.isPaid,
        stockDeducted: d.stockDeducted,
      ).toInsertMap());
    }
    for (final r in backup.reservations ?? []) {
      final ourProductId = idMap[r.productId];
      if (ourProductId == null) continue;
      await db.insert('reservations', Reservation(
        customerName: r.customerName,
        productId: ourProductId,
        quantity: r.quantity,
        reservationDate: r.reservationDate,
        expirationDate: r.expirationDate,
        isSold: r.isSold,
      ).toInsertMap());
    }
    if (backup.categoryOrder != null && backup.categoryOrder!.isNotEmpty) {
      await CategoryOrderService.setCategoryOrder(backup.categoryOrder!);
    }
    if (backup.categoryDisplayNames != null) {
      await CategoryDisplayService.setCustomNames(backup.categoryDisplayNames!);
    }
  }

  /// Переписывает JSON products в долге, заменяя productId на наши id. null если не удалось.
  String? _remapDebtProductIds(String productsJson, Map<int, int> idMap) {
    try {
      final list = jsonDecode(productsJson) as List<dynamic>;
      final remapped = <Map<String, dynamic>>[];
      for (final e in list) {
        final m = e as Map;
        final backupId = parseInt(m['productId'], 0);
        final ourId = idMap[backupId];
        if (ourId == null) continue;
        remapped.add({'productId': ourId, 'quantity': m['quantity']});
      }
      return jsonEncode(remapped);
    } catch (_) {
      return null;
    }
  }

  // Export (categoryOrder опционально — для совместимости со старым Android)
  Future<String> exportToJson({List<String>? categoryOrder}) async {
    final products = await getAllProducts();
    final sales = await getAllSales();
    final cards = await getAllPaymentCards();
    final debts = await getAllDebts();
    final reservations = await getAllReservations();
    final order = categoryOrder ?? await CategoryOrderService.getCategoryOrder();
    final categoryDisplayNames = await CategoryDisplayService.getCustomNames();
    final backup = {
      'schemaVersion': 2,
      'version': 3,
      'exportDate': DateTime.now().millisecondsSinceEpoch,
      'products': products.map((e) => e.toJson()).toList(),
      'sales': sales.where((s) => !s.isCancelled).map((e) => e.toJson()).toList(),
      'paymentCards': cards.map((e) => e.toJson()).toList(),
      'debts': debts.map((e) => e.toJson()).toList(),
      'reservations': reservations.map((e) => e.toJson()).toList(),
      if (order.isNotEmpty) 'categoryOrder': order,
      if (categoryDisplayNames.isNotEmpty) 'categoryDisplayNames': categoryDisplayNames,
    };
    return const JsonEncoder.withIndent('  ').convert(backup);
  }

  Future<List<Debt>> getAllDebts() async {
    final db = await _db;
    final maps = await db.query('debts', orderBy: 'date DESC');
    return maps.map((m) => Debt.fromMap(m)).toList();
  }

  Future<List<Reservation>> getAllReservations() async {
    final db = await _db;
    final maps = await db.query('reservations', orderBy: 'expirationDate ASC');
    return maps.map((m) => Reservation.fromMap(m)).toList();
  }

  Future<List<PaymentCard>> getAllPaymentCards() async {
    final db = await _db;
    final maps = await db.query('payment_cards');
    return maps.map((m) => PaymentCard.fromMap(m)).toList();
  }

  Future<List<PaymentCard>> getActivePaymentCards() async {
    final db = await _db;
    final maps = await db.query(
      'payment_cards',
      where: 'isArchived = ?',
      whereArgs: [0],
    );
    return maps.map((m) => PaymentCard.fromMap(m)).toList();
  }

  Future<PaymentCard> insertPaymentCard(String label) async {
    final db = await _db;
    final trimmed = label.trim();
    if (trimmed.isEmpty) throw Exception('Метка не может быть пустой');
    final now = DateTime.now().millisecondsSinceEpoch;
    final card = PaymentCard(label: trimmed, createdAt: now);
    final rowId = await db.insert('payment_cards', card.toInsertMap());
    return card.copyWith(id: rowId);
  }

  Future<void> updatePaymentCard(PaymentCard card) async {
    final db = await _db;
    await db.update('payment_cards', card.toMap(), where: 'id = ?', whereArgs: [card.id]);
  }

  Future<void> deletePaymentCard(int cardId) async {
    final db = await _db;
    await db.delete('payment_cards', where: 'id = ?', whereArgs: [cardId]);
  }

  /// Загружает тестовые данные в БД (только если БД пустая)
  Future<void> seedTestData() async {
    final db = await _db;
    final count = await db.rawQuery('SELECT COUNT(*) as c FROM products');
    if (parseInt(count.first['c'], 0) > 0) return; // БД не пустая

    final testProducts = [
      Product(
        brand: 'PODONKI CRITICAL 50 mg',
        flavor: 'АПЕЛЬСИНОВОЕ ДРАЖЕ',
        barcode: '4617586894280',
        purchasePrice: 8.0,
        retailPrice: 15.0,
        stock: 5,
        category: 'liquid',
        strength: '50',
      ),
      Product(
        brand: 'PODONKI CRITICAL 50 mg',
        flavor: 'МАНГО АПЕЛЬСИН',
        barcode: '4617586894372',
        purchasePrice: 8.0,
        retailPrice: 15.0,
        stock: 3,
        category: 'liquid',
        strength: '50',
      ),
      Product(
        brand: 'PODONKI CRITICAL 50 mg',
        flavor: 'КИСЛЫЙ МАЛИНОВЫЙ ЛИМОНАД',
        barcode: '4617586894297',
        purchasePrice: 8.0,
        retailPrice: 15.0,
        stock: 2,
        category: 'liquid',
        strength: '50',
      ),
      Product(
        brand: 'PODONKI SOUR 50 mg',
        flavor: 'МАЛИНОВЫЙ ЛИМОНАД',
        barcode: '4617586894303',
        purchasePrice: 8.0,
        retailPrice: 15.0,
        stock: 4,
        category: 'liquid',
        strength: '50',
      ),
      Product(
        brand: 'Испаритель aegis boost 0.2 (50-58W)',
        flavor: 'Испаритель aegis boost 0.2 (50-58W)',
        barcode: '6941291559744',
        purchasePrice: 6.4,
        retailPrice: 13.0,
        stock: 10,
        category: 'consumable',
        specification: '0.2',
      ),
      Product(
        brand: 'XROS 5 MINI',
        flavor: 'PURPLE',
        barcode: '6943498636129',
        purchasePrice: 40.0,
        retailPrice: 65.0,
        stock: 2,
        category: 'vape',
        specification: 'PURPLE',
      ),
      Product(
        brand: 'XROS 5 MINI',
        flavor: 'PASTEL CRYSTAL',
        barcode: '6943498636075',
        purchasePrice: 40.0,
        retailPrice: 65.0,
        stock: 1,
        category: 'vape',
        specification: 'PASTEL CRYSTAL',
      ),
    ];

    final batch = db.batch();
    for (final product in testProducts) {
      batch.insert('products', product.toInsertMap());
    }
    await batch.commit(noResult: true);
  }

  // ========== Долги ==========
  Future<List<Debt>> getAllActiveDebts() async {
    final db = await _db;
    final maps = await db.query(
      'debts',
      where: 'isPaid = ?',
      whereArgs: [0],
      orderBy: 'date DESC',
    );
    return maps.map((m) => Debt.fromMap(m)).toList();
  }

  Future<void> insertDebt(Debt debt) async {
    try {
      final db = await _db;
      await db.insert('debts', debt.toInsertMap());
      developer.log('Debt inserted: customer=${debt.customerName}, total=${debt.totalAmount}', name: 'VapeRepository');
    } catch (e, st) {
      developer.log('insertDebt failed: $e', name: 'VapeRepository');
      developer.log('Stack: $st', name: 'VapeRepository');
      rethrow;
    }
  }

  Future<Debt?> getDebtById(int id) async {
    final db = await _db;
    final maps = await db.query('debts', where: 'id = ?', whereArgs: [id]);
    if (maps.isEmpty) return null;
    return Debt.fromMap(maps.first);
  }

  /// Удаление долга: возврат товаров на склад, удаление из БД
  Future<bool> cancelDebt(int debtId) async {
    final db = await _db;
    final debt = await getDebtById(debtId);
    if (debt == null) return false;

    try {
      if (debt.stockDeducted) {
        final items = (jsonDecode(debt.products) as List<dynamic>)
            .map((e) => e as Map)
            .map((m) => {
                  'productId': m['productId'] as int,
                  'quantity': m['quantity'] as int,
                })
            .toList();

        for (final item in items) {
          final productId = item['productId'] as int;
          final quantity = item['quantity'] as int;
          if (productId > 0) {
            for (int i = 0; i < quantity; i++) {
              await increaseStock(productId);
            }
          }
        }
      }

      await db.delete('debts', where: 'id = ?', whereArgs: [debtId]);

      return true;
    } catch (e) {
      developer.log('cancelDebt error: $e', name: 'VapeRepository');
      return false;
    }
  }

  Future<bool> payDebt(
    int debtId, {
    double? amount,
    String paymentMethod = 'cash',
    double? cashAmount,
    double? cardAmount,
    int? cardId,
  }) async {
    final db = await _db;
    final debt = await getDebtById(debtId);
    if (debt == null || debt.isPaid) return false;

    try {
      final remaining = debt.remainingAmount;
      final payAmount = amount ?? remaining;
      if (payAmount <= 0) return false;
      if (payAmount > remaining + 0.01) return false;

      final newPaidAmount = debt.paidAmount + payAmount;
      final isNowFullyPaid = (debt.totalAmount - newPaidAmount) <= 0.01;

      // Частичная оплата: просто увеличиваем paidAmount, продажи создаём только при полном погашении.
      if (!isNowFullyPaid) {
        await db.update(
          'debts',
          {'paidAmount': newPaidAmount},
          where: 'id = ?',
          whereArgs: [debtId],
        );
        return true;
      }

      // Парсим JSON с товарами долга
      final items = (jsonDecode(debt.products) as List<dynamic>)
          .map((e) => e as Map)
          .map((m) => {
                'productId': m['productId'] as int,
                'quantity': m['quantity'] as int,
              })
          .toList();

      final now = DateTime.now().millisecondsSinceEpoch;

      final effectivePaymentMethod = paymentMethod;
      final effectiveCashAmount = cashAmount;
      final effectiveCardAmount = cardAmount;
      final effectiveCardId = cardId;

      // Создаём продажу для каждого товара
      for (final item in items) {
        final productId = item['productId'] as int;
        final quantity = item['quantity'] as int;
        final product = await getProductById(productId);
        if (product == null) continue;

        // Если склад ещё не был списан при создании долга — списываем сейчас
        if (!debt.stockDeducted) {
          if (product.stock < quantity) return false;
          await updateProduct(product.copyWith(stock: product.stock - quantity));
        }

        final revenue = product.retailPrice * quantity;
        final profit = (product.retailPrice - product.purchasePrice) * quantity;

        // Для split payment распределяем пропорционально
        double? itemCashAmount;
        double? itemCardAmount;
        if (effectiveCashAmount != null && effectiveCardAmount != null && debt.totalAmount > 0) {
          final ratio = revenue / debt.totalAmount;
          itemCashAmount = effectiveCashAmount * ratio;
          itemCardAmount = effectiveCardAmount * ratio;
        } else if (effectiveCashAmount != null && debt.totalAmount > 0) {
          itemCashAmount = revenue;
        } else if (effectiveCardAmount != null && debt.totalAmount > 0) {
          itemCardAmount = revenue;
        }

        await db.insert('sales', {
          'productId': productId,
          'date': now,
          'revenue': revenue,
          'profit': profit,
          'quantity': quantity,
          'paymentMethod': effectivePaymentMethod,
          'cashAmount': itemCashAmount,
          'cardAmount': itemCardAmount,
          'cardId': effectiveCardId,
          'sourceType': 'debt',
          'sourceId': debtId,
          'comment': 'Оплата долга #${debt.id}',
        });
      }

      // Помечаем долг как оплаченный
      await db.update(
        'debts',
        {'isPaid': 1, 'paidAmount': debt.totalAmount},
        where: 'id = ?',
        whereArgs: [debtId],
      );

      return true;
    } catch (e) {
      developer.log('payDebt error: $e', name: 'VapeRepository');
      return false;
    }
  }

  // ========== Резервы ==========
  Future<List<Reservation>> getAllActiveReservations() async {
    final db = await _db;
    final maps = await db.query(
      'reservations',
      where: 'isSold = ?',
      whereArgs: [0],
      orderBy: 'expirationDate ASC',
    );
    return maps.map((m) => Reservation.fromMap(m)).toList();
  }

  Future<void> insertReservation(Reservation reservation) async {
    try {
      final db = await _db;
      await db.insert('reservations', reservation.toInsertMap());
      developer.log('Reservation inserted: ${reservation.customerName}, productId=${reservation.productId}', name: 'VapeRepository');
    } catch (e, st) {
      developer.log('insertReservation failed: $e', name: 'VapeRepository');
      developer.log('Stack: $st', name: 'VapeRepository');
      rethrow;
    }
  }

  Future<Reservation?> getReservationById(int id) async {
    final db = await _db;
    final maps = await db.query('reservations', where: 'id = ?', whereArgs: [id]);
    if (maps.isEmpty) return null;
    return Reservation.fromMap(maps.first);
  }

  Future<List<Reservation>> getReservationsForProduct(int productId) async {
    final now = DateTime.now().millisecondsSinceEpoch;
    final db = await _db;
    final maps = await db.query(
      'reservations',
      where: 'productId = ? AND isSold = ?',
      whereArgs: [productId, 0],
    );
    return maps
        .map((m) => Reservation.fromMap(m))
        .where((r) => r.expirationDate > now)
        .toList();
  }

  /// Возвращает Map: productId -> зарезервированное количество (только активные, не проданные и не истекшие)
  /// Истекшие резервы должны предварительно очищены через returnExpiredReservations()
  Future<Map<int, int>> getReservedQuantityByProduct() async {
    final now = DateTime.now().millisecondsSinceEpoch;
    final reservations = await getAllActiveReservations();
    // Фильтруем только те, у которых срок ещё не истёк
    final activeReservations = reservations.where((r) => r.expirationDate > now);
    final Map<int, int> result = {};
    for (final r in activeReservations) {
      result[r.productId] = (result[r.productId] ?? 0) + r.quantity;
    }
    return result;
  }

  Future<bool> sellReservation(
    int reservationId, {
    String paymentMethod = 'cash',
    double? cashAmount,
    double? cardAmount,
    int? cardId,
    double discount = 0,
  }) async {
    final db = await _db;
    final reservation = await getReservationById(reservationId);
    if (reservation == null || reservation.isSold) return false;

    try {
      final product = await getProductById(reservation.productId);
      if (product == null) return false;
      if (product.stock < reservation.quantity) return false;

      for (int i = 0; i < reservation.quantity; i++) {
        await decreaseStock(reservation.productId);
      }

      await db.update(
        'reservations',
        {'isSold': 1},
        where: 'id = ?',
        whereArgs: [reservationId],
      );

      final now = DateTime.now().millisecondsSinceEpoch;
      final revenue = product.retailPrice * reservation.quantity - discount;
      final profit = (product.retailPrice - product.purchasePrice) * reservation.quantity - discount;

      await db.insert('sales', {
        'productId': reservation.productId,
        'date': now,
        'discount': discount,
        'revenue': revenue,
        'profit': profit,
        'quantity': reservation.quantity,
        'paymentMethod': paymentMethod,
        'cashAmount': cashAmount,
        'cardAmount': cardAmount,
        'cardId': cardId,
        'sourceType': 'reservation',
        'sourceId': reservationId,
      });

      return true;
    } catch (e) {
      return false;
    }
  }

  Future<void> updateReservation(Reservation reservation) async {
    final db = await _db;
    await db.update(
      'reservations',
      reservation.toMap(),
      where: 'id = ?',
      whereArgs: [reservation.id],
    );
  }

  Future<int> returnExpiredReservations() async {
    final db = await _db;
    final now = DateTime.now().millisecondsSinceEpoch;
    final expired = await db.query(
      'reservations',
      where: 'isSold = ? AND expirationDate < ?',
      whereArgs: [0, now],
    );
    // Помечаем как проданные (чтобы они не показывались в активных)
    // В реальности можно вернуть товар в свободную продажу
    int count = 0;
    for (final map in expired) {
      await db.update(
        'reservations',
        {'isSold': 1},
        where: 'id = ?',
        whereArgs: [map['id']],
      );
      count++;
    }
    return count;
  }
}
