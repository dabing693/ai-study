from src.agent.deep_agent import agent


class MarkdownUtil:
    """Markdown 工具类"""

    @staticmethod
    def array_to_markdown_table(data):
        if not data or len(data) == 0:
            return ""

        lines = []

        # 添加表头
        header = "| " + " | ".join(str(cell) for cell in data[0]) + " |"
        lines.append(header)

        # 添加分隔线
        separator = "|" + "|".join(" :--- " for _ in data[0]) + "|"
        lines.append(separator)

        # 添加数据行
        for row in data[1:]:
            # 处理每个单元格：替换换行符和管道符
            processed_cells = []
            for cell in row:
                cell_str = str(cell) if cell is not None else ""
                # 将 \n 替换为 <br>，将 | 替换为 &#124;
                cell_str = cell_str.replace("\n", "<br>").replace("|", "&#124;")
                processed_cells.append(cell_str)

            row_line = "| " + " | ".join(processed_cells) + " |"
            lines.append(row_line)

        return "\n".join(lines)


def tools_to_markdown_table(user_tools, filesystem_tools, system_tools):
    """
    将工具列表转换为 markdown 表格

    Args:
        user_tools: 用户自定义工具列表
        filesystem_tools: 文件系统工具列表
        system_tools: 系统工具列表

    Returns:
        markdown 表格字符串
    """
    # 构建二维数组
    data = [["类别", "工具名称", "描述"]]

    # 添加用户工具
    for i, tool in enumerate(user_tools):
        category = "用户工具" if i == 0 else ""
        desc = tool['description']
        data.append([category, tool['name'], desc])

    # 添加文件系统工具
    for i, tool in enumerate(filesystem_tools):
        category = "文件系统工具" if i == 0 else ""
        desc = tool['description']
        data.append([category, tool['name'], desc])

    # 添加系统工具
    for i, tool in enumerate(system_tools):
        category = "系统工具" if i == 0 else ""
        desc = tool['description']
        data.append([category, tool['name'], desc])

    return MarkdownUtil.array_to_markdown_table(data)


def print_agent_tools(agent):
    # 获取 agent 的 nodes (LangGraph 的节点)
    if hasattr(agent, 'nodes') and 'tools' in agent.nodes:
        tools_node = agent.nodes['tools']

        # tools_node 是 PregelNode，真正的 ToolNode 在 bound 属性中
        if not hasattr(tools_node, 'bound'):
            return
        tool_node = tools_node.bound
        # 从 ToolNode 获取工具
        if not hasattr(tool_node, 'tools_by_name'):
            return
        tools = tool_node.tools_by_name
        # 分类工具
        user_tools = []
        filesystem_tools = []
        system_tools = []

        for tool_name, tool in tools.items():
            tool_info = {
                'name': tool_name,
                'description': getattr(tool, 'description', '无描述')
            }

            # 分类
            if tool_name in ['ls', 'read_file', 'write_file', 'edit_file', 'glob', 'grep', 'execute']:
                filesystem_tools.append(tool_info)
            elif tool_name in ['write_todos', 'task']:
                system_tools.append(tool_info)
            else:
                user_tools.append(tool_info)

        # 生成 markdown 表格并保存到文件
        save_tools_to_markdown(user_tools, filesystem_tools, system_tools)


def save_tools_to_markdown(user_tools, filesystem_tools, system_tools,
                           filename="agent_tools.md"):
    table = tools_to_markdown_table(user_tools, filesystem_tools, system_tools)

    with open(filename, 'w', encoding='utf-8') as f:
        f.write(table)
    print(f"工具列表已保存到: {filename}")


if __name__ == '__main__':
    print_agent_tools(agent)
