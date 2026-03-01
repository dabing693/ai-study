import 'package:get/get.dart';
import '../models/pulse_message.dart';
import '../services/api_service.dart';

class PulseController extends GetxController {
  final ApiService _apiService = ApiService();
  
  var messages = <PulseMessage>[].obs;
  var isLoading = true.obs;

  @override
  void onInit() {
    super.onInit();
    fetchMessages();
  }

  Future<void> fetchMessages() async {
    try {
      isLoading.value = true;
      final result = await _apiService.getPulseMessages();
      messages.assignAll(result);
    } finally {
      isLoading.value = false;
    }
  }
}
