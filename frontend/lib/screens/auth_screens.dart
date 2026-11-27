import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api.dart';
import '../auth.dart';

// PRIJAVA

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _loading = false;
  String? _error;

  Future<void> _submit() async {
    if (_loading || !_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await context.read<AuthProvider>().login(_email.text, _password.text);
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    }
    if (mounted) setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    return _AuthLayout(
      title: 'Dobro došli',
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _emailField(_email),
            const SizedBox(height: 16),
            _PasswordField(_password, 'Lozinka',
                validator: (v) => (v ?? '').isEmpty ? 'Unesi lozinku.' : null),
            const SizedBox(height: 20),
            if (_error != null) _ErrorBox(_error!),
            _submitButton('Prijavi se', _loading, _submit),
            TextButton(
              onPressed: () => Navigator.push(context,
                  MaterialPageRoute(builder: (_) => const RegisterScreen())),
              child: const Text('Nemate nalog? Registrujte se'),
            ),
          ],
        ),
      ),
    );
  }
}

// REGISTRACIJA

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  bool _loading = false;
  String? _error;

  Future<void> _submit() async {
    if (_loading || !_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await context
          .read<AuthProvider>()
          .register(_name.text, _email.text, _password.text);
      // korisnik je prijavljen - zatvori ekran, ispod je vec pocetni ekran
      if (mounted) Navigator.pop(context);
      return;
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    }
    if (mounted) setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    return _AuthLayout(
      title: 'Napravite nalog',
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextFormField(
              controller: _name,
              decoration: const InputDecoration(
                  labelText: 'Ime i prezime',
                  prefixIcon: Icon(Icons.person_outline)),
              // ista pravila kao na backendu
              validator: (v) {
                final name = (v ?? '').trim();
                if (name.isEmpty) return 'Ime je obavezno.';
                if (name.length > 100) return 'Ime može imati najviše 100 karaktera.';
                if (!RegExp(r"^[\p{L} .'-]+$", unicode: true).hasMatch(name)) {
                  return 'Ime sme da sadrži samo slova, razmak, tačku, apostrof i crticu.';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            _emailField(_email),
            const SizedBox(height: 16),
            _PasswordField(_password, 'Lozinka', validator: (v) {
              final p = v ?? '';
              if (p.length < 8 || p.length > 72) {
                return 'Lozinka mora imati između 8 i 72 karaktera.';
              }
              return null;
            }),
            const SizedBox(height: 16),
            _PasswordField(_confirm, 'Ponovi lozinku',
                validator: (v) =>
                    v != _password.text ? 'Lozinke se ne poklapaju.' : null),
            const SizedBox(height: 20),
            if (_error != null) _ErrorBox(_error!),
            _submitButton('Registruj se', _loading, _submit),
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Već imate nalog? Prijavite se'),
            ),
          ],
        ),
      ),
    );
  }
}

// Zajedni deo za login i register

Widget _emailField(TextEditingController controller) => TextFormField(
      controller: controller,
      keyboardType: TextInputType.emailAddress,
      decoration: const InputDecoration(
          labelText: 'E-mail', prefixIcon: Icon(Icons.mail_outline)),
      validator: (v) {
        final email = (v ?? '').trim();
        if (email.isEmpty) return 'E-mail je obavezan.';
        if (email.length > 255) return 'E-mail može imati najviše 255 karaktera.';
        if (!RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(email)) {
          return 'E-mail nije ispravan.';
        }
        return null;
      },
    );

Widget _submitButton(String text, bool loading, VoidCallback onPressed) =>
    FilledButton(
      onPressed: loading ? null : onPressed,
      style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(50)),
      child: loading
          ? const SizedBox(
              width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2))
          : Text(text),
    );

// logo i naslov
class _AuthLayout extends StatelessWidget {
  const _AuthLayout({required this.title, required this.child});
  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.primary;
    return Scaffold(
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Column(
              children: [
                Icon(Icons.local_parking, size: 64, color: color),
                Text('SmartPark',
                    style: TextStyle(
                        fontSize: 22, fontWeight: FontWeight.bold, color: color)),
                const SizedBox(height: 16),
                Text(title, style: Theme.of(context).textTheme.headlineSmall),
                const SizedBox(height: 16),
                Card(
                  child: Padding(padding: const EdgeInsets.all(24), child: child),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// polje za lozinku
class _PasswordField extends StatefulWidget {
  const _PasswordField(this.controller, this.label, {this.validator});
  final TextEditingController controller;
  final String label;
  final String? Function(String?)? validator;

  @override
  State<_PasswordField> createState() => _PasswordFieldState();
}

class _PasswordFieldState extends State<_PasswordField> {
  bool _hidden = true;

  @override
  Widget build(BuildContext context) => TextFormField(
        controller: widget.controller,
        obscureText: _hidden,
        validator: widget.validator,
        decoration: InputDecoration(
          labelText: widget.label,
          prefixIcon: const Icon(Icons.lock_outline),
          suffixIcon: IconButton(
            icon: Icon(_hidden ? Icons.visibility : Icons.visibility_off),
            onPressed: () => setState(() => _hidden = !_hidden),
          ),
        ),
      );
}

// poruka greske
class _ErrorBox extends StatelessWidget {
  const _ErrorBox(this.message);
  final String message;

  @override
  Widget build(BuildContext context) => Container(
        margin: const EdgeInsets.only(bottom: 16),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.errorContainer,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Text(message),
      );
}
