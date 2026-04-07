import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../data/models/sale.dart';

/// Диалог подтверждения удаления продажи
class DeleteSaleDialog extends ConsumerStatefulWidget {
  final Sale sale;
  final VoidCallback onDismiss;
  final Future<void> Function() onConfirm;

  const DeleteSaleDialog({
    super.key,
    required this.sale,
    required this.onDismiss,
    required this.onConfirm,
  });

  @override
  ConsumerState<DeleteSaleDialog> createState() => _DeleteSaleDialogState();
}

class _DeleteSaleDialogState extends ConsumerState<DeleteSaleDialog> {
  bool _loading = false;

  String get _message {
    if (widget.sale.sourceType == 'debt') {
      return 'Долг будет помечен как неоплаченный.';
    }
    if (widget.sale.sourceType == 'reservation') {
      return 'Резерв будет восстановлен, товар вернётся на склад.';
    }
    return 'Товар будет возвращён на склад.';
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.surfaceDark,
      title: Text(
        'Удалить продажу?',
        style: TextStyle(color: AppColors.textPrimaryDark, fontWeight: FontWeight.w600),
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Вы уверены, что хотите удалить продажу #${widget.sale.id}?',
            style: TextStyle(color: AppColors.textPrimaryDark, fontSize: 15),
          ),
          const SizedBox(height: 8),
          Text(
            _message,
            style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 13),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: _loading ? null : widget.onDismiss,
          child: Text('Отмена', style: TextStyle(color: AppColors.textSecondaryDark)),
        ),
        FilledButton(
          onPressed: _loading ? null : _handleDelete,
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.pastelPink,
            foregroundColor: AppColors.textOnPastel,
          ),
          child: _loading
              ? SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: AppColors.textOnPastel,
                  ),
                )
              : const Text('Удалить'),
        ),
      ],
    );
  }

  Future<void> _handleDelete() async {
    setState(() => _loading = true);
    try {
      await widget.onConfirm();
      if (mounted) {
        widget.onDismiss();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Продажа удалена')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }
}
