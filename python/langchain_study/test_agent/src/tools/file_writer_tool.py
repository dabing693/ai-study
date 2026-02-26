from langchain.tools import tool
import os
from pathlib import Path
import json
from typing import Optional, Union
from datetime import datetime

@tool
def write_to_file(
    filename: str,
    content: str,
    directory: str = "output",
    create_if_not_exists: bool = True,
    overwrite: bool = False
) -> str:
    """
    将内容写入到文本文件中。

    Args:
        filename: 文件名（必需）
        content: 要写入的内容（必需）
        directory: 相对目录，默认为 "output"（可选）
        create_if_not_exists: 如果目录不存在是否创建，默认为 True（可选）
        overwrite: 如果文件已存在是否覆盖，默认为 False（可选）

    Returns:
        操作结果消息
    """
    try:
        # 处理文件路径
        dir_path = Path(directory)
        if create_if_not_exists and not dir_path.exists():
            dir_path.mkdir(parents=True, exist_ok=True)

        file_path = dir_path / filename

        # 检查文件是否已存在
        if file_path.exists() and not overwrite:
            return f"错误：文件 '{filename}' 已存在。设置 overwrite=True 可覆盖文件。"

        # 写入文件
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

        return f"✅ 成功写入文件：{file_path}\n📊 文件大小：{file_path.stat().st_size} 字节"

    except PermissionError:
        return f"权限错误：无法写入文件 '{filename}'，请检查文件权限。"
    except Exception as e:
        return f"写入文件时出错：{str(e)}"

@tool
def append_to_file(
    filename: str,
    content: str,
    directory: str = "output",
    create_if_not_exists: bool = True,
    add_timestamp: bool = True
) -> str:
    """
    向现有文件追加内容。

    Args:
        filename: 文件名（必需）
        content: 要追加的内容（必需）
        directory: 相对目录，默认为 "output"（可选）
        create_if_not_exists: 如果文件不存在是否创建，默认为 True（可选）
        add_timestamp: 是否在内容前添加时间戳，默认为 True（可选）

    Returns:
        操作结果消息
    """
    try:
        # 处理文件路径
        dir_path = Path(directory)
        if create_if_not_exists and not dir_path.exists():
            dir_path.mkdir(parents=True, exist_ok=True)

        file_path = dir_path / filename

        # 如果文件不存在且不允许创建
        if not file_path.exists() and not create_if_not_exists:
            return f"错误：文件 '{filename}' 不存在。"

        # 准备要追加的内容
        if add_timestamp:
            timestamp = datetime.now().strftime("[%Y-%m-%d %H:%M:%S]")
            content_to_add = f"\n\n{timestamp}\n{content}"
        else:
            content_to_add = f"\n\n{content}"

        # 追加内容
        with open(file_path, 'a', encoding='utf-8') as f:
            f.write(content_to_add)

        return f"✅ 成功追加内容到文件：{file_path}"

    except PermissionError:
        return f"权限错误：无法写入文件 '{filename}'，请检查文件权限。"
    except Exception as e:
        return f"追加内容时出错：{str(e)}"

@tool
def create_json_file(
    filename: str,
    data: str,
    directory: str = "output",
    pretty_print: bool = True
) -> str:
    """
    创建 JSON 文件。

    Args:
        filename: 文件名（必需）
        data: JSON 字符串格式的数据（必需）
        directory: 相对目录，默认为 "output"（可选）
        pretty_print: 是否格式化 JSON，默认为 True（可选）

    Returns:
        操作结果消息
    """
    try:
        # 处理文件路径
        dir_path = Path(directory)
        if not dir_path.exists():
            dir_path.mkdir(parents=True, exist_ok=True)

        file_path = dir_path / filename

        # 解析 JSON 字符串
        if pretty_print:
            json_content = json.dumps(json.loads(data), ensure_ascii=False, indent=2)
        else:
            json_content = data

        # 写入文件
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(json_content)

        return f"✅ 成功创建 JSON 文件：{file_path}\n📊 文件大小：{file_path.stat().st_size} 字节"

    except json.JSONDecodeError:
        return "错误：提供的 JSON 数据格式不正确，请检查。"
    except Exception as e:
        return f"创建 JSON 文件时出错：{str(e)}"

# 为了兼容性，也提供一个简单的写入工具
@tool
def simple_write_file(filename: str, content: str) -> str:
    """
    简化的文件写入工具。

    Args:
        filename: 文件名
        content: 文件内容

    Returns:
        操作结果消息
    """
    try:
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(content)
        return f"文件 '{filename}' 写入成功"
    except Exception as e:
        return f"写入失败：{str(e)}"