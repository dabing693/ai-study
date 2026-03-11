import time
from pathlib import Path

yyyymmdd = time.strftime("%Y%m%d", time.gmtime())
ROOT_DIR = Path(__file__).parent.parent
RAW_DATA_DIR = ROOT_DIR / "data" / "raw"
PROCESSED_DATA_DIR = ROOT_DIR / "data" / "processed"
PROCESSED_DATA_DATE_DIR = ROOT_DIR / "data" / f"processed_{yyyymmdd}"
LOGS_DIR = ROOT_DIR / "logs"
MODELS_DIR = ROOT_DIR / "models"

BATCH_SIZE = 1
LEARNING_RATE = 3e-4
EPOCHS = 2
