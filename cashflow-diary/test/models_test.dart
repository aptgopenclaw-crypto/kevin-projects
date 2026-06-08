import 'package:flutter_test/flutter_test.dart';
import 'package:cashflow_diary/models/transaction.dart';
import 'package:cashflow_diary/models/category.dart';
import 'package:cashflow_diary/models/budget.dart';

void main() {
  group('Transaction Model', () {
    test('should create from map and convert to map', () {
      final transaction = Transaction(
        id: 1,
        amount: 150.0,
        categoryId: 1,
        note: '午餐',
        tags: '公司報帳',
        timestamp: 1700000000,
        currency: 'TWD',
      );

      final map = transaction.toMap();
      expect(map['amount'], 150.0);
      expect(map['category_id'], 1);
      expect(map['note'], '午餐');

      final fromMap = Transaction.fromMap(map);
      expect(fromMap.amount, 150.0);
      expect(fromMap.note, '午餐');
    });

    test('should copy with new values', () {
      final original = Transaction(
        amount: 100.0,
        categoryId: 1,
        timestamp: 1700000000,
      );

      final modified = original.copyWith(amount: 200.0, note: '修改後');
      expect(modified.amount, 200.0);
      expect(modified.note, '修改後');
      expect(modified.categoryId, 1); // unchanged
    });
  });

  group('Category Model', () {
    test('should correctly identify system categories', () {
      final systemCat = Category(
        id: 1, name: '餐飲', iconCode: 'restaurant',
        sortOrder: 1, isSystem: true,
      );
      final customCat = Category(
        id: 99, name: '自訂', iconCode: 'star',
        sortOrder: 99, isSystem: false,
      );

      expect(systemCat.isSystem, true);
      expect(customCat.isSystem, false);
    });
  });

  group('Budget Model', () {
    test('should store month in YYYY-MM format', () {
      final budget = Budget(month: '2026-06', amount: 15000);
      expect(budget.month, '2026-06');
      expect(budget.amount, 15000);
    });
  });
}
