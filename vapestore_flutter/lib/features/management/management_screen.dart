import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:sqflite/sqflite.dart';

import '../../app.dart';
import '../../core/theme/app_colors.dart';
import '../../data/local/database.dart';
import '../../data/models/database_backup.dart';
import '../../data/repositories/vape_repository.dart';

class ManagementScreen extends ConsumerStatefulWidget {
  const ManagementScreen({super.key});

  @override
  ConsumerState<ManagementScreen> createState() => _ManagementScreenState();
}

class _ManagementScreenState extends ConsumerState<ManagementScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      appBar: AppBar(
        title: const Text('Управление', style: TextStyle(color: AppColors.textPrimaryDark)),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: 'Долги'),
            Tab(text: 'Резервы'),
            Tab(text: 'Продажи'),
            Tab(text: 'Настройки'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          const Center(child: Text('Долги', style: TextStyle(color: AppColors.textSecondaryDark))),
          const Center(child: Text('Резервы', style: TextStyle(color: AppColors.textSecondaryDark))),
          const Center(child: Text('Продажи', style: TextStyle(color: AppColors.textSecondaryDark))),
          _SettingsTab(),
        ],
      ),
    );
  }
}

class _SettingsTab extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        ListTile(
          leading: const Icon(Icons.download, color: AppColors.pastelMint),
          title: const Text('Импорт бэкапа', style: TextStyle(color: AppColors.textPrimaryDark)),
          subtitle: const Text('Загрузить JSON', style: TextStyle(color: AppColors.textSecondaryDark)),
          onTap: () => _importBackup(context, ref),
        ),
        ListTile(
          leading: const Icon(Icons.upload, color: AppColors.pastelMint),
          title: const Text('Экспорт бэкапа', style: TextStyle(color: AppColors.textPrimaryDark)),
          subtitle: const Text('Сохранить JSON', style: TextStyle(color: AppColors.textSecondaryDark)),
          onTap: () => _exportBackup(context, ref),
        ),
      ],
    );
  }

  static Future<void> _importBackup(BuildContext context, WidgetRef ref) async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
      );
      
      if (result == null || result.files.isEmpty) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Файл не выбран')),
          );
        }
        return;
      }

      final file = result.files.single;
      Uint8List bytes;
      if (file.bytes != null) {
        bytes = file.bytes!;
      } else if (file.path != null) {
        final fileData = await File(file.path!).readAsBytes();
        bytes = fileData;
      } else {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Не удалось прочитать файл')),
          );
        }
        return;
      }

      final json = utf8.decode(bytes);
      final data = jsonDecode(json) as Map<String, dynamic>;
      final backup = DatabaseBackup.fromJson(data);

      if (backup.products.isEmpty) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('В бэкапе нет товаров')),
          );
        }
        return;
      }

      final db = await AppDatabase.instance;
      await db.execute('DELETE FROM sales');
      await db.execute('DELETE FROM products');
      await db.execute('DELETE FROM payment_cards');

      final batch = db.batch();
      for (final p in backup.products) {
        batch.insert('products', p.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
      }
      for (final s in backup.sales) {
        batch.insert('sales', s.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
      }
      for (final c in backup.paymentCards ?? []) {
        batch.insert('payment_cards', c.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
      }
      await batch.commit(noResult: true);

      if (context.mounted) {
        ref.invalidate(productsCountProvider);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Импортировано: ${backup.products.length} товаров, ${backup.sales.length} продаж',
            ),
          ),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка импорта: $e')),
        );
      }
    }
  }

  static Future<void> _exportBackup(BuildContext context, WidgetRef ref) async {
    try {
      final repo = ref.read(repositoryProvider);
      final json = await repo.exportToJson();
      final dir = await getApplicationDocumentsDirectory();
      final date = DateFormat('yyyyMMdd_HHmmss').format(DateTime.now());
      final file = File('${dir.path}/VapeStoreBackup_$date.json');
      await file.writeAsString(json);
      if (context.mounted) {
        await Share.shareXFiles([XFile(file.path)], text: 'Бэкап VapeStore');
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка: $e')));
      }
    }
  }
}
