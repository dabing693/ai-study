#连接手机 idea_app\lib\services\api_service.dart填写baseUrl = 'http://127.0.0.1:8080/api'
adb reverse tcp:8080 tcp:8080
#关闭windows系统防火墙
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled False