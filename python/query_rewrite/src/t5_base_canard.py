from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch

app = FastAPI()

MODEL_NAME = "castorini/t5-base-canard"

# 加载模型
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
    history: ["北京天气怎么样", "今天多云"]
    query: "那上海呢"
    """

    history_text = " ||| ".join(history)

    input_text = f"{history_text} ||| {query}"

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
            num_beams=4
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


# uvicorn t5_base_canard:app --host 0.0.0.0 --port 8000
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
