import 'package:get/get.dart';
import '../models/idea.dart';
import '../services/api_service.dart';

class IdeaFeedController extends GetxController {
  final ApiService _apiService = ApiService();
  
  var ideas = <Idea>[].obs;
  var isLoading = true.obs;

  @override
  void onInit() {
    super.onInit();
    fetchIdeas();
  }

  Future<void> fetchIdeas() async {
    try {
      isLoading.value = true;
      final result = await _apiService.getIdeas();
      ideas.assignAll(result);
    } finally {
      isLoading.value = false;
    }
  }
}
