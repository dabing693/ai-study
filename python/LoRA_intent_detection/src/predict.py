import os

from LoRA_intent_detection.src import config

# os.environ["HF_HUB_OFFLINE"] = "1"  # 强制离线模式
os.environ["HF_HOME"] = r"D:\opt\apps\hf_cache"  # 指定你的缓存目录
import torch
from peft import PeftModel
from transformers import AutoTokenizer, AutoModelForCausalLM

# -------------------------- 全局配置（根据你的实际路径/模型名修改） --------------------------
# 模型路径（本地训练好的模型目录 或 huggingface模型名）
BASE_MODEL_PATH = r"D:\opt\apps\hf_cache\hub\models--Qwen--Qwen2.5-0.5B\snapshots\060db6499f32faf8b98477b0a26969ef7d8b9987"  # 本地基础模型路径（需要你先下载好）
LORA_PATH = config.MODELS_DIR / "qwen_finance_intent_lora"
# 设备配置（自动检测GPU/CPU）
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
# 推理参数（可根据效果调整）
MAX_NEW_TOKENS = 10  # 意图标签短，最多生成10个token
TEMPERATURE = 0.1  # 降低随机性，提升确定性
TOP_P = 0.9
DO_SAMPLE = True  # 确定性生成


def load_model_and_tokenizer():
    """
    加载训练好的模型和分词器
    :param model_path: 模型路径
    :param device: 运行设备（cuda/cpu）
    :return: tokenizer, model
    """
    try:
        # 1. 加载基础模型
        print("加载基础模型...")
        base_model = AutoModelForCausalLM.from_pretrained(
            BASE_MODEL_PATH,
            dtype=torch.float16 if torch.cuda.is_available() else torch.float32,
            device_map="cuda" if torch.cuda.is_available() else "cpu",
            trust_remote_code=True
        )

        # 2. 加载 LoRA 适配器
        print("加载 LoRA 适配器...")
        model = PeftModel.from_pretrained(
            base_model,
            LORA_PATH,
            ensure_weight_tying=True  # 关键参数：解决词嵌入层绑定警告
        )
        model = model.merge_and_unload()  # 合并权重，提升推理速度（可选）
        model.eval()

        # 3. 加载分词器（从基础模型或 LoRA 目录都可以）
        tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL_PATH)

        # 补充：确保pad_token存在（Qwen模型可能默认没有，消除生成时的潜在警告）
        if tokenizer.pad_token is None:
            tokenizer.pad_token = tokenizer.eos_token

        return tokenizer, model
    except Exception as e:
        print(f"模型加载失败：{str(e)}")
        raise e


def predict_intent(text, tokenizer, model, device=DEVICE):
    """
    独立的意图预测函数（依赖加载好的tokenizer和model）
    :param text: 待识别的金融语句
    :param tokenizer: 加载好的分词器
    :param model: 加载好的模型
    :param device: 运行设备
    :return: 识别出的意图标签
    """
    # 1. 构造符合训练格式的输入prompt
    prompt = f"<|im_start|>user\n识别以下金融语句的意图\n{text}<|im_end|>\n<|im_start|>assistant\n"

    # 2. 文本编码（转为模型可识别的tensor）
    inputs = tokenizer(
        prompt,
        return_tensors="pt",
        padding=True,
        truncation=True,
        max_length=256
    ).to(device)

    # 3. 模型推理（关闭梯度计算，提升速度并节省显存）
    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=MAX_NEW_TOKENS,
            temperature=TEMPERATURE,
            top_p=TOP_P,
            do_sample=DO_SAMPLE,
            eos_token_id=tokenizer.eos_token_id,
            pad_token_id=tokenizer.pad_token_id  # 显式设置，消除警告
        )

    # 4. 解析输出结果，提取意图标签
    response = tokenizer.decode(outputs[0], skip_special_tokens=False)
    # 截取assistant回复部分（核心解析逻辑）
    intent = response.split("<|im_start|>assistant\n")[-1].split("<|im_end|>")[0].strip()

    return intent


def run_intent_prediction_test(test_cases):
    # 加载模型和分词器
    tokenizer, model = load_model_and_tokenizer()
    res = []
    for case in test_cases:
        intent = predict_intent(case, tokenizer, model)
        res.append(intent)
    return res


if __name__ == "__main__":
    r = run_intent_prediction_test(['东方财富的涨跌幅', "东方财富涨了多少", "东方财富跌了多少",
                                    "A股;东财二级行业包含半导体市净率0-5;",
                                    "贵州茅台的股东增长率",
                                    "分析宁德时代的综合情况",
                                    "市净率和市盈率的区别",
                                    "中欧医疗健康表现怎么样",
                                    "大盘今天为什么跳水",
                                    "帮我诊断下我的自选股"
                                    ])
    for i in r:
        print(i)
