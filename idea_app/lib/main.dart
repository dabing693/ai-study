import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'theme/app_theme.dart';
import 'pages/home_layout.dart';

void main() {
  runApp(const IdeaApp());
}

class IdeaApp extends StatelessWidget {
  const IdeaApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      title: 'Idea Discovery App',
      theme: AppTheme.lightTheme,
      home: const HomeLayout(),
      debugShowCheckedModeBanner: false,
    );
  }
}
