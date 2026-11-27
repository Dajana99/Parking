import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'api.dart';
import 'auth.dart';
import 'screens/auth_screens.dart';
import 'screens/home_screen.dart';

void main() => runApp(const SmartParkApp());

class SmartParkApp extends StatelessWidget {
  const SmartParkApp({super.key});

  @override
  Widget build(BuildContext context) {
    final api = ApiClient();

    return MultiProvider(
      providers: [
        Provider.value(value: api),
        ChangeNotifierProvider(create: (_) => AuthProvider(api)..init()),
      ],
      child: MaterialApp(
        title: 'SmartPark',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
        ),
        home: const AuthGate(),
      ),
    );
  }
}

// biranje ekrana -> prijava -> pocetni ekran
class AuthGate extends StatelessWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    if (auth.loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return auth.user == null ? const LoginScreen() : const HomeScreen();
  }
}
