import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cashflow_diary/app.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const CashFlowDiaryApp());
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
