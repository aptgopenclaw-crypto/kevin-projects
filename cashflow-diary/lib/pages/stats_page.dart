import 'package:flutter/material.dart';
import '../core/database/app_database.dart';
import '../core/utils/date_utils.dart' as du;
import '../core/utils/currency_utils.dart';
import '../core/theme/app_colors.dart';
import 'package:fl_chart/fl_chart.dart';

class StatsPage extends StatefulWidget {
  const StatsPage({super.key});

  @override
  State<StatsPage> createState() => _StatsPageState();
}

class _StatsPageState extends State<StatsPage> with SingleTickerProviderStateMixin {
  final AppDatabase _db = AppDatabase.instance;
  late TabController _tabController;

  // Data
  double _weeklyTotal = 0;
  double _weeklyLastPeriodTotal = 0;
  double _monthlyTotal = 0;
  double _monthlyLastPeriodTotal = 0;
  double _dailyAverage = 0;
  double _projectedTotal = 0;
  double _budgetAmount = 15000;

  List<Map<String, dynamic>> _dailyData = [];
  List<Map<String, dynamic>> _categoryData = [];
  List<Map<String, dynamic>> _monthlyDailyData = [];
  List<Map<String, dynamic>> _monthlyCategoryData = [];

  String _defaultCurrency = 'TWD';
  bool _isLoading = true;

