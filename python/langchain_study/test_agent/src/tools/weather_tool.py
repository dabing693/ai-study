from langchain.tools import tool
import requests
import json
from typing import Optional
import os

@tool
def get_weather(city: str, unit: str = "celsius") -> str:
    """
    查询指定城市的天气信息。

    Args:
        city: 城市名称（必需）
        unit: 温度单位，支持 "celsius"（摄氏度）或 "fahrenheit"（华氏度），默认为 "celsius"

    Returns:
        天气信息字符串，包含温度、天气状况、湿度、风速等信息
    """
    try:
        # 使用 OpenWeatherMap API 的免费版本
        # 注意：你需要注册获取免费的 API key
        api_key = os.getenv('OPENWEATHER_API_KEY')
        if not api_key:
            return "错误：未设置 OPENWEATHER_API_KEY 环境变量。请前往 https://openweathermap.org/ 注册获取免费 API key。"

        # 根据城市名称获取坐标
        geocode_url = f"http://api.openweathermap.org/geo/1.0/direct?q={city}&limit=1&appid={api_key}"
        response = requests.get(geocode_url, timeout=10)
        response.raise_for_status()

        locations = response.json()
        if not locations:
            return f"错误：未找到城市 '{city}' 的信息，请检查城市名称是否正确。"

        # 获取城市坐标
        lat = locations[0]['lat']
        lon = locations[0]['lon']

        # 获取天气数据
        weather_url = f"https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid={api_key}&units=metric"
        response = requests.get(weather_url, timeout=10)
        response.raise_for_status()

        weather_data = response.json()

        # 提取天气信息
        temp_c = weather_data['main']['temp']
        temp_f = temp_c * 9/5 + 32
        description = weather_data['weather'][0]['description']
        humidity = weather_data['main']['humidity']
        wind_speed = weather_data['wind']['speed']
        pressure = weather_data['main']['pressure']

        if unit.lower() == "fahrenheit":
            temp = f"{temp_f:.1f}°F"
        else:
            temp = f"{temp_c:.1f}°C"

        # 格式化输出
        weather_info = f"""
🌤️  {city} 天气信息
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🌡️  温度: {temp}
☁️  天气: {description}
💧  湿度: {humidity}%
💨  风速: {wind_speed} m/s
🔽  气压: {pressure} hPa
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.strip()

        return weather_info

    except requests.RequestException as e:
        return f"网络错误：无法获取天气信息，请检查网络连接。"
    except KeyError as e:
        return f"数据解析错误：返回的天气数据格式不正确。"
    except Exception as e:
        return f"未知错误：{str(e)}"