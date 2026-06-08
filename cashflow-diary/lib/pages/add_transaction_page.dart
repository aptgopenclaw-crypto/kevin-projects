import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/database/app_database.dart';
import '../core/constants/app_constants.dart';
import '../core/theme/app_colors.dart';
import '../core/utils/currency_utils.dart';
import '../models/transaction.dart';
import '../models/category.dart';

class AddTransactionPage extends StatefulWidget {
  final String defaultCurrency;
  final int? lastCategoryId;

  const AddTransactionPage({
    super.key,
    required this.defaultCurrency,
    this.lastCategoryId,
  });

  @override
  State<AddTransactionPage> createState() => _AddTransactionPageState();
}

class _AddTransactionPageState extends State<AddTransactionPage> {
  final AppDatabase _db = AppDatabase.instance;
  final TextEditingController _amountController = TextEditingController();
  final TextEditingController _noteController = TextEditingController();

  List<Category> _categories = [];
  Category? _selectedCategory;
  String _selectedCurrency = 'TWD';
  DateTime _selectedDate = DateTime.now();
  TimeOfDay _selectedTime = TimeOfDay.now();
  String _tags = '';
  bool _isLoading = false;
  bool _continueMode = false;

  @override
  void initState() {
    super.initState();
    _selectedCurrency = widget.defaultCurrency;
    _loadCategories();
  }

  Future<void> _loadCategories() async {
    final categories = await _db.getAllCategories();
    if (!mounted) return;
    setState(() {
      _categories = categories;
      if (widget.lastCategoryId != null) {
        _selectedCategory = categories.cast<Category?>().firstWhere(
          (c) => c!.id == widget.lastCategoryId,
          orElse: () => categories.first,
        );
      }
      if (_selectedCategory == null && categories.isNotEmpty) {
        _selectedCategory = categories.first;
      }
    });
  }

  Future<void> _save() async {
    final amountText = _amountController.text.trim();
    if (amountText.isEmpty || amountText == '.' ) {
      _showSnackBar('請輸入金額');
      return;
    }
    if (_selectedCategory == null) {
      _showSnackBar('請選擇分類');
      return;
    }

    final amount = double.tryParse(amountText);
    if (amount == null || amount <= 0) {
      _showSnackBar('請輸入有效金額');
      return;
    }

    setState(() => _isLoading = true);

    final dateTime = DateTime(
      _selectedDate.year, _selectedDate.month, _selectedDate.day,
      _selectedTime.hour, _selectedTime.minute,
    );

    await _db.insertTransaction(Transaction(
      amount: amount,
      categoryId: _selectedCategory!.id,
      note: _noteController.text.trim().isEmpty ? null : _noteController.text.trim(),
      tags: _tags.isEmpty ? null : _tags,
      timestamp: dateTime.millisecondsSinceEpoch ~/ 1000,
      currency: _selectedCurrency,
    ));

    HapticFeedback.lightImpact();

    if (_continueMode) {
      _amountController.text = '';
      if (mounted) setState(() => _isLoading = false);
      if (mounted) _showSnackBar('已儲存，繼續記帳');
    } else {
      if (mounted) Navigator.pop(context, true);
    }
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }

