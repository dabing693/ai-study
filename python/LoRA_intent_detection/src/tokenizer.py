from transformers import AutoTokenizer


class MyTokenizer:

    def __init__(self, model_name):
        tokenizer = AutoTokenizer.from_pretrained(
            model_name,
            trust_remote_code=True,
            padding_side="right",
            use_fast=False
        )
        if tokenizer.pad_token:
            tokenizer.pad_token = tokenizer.eos_token
        self.tokenizer = tokenizer

    def tokenize(self, texts):
        return self.tokenizer(
            texts,
            truncation=True,
            max_length=256,  # 金融意图语句短，256足够
            padding="max_length",
            return_attention_mask=True
        )

    def save_pretrained(self, save_dir):
        self.tokenizer.save_pretrained(save_dir)

    def get_tokenizer(self):
        return self.tokenizer
