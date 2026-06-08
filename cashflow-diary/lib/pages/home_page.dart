import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/database/app_database.dart';
import '../core/theme/app_colors.dart';
import '../core/utils/date_utils.dart' as du;
import '../core/utils/currency_utils.dart';
import '../models/transaction.dart';
import '../models/category.dart';
import 'add_transaction_page.dart';
import 'edit_transaction_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final AppDatabase _db = AppDatabase.instance;
  List<Transaction> _transactions = [];
  Map<int, Category> _categoryMap = {};
  double _todayTotal = 0;
  double _monthTotal = 0;
  String _defaultCurrency = 'TWD';
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    final transactions = await _db.getAllTransactions();
    final categories = await _db.getAllCategories();
    final currency = await _db.getString('default_currency') ?? 'TWD';

    final todayStart = du.DateUtils.todayStart;
    final now = du.DateUtils.nowSeconds;
    final monthStart = du.DateUtils.monthStart;

    final todayTotal = await _db.getTotalBetween(todayStart, now);
    final monthTotal = await _db.getTotalBetween(monthStart, now);

    if (!mounted) return;
    setState(() {
      _transactions = transactions;
      _categoryMap = {for (final c in categories) c.id: c};
      _defaultCurrency = currency;
      _todayTotal = todayTotal;
      _monthTotal = monthTotal;
    });
  }

  List<Transaction> _getFilteredTransactions() {
    if (_searchQuery.isEmpty) return _transactions;
    return _transactions
        .where((t) => (t.note ?? '').toLowerCase().contains(_searchQuery.toLowerCase()))
        .toList();
  }

  Map<String, List<Transaction>> _groupByDate(List<Transaction> transactions) {
    final grouped = <String, List<Transaction>>{};
    for (final t in transactions) {
      final label = du.DateUtils.formatDate(t.timestamp);
      grouped.putIfAbsent(label, () => []).add(t);
    }
    return grouped;
  }

  void _navigateToAdd() async {
    HapticFeedback.lightImpact();
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => AddTransactionPage(
          defaultCurrency: _defaultCurrency,
          lastCategoryId: _transactions.isNotEmpty ? _transactions.first.categoryId : null,
        ),
      ),
    );
    if (result == true) _loadData();
  }

  void _navigateToEdit(Transaction t) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => EditTransactionPage(transaction: t, categoryMap: _categoryMap),
      ),
    );
    if (result == true) _loadData();
  }

  Future<void> _deleteTransaction(Transaction t) async {
    HapticFeedback.mediumImpact();
    final confirm = await showAdaptiveDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog.adaptive(
        title: const Text('刪除紀錄'),
        content: Text('確定要刪除這筆 ${CurrencyUtils.format(t.amount, t.currency)} 的紀錄嗎？'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('取消')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('刪除', style: TextStyle(color: AppColors.systemRed)),
          ),
        ],
      ),
    );
    if (confirm == true && mounted) {
      await _db.deleteTransaction(t.id!);
      _loadData();
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final filtered = _getFilteredTransactions();
    final grouped = _groupByDate(filtered);

    return Scaffold(
      appBar: AppBar(
        title: const Text('現金流日記'),
        actions: [
          IconButton(
            icon: Icon(
              _searchQuery.isEmpty ? Icons.search : Icons.cancel,
              size: 22,
            ),
            onPressed: () {
              if (_searchQuery.isNotEmpty) {
                setState(() => _searchQuery = '');
              } else {
                _showSearchBar(context);
              }
            },
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: CustomScrollView(
        slivers: [
          // Summary cards
          SliverToBoxAdapter(child: _buildSummarySection(isDark)),
          // Search bar chip (when active)
          if (_searchQuery.isNotEmpty)
            SliverToBoxAdapter(child: _buildSearchChip()),
          // Transaction groups
          if (filtered.isEmpty)
            SliverFillRemaining(child: _buildEmptyState())
          else
            SliverPadding(
              padding: const EdgeInsets.only(bottom: 100),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (ctx, i) {
                    final dateLabel = grouped.keys.elementAt(i);
                    final dayTx = grouped[dateLabel]!;
                    final dayTotal = dayTx.fold<double>(0, (s, t) => s + t.amount);
                    return _buildDaySection(isDark, dateLabel, dayTotal, dayTx);
                  },
                  childCount: grouped.length,
                ),
              ),
            ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _navigateToAdd,
        elevation: 3,
        child: const Icon(Icons.add, size: 28),
      ),
    );
  }

  // ── Summary ─────────────────────────────────────────────────────────────

  Widget _buildSummarySection(bool isDark) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      child: Container(
        decoration: BoxDecoration(
          color: isDark ? AppColors.bgSecondaryDark : AppColors.bgPrimary,
          borderRadius: BorderRadius.circular(16),
        ),
        child: IntrinsicHeight(
          child: Row(
            children: [
              Expanded(child: _summaryCell('今日支出', _todayTotal, isExpense: true)),
              VerticalDivider(
                width: 1,
                thickness: 0.5,
                color: isDark ? AppColors.separatorDark : AppColors.separator,
              ),
              Expanded(child: _summaryCell('本月支出', _monthTotal)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _summaryCell(String label, double amount, {bool isExpense = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label,
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: AppColors.labelSecondary,
                letterSpacing: 0.2,
              )),
          const SizedBox(height: 6),
          Text(
            CurrencyUtils.format(amount, _defaultCurrency),
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w700,
              letterSpacing: -0.5,
              color: isExpense && amount > 0 ? AppColors.systemRed : null,
            ),
          ),
        ],
      ),
    );
  }

  // ── Search chip ──────────────────────────────────────────────────────────

  Widget _buildSearchChip() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 4),
      child: Row(
        children: [
          const Icon(Icons.search, size: 14, color: AppColors.labelSecondary),
          const SizedBox(width: 4),
          Text('"$_searchQuery"',
              style: const TextStyle(
                fontSize: 13, color: AppColors.labelSecondary)),
        ],
      ),
    );
  }

  // ── Day section ──────────────────────────────────────────────────────────

  Widget _buildDaySection(
      bool isDark, String label, double total, List<Transaction> txs) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Section header
          Padding(
            padding: const EdgeInsets.fromLTRB(4, 8, 4, 6),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(label,
                    style: const TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: AppColors.labelSecondary,
                      letterSpacing: 0.1,
                    )),
                Text(
                  CurrencyUtils.format(total, _defaultCurrency),
                  style: const TextStyle(
                    fontSize: 13,
                    color: AppColors.labelSecondary,
                    letterSpacing: -0.1,
                  ),
                ),
              ],
            ),
          ),
          // Card group (iOS grouped style)
          ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: Container(
              color: isDark ? AppColors.bgSecondaryDark : AppColors.bgPrimary,
              child: Column(
                children: [
                  for (int i = 0; i < txs.length; i++) ...[
                    _buildTransactionTile(isDark, txs[i]),
                    if (i < txs.length - 1)
                      Divider(
                        height: 0.5,
                        thickness: 0.5,
                        indent: 56,
                        color: isDark ? AppColors.separatorDark : AppColors.separator,
                      ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTransactionTile(bool isDark, Transaction t) {
    final category = _categoryMap[t.categoryId];
    final iconCode = category?.iconCode ?? 'more_horiz';
    final categoryName = category?.name ?? '未分類';
    final color = _iconColor(iconCode);

    return Dismissible(
      key: ValueKey(t.id),
      direction: DismissDirection.endToStart,
      background: Container(
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 20),
        color: AppColors.systemRed,
        child: const Icon(Icons.delete, color: Colors.white, size: 22),
      ),
      confirmDismiss: (_) async {
        await _deleteTransaction(t);
        return false;
      },
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => _navigateToEdit(t),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
            child: Row(
              children: [
                // Icon
                Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(_getIconData(iconCode), color: color, size: 18),
                ),
                const SizedBox(width: 12),
                // Text
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(categoryName,
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w500,
                            letterSpacing: -0.2,
                          )),
                      if (t.note != null && t.note!.isNotEmpty)
                        Text(t.note!,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppColors.labelSecondary,
                            )),
                    ],
                  ),
                ),
                // Amount
                Text(
                  CurrencyUtils.format(t.amount, t.currency),
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: AppColors.systemRed,
                    letterSpacing: -0.3,
                  ),
                ),
                const SizedBox(width: 4),
                const Icon(Icons.chevron_right,
                    size: 16, color: AppColors.labelTertiary),
              ],
            ),
          ),
        ),
      ),
    );
  }

  // ── Empty state ──────────────────────────────────────────────────────────

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 72,
            height: 72,
            decoration: BoxDecoration(
              color: AppColors.labelTertiary.withValues(alpha: 0.2),
              borderRadius: BorderRadius.circular(18),
            ),
            child: const Icon(Icons.receipt_long_outlined,
                size: 36, color: AppColors.labelSecondary),
          ),
          const SizedBox(height: 16),
          const Text('還沒有任何紀錄',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w500,
                color: AppColors.labelSecondary,
              )),
          const SizedBox(height: 6),
          const Text('點擊右下角 + 開始記帳',
              style: TextStyle(fontSize: 13, color: AppColors.labelSecondary)),
        ],
      ),
    );
  }

  // ── Search ───────────────────────────────────────────────────────────────

  void _showSearchBar(BuildContext context) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        title: const Text('搜尋備註'),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(
            hintText: '輸入關鍵字...',
            prefixIcon: Icon(Icons.search),
          ),
          onSubmitted: (v) {
            Navigator.pop(ctx);
            setState(() => _searchQuery = v.trim());
          },
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('取消')),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              setState(() => _searchQuery = controller.text.trim());
            },
            child: const Text('搜尋'),
          ),
        ],
      ),
    );
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  Color _iconColor(String code) {
    switch (code) {
      case 'restaurant':    return AppColors.systemOrange;
      case 'directions_bus':return AppColors.systemBlue;
      case 'shopping_cart': return AppColors.systemGreen;
      case 'shopping_bag':  return AppColors.systemPurple;
      case 'movie':         return AppColors.systemRed;
      case 'phone_android': return AppColors.systemTeal;
      case 'local_hospital':return AppColors.systemRed;
      case 'home':          return AppColors.systemBlue;
      case 'school':        return AppColors.systemIndigo;
      case 'card_giftcard': return AppColors.systemPurple;
      case 'checkroom':     return AppColors.systemPurple;
      case 'spa':           return AppColors.systemGreen;
      case 'pets':          return AppColors.systemOrange;
      case 'fitness_center':return AppColors.systemRed;
      case 'flight':        return AppColors.systemBlue;
      case 'security':      return AppColors.systemIndigo;
      case 'receipt_long':  return AppColors.systemGreen;
      case 'trending_up':   return AppColors.systemGreen;
      default:              return AppColors.labelSecondary;
    }
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

