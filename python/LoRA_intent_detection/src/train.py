import os
import torch

from LoRA_intent_detection.src import config
from LoRA_intent_detection.src.dataset import FinanceDataset, load_finance_data
from LoRA_intent_detection.src.tokenizer import MyTokenizer

from transformers import (
    AutoModelForCausalLM,
    BitsAndBytesConfig,
    TrainingArguments,
    Trainer,
    DataCollatorForLanguageModeling
)
from peft import LoraConfig, get_peft_model, TaskType

os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"  # 强制使用CPU
os.environ["TOKENIZERS_PARALLELISM"] = "false"  # 关闭tokenizer并行，避免CPU占用过高
device = torch.device("cpu")
# 1.5B本机微调很慢；Instruct版：已有通用对话模式，可能干扰分类任务
# model_name = "Qwen/Qwen2.5-1.5B-Instruct"
model_name = "Qwen/Qwen2.5-0.5B"
tokenizer = MyTokenizer(model_name)

# 量化配置（INT4，核心！降低内存占用）
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,  # INT4量化
    bnb_4bit_use_double_quant=True,  # 双重量化，进一步降低内存
    bnb_4bit_quant_type="nf4",  # 适合LLM的量化类型
    bnb_4bit_compute_dtype=torch.float32,  # CPU计算用float32
    llm_int8_enable_fp32_cpu_offload=True,  # 强制CPU量化兼容
    llm_int8_skip_modules=["lm_head"]  # 跳过输出层量化，避免推理错误
)

# 加载模型（CPU + INT4）
model = AutoModelForCausalLM.from_pretrained(
    model_name,
    trust_remote_code=True,
    quantization_config=bnb_config,
    device_map="cpu",
    dtype=torch.float32
)
model.config.use_cache = False  # 训练时关闭缓存

# -------------------------- 5. LoRA配置（金融意图识别优化） --------------------------
lora_config = LoraConfig(
    task_type=TaskType.CAUSAL_LM,
    r=8,  # 秩，越小内存占用越低
    lora_alpha=16,  # 缩放因子
    lora_dropout=0.05,  # dropout率，防止过拟合
    target_modules=["q_proj", "v_proj"],  # 只训练注意力层的q/v投影，效率最高
    bias="none",  # 不训练偏置
    modules_to_save=["lm_head"],  # 保存输出层，适配分类任务
)

# 应用LoRA
model = get_peft_model(model, lora_config)
model.print_trainable_parameters()  # 打印可训练参数（仅≈0.1%）

# 分词训练集和验证集
train_texts, val_texts = load_finance_data(config.PROCESSED_DATA_DIR / "doubao_finance_intent_5000.csv")
print(f"训练集数量：{len(train_texts)}, 验证集数量：{len(val_texts)}")
train_encodings = tokenizer.tokenize(train_texts)
val_encodings = tokenizer.tokenize(val_texts)

train_dataset = FinanceDataset(train_encodings)
val_dataset = FinanceDataset(val_encodings)

# 数据收集器
data_collator = DataCollatorForLanguageModeling(
    tokenizer=tokenizer.get_tokenizer(),
    mlm=False,  # 自回归任务，关闭MLM
)

# -------------------------- 7. 训练参数配置（适配16G内存） --------------------------
training_args = TrainingArguments(
    output_dir=config.MODELS_DIR / "qwen_finance_intent",  # 输出目录
    per_device_train_batch_size=config.BATCH_SIZE,  # 单批次1条，降低内存占用
    per_device_eval_batch_size=1,
    gradient_accumulation_steps=2,  # 梯度累积，等效batch_size=4 # 从4降到2，减少内存占用和计算步骤
    learning_rate=config.LEARNING_RATE,  # LoRA学习率 4bit量化下可适当提高学习率，不影响收敛
    num_train_epochs=config.EPOCHS,  # 训练2轮，足够收敛 4bit量化收敛更快，减少训练轮数
    logging_steps=20,  # 每10步打印日志
    eval_strategy="no",  # 每轮验证一次 关闭验证（纯CPU训练优先保证速度，后续单独验证）
    save_strategy="epoch",  # 每轮保存一次
    save_total_limit=1,  # 只保留最新的1个模型
    load_best_model_at_end=False,  # 训练结束加载最优模型
    # metric_for_best_model="eval_loss",# load_best_model_at_end为Ture时指定
    fp16=False,  # CPU不支持FP16，关闭
    bf16=False,
    weight_decay=0.01,  # 权重衰减，防止过拟合
    lr_scheduler_type="constant",  # cosine：余弦学习率衰减；constant：固定学习率，减少计算
    report_to="none",  # 不使用wandb等日志工具
    dataloader_num_workers=0,  # CPU环境关闭dataloader多线程，避免冲突
)

# -------------------------- 8. 开始训练 --------------------------
trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=train_dataset,
    eval_dataset=val_dataset,
    data_collator=data_collator,
)

# 启动训练
trainer.train()

# -------------------------- 9. 保存模型 --------------------------
save_dir = config.MODELS_DIR / 'qwen_finance_intent_lora'
# 保存LoRA权重（仅几MB）
model.save_pretrained(save_dir)
tokenizer.save_pretrained(save_dir)
print(f"LoRA模型已保存到：{save_dir}")