  // Week offset: 0 = this week, -1 = last week
  int _weekOffset = 0;
  // Month offset: 0 = this month, -1 = last month
  int _monthOffset = 0;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _tabController.addListener(() {
      if (!_tabController.indexIsChanging) {
        setState(() {}); // Rebuild on tab switch
      }
    });
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);

    final currency = await _db.getString('default_currency') ?? 'TWD';
    final budgetSetting = await _db.getString('budget_amount');
    final budget = budgetSetting != null ? double.tryParse(budgetSetting) : null;

    // Weekly data
    final weekStart = du.DateUtils.getWeekStart(_weekOffset);
    final lastWeekStart = du.DateUtils.getWeekStart(_weekOffset - 1);
    final weekEnd = du.DateUtils.nowSeconds;

    _weeklyTotal = await _db.getTotalBetween(weekStart, weekEnd);
    _weeklyLastPeriodTotal = await _db.getTotalBetween(lastWeekStart, weekStart);
    _dailyData = await _db.getDailyTotals(weekStart, weekEnd);
    _categoryData = await _db.getCategoryTotals(weekStart, weekEnd);

    // Monthly data
    final monthStart = du.DateUtils.getMonthStart(_monthOffset);
    final monthEnd = du.DateUtils.getMonthEnd(_monthOffset);
    final lastMonthStart = du.DateUtils.getMonthStart(_monthOffset - 1);
    final lastMonthEnd = du.DateUtils.getMonthEnd(_monthOffset - 1);

    _monthlyTotal = await _db.getTotalBetween(monthStart, monthEnd);
    _monthlyLastPeriodTotal = await _db.getTotalBetween(lastMonthStart, lastMonthEnd);
    _monthlyDailyData = await _db.getDailyTotals(monthStart, monthEnd);
    _monthlyCategoryData = await _db.getCategoryTotals(monthStart, monthEnd);

    // Daily average & projection
    final daysPassed = DateTime.now().day;
    _dailyAverage = daysPassed > 0 ? _monthlyTotal / daysPassed : 0;
    final daysInMonth = du.DateUtils.daysInCurrentMonth;
    _projectedTotal = _dailyAverage * daysInMonth;

    _defaultCurrency = currency;
    _budgetAmount = budget ?? 15000;

    if (mounted) setState(() => _isLoading = false);
  }

  String _formatPercentChange(double current, double previous) {
    if (previous <= 0) return '';
    final change = ((current - previous) / previous * 100);
    final symbol = change >= 0 ? '🔺' : '🔻';
    return '$symbol ${change.abs().toStringAsFixed(1)}%';
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('統計'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '週統計'),
            Tab(text: '月統計'),
          ],
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : TabBarView(
              controller: _tabController,
              children: [
                _buildWeeklyTab(theme),
                _buildMonthlyTab(theme),
              ],
            ),
    );
  }

  // ===================== Weekly Tab =====================

  Widget _buildWeeklyTab(ThemeData theme) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Time switcher
          Row(
            children: [
              _buildTimeToggle(-1, '上週'),
              const SizedBox(width: 8),
              _buildTimeToggle(0, '本週'),
              const Spacer(),
              // Future: "過去7天" toggle
            ],
          ),
          const SizedBox(height: 20),

          // Total spending
          Text('本週總支出', style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          )),
          const SizedBox(height: 4),
          Row(
            children: [
              Text(
                CurrencyUtils.format(_weeklyTotal, _defaultCurrency),
                style: theme.textTheme.headlineLarge?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(width: 12),
              if (_weeklyLastPeriodTotal > 0)
                Text(
                  _formatPercentChange(_weeklyTotal, _weeklyLastPeriodTotal),
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: _weeklyTotal > _weeklyLastPeriodTotal
                        ? Colors.red : Colors.green,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 24),

          // Daily bar chart
          Text('每日花費', style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          )),
          const SizedBox(height: 12),
          SizedBox(
            height: 200,
            child: _buildDailyBarChart(theme),
          ),
          const SizedBox(height: 24),

          // Category pie chart
          Text('分類佔比', style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          )),
          const SizedBox(height: 12),
          SizedBox(
            height: 220,
            child: _buildCategoryPieChart(theme),
          ),
          const SizedBox(height: 12),
          _buildPieLegend(theme),
        ],
      ),
    );
  }

  Widget _buildTimeToggle(int offset, String label) {
    final isSelected = (offset == _weekOffset);
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (v) {
        if (v) {
          setState(() => _weekOffset = offset);
          _loadData();
        }
      },
    );
  }

  // ===================== Monthly Tab =====================

  Widget _buildMonthlyTab(ThemeData theme) {
    final budgetPercent = _budgetAmount > 0 ? (_monthlyTotal / _budgetAmount) : 0.0;
    final isOverBudget = budgetPercent > 1.0;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Month switcher
          Row(
            children: [
              _buildMonthToggle(-1, '上月'),
              const SizedBox(width: 8),
              _buildMonthToggle(0, '本月'),
            ],
          ),
          const SizedBox(height: 20),

          // Total spending
          Text('本月總支出', style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          )),
          const SizedBox(height: 4),
          Row(
            children: [
              Text(
                CurrencyUtils.format(_monthlyTotal, _defaultCurrency),
                style: theme.textTheme.headlineLarge?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(width: 12),
              if (_monthlyLastPeriodTotal > 0)
                Text(
                  _formatPercentChange(_monthlyTotal, _monthlyLastPeriodTotal),
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: _monthlyTotal > _monthlyLastPeriodTotal
                        ? Colors.red : Colors.green,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 16),

          // Budget progress bar
          _buildBudgetProgress(theme, budgetPercent, isOverBudget),
          const SizedBox(height: 24),

          // Daily average & projection
          Row(
            children: [
              _buildStatCard(theme, '日均花費',
                  CurrencyUtils.format(_dailyAverage, _defaultCurrency)),
              const SizedBox(width: 12),
              _buildStatCard(theme, '預估總支出',
                  CurrencyUtils.format(_projectedTotal, _defaultCurrency)),
            ],
          ),
          const SizedBox(height: 24),

          // Daily trend chart
          Text('每日花費趨勢', style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          )),
          const SizedBox(height: 12),
          SizedBox(
            height: 200,
            child: _buildTrendLineChart(theme),
          ),
          const SizedBox(height: 24),

          // Category ranking bar chart
          Text('分類排行榜', style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          )),
          const SizedBox(height: 12),
          _buildCategoryRanking(theme),
        ],
      ),
    );
  }

  Widget _buildMonthToggle(int offset, String label) {
    final isSelected = (offset == _monthOffset);
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (v) {
        if (v) {
          setState(() => _monthOffset = offset);
          _loadData();
        }
      },
    );
  }

  Widget _buildBudgetProgress(ThemeData theme, double percent, bool isOverBudget) {
    final clampedPercent = percent.clamp(0.0, 1.2);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('預算進度', style: theme.textTheme.bodyMedium),
                Text(
                  '${CurrencyUtils.format(_monthlyTotal, _defaultCurrency)} / '
                  '${CurrencyUtils.format(_budgetAmount, _defaultCurrency)}',
                  style: theme.textTheme.bodySmall,
                ),
              ],
            ),
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: LinearProgressIndicator(
                value: clampedPercent > 1.0 ? 1.0 : clampedPercent,
                minHeight: 12,
                backgroundColor: theme.colorScheme.surfaceContainerHighest,
                valueColor: AlwaysStoppedAnimation(
                  isOverBudget ? Colors.red : theme.colorScheme.primary,
                ),
              ),
            ),
            if (isOverBudget) ...[
              const SizedBox(height: 8),
              Row(
                children: [
                  const Icon(Icons.warning_amber_rounded, color: Colors.red, size: 16),
                  const SizedBox(width: 4),
                  Text(
                    '已超支 ${CurrencyUtils.format(_monthlyTotal - _budgetAmount, _defaultCurrency)}',
                    style: theme.textTheme.bodySmall?.copyWith(color: Colors.red),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildStatCard(ThemeData theme, String label, String value) {
    return Expanded(
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              )),
              const SizedBox(height: 8),
              Text(value, style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.bold,
              )),
            ],
          ),
        ),
      ),
    );
  }

  // ===================== Charts =====================

  Widget _buildDailyBarChart(ThemeData theme) {
    final maxY = _dailyData.isEmpty
        ? 1000.0
        : (_dailyData.map((d) => (d['total'] as num).toDouble()).reduce(
              (a, b) => a > b ? a : b) * 1.2);

    final weekDays = ['週一', '週二', '週三', '週四', '週五', '週六', '週日'];
    final Map<int, double> dayTotals = {};
    for (final d in _dailyData) {
      final day = DateTime.fromMillisecondsSinceEpoch(
          (d['day'] as int) * 1000).weekday;
      dayTotals[day] = (d['total'] as num).toDouble();
    }

    return BarChart(
      BarChartData(
        alignment: BarChartAlignment.spaceAround,
        maxY: maxY,
        barTouchData: BarTouchData(
          touchTooltipData: BarTouchTooltipData(
            getTooltipItem: (group, groupIndex, rod, rodIndex) {
              final dayIndex = group.x.toInt();
              return BarTooltipItem(
                '${weekDays[dayIndex]}\n${CurrencyUtils.format(rod.toY, _defaultCurrency)}',
                const TextStyle(color: Colors.white, fontSize: 12),
              );
            },
          ),
        ),
        titlesData: FlTitlesData(
          show: true,
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (value, meta) {
                final idx = value.toInt();
                if (idx < 0 || idx >= weekDays.length) return const SizedBox();
                return Text(weekDays[idx], style: const TextStyle(fontSize: 10));
              },
            ),
          ),
          leftTitles: AxisTitles(
            sideTitles: SideTitles(showTitles: false),
          ),
          topTitles: AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles: AxisTitles(sideTitles: SideTitles(showTitles: false)),
        ),
        borderData: FlBorderData(show: false),
        barGroups: List.generate(7, (i) {
          final day = i + 1; // weekday 1-7 (Mon-Sun)
          return BarChartGroupData(
            x: i,
            barRods: [
              BarChartRodData(
                toY: dayTotals[day] ?? 0,
                color: AppColors.chartColors[i % AppColors.chartColors.length],
                width: 16,
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(6),
                  topRight: Radius.circular(6),
                ),
              ),
            ],
          );
        }),
        gridData: FlGridData(show: false),
      ),
    );
  }

  Widget _buildCategoryPieChart(ThemeData theme) {
    if (_categoryData.isEmpty) {
      return Center(
        child: Text('本週尚無數據', style: theme.textTheme.bodyMedium?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        )),
      );
    }

    final total = _categoryData.fold<double>(
      0, (sum, d) => sum + (d['total'] as num).toDouble());

    return PieChart(
      PieChartData(
        sections: List.generate(_categoryData.length, (i) {
          final d = _categoryData[i];
          final value = (d['total'] as num).toDouble();
          final percent = total > 0 ? (value / total * 100) : 0.0;

          return PieChartSectionData(
            value: value,
            color: AppColors.chartColors[i % AppColors.chartColors.length],
            radius: 55,
            title: '${percent.toStringAsFixed(0)}%',
            titleStyle: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
            badgeWidget: null,
          );
        }),
        sectionsSpace: 2,
        centerSpaceRadius: 36,
        pieTouchData: PieTouchData(
          touchCallback: (event, response) {},
        ),
      ),
    );
  }

  Widget _buildPieLegend(ThemeData theme) {
    if (_categoryData.isEmpty) return const SizedBox();

    final total = _categoryData.fold<double>(
      0, (sum, d) => sum + (d['total'] as num).toDouble());

    return Wrap(
      spacing: 12,
      runSpacing: 8,
      children: List.generate(_categoryData.length, (i) {
        final d = _categoryData[i];
        final name = d['name'] as String;
        final value = (d['total'] as num).toDouble();
        final percent = total > 0 ? (value / total * 100) : 0.0;
        final color = AppColors.chartColors[i % AppColors.chartColors.length];

        return Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                color: color,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 4),
            Text(
              '$name ${percent.toStringAsFixed(0)}%',
              style: const TextStyle(fontSize: 12, color: AppColors.labelSecondary),
            ),
          ],
        );
      }),
    );
  }

  Widget _buildTrendLineChart(ThemeData theme) {
    if (_monthlyDailyData.isEmpty) {
      return Center(child: Text('本月尚無數據', style: theme.textTheme.bodyMedium));
    }

    final spots = _monthlyDailyData.asMap().entries.map((entry) {
      return FlSpot(
        entry.key.toDouble(),
        (entry.value['total'] as num).toDouble(),
      );
    }).toList();

    return LineChart(
      LineChartData(
        lineBarsData: [
          LineChartBarData(
            spots: spots,
            isCurved: true,
            color: theme.colorScheme.primary,
            barWidth: 3,
            dotData: FlDotData(
              show: true,
              getDotPainter: (spot, percent, barData, index) {
                return FlDotCirclePainter(
                  radius: 4,
                  color: theme.colorScheme.primary,
                  strokeWidth: 0,
                );
              },
            ),
            belowBarData: BarAreaData(
              show: true,
              color: theme.colorScheme.primary.withOpacity(0.1),
            ),
          ),
        ],
        titlesData: FlTitlesData(
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              interval: 5,
              getTitlesWidget: (value, meta) {
                final day = value.toInt() + 1;
                if (day > 31) return const SizedBox();
                return Text('$day', style: const TextStyle(fontSize: 10));
              },
            ),
          ),
          leftTitles: AxisTitles(
            sideTitles: SideTitles(showTitles: false),
          ),
          topTitles: AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles: AxisTitles(sideTitles: SideTitles(showTitles: false)),
        ),
        borderData: FlBorderData(show: false),
        gridData: FlGridData(
          show: true,
          drawVerticalLine: false,
          horizontalInterval: null,
        ),
      ),
    );
  }

  Widget _buildCategoryRanking(ThemeData theme) {
    if (_monthlyCategoryData.isEmpty) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Center(
            child: Text('本月尚無數據', style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            )),
          ),
        ),
      );
    }

    final maxTotal = (_monthlyCategoryData.first['total'] as num).toDouble();

    return Column(
      children: _monthlyCategoryData.map((d) {
        final name = d['name'] as String;
        final total = (d['total'] as num).toDouble();
        final ratio = maxTotal > 0 ? total / maxTotal : 0.0;
        final iconCode = d['icon_code'] as String? ?? 'more_horiz';

        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 6),
          child: Row(
            children: [
              Icon(_getIconData(iconCode), size: 20,
                  color: theme.colorScheme.onSurfaceVariant),
              const SizedBox(width: 12),
              SizedBox(width: 60, child: Text(name, style: theme.textTheme.bodyMedium)),
              const SizedBox(width: 8),
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: ratio,
                    minHeight: 8,
                    backgroundColor: theme.colorScheme.surfaceContainerHighest,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              SizedBox(
                width: 80,
                child: Text(
                  CurrencyUtils.format(total, _defaultCurrency),
                  style: theme.textTheme.bodySmall?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                  textAlign: TextAlign.right,
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }

  IconData _getIconData(String code) {
    switch (code) {
      case 'restaurant': return Icons.restaurant;
      case 'directions_bus': return Icons.directions_bus;
      case 'shopping_cart': return Icons.shopping_cart;
      case 'shopping_bag': return Icons.shopping_bag;
      case 'movie': return Icons.movie;
      case 'phone_android': return Icons.phone_android;
      case 'local_hospital': return Icons.local_hospital;
      case 'home': return Icons.home;
      case 'school': return Icons.school;
      case 'card_giftcard': return Icons.card_giftcard;
      case 'checkroom': return Icons.checkroom;
      case 'spa': return Icons.spa;
      case 'pets': return Icons.pets;
      case 'fitness_center': return Icons.fitness_center;
      case 'flight': return Icons.flight;
      case 'security': return Icons.security;
      case 'receipt_long': return Icons.receipt_long;
      case 'trending_up': return Icons.trending_up;
      case 'more_horiz': return Icons.more_horiz;
      default: return Icons.circle;
    }
  }
}
