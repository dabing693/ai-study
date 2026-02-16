import torch
import pandas as pd


# 构造数据集类
class FinanceDataset(torch.utils.data.Dataset):
    def __init__(self, encodings):
        self.input_ids = encodings["input_ids"]
        self.attention_mask = encodings["attention_mask"]

    def __getitem__(self, idx):
        return {
            "input_ids": torch.tensor(self.input_ids[idx], dtype=torch.long),
            "attention_mask": torch.tensor(self.attention_mask[idx], dtype=torch.long),
            "labels": torch.tensor(self.input_ids[idx], dtype=torch.long)  # 自回归训练，labels=input_ids
        }

    def __len__(self):
        return len(self.input_ids)


def load_finance_data(csv_path):
    # 读取CSV
    df = pd.read_csv(csv_path, encoding="utf-8")
    # 过滤空值
    df = df.dropna(subset=["input", "output"])
    instruction = "识别以下金融语句的意图"

    # 构造Qwen的对话格式：<|im_start|>user\n{instruction}\n{input}<|im_end|>\n<|im_start|>assistant\n{output}<|im_end|>
    def format_prompt(row):
        return f"<|im_start|>user\n{instruction}\n{row['input']}<|im_end|>\n<|im_start|>assistant\n{row['output']}<|im_end|>"

    df["text"] = df.apply(format_prompt, axis=1)
    # 划分训练集和验证集（9:1）
    train_df = df.sample(frac=0.9, random_state=42)
    val_df = df.drop(train_df.index)

    return train_df["text"].tolist(), val_df["text"].tolist()
