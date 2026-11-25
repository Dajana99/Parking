import 'package:flutter_test/flutter_test.dart';
import 'package:smartpark_app/main.dart';

void main() {
  testWidgets('Aplikacija se pokrece', (WidgetTester tester) async {
    await tester.pumpWidget(const SmartParkApp());
    expect(find.text('SmartPark'), findsOneWidget);
  });
}