  @override
  void dispose() {
    _amountController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: isDark ? AppColors.bgPrimaryDark : AppColors.bgSecondary,
      appBar: AppBar(
        backgroundColor: isDark ? AppColors.bgPrimaryDark : AppColors.bgSecondary,
        leading: TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('取消',
              style: TextStyle(color: AppColors.systemBlue, fontSize: 17)),
        ),
        leadingWidth: 70,
        title: const Text('新增記帳'),
        actions: [
          TextButton(
            onPressed: _isLoading ? null : _save,
            child: Text(
              '儲存',
              style: TextStyle(
                color: _isLoading ? AppColors.labelSecondary : AppColors.systemGreen,
                fontSize: 17,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: Column(
        children: [
          // ─── Amount display ────────────────────────────────────────────
          _buildAmountDisplay(isDark),
          // ─── Form fields ───────────────────────────────────────────────
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildSection(isDark, [
                    _buildCategoryRow(isDark),
                  ]),
                  const SizedBox(height: 20),
                  _buildSection(isDark, [
                    _buildNoteRow(isDark),
                    _buildDivider(isDark),
                    _buildTagsRow(isDark),
                  ]),
                  const SizedBox(height: 20),
                  _buildSection(isDark, [
                    _buildCurrencyRow(isDark),
                    _buildDivider(isDark),
                    _buildDateRow(isDark),
                    _buildDivider(isDark),
                    _buildTimeRow(isDark),
                  ]),
                  const SizedBox(height: 20),
                  _buildSection(isDark, [
                    SwitchListTile(
                      dense: true,
                      title: const Text('連續記帳模式',
                          style: TextStyle(fontSize: 15)),
                      subtitle: const Text('儲存後清空金額，繼續輸入下一筆',
                          style: TextStyle(fontSize: 12)),
                      value: _continueMode,
                      activeColor: AppColors.systemGreen,
                      onChanged: (v) => setState(() => _continueMode = v),
                    ),
                  ]),
                ],
              ),
            ),
          ),
          // ─── Calculator keyboard ───────────────────────────────────────
          _buildKeyboard(isDark),
        ],
      ),
    );
  }

  // ── Amount display ───────────────────────────────────────────────────────

  Widget _buildAmountDisplay(bool isDark) {
    return Container(
      width: double.infinity,
      color: isDark ? AppColors.bgPrimaryDark : AppColors.bgSecondary,
      padding: const EdgeInsets.fromLTRB(24, 12, 24, 20),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            CurrencyUtils.getSymbol(_selectedCurrency),
            style: TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.w300,
              color: isDark ? AppColors.labelSecondaryDark : AppColors.labelSecondary,
              height: 1.3,
            ),
          ),
          const SizedBox(width: 4),
          Expanded(
            child: Text(
              _amountController.text.isEmpty ? '0' : _amountController.text,
              style: TextStyle(
                fontSize: 52,
                fontWeight: FontWeight.w300,
                letterSpacing: -2,
                color: _amountController.text.isEmpty
                    ? (isDark ? AppColors.labelSecondaryDark : AppColors.labelTertiary)
                    : null,
              ),
              textAlign: TextAlign.right,
            ),
          ),
        ],
      ),
    );
  }

  // ── iOS grouped section helpers ──────────────────────────────────────────

  Widget _buildSection(bool isDark, List<Widget> children) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: Material(
        color: isDark ? AppColors.bgSecondaryDark : AppColors.bgPrimary,
        child: Column(mainAxisSize: MainAxisSize.min, children: children),
      ),
    );
  }

  Widget _buildDivider(bool isDark) => Divider(
      height: 0.5,
      thickness: 0.5,
      indent: 16,
      color: isDark ? AppColors.separatorDark : AppColors.separator);

  Widget _buildCategoryRow(bool isDark) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.only(bottom: 8),
            child: Text('分類',
                style: TextStyle(
                    fontSize: 13,
                    color: AppColors.labelSecondary,
                    fontWeight: FontWeight.w500)),
          ),
          SizedBox(
            height: 40,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: _categories.length,
              separatorBuilder: (_, __) => const SizedBox(width: 8),
              itemBuilder: (ctx, i) {
                final cat = _categories[i];
                final isSelected = _selectedCategory?.id == cat.id;
                return GestureDetector(
                  onTap: () => setState(() => _selectedCategory = cat),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 150),
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: isSelected
                          ? AppColors.systemGreen
                          : (isDark ? AppColors.bgTertiaryDark : AppColors.bgTertiary),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      cat.name,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                        color: isSelected ? Colors.white
                            : (isDark ? AppColors.labelPrimaryDark : AppColors.labelPrimary),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNoteRow(bool isDark) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: TextField(
        controller: _noteController,
        style: const TextStyle(fontSize: 15),
        decoration: const InputDecoration(
          hintText: '備註 (選填)',
          border: InputBorder.none,
          enabledBorder: InputBorder.none,
          focusedBorder: InputBorder.none,
          fillColor: Colors.transparent,
          prefixIcon: Icon(Icons.notes_outlined, size: 20, color: AppColors.labelSecondary),
          contentPadding: EdgeInsets.symmetric(vertical: 14),
        ),
      ),
    );
  }

  Widget _buildTagsRow(bool isDark) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: TextField(
        style: const TextStyle(fontSize: 15),
        decoration: const InputDecoration(
          hintText: '標籤 (選填，逗號分隔)',
          border: InputBorder.none,
          enabledBorder: InputBorder.none,
          focusedBorder: InputBorder.none,
          fillColor: Colors.transparent,
          prefixIcon: Icon(Icons.label_outline, size: 20, color: AppColors.labelSecondary),
          contentPadding: EdgeInsets.symmetric(vertical: 14),
        ),
        onChanged: (v) => _tags = v,
      ),
    );
  }

  Widget _buildCurrencyRow(bool isDark) {
    return ListTile(
      dense: true,
      leading: const Icon(Icons.currency_exchange_outlined,
          size: 20, color: AppColors.labelSecondary),
      title: const Text('幣別', style: TextStyle(fontSize: 15)),
      trailing: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: _selectedCurrency,
          isDense: true,
          style: const TextStyle(
              fontSize: 15, color: AppColors.systemBlue, fontWeight: FontWeight.w500),
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
      ),
    );
  }

  Widget _buildDateRow(bool isDark) {
    return ListTile(
      dense: true,
      leading: const Icon(Icons.calendar_today_outlined,
          size: 20, color: AppColors.labelSecondary),
      title: const Text('日期', style: TextStyle(fontSize: 15)),
      trailing: GestureDetector(
        onTap: _pickDate,
        child: Text(
          '${_selectedDate.year}/${_selectedDate.month}/${_selectedDate.day}',
          style: const TextStyle(
              fontSize: 15, color: AppColors.systemBlue, fontWeight: FontWeight.w500),
        ),
      ),
    );
  }

  Widget _buildTimeRow(bool isDark) {
    return ListTile(
      dense: true,
      leading: const Icon(Icons.access_time_outlined,
          size: 20, color: AppColors.labelSecondary),
      title: const Text('時間', style: TextStyle(fontSize: 15)),
      trailing: GestureDetector(
        onTap: _pickTime,
        child: Text(
          _selectedTime.format(context),
          style: const TextStyle(
              fontSize: 15, color: AppColors.systemBlue, fontWeight: FontWeight.w500),
        ),
      ),
    );
  }

  // ── iOS-style Calculator keyboard ────────────────────────────────────────

  Widget _buildKeyboard(bool isDark) {
    final bgKey  = isDark ? AppColors.bgTertiaryDark : const Color(0xFFD4D4D2);
    final bgNum  = isDark ? const Color(0xFF1C1C1E)  : Colors.white;
    final fgKey  = isDark ? Colors.white : AppColors.labelPrimary;

    const rows = [
      ['7', '8', '9', '⌫'],
      ['4', '5', '6', 'C'],
      ['1', '2', '3', ''],
      ['0', '.', '確認', ''],
    ];

    return Container(
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1C1C1E) : const Color(0xFFD4D4D2),
        border: Border(
          top: BorderSide(
            color: isDark ? AppColors.separatorDark : AppColors.separator,
            width: 0.5,
          ),
        ),
      ),
      padding: const EdgeInsets.fromLTRB(4, 6, 4, 8),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: rows.map((row) {
          return Row(
            children: row.map((key) {
              if (key.isEmpty) return const Expanded(child: SizedBox());

              final isConfirm = key == '確認';
              final isAction  = key == '⌫' || key == 'C';
              final bgColor   = isConfirm ? AppColors.systemGreen
                                          : isAction ? bgKey : bgNum;
              final fgColor   = isConfirm ? Colors.white
                                          : isAction ? fgKey : fgKey;

              return Expanded(
                child: Padding(
                  padding: const EdgeInsets.all(3),
                  child: GestureDetector(
                    onTap: () {
                      HapticFeedback.selectionClick();
                      _onKeyPressed(key);
                    },
                    child: Container(
                      height: 54,
                      decoration: BoxDecoration(
                        color: bgColor,
                        borderRadius: BorderRadius.circular(10),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.12),
                            blurRadius: 1,
                            offset: const Offset(0, 1),
                          ),
                        ],
                      ),
                      child: Center(
                        child: key == '⌫'
                            ? Icon(Icons.backspace_outlined, color: fgColor, size: 20)
                            : Text(
                                key,
                                style: TextStyle(
                                  fontSize: isConfirm ? 16 : 22,
                                  fontWeight: isConfirm
                                      ? FontWeight.w600
                                      : FontWeight.w400,
                                  color: fgColor,
                                  letterSpacing: -0.5,
                                ),
                              ),
                      ),
                    ),
                  ),
                ),
              );
            }).toList(),
          );
        }).toList(),
      ),
    );
  }

  void _onKeyPressed(String key) {
    setState(() {
      if (key == '⌫') {
        final text = _amountController.text;
        if (text.isNotEmpty) {
          _amountController.text = text.substring(0, text.length - 1);
        }
      } else if (key == 'C') {
        _amountController.text = '';
      } else if (key == '確認') {
        // handled outside setState
      } else if (key == '.') {
        if (!_amountController.text.contains('.')) {
          _amountController.text += '.';
        }
      } else {
        final current = _amountController.text;
        if (current.contains('.') && current.split('.')[1].length >= 2) return;
        if (current == '0' && key != '.') {
          _amountController.text = key;
        } else {
          _amountController.text += key;
        }
      }
    });

    if (key == '確認') _save();
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
    );
    if (picked != null) setState(() => _selectedDate = picked);
  }

  Future<void> _pickTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: _selectedTime,
    );
    if (picked != null) setState(() => _selectedTime = picked);
  }

  IconData _getIconData(String code) {
    switch (code) {
      case 'restaurant':    return Icons.restaurant;
      case 'directions_bus':return Icons.directions_bus;
      case 'shopping_cart': return Icons.shopping_cart;
      case 'shopping_bag':  return Icons.shopping_bag;
      case 'movie':         return Icons.movie;
      case 'phone_android': return Icons.phone_android;
      case 'local_hospital':return Icons.local_hospital;
      case 'home':          return Icons.home;
      case 'school':        return Icons.school;
      case 'card_giftcard': return Icons.card_giftcard;
      case 'checkroom':     return Icons.checkroom;
      case 'spa':           return Icons.spa;
      case 'pets':          return Icons.pets;
      case 'fitness_center':return Icons.fitness_center;
      case 'flight':        return Icons.flight;
      case 'security':      return Icons.security;
      case 'receipt_long':  return Icons.receipt_long;
      case 'trending_up':   return Icons.trending_up;
      case 'more_horiz':    return Icons.more_horiz;
      default:              return Icons.circle;
    }
  }
}

