import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import '../models/idea.dart';
import '../models/pulse_message.dart';
import '../models/project_stats.dart';

class ApiService {
  // Use 127.0.0.1 combined with 'adb reverse tcp:8100 tcp:8100'
  static final String baseUrl = 'http://127.0.0.1:8100/api';

  final Dio _dio = Dio(BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 5),
    receiveTimeout: const Duration(seconds: 3),
  ))..interceptors.add(LogInterceptor(responseBody: true, requestBody: true, error: true));

  Future<List<Idea>> getIdeas() async {
    try {
      final response = await _dio.get('/v1/ideas');
      if (response.statusCode == 200) {
        return (response.data as List).map((e) => Idea.fromJson(e)).toList();
      }
      return [];
    } catch (e) {
      debugPrint('Error fetching ideas: $e');
      return [];
    }
  }

  Future<List<PulseMessage>> getPulseMessages() async {
    try {
      final response = await _dio.get('/v1/pulse');
      if (response.statusCode == 200) {
        return (response.data as List).map((e) => PulseMessage.fromJson(e)).toList();
      }
      return [];
    } catch (e) {
      debugPrint('Error fetching pulse messages: $e');
      return [];
    }
  }

  Future<ProjectStats?> getDashboardStats() async {
    try {
      final response = await _dio.get('/v1/dashboard/stats');
      if (response.statusCode == 200) {
        return ProjectStats.fromJson(response.data);
      }
      return null;
    } catch (e) {
      debugPrint('Error fetching dashboard stats: $e');
      return null;
    }
  }
}
