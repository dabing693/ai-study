import os
from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch

os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"

app = FastAPI()
MODEL_NAME = "IDEA-CCNL/Randeng-T5-784M-MultiTask-Chinese"
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForSeq2SeqLM.from_pretrained(MODEL_NAME)

device = "cuda" if torch.cuda.is_available() else "cpu"
model.to(device)
model.eval()


class RewriteRequest(BaseModel):
    history: list[str]
    query: str


class RewriteResponse(BaseModel):
    rewritten_query: str


def rewrite_query(history, query):
    """
    {
      "history": [
        "上海今天的天气咋样",
        "今天多云25度"
      ],
      "query": "那北京呢"
    }
    """

    if history:
        history_text = " ".join(history)
        # input_text = f"query改写: {history_text} [SEP] {query}"
        # input_text = f"将对话改写为独立的问题: [历史] {' | '.join(history)} [当前] {query}"
        # 构造一个极简的示例
        examples = "示例1: [历史] 上海天气如何 | 多云 [当前] 那北京呢 -> 北京今天天气如何\n"
        current_task = f"任务: [历史] {' | '.join(history)} [当前] {query} ->"
        input_text = examples + current_task
    else:
        input_text = query
    print(input_text)
    inputs = tokenizer(
        input_text,
        return_tensors="pt",
        truncation=True,
        max_length=512
    ).to(device)

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=64,
            num_beams=5,
            length_penalty=0.6,  # 设为小于 1.0 的值，鼓励模型输出更简短、精炼的改写
            no_repeat_ngram_size=2,
            early_stopping=True
        )

    rewritten = tokenizer.decode(outputs[0], skip_special_tokens=True)

    return rewritten


@app.post("/rewrite", response_model=RewriteResponse)
def rewrite(req: RewriteRequest):
    rewritten = rewrite_query(req.history, req.query)

    return RewriteResponse(
        rewritten_query=rewritten
    )


@app.get("/health")
def health():
    return {"status": "ok"}


# set HF_ENDPOINT=https://hf-mirror.com
# uvicorn mengzi:app --host 0.0.0.0 --port 8000
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
