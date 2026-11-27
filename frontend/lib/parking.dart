import 'api.dart';

// MODELI

// Parking lokacija sa brojem mesta po statusu
class Lot {
  final int id;
  final String name;
  final String address;
  final int capacity;
  final bool active;
  final int totalSpots;
  final int freeSpots;
  final int reservedSpots;
  final int occupiedSpots;
  final int outOfServiceSpots;

  Lot.fromJson(Map<String, dynamic> json)
      : id = (json['id'] as num).toInt(),
        name = json['name'] as String,
        address = json['address'] as String,
        capacity = (json['capacity'] as num).toInt(),
        active = json['active'] as bool,
        totalSpots = (json['totalSpots'] as num).toInt(),
        freeSpots = (json['freeSpots'] as num).toInt(),
        reservedSpots = (json['reservedSpots'] as num).toInt(),
        occupiedSpots = (json['occupiedSpots'] as num).toInt(),
        outOfServiceSpots = (json['outOfServiceSpots'] as num).toInt();
}

// Status mesta
enum SpotStatus {
  slobodno('SLOBODNO', 'Slobodno'),
  rezervisano('REZERVISANO', 'Rezervisano'),
  zauzeto('ZAUZETO', 'Zauzeto'),
  vanUpotrebe('VAN_UPOTREBE', 'Van upotrebe');

  const SpotStatus(this.apiValue, this.label);
  final String apiValue;
  final String label;

  static SpotStatus fromApi(String value) =>
      values.firstWhere((s) => s.apiValue == value, orElse: () => slobodno);
}

// Jedno parking mesto
class Spot {
  final int id;
  final int lotId;
  final String spotNumber;
  final SpotStatus status;
  final String? outOfServiceReason;

  // popunjeno samo kad se pita za termin
  final bool? availableInInterval;

  Spot.fromJson(Map<String, dynamic> json)
      : id = (json['id'] as num).toInt(),
        lotId = (json['lotId'] as num).toInt(),
        spotNumber = json['spotNumber'] as String,
        status = SpotStatus.fromApi(json['status'] as String),
        outOfServiceReason = json['outOfServiceReason'] as String?,
        availableInInterval = json['availableInInterval'] as bool?;
}

// cenovnik lokacije
class PriceList {
  final double pricePerHour;
  final double minCharge;
  final double overstayFine;
  final double noShowFine;

  PriceList.fromJson(Map<String, dynamic> json)
      : pricePerHour = (json['pricePerHour'] as num).toDouble(),
        minCharge = (json['minCharge'] as num).toDouble(),
        overstayFine = (json['overstayFine'] as num).toDouble(),
        noShowFine = (json['noShowFine'] as num).toDouble();
}

// stanja rezervacije - iste vrednosti kao ReservationStatus na backendu
enum ReservationStatus {
  kreirana('KREIRANA', 'Kreirana'),
  potvrdjena('POTVRDJENA', 'Potvrđena'),
  otkazana('OTKAZANA', 'Otkazana'),
  istekla('ISTEKLA', 'Istekla'),
  aktivna('AKTIVNA', 'Parkiranje u toku'),
  placanjeNeuspesno('PLACANJE_NEUSPESNO', 'Plaćanje neuspešno'),
  zavrsena('ZAVRSENA', 'Završena');

  const ReservationStatus(this.apiValue, this.label);
  final String apiValue;
  final String label;

  // vozac moze da otkaze samo rezervaciju pre pocetka parkiranja (FR-10)
  bool get canCancel => this == kreirana || this == potvrdjena;

  static ReservationStatus fromApi(String value) =>
      values.firstWhere((s) => s.apiValue == value, orElse: () => kreirana);
}

// rezervacija
class Reservation {
  final int id;
  final int lotId;
  final String lotName;
  final int spotId;
  final String spotNumber;
  final String plateNumber;
  final DateTime startAt;
  final DateTime endAt;
  final ReservationStatus status;

  Reservation.fromJson(Map<String, dynamic> json)
      : id = (json['id'] as num).toInt(),
        lotId = (json['lotId'] as num).toInt(),
        lotName = json['lotName'] as String,
        spotId = (json['spotId'] as num).toInt(),
        spotNumber = json['spotNumber'] as String,
        plateNumber = json['plateNumber'] as String,
        startAt = DateTime.parse(json['startAt'] as String).toLocal(),
        endAt = DateTime.parse(json['endAt'] as String).toLocal(),
        status = ReservationStatus.fromApi(json['status'] as String);
}

//

class ParkingApi {
  ParkingApi(this._api);
  final ApiClient _api;

  // GET /api/lots - aktivne lokacije
  Future<List<Lot>> lots() async {
    final list = await _api.get('/lots') as List;
    return list.map((e) => Lot.fromJson(e)).toList();
  }

  // GET /api/lots/{id}/spots - sva mesta na lokaciji
  Future<List<Spot>> spots(int lotId, {DateTime? from, DateTime? to}) async {
    var path = '/lots/$lotId/spots';
    if (from != null && to != null) {
      path += '?from=${_iso(from)}&to=${_iso(to)}';
    }
    final list = await _api.get(path) as List;
    return list.map((e) => Spot.fromJson(e)).toList();
  }

  // GET /api/lots/{id}/price-list - vraca null ako lokacija jos nema cenovnik
  Future<PriceList?> priceList(int lotId) async {
    try {
      return PriceList.fromJson(await _api.get('/lots/$lotId/price-list'));
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }

  // POST /api/reservations - nova rezervacija
  Future<Reservation> createReservation({
    required int spotId,
    required String plateNumber,
    required DateTime startAt,
    required DateTime endAt,
  }) async {
    final json = await _api.post('/reservations', {
      'spotId': spotId,
      'plateNumber': plateNumber.trim(),
      'startAt': startAt.toUtc().toIso8601String(),
      'endAt': endAt.toUtc().toIso8601String(),
    });
    return Reservation.fromJson(json);
  }

  // GET /api/reservations/my - rezervacije vozaca
  Future<List<Reservation>> myReservations() async {
    final list = await _api.get('/reservations/my') as List;
    return list.map((e) => Reservation.fromJson(e)).toList()
      ..sort((a, b) => b.startAt.compareTo(a.startAt));
  }

  // POST /api/reservations/{id}/cancel - otkazivanje rezervacije
  Future<void> cancelReservation(int id) => _api.post('/reservations/$id/cancel');

  // Backend ocekuje vreme sa vremenskom zonom, npr. 2026-12-01T10:00:00Z
  static String _iso(DateTime t) =>
      Uri.encodeQueryComponent(t.toUtc().toIso8601String());
}

// prikaz datuma i vremena

String _two(int n) => n.toString().padLeft(2, '0');
String formatDate(DateTime t) => '${_two(t.day)}.${_two(t.month)}.${t.year}.';
String formatTime(DateTime t) => '${_two(t.hour)}:${_two(t.minute)}';
