import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app.dart';
import 'core/theme/app_theme.dart';
import 'data/repositories/vape_repository.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('ru_RU', null);

  // Загружаем тестовые данные при первом запуске
  final repo = VapeRepository();
  await repo.seedTestData();

  runApp(
    const ProviderScope(
      child: VapeStoreApp(),
    ),
  );
}

class VapeStoreApp extends StatelessWidget {
  const VapeStoreApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Vape Store',
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: ThemeMode.dark,
      home: const AppShell(),
      navigatorObservers: [routeObserver],
    );
  }
}
