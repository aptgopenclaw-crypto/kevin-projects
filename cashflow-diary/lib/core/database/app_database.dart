import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart' as p;

import '../../models/transaction.dart' as model;
import '../../models/category.dart' as model;
import '../../models/budget.dart' as model;

class AppDatabase {
  static Database? _database;
  static final AppDatabase instance = AppDatabase._internal();

  AppDatabase._internal();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = p.join(dbPath, 'cashflow_diary.db');

    return await openDatabase(
      path,
      version: 1,
      onCreate: _onCreate,
      onConfigure: _onConfigure,
    );
  }

  Future<void> _onConfigure(Database db) async {
    await db.execute('PRAGMA foreign_keys = ON');
  }

  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE categories (
        id INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        icon_code TEXT NOT NULL,
        sort_order INTEGER DEFAULT 0,
        is_system INTEGER DEFAULT 0
      )
    ''');

    await db.execute('''
      CREATE TABLE transactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        amount REAL NOT NULL,
        category_id INTEGER NOT NULL,
        note TEXT,
        tags TEXT,
        timestamp INTEGER NOT NULL,
        currency TEXT DEFAULT 'TWD',
        FOREIGN KEY (category_id) REFERENCES categories(id)
      )
    ''');

    await db.execute('CREATE INDEX idx_transactions_timestamp ON transactions(timestamp)');
    await db.execute('CREATE INDEX idx_transactions_category ON transactions(category_id)');

    await db.execute('''
      CREATE TABLE budgets (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        month TEXT NOT NULL UNIQUE,
        amount REAL NOT NULL
      )
    ''');

    await db.execute('CREATE INDEX idx_budgets_month ON budgets(month)');

    await db.execute('''
      CREATE TABLE app_settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      )
    ''');

    await _seedDefaultCategories(db);
    await db.insert('app_settings', {'key': 'default_currency', 'value': 'TWD'});
  }

  Future<void> _seedDefaultCategories(Database db) async {
    const categories = [
      {'id': 1, 'name': '餐飲', 'icon_code': 'restaurant', 'sort_order': 1, 'is_system': 1},
      {'id': 2, 'name': '交通', 'icon_code': 'directions_bus', 'sort_order': 2, 'is_system': 1},
      {'id': 3, 'name': '日常用品', 'icon_code': 'shopping_cart', 'sort_order': 3, 'is_system': 1},
      {'id': 4, 'name': '購物', 'icon_code': 'shopping_bag', 'sort_order': 4, 'is_system': 1},
      {'id': 5, 'name': '娛樂', 'icon_code': 'movie', 'sort_order': 5, 'is_system': 1},
      {'id': 6, 'name': '通訊', 'icon_code': 'phone_android', 'sort_order': 6, 'is_system': 1},
      {'id': 7, 'name': '醫療', 'icon_code': 'local_hospital', 'sort_order': 7, 'is_system': 1},
      {'id': 8, 'name': '居住', 'icon_code': 'home', 'sort_order': 8, 'is_system': 1},
      {'id': 9, 'name': '教育', 'icon_code': 'school', 'sort_order': 9, 'is_system': 1},
      {'id': 10, 'name': '人情', 'icon_code': 'card_giftcard', 'sort_order': 10, 'is_system': 1},
      {'id': 11, 'name': '服飾', 'icon_code': 'checkroom', 'sort_order': 11, 'is_system': 1},
      {'id': 12, 'name': '美容', 'icon_code': 'spa', 'sort_order': 12, 'is_system': 1},
      {'id': 13, 'name': '寵物', 'icon_code': 'pets', 'sort_order': 13, 'is_system': 1},
      {'id': 14, 'name': '運動', 'icon_code': 'fitness_center', 'sort_order': 14, 'is_system': 1},
      {'id': 15, 'name': '旅遊', 'icon_code': 'flight', 'sort_order': 15, 'is_system': 1},
      {'id': 16, 'name': '保險', 'icon_code': 'security', 'sort_order': 16, 'is_system': 1},
      {'id': 17, 'name': '稅務', 'icon_code': 'receipt_long', 'sort_order': 17, 'is_system': 1},
      {'id': 18, 'name': '投資', 'icon_code': 'trending_up', 'sort_order': 18, 'is_system': 1},
      {'id': 19, 'name': '其他', 'icon_code': 'more_horiz', 'sort_order': 99, 'is_system': 1},
    ];
    for (final c in categories) {
      await db.insert('categories', c);
    }
  }

  // ==================== Transaction Methods ====================

  Future<int> insertTransaction(model.Transaction t) async {
    final db = await database;
    return await db.insert('transactions', t.toMap());
  }

  Future<int> updateTransaction(model.Transaction t) async {
    final db = await database;
    return await db.update('transactions', t.toMap(),
        where: 'id = ?', whereArgs: [t.id]);
  }

  Future<int> deleteTransaction(int id) async {
    final db = await database;
    return await db.delete('transactions', where: 'id = ?', whereArgs: [id]);
  }

  Future<model.Transaction?> getTransaction(int id) async {
    final db = await database;
    final rows = await db.query('transactions', where: 'id = ?', whereArgs: [id]);
    if (rows.isEmpty) return null;
    return model.Transaction.fromMap(rows.first);
  }

  Future<List<model.Transaction>> getAllTransactions() async {
    final db = await database;
    final rows = await db.query('transactions', orderBy: 'timestamp DESC');
    return rows.map((r) => model.Transaction.fromMap(r)).toList();
  }

  Future<List<model.Transaction>> getTransactionsBetween(int start, int end) async {
    final db = await database;
    final rows = await db.query(
      'transactions',
      where: 'timestamp BETWEEN ? AND ?',
      whereArgs: [start, end],
      orderBy: 'timestamp DESC',
    );
    return rows.map((r) => model.Transaction.fromMap(r)).toList();
  }

  Future<List<model.Transaction>> searchByKeyword(String keyword) async {
    final db = await database;
    final rows = await db.query(
      'transactions',
      where: 'note LIKE ?',
      whereArgs: ['%$keyword%'],
      orderBy: 'timestamp DESC',
    );
    return rows.map((r) => model.Transaction.fromMap(r)).toList();
  }

  Future<List<Map<String, dynamic>>> getDailyTotals(int start, int end) async {
    final db = await database;
    return await db.rawQuery(
      'SELECT (timestamp / 86400) * 86400 AS day, SUM(amount) AS total '
      'FROM transactions WHERE timestamp BETWEEN ? AND ? '
      'GROUP BY day ORDER BY day ASC',
      [start, end],
    );
  }

  Future<List<Map<String, dynamic>>> getCategoryTotals(int start, int end) async {
    final db = await database;
    return await db.rawQuery(
      'SELECT t.category_id, c.name, c.icon_code, SUM(t.amount) AS total '
      'FROM transactions t JOIN categories c ON t.category_id = c.id '
      'WHERE t.timestamp BETWEEN ? AND ? '
      'GROUP BY t.category_id ORDER BY total DESC',
      [start, end],
    );
  }

  Future<double> getTotalBetween(int start, int end) async {
    final db = await database;
    final result = await db.rawQuery(
      'SELECT COALESCE(SUM(amount), 0) AS total '
      'FROM transactions WHERE timestamp BETWEEN ? AND ?',
      [start, end],
    );
    return (result.first['total'] as num).toDouble();
  }

  // ==================== Category Methods ====================

  Future<int> insertCategory(model.Category c) async {
    final db = await database;
    return await db.insert('categories', c.toMap());
  }

  Future<int> updateCategory(model.Category c) async {
    final db = await database;
    return await db.update('categories', c.toMap(),
        where: 'id = ?', whereArgs: [c.id]);
  }

  Future<int> deleteCategory(int id) async {
    final db = await database;
    return await db.delete('categories', where: 'id = ?', whereArgs: [id]);
  }

  Future<List<model.Category>> getAllCategories() async {
    final db = await database;
    final rows = await db.query('categories', orderBy: 'sort_order ASC');
    return rows.map((r) => model.Category.fromMap(r)).toList();
  }

  Future<model.Category?> getCategory(int id) async {
    final db = await database;
    final rows = await db.query('categories', where: 'id = ?', whereArgs: [id]);
    if (rows.isEmpty) return null;
    return model.Category.fromMap(rows.first);
  }

  // ==================== Budget Methods ====================

  Future<int> setBudget(model.Budget b) async {
    final db = await database;
    final existing = await db.query('budgets', where: 'month = ?', whereArgs: [b.month]);
    if (existing.isNotEmpty) {
      return await db.update('budgets', b.toMap(),
          where: 'month = ?', whereArgs: [b.month]);
    }
    return await db.insert('budgets', b.toMap());
  }

  Future<model.Budget?> getBudget(String month) async {
    final db = await database;
    final rows = await db.query('budgets', where: 'month = ?', whereArgs: [month]);
    if (rows.isEmpty) return null;
    return model.Budget.fromMap(rows.first);
  }

  // ==================== Settings Methods ====================

  Future<void> setString(String key, String value) async {
    final db = await database;
    await db.insert('app_settings', {'key': key, 'value': value},
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<String?> getString(String key) async {
    final db = await database;
    final rows = await db.query('app_settings', where: 'key = ?', whereArgs: [key]);
    if (rows.isEmpty) return null;
    return rows.first['value'] as String;
  }
}
