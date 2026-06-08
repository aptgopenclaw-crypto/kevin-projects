class AppConstants {
  AppConstants._();

  // App Info
  static const String appName = 'CashFlow Diary';
  static const String appVersion = '1.0.0';

  // Defaults
  static const double defaultBudgetAmount = 15000;
  static const String defaultCurrency = 'TWD';

  // Supported currencies
  static const Map<String, String> supportedCurrencies = {
    'TWD': 'NT\$',
    'RMB': '¥',
    'USD': '\$',
    'JPY': '¥',
    'EUR': '€',
    'GBP': '£',
    'KRW': '₩',
    'HKD': 'HK\$',
    'SGD': 'S\$',
    'THB': '฿',
  };

  // Database
  static const String databaseName = 'cashflow_diary.db';
  static const int databaseVersion = 1;

  // Animations
  static const Duration hapticDelay = Duration(milliseconds: 50);
  static const Duration pageTransitionDuration = Duration(milliseconds: 300);
}
