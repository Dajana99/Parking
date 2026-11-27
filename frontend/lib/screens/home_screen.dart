import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../auth.dart';

// pocetni ekran - prikazuje funkcije u zavisnosti od uloge
// za sada pored svega pise uskoro
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  static const _menu = {
    UserRole.vozac: [
      (Icons.local_parking, 'Lokacije i mesta'),
      (Icons.event_available, 'Moje rezervacije'),
      (Icons.receipt_long, 'Istorija i kazne'),
      (Icons.card_membership, 'Pretplate'),
      (Icons.person_outline, 'Profil'),
    ],
    UserRole.operater: [
      (Icons.monitor_heart_outlined, 'Zauzetost'),
      (Icons.warning_amber, 'Upozorenja'),
      (Icons.build_outlined, 'Status mesta'),
      (Icons.gavel, 'Kazne'),
    ],
    UserRole.administrator: [
      (Icons.location_city, 'Lokacije i mesta'),
      (Icons.price_change_outlined, 'Cenovnici i pretplate'),
      (Icons.manage_accounts_outlined, 'Nalozi i uloge'),
      (Icons.bar_chart, 'Izveštaji'),
    ],
  };

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final user = auth.user!;

    return Scaffold(
      appBar: AppBar(
        title: Text('SmartPark – ${user.role.label}'),
        actions: [
          IconButton(
            tooltip: 'Odjava',
            icon: const Icon(Icons.logout),
            onPressed: auth.logout,
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Zdravo, ${user.name}!',
              style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 16),
          for (final (icon, title) in _menu[user.role]!)
            Card(
              child: ListTile(
                leading: Icon(icon),
                title: Text(title),
                trailing: const Text('uskoro'),
              ),
            ),
        ],
      ),
    );
  }
}
