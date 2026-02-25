import time

import requests
import json
import uuid

url = 'https://apis.iflow.cn/v1/chat/completions'
data_prefix = 'data:'


def handle_request(req: dict):
    messages = [{
        "role": "system",
        "content": req.pop('instructions')
    }]
    input_messages = req.pop('input')
    for it in input_messages:
        if it['role'] not in ['system', 'user', 'assistant', 'tool']:
            print(f"位置的角色类型: {it['role']}")
            it['role'] = 'user'
        it['content'] = '\n'.join([i['text'] for i in it['content']])
    req['messages'] = messages + input_messages
    # 处理工具调用
    req['tools'] = tool_trans(req['tools'])


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


def msg_id():
    return f'msg_{str(uuid.uuid4()).replace("-", "")[:23]}'


def resp_id():
    return f'resp_{str(uuid.uuid4()).replace("-", "")}'


def tool_trans(tools: list):
    new_tools = []
    for it in tools:
        try:
            tool = {
                "type": it.get("type", 'function'),
                "function": {
                    "description": it.get("description", it.get('type')),
                    "name": it.get("name", it.get('type')),
                    "parameters": it.get('parameters', {}),
                    "strict": it.get("strict")
                }
            }
            new_tools.append(tool)
        except Exception as e:
            print(f'工具转换异常: {it}, err: {repr(e)}')
    return new_tools


def get_response_created():
    response_created = {
        "type": "response.created",
        "response": {
            "id": resp_id(),
            "object": "response",
            "created_at": time.time(),
            "status": "in_progress",
            "completed_at": None,
            "error": None,
            "incomplete_details": None,
            "instructions": None,
            "max_output_tokens": None,
            "model": "gpt-4o-2024-08-06",
            "output": [],
            "parallel_tool_calls": True,
            "previous_response_id": None,
            "reasoning": {
                "effort": None,
                "summary": None
            },
            "store": True,
            "temperature": 1,
            "text": {
                "format": {
                    "type": "text"
                }
            },
            "tool_choice": "auto",
            "tools": [],
            "top_p": 1,
            "truncation": "disabled",
            "usage": None,
            "user": None,
            "metadata": {}
        },
        "sequence_number": 1
    }
    return data_prefix + json.dumps(response_created, ensure_ascii=False)


def get_response_completed():
    response_created = {
        "type": "response.completed",
        "response": {
            "id": resp_id(),
            "object": "response",
            "created_at": time.time(),
            "status": "completed",
            "completed_at": time.time() - 20,
            "error": None,
            "incomplete_details": None,
            "input": [],
            "instructions": None,
            "max_output_tokens": None,
            "model": "gpt-4o-mini-2024-07-18",
            "output": [

            ],
            "previous_response_id": None,
            "reasoning_effort": None,
            "store": False,
            "temperature": 1,
            "text": {
                "format": {
                    "type": "text"
                }
            },
            "tool_choice": "auto",
            "tools": [],
            "top_p": 1,
            "truncation": "disabled",
            "usage": {
                "input_tokens": 0,
                "output_tokens": 0,
                "output_tokens_details": {
                    "reasoning_tokens": 0
                },
                "total_tokens": 0
            },
            "user": None,
            "metadata": {}
        },
        "sequence_number": 1
    }
    output = [
        {
            "id": "msg_123",
            "type": "message",
            "role": "assistant",
            "content": [
                {
                    "type": "output_text",
                    "text": "In a shimmering forest under a sky full of stars, a lonely unicorn named Lila discovered a hidden pond that glowed with moonlight. Every night, she would leave sparkling, magical flowers by the water's edge, hoping to share her beauty with others. One enchanting evening, she woke to find a group of friendly animals gathered around, eager to be friends and share in her magic.",
                    "annotations": []
                }
            ]
        }
    ]
    return data_prefix + json.dumps(response_created, ensure_ascii=False)


def get_output_text_delta(chunk: str):
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
        delta = {
            "type": "response.in_progress",
            "response": {
                "id": resp_id(),
                "object": "response",
                "created_at": time.time(),
                "status": "in_progress",
                "completed_at": None,
                "error": None,
                "incomplete_details": None,
                "instructions": None,
                "max_output_tokens": None,
                "model": "gpt-4o-2024-08-06",
                "output": [],
                "parallel_tool_calls": True,
                "previous_response_id": None,
                "reasoning": {
                    "effort": None,
                    "summary": None
                },
                "store": True,
                "temperature": 1,
                "text": {
                    "format": {
                        "type": "text"
                    }
                },
                "tool_choice": "auto",
                "tools": [],
                "top_p": 1,
                "truncation": "disabled",
                "usage": None,
                "user": None,
                "metadata": {}
            },
            "sequence_number": 1
        }
        output = [{
            'id': str(uuid.uuid4()),
            'content': {
                "text": content
            },
            "role": 'assistant',
            "type": "message"
        }]
        delta['response']['output'] = output
        return data_prefix + json.dumps(delta, ensure_ascii=False)
    except Exception as e:
        e_str = repr(e)
        print(f'异常: {e_str}')
        return None
