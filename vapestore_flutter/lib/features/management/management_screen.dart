import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../../core/theme/app_colors.dart';
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
          leading: const Icon(Icons.upload, color: AppColors.pastelMint),
          title: const Text('Экспорт бэкапа', style: TextStyle(color: AppColors.textPrimaryDark)),
          subtitle: const Text('Сохранить JSON', style: TextStyle(color: AppColors.textSecondaryDark)),
          onTap: () => _exportBackup(context, ref),
        ),
      ],
    );
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
