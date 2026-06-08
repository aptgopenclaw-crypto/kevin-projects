import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/database/app_database.dart';
import '../core/constants/app_constants.dart';
import '../core/utils/currency_utils.dart';
import '../models/transaction.dart';
import '../models/category.dart';

class EditTransactionPage extends StatefulWidget {
  final Transaction transaction;
  final Map<int, Category> categoryMap;

  const EditTransactionPage({
    super.key,
    required this.transaction,
    required this.categoryMap,
  });

  @override
  State<EditTransactionPage> createState() => _EditTransactionPageState();
}

class _EditTransactionPageState extends State<EditTransactionPage> {
  final AppDatabase _db = AppDatabase.instance;
  late TextEditingController _amountController;
  late TextEditingController _noteController;

  late Category _selectedCategory;
  late String _selectedCurrency;
  late DateTime _selectedDate;
  late TimeOfDay _selectedTime;
  late String _tags;

  @override
  void initState() {
    super.initState();
    final t = widget.transaction;
    _amountController = TextEditingController(text: t.amount.toString());
    _noteController = TextEditingController(text: t.note ?? '');

    final date = DateTime.fromMillisecondsSinceEpoch(t.timestamp * 1000);
    _selectedCategory = widget.categoryMap[t.categoryId] ??
        Category(id: t.categoryId, name: '未分類', iconCode: 'more_horiz', sortOrder: 99);
    _selectedCurrency = t.currency;
    _selectedDate = date;
    _selectedTime = TimeOfDay.fromDateTime(date);
    _tags = t.tags ?? '';
  }

  Future<void> _save() async {
    final amountText = _amountController.text.trim();
    if (amountText.isEmpty) return;

    final amount = double.tryParse(amountText);
    if (amount == null || amount <= 0) return;

    final dateTime = DateTime(
      _selectedDate.year, _selectedDate.month, _selectedDate.day,
      _selectedTime.hour, _selectedTime.minute,
    );

    await _db.updateTransaction(widget.transaction.copyWith(
      amount: amount,
      categoryId: _selectedCategory.id,
      note: _noteController.text.trim().isEmpty ? null : _noteController.text.trim(),
      tags: _tags.isEmpty ? null : _tags,
      timestamp: dateTime.millisecondsSinceEpoch ~/ 1000,
      currency: _selectedCurrency,
    ));

    HapticFeedback.lightImpact();
    if (mounted) Navigator.pop(context, true);
  }

  Future<void> _delete() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('刪除紀錄'),
        content: const Text('確定要刪除這筆紀錄嗎？'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('取消')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('刪除', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
    if (confirm == true) {
      await _db.deleteTransaction(widget.transaction.id!);
      HapticFeedback.mediumImpact();
      if (mounted) Navigator.pop(context, true);
    }
  }

  @override
  void dispose() {
    _amountController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('編輯紀錄'),
        actions: [
          TextButton(onPressed: _save, child: const Text('儲存')),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: _amountController,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
              labelText: '金額',
              prefixText: '${CurrencyUtils.getSymbol(_selectedCurrency)} ',
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _noteController,
            decoration: const InputDecoration(
              labelText: '備註',
              hintText: '例如：與同事吃午餐',
            ),
            maxLines: 2,
          ),
          const SizedBox(height: 16),
          DropdownButtonFormField<String>(
            value: _selectedCurrency,
            decoration: const InputDecoration(labelText: '幣別'),
            items: AppConstants.supportedCurrencies.keys.map((code) {
              return DropdownMenuItem(
                value: code,
                child: Text('$code (${AppConstants.supportedCurrencies[code]})'),
              );
            }).toList(),
            onChanged: (v) {
              if (v != null) setState(() => _selectedCurrency = v);
            },
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () async {
                    final picked = await showDatePicker(
                      context: context,
                      initialDate: _selectedDate,
                      firstDate: DateTime(2020),
                      lastDate: DateTime.now(),
                    );
                    if (picked != null) setState(() => _selectedDate = picked);
                  },
                  icon: const Icon(Icons.calendar_today),
                  label: Text(
                    '${_selectedDate.year}/${_selectedDate.month}/${_selectedDate.day}',
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () async {
                    final picked = await showTimePicker(
                      context: context,
                      initialTime: _selectedTime,
                    );
                    if (picked != null) setState(() => _selectedTime = picked);
                  },
                  icon: const Icon(Icons.access_time),
                  label: Text(_selectedTime.format(context)),
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          Center(
            child: TextButton.icon(
              onPressed: _delete,
              icon: const Icon(Icons.delete_outline, color: Colors.red),
              label: const Text('刪除此筆紀錄',
                  style: TextStyle(color: Colors.red)),
            ),
          ),
        ],
      ),
    );
  }
}
