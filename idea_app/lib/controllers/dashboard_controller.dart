import 'package:get/get.dart';
import '../models/project_stats.dart';
import '../services/api_service.dart';

class DashboardController extends GetxController {
  final ApiService _apiService = ApiService();
  
  var stats = Rxn<ProjectStats>();
  var isLoading = true.obs;

  @override
  void onInit() {
    super.onInit();
    fetchStats();
  }

  Future<void> fetchStats() async {
    try {
      isLoading.value = true;
      final result = await _apiService.getDashboardStats();
      if (result != null) {
        stats.value = result;
      }
    } finally {
      isLoading.value = false;
    }
  }
}
