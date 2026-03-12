1. castorini/t5-base-canard

二、效果更强的大模型版本
2. zhuqi/t5-large-coqr-canard
   特点：
   T5-large
   参数：770M
   BLEU ≈ 77
   这是一个 T5-large 在 CANARD 上微调的模型。
   三、研究型模型（复杂任务）
3. gaussalgo/T5-LM-Large_Canard-HotpotQA-rephrase
   特点：
   同时训练三任务：
   QA
   rewrite
   reasoning
   数据集：
   CANARD
   HotpotQA
   模型约 0.8B参数。
   优点：
   rewrite能力强
   多跳问题更好
   缺点：
   推理较慢
   不太适合线上高QPS


最经典的 Query Rewrite 模型
[推荐] Langboat/Mengzi-T5-Base-MT
特点： 孟子模型在中文语义理解上非常扎实，Base 版本参数量适中，适合在 Java 环境下通过 ONNX Runtime 或 TensorRT 部署。
场景： 能够很好地处理指代消解（如“它”指代什么）和信息补全（如补全省略的主语）。

[推荐] Idea-CCNL/Randeng-T5-784M-MultiTask-Chinese
特点： 燃灯系列模型，针对多任务进行了预训练，改写逻辑非常稳健。
场景： 适合复杂的金融、法律意图补全，泛化能力比纯 BERT 强。


BERT 原生并不适合 rewrite，因为：
BERT 是 encoder-only
rewrite 是 seq2seq generation
因此通常使用：
BART
T5
如果一定要 BERT，可以用：
BERT encoder + decoder head