import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'api.dart';

// Modeli

// Uloge: VOZAC, OPERATER, ADMINISTRATOR.
enum UserRole {
  vozac('Vozač'),
  operater('Operater'),
  administrator('Administrator');

  const UserRole(this.label);
  final String label;

  static UserRole fromApi(String value) => switch (value) {
        'OPERATER' => UserRole.operater,
        'ADMINISTRATOR' => UserRole.administrator,
        _ => UserRole.vozac,
      };
}

// ista kao klasa na backendu
class User {
  final int id;
  final String name;
  final String email;
  final UserRole role;

  const User(this.id, this.name, this.email, this.role);

  factory User.fromJson(Map<String, dynamic> json) => User(
        (json['id'] as num).toInt(),
        json['name'] as String,
        json['email'] as String,
        UserRole.fromApi(json['role'] as String),
      );

  Map<String, dynamic> toJson() =>
      {'id': id, 'name': name, 'email': email, 'role': role.name.toUpperCase()};
}

// PRIJAVA / REGISTRACIJA

class AuthProvider extends ChangeNotifier {
  AuthProvider(this._api);

  final ApiClient _api;
  static const _storage = FlutterSecureStorage();

  bool loading = true; // loading dok se proverava da li je korisnik vec prijavljen
  User? user;

  // pri pokretanju ako postoji sacuvan token, proveri ga na backendu (GET /api/me).
  // tokken traje 60 minuta - ako je istekao, backend vraca 401 i korisnik se odjavljuje
  Future<void> init() async {
    final token = await _api.readToken();
    final savedUser = await _storage.read(key: 'user');
    if (token != null && savedUser != null) {
      user = User.fromJson(jsonDecode(savedUser));
      try {
        user = User.fromJson(await _api.get('/me'));
      } on ApiException catch (e) {
        if (e.statusCode == 401) await _clear();
      }
    }
    loading = false;
    notifyListeners();
  }

  // POST /api/auth/login
  Future<void> login(String email, String password) async {
    final json = await _api.post('/auth/login', {
      'email': email.trim(),
      'password': password,
    });
    await _save(json);
  }

  // POST /api/auth/register
  Future<void> register(String name, String email, String password) async {
    final json = await _api.post('/auth/register', {
      'name': name.trim(),
      'email': email.trim(),
      'password': password,
    });
    await _save(json);
  }

  // POST /api/auth/logout
  Future<void> logout() async {
    try {
      await _api.post('/auth/logout');
    } on ApiException catch (_) {}
    await _clear();
    notifyListeners();
  }

  Future<void> _save(dynamic json) async {
    final u = User.fromJson(json['user']);
    await _api.saveToken(json['token'] as String);
    await _storage.write(key: 'user', value: jsonEncode(u.toJson()));
    user = u;
    notifyListeners();
  }

  Future<void> _clear() async {
    await _api.deleteToken();
    await _storage.delete(key: 'user');
    user = null;
  }
}
