import os
from dotenv import load_dotenv
from langchain_tavily import TavilySearch

load_dotenv(override=True)


def build_tavily_search():
    return TavilySearch(max_results=2, api_key=os.getenv('TAVILY_API_KEY'))


tavily_search = build_tavily_search()
