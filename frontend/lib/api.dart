import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

String get baseUrl =>
    (!kIsWeb && defaultTargetPlatform == TargetPlatform.android)
        ? 'http://10.0.2.2:8080/api'
        : 'http://localhost:8080/api';

class ApiException implements Exception {
  final int statusCode; // 0 = server nije dostupan
  final String message;
  const ApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}

class ApiClient {
  static const _storage = FlutterSecureStorage();
  static const _tokenKey = 'jwt_token';

  // token
  Future<void> saveToken(String token) =>
      _storage.write(key: _tokenKey, value: token);
  Future<String?> readToken() => _storage.read(key: _tokenKey);
  Future<void> deleteToken() => _storage.delete(key: _tokenKey);

  // zahtevi
  Future<dynamic> get(String path) => _send('GET', path);
  Future<dynamic> post(String path, [Object? body]) =>
      _send('POST', path, body);
  Future<dynamic> put(String path, [Object? body]) => _send('PUT', path, body);
  Future<dynamic> delete(String path) => _send('DELETE', path);

  Future<dynamic> _send(String method, String path, [Object? body]) async {
    final token = await readToken();
    final request = http.Request(method, Uri.parse('$baseUrl$path'));
    request.headers['Content-Type'] = 'application/json';
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    if (body != null) request.body = jsonEncode(body);

    final http.Response response;
    try {
      response = await http.Response.fromStream(
          await request.send().timeout(const Duration(seconds: 15)));
    } on Exception {
      throw const ApiException(0, 'Server nije dostupan.');
    }

    final text = utf8.decode(response.bodyBytes);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return text.isEmpty ? null : jsonDecode(text);
    }
    throw ApiException(response.statusCode, _errorMessage(response.statusCode, text));
  }

  // greske koje vraca backend
  String _errorMessage(int status, String body) {
    try {
      final json = jsonDecode(body);
      if (json is Map) {
        final fields = json['fieldErrors'];
        if (fields is Map && fields.isNotEmpty) return fields.values.join('\n');
        if (json['message'] is String) return json['message'];
      }
    } catch (_) {}
    return switch (status) {
      400 => 'Neispravni podaci. Proveri unos.',
      401 => 'Niste prijavljeni ili je sesija istekla.',
      403 => 'Nemate dozvolu za ovu akciju.',
      409 => 'Akcija nije dozvoljena u trenutnom stanju.',
      _ => 'Došlo je do greške (kod $status).',
    };
  }
}
