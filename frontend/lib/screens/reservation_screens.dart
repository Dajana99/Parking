import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api.dart';
import '../parking.dart';
import 'lots_screen.dart';

// NOVA REZERVACIJA

class NewReservationScreen extends StatefulWidget {
  const NewReservationScreen({super.key, required this.lot});
  final Lot lot;

  @override
  State<NewReservationScreen> createState() => _NewReservationScreenState();
}

class _NewReservationScreenState extends State<NewReservationScreen> {
  static const _durations = [30, 60, 90, 120, 180, 240, 480, 720, 1440];

  final _formKey = GlobalKey<FormState>();
  final _plate = TextEditingController();

  late DateTime _start = _now();
  int _minutes = 60;
  DateTime get _end => _start.add(Duration(minutes: _minutes));

  List<Spot>? _spots; // mesta sa availableInInterval za izabrani termin
  bool _loadingSpots = true;
  int? _selectedSpotId;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchSpots();
  }

  // podrazumevani pocetak
  static DateTime _now() {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day, now.hour, now.minute);
  }

  // dugme "Odmah" - pocetak je trenutno vreme
  void _startNow() {
    _start = _now();
    _loadSpots();
  }

  // poziva se kada se promeni tremin
  void _loadSpots() {
    setState(() {
      _loadingSpots = true;
      _selectedSpotId = null;
      _error = null;
    });
    _fetchSpots();
  }

  // proveravamo koja su mesta slobonda
  Future<void> _fetchSpots() async {
    List<Spot>? spots;
    String? error;
    try {
      spots = await ParkingApi(context.read<ApiClient>())
          .spots(widget.lot.id, from: _start, to: _end);
    } on ApiException catch (e) {
      error = e.message;
    }
    if (!mounted) return;
    setState(() {
      _spots = spots;
      _error = error;
      _loadingSpots = false;
    });
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: _start,
      firstDate: DateTime(now.year, now.month, now.day),
      lastDate: now.add(const Duration(days: 30)), // najvise 30 dana unapred
    );
    if (date == null) return;
    _start = DateTime(date.year, date.month, date.day, _start.hour, _start.minute);
    _loadSpots();
  }

  Future<void> _pickTime() async {
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_start),
    );
    if (time == null) return;
    _start = DateTime(_start.year, _start.month, _start.day, time.hour, time.minute);
    _loadSpots();
  }

  Future<void> _submit() async {
    if (_saving || !_formKey.currentState!.validate()) return;
    if (_start.isBefore(DateTime.now().subtract(const Duration(minutes: 1)))) {
      setState(() => _error =
          'Početak rezervacije je u prošlosti. Kliknite „Odmah” ili izaberite novo vreme.');
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await ParkingApi(context.read<ApiClient>()).createReservation(
        spotId: _selectedSpotId!,
        plateNumber: _plate.text,
        startAt: _start,
        endAt: _end,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Rezervacija je potvrđena.')));
      Navigator.pop(context, true);
      return;
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    }
    if (mounted) setState(() => _saving = false);
  }

  String _durationLabel(int m) =>
      m < 60 ? '$m min' : (m % 60 == 0 ? '${m ~/ 60} h' : '${m ~/ 60} h ${m % 60} min');

  @override
  Widget build(BuildContext context) {
    final freeCount = _spots?.where((s) => s.availableInInterval == true).length ?? 0;

    return Scaffold(
      appBar: AppBar(title: Text('Rezervacija – ${widget.lot.name}')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // TERMIN
            Card(
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.calendar_today),
                    title: const Text('Datum'),
                    trailing: Text(formatDate(_start)),
                    onTap: _pickDate,
                  ),
                  ListTile(
                    leading: const Icon(Icons.schedule),
                    title: const Text('Početak'),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        TextButton(onPressed: _startNow, child: const Text('Odmah')),
                        Text(formatTime(_start)),
                      ],
                    ),
                    onTap: _pickTime,
                  ),
                  ListTile(
                    leading: const Icon(Icons.timelapse),
                    title: const Text('Trajanje'),
                    trailing: DropdownButton<int>(
                      value: _minutes,
                      underline: const SizedBox(),
                      items: [
                        for (final m in _durations)
                          DropdownMenuItem(value: m, child: Text(_durationLabel(m))),
                      ],
                      onChanged: (m) {
                        _minutes = m!;
                        _loadSpots();
                      },
                    ),
                  ),
                  ListTile(
                    leading: const Icon(Icons.flag_outlined),
                    title: const Text('Kraj'),
                    trailing: Text('${formatDate(_end)} ${formatTime(_end)}'),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // TABLICE
            TextFormField(
              controller: _plate,
              textCapitalization: TextCapitalization.characters,
              decoration: const InputDecoration(
                labelText: 'Registarska oznaka',
                hintText: 'npr. NS-123-AB',
                prefixIcon: Icon(Icons.directions_car),
              ),
              validator: (v) {
                final plate = (v ?? '').trim();
                if (plate.isEmpty) return 'Registarska oznaka je obavezna.';
                if (!RegExp(r'^[A-Za-z0-9ČĆŠĐŽčćšđž -]{2,15}$').hasMatch(plate)) {
                  return 'Dozvoljena su slova, brojevi, razmak i crtica (2–15 znakova).';
                }
                return null;
              },
            ),
            const SizedBox(height: 24),

            // IZBOR MESTA
            Text('Izaberite mesto', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 4),
            if (_loadingSpots)
              const Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_spots != null) ...[
              Text('Slobodno u ovom terminu: $freeCount od ${_spots!.length}'),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final s in _spots!)
                    _SelectableSpot(
                      spot: s,
                      selected: s.id == _selectedSpotId,
                      onTap: () => setState(() => _selectedSpotId = s.id),
                    ),
                ],
              ),
            ],
            const SizedBox(height: 24),

            if (_error != null)
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.errorContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(_error!),
              ),

            FilledButton(
              onPressed: _selectedSpotId == null || _saving ? null : _submit,
              style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(50)),
              child: _saving
                  ? const SizedBox(
                      width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Potvrdi rezervaciju'),
            ),
            const SizedBox(height: 12),
            const Text(
              'Ako ne stignete u roku od 15 minuta od početka, rezervacija ističe '
              'i naplaćuje se kazna za neiskorišćenu rezervaciju.',
              style: TextStyle(fontSize: 12),
            ),
          ],
        ),
      ),
    );
  }
}

