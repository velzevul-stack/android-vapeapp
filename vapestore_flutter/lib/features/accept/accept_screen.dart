import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../app.dart';
import '../../core/theme/app_colors.dart';

class AcceptScreen extends ConsumerStatefulWidget {
  const AcceptScreen({super.key});

  @override
  ConsumerState<AcceptScreen> createState() => _AcceptScreenState();
}

class _AcceptScreenState extends ConsumerState<AcceptScreen> {
  final _barcodeController = TextEditingController();
  String _result = '';
  bool _isLoading = false;

  @override
  void dispose() {
    _barcodeController.dispose();
    super.dispose();
  }

  Future<void> _processBarcode(String barcode) async {
    final digits = barcode.replaceAll(RegExp(r'\D'), '');
    final normalized = digits.length >= 13 ? digits.substring(0, 13) : digits;
    if (normalized.length != 13) return;
    setState(() {
      _isLoading = true;
      _result = '';
    });
    try {
      final repo = ref.read(repositoryProvider);
      final product = await repo.getProductByBarcode(normalized);
      if (product != null) {
        await repo.increaseStock(product.id);
        setState(() {
          _result = '✅ ${product.brand} ${product.flavor} — +1';
          _barcodeController.clear();
        });
      } else {
        setState(() {
          _result = '❗ Товар с кодом $normalized не найден';
        });
      }
    } finally {
      setState(() => _isLoading = false);
    }
  }

  void _submitBarcode() {
    final text = _barcodeController.text.trim();
    if (text.isNotEmpty) _processBarcode(text);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      appBar: AppBar(
        title: const Text('Приёмка', style: TextStyle(color: AppColors.textPrimaryDark)),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Введите или отсканируйте штрих-код:',
              style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 14),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _barcodeController,
              decoration: const InputDecoration(
                hintText: 'Штрих-код (13 цифр)',
              ),
              keyboardType: TextInputType.number,
              onSubmitted: (_) => _submitBarcode(),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: FilledButton.icon(
                    onPressed: _isLoading ? null : _submitBarcode,
                    icon: const Icon(Icons.check),
                    label: const Text('Принять'),
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.pastelMint,
                      foregroundColor: AppColors.textOnPastel,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _showBarcodeScanner,
                    icon: const Icon(Icons.camera_alt),
                    label: const Text('Сканер'),
                  ),
                ),
              ],
            ),
            if (_result.isNotEmpty) ...[
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.surfaceElevatedDark,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  _result,
                  style: const TextStyle(
                    color: AppColors.textPrimaryDark,
                    fontSize: 14,
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  void _showBarcodeScanner() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.black,
      builder: (ctx) => SizedBox(
        height: MediaQuery.of(ctx).size.height * 0.7,
        child: Stack(
          children: [
            MobileScanner(
              onDetect: (capture) {
                final barcodes = capture.barcodes;
                if (barcodes.isNotEmpty) {
                  final code = barcodes.first.rawValue;
                  if (code != null && code.isNotEmpty) {
                    Navigator.pop(ctx);
                    _processBarcode(code);
                  }
                }
              },
            ),
            Positioned(
              top: 16,
              right: 16,
              child: IconButton(
                icon: const Icon(Icons.close, color: Colors.white),
                onPressed: () => Navigator.pop(ctx),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
