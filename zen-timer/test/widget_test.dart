import 'package:flutter_test/flutter_test.dart';
import 'package:zen_timer/main.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const ZenTimerApp());
    expect(find.text('ZenTimer'), findsOneWidget);
  });
}
