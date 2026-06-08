import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/database/app_database.dart';
import '../core/constants/app_constants.dart';
import '../core/theme/app_colors.dart';
import '../core/utils/currency_utils.dart';
import '../models/category.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  final AppDatabase _db = AppDatabase.instance;

  List<Category> _categories = [];
  String _defaultCurrency = 'TWD';
  double _budgetAmount = 15000;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final categories = await _db.getAllCategories();
    final currency = await _db.getString('default_currency') ?? 'TWD';
    final budgetStr = await _db.getString('budget_amount');
    final budget = budgetStr != null ? double.tryParse(budgetStr) : null;

    if (mounted) {
      setState(() {
        _categories = categories;
        _defaultCurrency = currency;
        _budgetAmount = budget ?? 15000;
        _isLoading = false;
      });
    }
  }

  Future<void> _saveCurrency(String code) async {
    await _db.setString('default_currency', code);
    setState(() => _defaultCurrency = code);
    HapticFeedback.lightImpact();
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('本位幣已更新')),
      );
    }
  }

  Future<void> _saveBudget() async {
    final controller = TextEditingController(text: _budgetAmount.toStringAsFixed(0));
    final result = await showDialog<double>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('設定每月預算'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: '每月預算金額',
            prefixText: 'NT\$ ',
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () {
              final value = double.tryParse(controller.text);
              if (value != null && value > 0) {
                Navigator.pop(ctx, value);
              }
            },
            child: const Text('儲存'),
          ),
        ],
      ),
    );

    if (result != null) {
      await _db.setString('budget_amount', result.toString());
      setState(() => _budgetAmount = result);
      HapticFeedback.lightImpact();
    }
  }

  Future<void> _showCategoryEditor(Category category) async {
    final nameController = TextEditingController(text: category.name);

    final result = await showDialog<Category>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(category.isSystem ? '檢視分類' : '編輯分類'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (!category.isSystem)
              TextField(
                controller: nameController,
                decoration: const InputDecoration(labelText: '分類名稱'),
                autofocus: true,
              ),
            if (category.isSystem)
              ListTile(
                leading: Icon(_getIconData(category.iconCode)),
                title: Text(category.name),
                subtitle: Text('icon: ${category.iconCode}'),
              ),
          ],
        ),
        actions: [
          if (!category.isSystem)
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
                _deleteCategory(category);
              },
              child: const Text('刪除', style: TextStyle(color: Colors.red)),
            ),
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('取消'),
          ),
          if (!category.isSystem)
            TextButton(
              onPressed: () {
                final newName = nameController.text.trim();
                if (newName.isNotEmpty) {
                  Navigator.pop(ctx, category.copyWith(name: newName));
                }
              },
              child: const Text('儲存'),
            ),
        ],
      ),
    );

    if (result != null) {
      await _db.updateCategory(result);
      _loadSettings();
    }
  }

  Future<void> _addCategory() async {
    final nameController = TextEditingController();
    String selectedIcon = 'more_horiz';

    final result = await showDialog<Category>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: const Text('新增分類'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameController,
                decoration: const InputDecoration(labelText: '分類名稱'),
                autofocus: true,
              ),
              const SizedBox(height: 16),
              Text('選擇圖示', style: Theme.of(ctx).textTheme.bodySmall),
              const SizedBox(height: 8),
              SizedBox(
                height: 200,
                child: GridView.builder(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 6,
                    mainAxisSpacing: 4,
                    crossAxisSpacing: 4,
                  ),
                  itemCount: _commonIcons.length,
                  itemBuilder: (ctx, i) {
                    final icon = _commonIcons[i];
                    final isSelected = icon == selectedIcon;
                    return GestureDetector(
                      onTap: () => setDialogState(() => selectedIcon = icon),
                      child: Container(
                        decoration: BoxDecoration(
                          color: isSelected
                              ? Theme.of(ctx).colorScheme.primaryContainer
                              : null,
                          borderRadius: BorderRadius.circular(8),
                          border: isSelected
                              ? Border.all(
                                  color: Theme.of(ctx).colorScheme.primary,
                                  width: 2)
                              : null,
                        ),
                        child: Icon(_getIconData(icon)),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('取消'),
            ),
            TextButton(
              onPressed: () {
                final name = nameController.text.trim();
                if (name.isNotEmpty) {
                  final maxId = _categories.fold<int>(
                    0, (max, c) => c.id > max ? c.id : max);
                  Navigator.pop(ctx, Category(
                    id: maxId + 1,
                    name: name,
                    iconCode: selectedIcon,
                    sortOrder: 99,
                    isSystem: false,
                  ));
                }
              },
              child: const Text('新增'),
            ),
          ],
        ),
      ),
    );

    if (result != null) {
      await _db.insertCategory(result);
      _loadSettings();
    }
  }

  Future<void> _deleteCategory(Category category) async {
    if (category.isSystem) return;
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('刪除分類'),
        content: Text('確定要刪除「${category.name}」嗎？\n已有此分類的紀錄將不受影響。'),
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
      await _db.deleteCategory(category.id);
      _loadSettings();
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    if (_isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      backgroundColor: isDark ? AppColors.bgPrimaryDark : AppColors.bgSecondary,
      appBar: AppBar(
        backgroundColor: isDark ? AppColors.bgPrimaryDark : AppColors.bgSecondary,
        title: const Text('設定'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 40),
        children: [
          // ─── 預算 ──────────────────────────────────────────────────────
          _sectionLabel('預算'),
          _iosSection(isDark, [
            _iosTile(
              isDark,
              leading: _tileIcon(Icons.wallet_outlined, AppColors.systemGreen),
              title: '每月預算',
              trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                Text(CurrencyUtils.format(_budgetAmount, _defaultCurrency),
                    style: const TextStyle(
                        color: AppColors.labelSecondary, fontSize: 15)),
                const SizedBox(width: 4),
                const Icon(Icons.chevron_right,
                    size: 18, color: AppColors.labelTertiary),
              ]),
              onTap: _saveBudget,
            ),
          ]),

          // ─── 本位幣 ────────────────────────────────────────────────────
          _sectionLabel('本位幣'),
          _iosSection(isDark, [
            for (int i = 0; i < AppConstants.supportedCurrencies.length; i++) ...[
              Builder(builder: (ctx) {
                final code = AppConstants.supportedCurrencies.keys.elementAt(i);
                final symbol = AppConstants.supportedCurrencies[code]!;
                final isSelected = _defaultCurrency == code;
                return _iosTile(
                  isDark,
                  leading: _tileIcon(Icons.attach_money, AppColors.systemBlue),
                  title: '$code  $symbol',
                  trailing: isSelected
                      ? const Icon(Icons.check_circle,
                          color: AppColors.systemGreen, size: 20)
                      : null,
                  onTap: () => _saveCurrency(code),
                );
              }),
              if (i < AppConstants.supportedCurrencies.length - 1)
                _divider(isDark),
            ],
          ]),

          // ─── 分類管理 ──────────────────────────────────────────────────
          _sectionLabel('分類管理'),
          _iosSection(isDark, [
            for (int i = 0; i < _categories.length; i++) ...[
              Builder(builder: (ctx) {
                final cat = _categories[i];
                return _iosTile(
                  isDark,
                  leading: _tileIcon(_getIconData(cat.iconCode),
                      cat.isSystem ? AppColors.systemBlue : AppColors.systemPurple),
                  title: cat.name,
                  subtitle: cat.isSystem ? '系統預設' : null,
                  trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                    if (cat.isSystem)
                      const Icon(Icons.lock_outline,
                          size: 14, color: AppColors.labelTertiary),
                    const SizedBox(width: 4),
                    const Icon(Icons.chevron_right,
                        size: 18, color: AppColors.labelTertiary),
                  ]),
                  onTap: () => _showCategoryEditor(cat),
                );
              }),
              if (i < _categories.length - 1) _divider(isDark),
            ],
            _divider(isDark),
            _iosTile(
              isDark,
              leading: _tileIcon(Icons.add_circle_outline, AppColors.systemGreen),
              title: '新增自訂分類',
              titleColor: AppColors.systemGreen,
              onTap: _addCategory,
            ),
          ]),

          // ─── 關於 ──────────────────────────────────────────────────────
          _sectionLabel('關於'),
          _iosSection(isDark, [
            _iosTile(
              isDark,
              leading: _tileIcon(Icons.info_outline, AppColors.systemBlue),
              title: '版本',
              trailing: Text(AppConstants.appVersion,
                  style: const TextStyle(
                      color: AppColors.labelSecondary, fontSize: 15)),
            ),
            _divider(isDark),
            _iosTile(
              isDark,
              leading: _tileIcon(Icons.shield_outlined, AppColors.systemGreen),
              title: '資料安全',
              subtitle: '所有資料儲存於手機本地，不會上傳至網路',
            ),
          ]),
        ],
      ),
    );
  }

  // ── iOS layout helpers ───────────────────────────────────────────────────

  Widget _sectionLabel(String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 20, 4, 6),
      child: Text(
        title.toUpperCase(),
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w500,
          color: AppColors.labelSecondary,
          letterSpacing: 0.4,
        ),
      ),
    );
  }

  Widget _iosSection(bool isDark, List<Widget> children) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: Material(
        color: isDark ? AppColors.bgSecondaryDark : AppColors.bgPrimary,
        child: Column(mainAxisSize: MainAxisSize.min, children: children),
      ),
    );
  }

  Widget _iosTile(bool isDark, {
    required Widget leading,
    required String title,
    String? subtitle,
    Widget? trailing,
    Color? titleColor,
    VoidCallback? onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
          child: Row(
            children: [
              leading,
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(title,
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w400,
                          color: titleColor,
                        )),
                    if (subtitle != null)
                      Text(subtitle,
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.labelSecondary,
                          )),
                  ],
                ),
              ),
              if (trailing != null) trailing,
            ],
          ),
        ),
      ),
    );
  }

  Widget _tileIcon(IconData icon, Color color) {
    return Container(
      width: 30,
      height: 30,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(7),
      ),
      child: Icon(icon, color: Colors.white, size: 17),
    );
  }

  Widget _divider(bool isDark) => Divider(
      height: 0.5,
      thickness: 0.5,
      indent: 56,
      color: isDark ? AppColors.separatorDark : AppColors.separator);

  static const List<String> _commonIcons = [
    'restaurant', 'directions_bus', 'shopping_cart', 'shopping_bag',
    'movie', 'phone_android', 'local_hospital', 'home', 'school',
    'card_giftcard', 'checkroom', 'spa', 'pets', 'fitness_center',
    'flight', 'security', 'receipt_long', 'trending_up', 'more_horiz',
    'favorite', 'star', 'weekend', 'palette', 'build',
  ];

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
      case 'favorite':      return Icons.favorite;
      case 'star':          return Icons.star;
      case 'weekend':       return Icons.weekend;
      case 'palette':       return Icons.palette;
      case 'build':         return Icons.build;
      default:              return Icons.circle;
    }
  }
}
