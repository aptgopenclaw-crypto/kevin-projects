import 'dart:io';
import 'package:flutter/material.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'app.dart';
import 'core/database/app_database.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // sqflite needs FFI on non-mobile platforms (Linux, Windows, macOS)
  if (!Platform.isAndroid && !Platform.isIOS) {
    sqfliteFfiInit();
    databaseFactory = databaseFactoryFfi;
  }

  // Initialize database (seeds default categories on first run)
  await AppDatabase.instance.database;

  runApp(const CashFlowDiaryApp());
}
