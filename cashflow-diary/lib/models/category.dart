class Category {
  final int id;
  final String name;
  final String iconCode;
  final int sortOrder;
  final bool isSystem;

  const Category({
    required this.id,
    required this.name,
    required this.iconCode,
    this.sortOrder = 0,
    this.isSystem = false,
  });

  Category copyWith({
    int? id,
    String? name,
    String? iconCode,
    int? sortOrder,
    bool? isSystem,
  }) {
    return Category(
      id: id ?? this.id,
      name: name ?? this.name,
      iconCode: iconCode ?? this.iconCode,
      sortOrder: sortOrder ?? this.sortOrder,
      isSystem: isSystem ?? this.isSystem,
    );
  }

  Map<String, dynamic> toMap() => {
    'id': id,
    'name': name,
    'icon_code': iconCode,
    'sort_order': sortOrder,
    'is_system': isSystem ? 1 : 0,
  };

  factory Category.fromMap(Map<String, dynamic> map) => Category(
    id: map['id'] as int,
    name: map['name'] as String,
    iconCode: map['icon_code'] as String,
    sortOrder: map['sort_order'] as int? ?? 0,
    isSystem: (map['is_system'] as int? ?? 0) == 1,
  );
}
