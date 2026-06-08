class Transaction {
  final int? id;
  final double amount;
  final int categoryId;
  final String? note;
  final String? tags;
  final int timestamp;
  final String currency;

  const Transaction({
    this.id,
    required this.amount,
    required this.categoryId,
    this.note,
    this.tags,
    required this.timestamp,
    this.currency = 'TWD',
  });

  Transaction copyWith({
    int? id,
    double? amount,
    int? categoryId,
    String? note,
    String? tags,
    int? timestamp,
    String? currency,
  }) {
    return Transaction(
      id: id ?? this.id,
      amount: amount ?? this.amount,
      categoryId: categoryId ?? this.categoryId,
      note: note ?? this.note,
      tags: tags ?? this.tags,
      timestamp: timestamp ?? this.timestamp,
      currency: currency ?? this.currency,
    );
  }

  Map<String, dynamic> toMap() => {
    if (id != null) 'id': id,
    'amount': amount,
    'category_id': categoryId,
    'note': note,
    'tags': tags,
    'timestamp': timestamp,
    'currency': currency,
  };

  factory Transaction.fromMap(Map<String, dynamic> map) => Transaction(
    id: map['id'] as int?,
    amount: (map['amount'] as num).toDouble(),
    categoryId: map['category_id'] as int,
    note: map['note'] as String?,
    tags: map['tags'] as String?,
    timestamp: map['timestamp'] as int,
    currency: map['currency'] as String? ?? 'TWD',
  );
}
