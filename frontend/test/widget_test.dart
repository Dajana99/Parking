import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:smartpark_app/main.dart';

void main() {
  // U testovima nema pravog skladista - koristimo prazno lazno.
  setUp(() => FlutterSecureStorage.setMockInitialValues({}));

  testWidgets('Bez sacuvanog tokena aplikacija otvara prijavu', (tester) async {
    await tester.pumpWidget(const SmartParkApp());
    await tester.pumpAndSettle();
    expect(find.text('Prijavi se'), findsOneWidget);
  });
}
