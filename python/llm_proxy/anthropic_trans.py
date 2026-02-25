import json
import uuid
import requests

url = 'https://apis.iflow.cn/v1/chat/completions'
data_prefix = 'data:'


def handle_request(req: dict):
    curr_messages = req.get('messages')
    for it in curr_messages:
        role = it.get('role')
        if role == 'user' and isinstance(it['content'], list):
            try:
                content = '\n'.join([i['text'] for i in it['content']])
                it['content'] = content
            except Exception as e:
                print(f'异常: {repr(e)}')
    system_message = '\n'.join([it['text'] for it in req.get('system')])
    # 思考模式
    curr_messages[-1]['content'] = curr_messages[-1]['content'] + "\nthink:high"
    messages = [{
        'role': 'system',
        'content': system_message
    }] + curr_messages
    req.pop('system')
    req['messages'] = messages
    # 处理工具调用
    req['tools'] = tool_trans(req['tools'])


def msg_id():
    return f'msg_{str(uuid.uuid4()).replace("-", "")[:23]}'


def get_message_start(model: str):
    message_start = {
        "type": "message_start",
        "message": {
            "id": "msg_013Zva2CMHLNnXjNJKqJ2EF",
            "model": "claude-3-5-sonnet-20241022",
            "role": "assistant",
            "type": "message"
        }
    }
    message_start['message']['id'] = msg_id()
    message_start['message']['model'] = model
    return json.dumps(message_start, ensure_ascii=False)


def get_content_block_start():
    content_block_start = {
        "type": "content_block_start",
        "index": 0,
        "content_block": {
            "type": "text"
        }
    }
    return json.dumps(content_block_start, ensure_ascii=False)


def get_content_block_delta(chunk: str):
    if chunk:
        chunk = chunk.strip()
    if chunk.startswith(data_prefix):
        chunk = chunk[len(data_prefix):].strip()
    if not chunk:
        return None
    try:
        content: str = json.loads(chunk)['choices'][0]['delta']['content'].strip()
        if not content:
            return None
        content_block_delta = {
            "type": "content_block_delta",
            "index": 0,
            "delta": {
                "text": "从前"
            }
        }
        content_block_delta['delta']['text'] = content
        return json.dumps(content_block_delta, ensure_ascii=False)
    except Exception as e:
        e_str = repr(e)
        print(f'异常: {e_str}')
        return None


def get_content_block_stop():
    content_block_stop = {
        "type": "content_block_stop",
        "index": 0
    }
    return json.dumps(content_block_stop, ensure_ascii=False)


def get_message_delta():
    message_delta = {
        "type": "message_delta",
        "delta": {
            "stop_reason": "end_turn",
            "usage": {
                "input_tokens": 0,
                "output_tokens": 0
            }
        }
    }
    return json.dumps(message_delta, ensure_ascii=False)


def get_message_stop():
    message_stop = {
        "type": "message_stop"
    }
    return json.dumps(message_stop, ensure_ascii=False)


def tool_trans(tools: list):
    new_tools = []
    for it in tools:
        try:
            tool = {
                "type": "function",
                "function": {
                    "description": it["description"],
                    "name": it["name"],
                    "parameters": it['input_schema']['properties'],
                    "strict": False
                }
            }
            new_tools.append(tool)
        except Exception as e:
            print(f'工具转换异常: {it}, err: {repr(e)}')
    return new_tools


def sync_request(req: dict, headers: dict):
    resp = requests.post(url, json=req, headers=headers).json()
    text = ''
    try:
        text = resp['choices'][0]['message']['content']
    except Exception as e:
        print(f'非流式请求解析异常：{repr(e)}')
        print(json.dumps(resp, ensure_ascii=False))
    anthropic_resp = {
        "content": [
            {
                "text": text,
                "type": "text"
            }
        ],
        "id": msg_id(),
        "model": req['model'],
        "role": "assistant",
        "stop_reason": "end_turn",
        "stop_sequence": None,
        "type": "message",
        "usage": {
            "input_tokens": 0,
            "output_tokens": 0
        }
    }
    return anthropic_resp
