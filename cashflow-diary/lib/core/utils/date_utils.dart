import 'package:intl/intl.dart';

class DateUtils {
  DateUtils._();

  /// Get Unix timestamp (seconds) for today at 00:00:00
  static int get todayStart {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day).millisecondsSinceEpoch ~/ 1000;
  }

  /// Get Unix timestamp (seconds) for now
  static int get nowSeconds => DateTime.now().millisecondsSinceEpoch ~/ 1000;

  /// Get the start of the current week (Monday)
  static int get weekStart {
    final now = DateTime.now();
    final monday = now.subtract(Duration(days: now.weekday - 1));
    return DateTime(monday.year, monday.month, monday.day).millisecondsSinceEpoch ~/ 1000;
  }

  /// Get the start of a specific week by offset (0 = this week, -1 = last week)
  static int getWeekStart(int offset) {
    final now = DateTime.now();
    final monday = now.subtract(Duration(days: now.weekday - 1 + offset * 7));
    return DateTime(monday.year, monday.month, monday.day).millisecondsSinceEpoch ~/ 1000;
  }

  /// Get the start of the current month
  static int get monthStart {
    final now = DateTime.now();
    return DateTime(now.year, now.month, 1).millisecondsSinceEpoch ~/ 1000;
  }

  /// Get the start of a month by offset (0 = this month, -1 = last month)
  static int getMonthStart(int offset) {
    final now = DateTime.now();
    final target = DateTime(now.year, now.month + offset, 1);
    return target.millisecondsSinceEpoch ~/ 1000;
  }

  /// Get the end of a month (last second of the month)
  static int getMonthEnd(int offset) {
    final now = DateTime.now();
    final target = DateTime(now.year, now.month + offset + 1, 1);
    return target.millisecondsSinceEpoch ~/ 1000 - 1;
  }

  /// Format timestamp to readable date string
  static String formatDate(int timestampSeconds) {
    final date = DateTime.fromMillisecondsSinceEpoch(timestampSeconds * 1000);
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final target = DateTime(date.year, date.month, date.day);

    final diff = today.difference(target).inDays;

    if (diff == 0) return '今天';
    if (diff == 1) return '昨天';
    if (diff == 2) return '前天';
    if (diff < 7) {
      const weekdays = ['週一', '週二', '週三', '週四', '週五', '週六', '週日'];
      return '本${weekdays[date.weekday - 1]}';
    }
    return DateFormat('M/d').format(date);
  }

  /// Format timestamp to full date string
  static String formatFullDate(int timestampSeconds) {
    final date = DateTime.fromMillisecondsSinceEpoch(timestampSeconds * 1000);
    return DateFormat('yyyy/M/d').format(date);
  }

  /// Format timestamp to time string
  static String formatTime(int timestampSeconds) {
    final date = DateTime.fromMillisecondsSinceEpoch(timestampSeconds * 1000);
    return DateFormat('HH:mm').format(date);
  }

  /// Get current month as YYYY-MM string
  static String get currentMonth {
    final now = DateTime.now();
    return DateFormat('yyyy-MM').format(now);
  }

  /// Get number of days in current month
  static int get daysInCurrentMonth {
    final now = DateTime.now();
    return DateTime(now.year, now.month + 1, 0).day;
  }
}
