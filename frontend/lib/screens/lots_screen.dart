import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api.dart';
import '../auth.dart';
import '../parking.dart';

// LISTA LOKACIJA

class LotsScreen extends StatefulWidget {
  const LotsScreen({super.key});

  @override
  State<LotsScreen> createState() => _LotsScreenState();
}

class _LotsScreenState extends State<LotsScreen> {
  late Future<List<Lot>> _lots;

  @override
  void initState() {
    super.initState();
    _lots = _load();
  }

  Future<List<Lot>> _load() => ParkingApi(context.read<ApiClient>()).lots();

  Future<void> _refresh() async {
    setState(() => _lots = _load());
    await _lots;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Lokacije')),
      body: FutureBuilder(
        future: _lots,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return ErrorView(snapshot.error!, onRetry: _refresh);
          }
          final lots = snapshot.data!;
          if (lots.isEmpty) {
            return const Center(child: Text('Još nema nijedne lokacije.'));
          }
          return RefreshIndicator(
            onRefresh: _refresh,
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [for (final lot in lots) _LotCard(lot)],
            ),
          );
        },
      ),
    );
  }
}

class _LotCard extends StatelessWidget {
  const _LotCard(this.lot);
  final Lot lot;

  @override
  Widget build(BuildContext context) {
    final hasFree = lot.freeSpots > 0;
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        leading: const Icon(Icons.local_parking, size: 36),
        title: Text(lot.name, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text('${lot.address}\n'
            'Slobodnih mesta: ${lot.freeSpots} od ${lot.totalSpots}'),
        isThreeLine: true,
        trailing: Text('${lot.freeSpots}',
            style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: hasFree ? Colors.green : Colors.red)),
        onTap: () => Navigator.push(context,
            MaterialPageRoute(builder: (_) => LotDetailScreen(lot: lot))),
      ),
    );
  }
}

// DETALJI LOKACIJE
// mesta sa statusom + cenovnik

class LotDetailScreen extends StatefulWidget {
  const LotDetailScreen({super.key, required this.lot});
  final Lot lot;

  @override
  State<LotDetailScreen> createState() => _LotDetailScreenState();
}

class _LotDetailScreenState extends State<LotDetailScreen> {
  late Future<(List<Spot>, PriceList?)> _data;

  @override
  void initState() {
    super.initState();
    _data = _load();
  }

  // ucitava mesta i cenovnik lokacije
  Future<(List<Spot>, PriceList?)> _load() async {
    final api = ParkingApi(context.read<ApiClient>());
    final spots = await api.spots(widget.lot.id);
    final priceList = await api.priceList(widget.lot.id);
    return (spots, priceList);
  }

  Future<void> _refresh() async {
    setState(() => _data = _load());
    await _data;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.lot.name)),
      body: FutureBuilder(
        future: _data,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return ErrorView(snapshot.error!, onRetry: _refresh);
          }
          final (spots, priceList) = snapshot.data!;
          return RefreshIndicator(
            onRefresh: _refresh,
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Text(widget.lot.address),
                const SizedBox(height: 16),
                _PriceCard(priceList),
                const SizedBox(height: 24),
                Text('Mesta (${spots.length})',
                    style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                const _Legend(),
                const SizedBox(height: 12),
                if (spots.isEmpty)
                  const Text('Na ovoj lokaciji još nema mesta.')
                else
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [for (final s in spots) _SpotTile(s)],
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _PriceCard extends StatelessWidget {
  const _PriceCard(this.priceList);
  final PriceList? priceList;

  String _din(double v) =>
      '${v == v.roundToDouble() ? v.toStringAsFixed(0) : v.toStringAsFixed(2)} din';

  @override
  Widget build(BuildContext context) {
    final p = priceList;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: p == null
            ? const Text('Cenovnik za ovu lokaciju još nije definisan.')
            : Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Cenovnik', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  Text('Cena po satu: ${_din(p.pricePerHour)}'),
                  Text('Minimalna naplata: ${_din(p.minCharge)}'),
                  Text('Kazna za prekoračenje: ${_din(p.overstayFine)}'),
                  Text('Kazna za neiskorišćenu rezervaciju: ${_din(p.noShowFine)}'),
                ],
              ),
      ),
    );
  }
}

// boja mesta na osnovu statusa
Color spotColor(SpotStatus s) => switch (s) {
      SpotStatus.slobodno => Colors.green,
      SpotStatus.rezervisano => Colors.orange,
      SpotStatus.zauzeto => Colors.red,
      SpotStatus.vanUpotrebe => Colors.grey,
    };

class _SpotTile extends StatelessWidget {
  const _SpotTile(this.spot);
  final Spot spot;

  @override
  Widget build(BuildContext context) {
    final reason = spot.outOfServiceReason;
    return InkWell(
      // klik prikazuje status mesta
      onTap: () => ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text('Mesto ${spot.spotNumber}: ${spot.status.label}'
            '${reason != null ? ' – $reason' : ''}'),
      )),
      child: Container(
        width: 64,
        height: 48,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: spotColor(spot.status),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Text(spot.spotNumber,
            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
    );
  }
}

class _Legend extends StatelessWidget {
  const _Legend();

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 16,
      runSpacing: 4,
      children: [
        for (final s in SpotStatus.values)
          Row(mainAxisSize: MainAxisSize.min, children: [
            Container(width: 12, height: 12, color: spotColor(s)),
            const SizedBox(width: 4),
            Text(s.label),
          ]),
      ],
    );
  }
}

// GRESKA

// prikazuje poruku greske i dugme "Pokušaj ponovo"
// aako je sesija istekla nudi ponovnu prijavu
class ErrorView extends StatelessWidget {
  const ErrorView(this.error, {super.key, required this.onRetry});
  final Object error;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final expired = error is ApiException && (error as ApiException).statusCode == 401;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48),
            const SizedBox(height: 12),
            Text(error is ApiException ? error.toString() : 'Neočekivana greška.',
                textAlign: TextAlign.center),
            const SizedBox(height: 16),
            if (expired)
              FilledButton(
                onPressed: () {
                  final auth = context.read<AuthProvider>();
                  Navigator.popUntil(context, (route) => route.isFirst);
                  auth.logout();
                },
                child: const Text('Prijavi se ponovo'),
              )
            else
              FilledButton(onPressed: onRetry, child: const Text('Pokušaj ponovo')),
          ],
        ),
      ),
    );
  }
}
