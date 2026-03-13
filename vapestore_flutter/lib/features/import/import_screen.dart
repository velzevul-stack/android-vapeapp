import 'dart:convert';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';

import '../../core/theme/app_colors.dart';
import '../../data/local/database.dart';
import '../../data/models/database_backup.dart';
import '../../data/repositories/vape_repository.dart';

class ImportScreen extends ConsumerStatefulWidget {
  const ImportScreen({super.key});

  @override
  ConsumerState<ImportScreen> createState() => _ImportScreenState();
}

class _ImportScreenState extends ConsumerState<ImportScreen> {
  String _status = 'Выберите JSON-бэкап (экспорт из Android VapeStoreApp)';
  bool _isLoading = false;
  String? _error;

  Future<void> _pickAndImport() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
      );
      if (result == null || result.files.isEmpty) {
        setState(() {
          _isLoading = false;
          _status = 'Файл не выбран';
        });
        return;
      }
      final path = result.files.single.path;
      if (path == null) {
        setState(() {
          _isLoading = false;
          _error = 'Не удалось получить путь к файлу';
        });
        return;
      }
      final bytes = await result.files.single.bytes;
      if (bytes == null) {
        setState(() {
          _isLoading = false;
          _error = 'Не удалось прочитать файл';
        });
        return;
      }
      final json = utf8.decode(bytes);
      final data = jsonDecode(json) as Map<String, dynamic>;
      final backup = DatabaseBackup.fromJson(data);

      if (backup.products.isEmpty) {
        setState(() {
          _isLoading = false;
          _error = 'В бэкапе нет товаров';
        });
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

      if (mounted) {
        setState(() {
          _isLoading = false;
          _status = 'Импортировано: ${backup.products.length} товаров, ${backup.sales.length} продаж';
        });
        ref.invalidate(productsCountProvider);
      }
    } catch (e, st) {
      setState(() {
        _isLoading = false;
        _error = 'Ошибка: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Icon(
                Icons.upload_file,
                size: 80,
                color: AppColors.pastelMint,
              ),
              const SizedBox(height: 24),
              Text(
                'Импорт данных',
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      color: AppColors.textPrimaryDark,
                      fontWeight: FontWeight.w600,
                    ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              Text(
                _status,
                style: TextStyle(
                  color: AppColors.textSecondaryDark,
                  fontSize: 14,
                ),
                textAlign: TextAlign.center,
              ),
              if (_error != null) ...[
                const SizedBox(height: 16),
                Text(
                  _error!,
                  style: const TextStyle(
                    color: AppColors.accentError,
                    fontSize: 14,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
              const SizedBox(height: 32),
              FilledButton(
                onPressed: _isLoading ? null : _pickAndImport,
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.pastelMint,
                  foregroundColor: AppColors.textOnPastel,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(24),
                  ),
                ),
                child: _isLoading
                    ? const SizedBox(
                        height: 24,
                        width: 24,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: AppColors.textOnPastel,
                        ),
                      )
                    : const Text('Выбрать JSON-файл'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
