import '../constants/app_constants.dart';

class CurrencyUtils {
  CurrencyUtils._();

  /// Format amount with currency symbol
  static String format(double amount, String currencyCode) {
    final symbol = AppConstants.supportedCurrencies[currencyCode] ?? currencyCode;

    if (currencyCode == 'JPY' || currencyCode == 'KRW') {
      return '$symbol${amount.round()}';
    }

    return '$symbol${amount.toStringAsFixed(2)}';
  }

  /// Get currency symbol
  static String getSymbol(String currencyCode) {
    return AppConstants.supportedCurrencies[currencyCode] ?? currencyCode;
  }

  /// Get list of available currency codes
  static List<String> get availableCurrencies =>
      AppConstants.supportedCurrencies.keys.toList();
}