// boja mesta
class _SelectableSpot extends StatelessWidget {
  const _SelectableSpot({required this.spot, required this.selected, required this.onTap});
  final Spot spot;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final available = spot.availableInInterval == true;
    return InkWell(
      onTap: available ? onTap : null,
      child: Container(
        width: 64,
        height: 48,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: available ? Colors.green : Colors.grey.shade400,
          borderRadius: BorderRadius.circular(8),
          border: selected ? Border.all(color: Colors.black, width: 3) : null,
        ),
        child: selected
            ? const Icon(Icons.check, color: Colors.white)
            : Text(spot.spotNumber,
                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
    );
  }
}

// MOJE REZERVACIJE

class MyReservationsScreen extends StatefulWidget {
  const MyReservationsScreen({super.key});

  @override
  State<MyReservationsScreen> createState() => _MyReservationsScreenState();
}

class _MyReservationsScreenState extends State<MyReservationsScreen> {
  late Future<List<Reservation>> _reservations;

  @override
  void initState() {
    super.initState();
    _reservations = _load();
  }

  Future<List<Reservation>> _load() =>
      ParkingApi(context.read<ApiClient>()).myReservations();

  Future<void> _refresh() async {
    setState(() {
      _reservations = _load();
    });
    await _reservations;
  }

  Future<void> _cancel(Reservation r) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Otkazivanje'),
        content: Text('Otkazati rezervaciju mesta ${r.spotNumber} '
            '(${formatDate(r.startAt)} u ${formatTime(r.startAt)})?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Ne')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Otkaži')),
        ],
      ),
    );
    if (ok != true || !mounted) return;

    final messenger = ScaffoldMessenger.of(context);
    try {
      await ParkingApi(context.read<ApiClient>()).cancelReservation(r.id);
      messenger.showSnackBar(const SnackBar(content: Text('Rezervacija je otkazana.')));
    } on ApiException catch (e) {
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
    }
    _refresh();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Moje rezervacije')),
      body: FutureBuilder(
        future: _reservations,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return ErrorView(snapshot.error!, onRetry: _refresh);
          }
          final list = snapshot.data!;
          if (list.isEmpty) {
            return const Center(child: Text('Nemate nijednu rezervaciju.'));
          }
          return RefreshIndicator(
            onRefresh: _refresh,
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                for (final r in list) _ReservationCard(r, onCancel: () => _cancel(r)),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _ReservationCard extends StatelessWidget {
  const _ReservationCard(this.r, {required this.onCancel});
  final Reservation r;
  final VoidCallback onCancel;

  Color _statusColor(ReservationStatus s) => switch (s) {
        ReservationStatus.kreirana || ReservationStatus.potvrdjena => Colors.blue,
        ReservationStatus.aktivna => Colors.green,
        ReservationStatus.placanjeNeuspesno => Colors.orange,
        ReservationStatus.otkazana || ReservationStatus.istekla => Colors.red,
        ReservationStatus.zavrsena => Colors.grey,
      };

  @override
  Widget build(BuildContext context) {
    final sameDay = formatDate(r.startAt) == formatDate(r.endAt);
    final end = sameDay ? formatTime(r.endAt) : '${formatDate(r.endAt)} ${formatTime(r.endAt)}';
    final color = _statusColor(r.status);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text('${r.lotName} – mesto ${r.spotNumber}',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(r.status.label, style: TextStyle(color: color)),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text('${formatDate(r.startAt)} ${formatTime(r.startAt)} – $end'),
            Text('Tablice: ${r.plateNumber}'),
            if (r.status.canCancel)
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  icon: const Icon(Icons.close),
                  label: const Text('Otkaži'),
                  onPressed: onCancel,
                ),
              ),
          ],
        ),
      ),
    );
  }
}
