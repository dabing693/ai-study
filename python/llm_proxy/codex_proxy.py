import httpx
from fastapi import FastAPI, Header, Request, HTTPException
from fastapi.responses import StreamingResponse
from codex_trans import *

app = FastAPI()
auth_prefix = 'Bearer '


@app.post("/iflow/v1/responses")
async def iflow_v1_messages(req: dict, request: Request):
    authorization = dict(request.headers).get('authorization', '')
    print(f'api-key: {authorization}')
    headers = {
        "Content-Type": "application/json;charset=UTF-8",
        "Authorization": authorization
    }

    print(json.dumps(req, ensure_ascii=False))
    # 处理请求格式
    handle_request(req)
    dumps = json.dumps(req, ensure_ascii=False)
    print(dumps)
    if not req.get('stream'):
        # 非流式请求
        return sync_request(req, headers)

    async def async_generate():
        # 使用 httpx.AsyncClient 进行异步流式请求
        async with httpx.AsyncClient(timeout=None) as client:
            async with client.stream("POST", url, json=req, headers=headers) as response:
                if response.status_code != 200:
                    error_text = await response.aread()
                    raise HTTPException(status_code=response.status_code, detail=error_text.decode())
                yield get_response_created()
                # 逐块读取并转发
                async for chunk in response.aiter_text():
                    if chunk:
                        # SSE 格式：data: {...}\n\n
                        # yield f"{chunk}"
                        delta = get_output_text_delta(chunk)
                        if delta:
                            yield delta
                # 结束标记
                # yield "data: [DONE]\n\n"
                yield get_response_completed()

    return StreamingResponse(
        async_generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
        }
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
