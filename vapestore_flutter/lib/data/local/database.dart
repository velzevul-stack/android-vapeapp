import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/payment_card.dart';
import '../models/product.dart';
import '../models/sale.dart';

class AppDatabase {
  static const _version = 7;
  static const _dbName = 'vape_database.db';

  static Database? _db;

  static Future<Database> get instance async {
    if (_db != null) return _db!;
    _db = await _init();
    return _db!;
  }

  static Future<Database> _init() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, _dbName);
    return openDatabase(
      path,
      version: _version,
      onCreate: _onCreate,
      onUpgrade: _onUpgrade,
    );
  }

  static Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE products (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        brand TEXT NOT NULL,
        flavor TEXT NOT NULL,
        barcode TEXT,
        purchasePrice REAL NOT NULL,
        retailPrice REAL NOT NULL,
        stock INTEGER NOT NULL DEFAULT 0,
        category TEXT NOT NULL DEFAULT 'liquid',
        strength TEXT NOT NULL DEFAULT '',
        specification TEXT NOT NULL DEFAULT '',
        orderIndex INTEGER NOT NULL DEFAULT 0
      )
    ''');
    await db.execute('''
      CREATE TABLE sales (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        productId INTEGER NOT NULL,
        date INTEGER NOT NULL,
        comment TEXT,
        discount REAL NOT NULL DEFAULT 0,
        revenue REAL NOT NULL,
        profit REAL NOT NULL,
        quantity INTEGER NOT NULL DEFAULT 1,
        paymentMethod TEXT NOT NULL DEFAULT 'cash',
        cashAmount REAL,
        cardAmount REAL,
        cardId INTEGER,
        isCancelled INTEGER NOT NULL DEFAULT 0,
        originalSaleId INTEGER,
        sourceType TEXT,
        sourceId INTEGER
      )
    ''');
    await db.execute('''
      CREATE TABLE debts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        customerName TEXT NOT NULL,
        products TEXT NOT NULL,
        date INTEGER NOT NULL,
        totalAmount REAL NOT NULL,
        isPaid INTEGER NOT NULL DEFAULT 0,
        stockDeducted INTEGER NOT NULL DEFAULT 0
      )
    ''');
    await db.execute('''
      CREATE TABLE reservations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        customerName TEXT NOT NULL,
        productId INTEGER NOT NULL,
        quantity INTEGER NOT NULL,
        reservationDate INTEGER NOT NULL,
        expirationDate INTEGER NOT NULL,
        isSold INTEGER NOT NULL DEFAULT 0
      )
    ''');
    await db.execute('''
      CREATE TABLE payment_cards (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        label TEXT NOT NULL,
        isArchived INTEGER NOT NULL DEFAULT 0,
        createdAt INTEGER NOT NULL
      )
    ''');
  }

  static Future<void> _onUpgrade(Database db, int oldV, int newV) async {
    // Migrations if needed - schema matches v7 from Android
  }

  static Future<void> close() async {
    if (_db != null) {
      await _db!.close();
      _db = null;
    }
  }
}
