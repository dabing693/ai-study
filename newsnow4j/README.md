# NewsNow4J - Financial News Aggregator Service

## Overview
This project is a Java Spring Boot implementation of the newsnow financial news aggregation service. It replicates the functionality of the original TypeScript implementation and provides the same API interface.

## Features Implemented

### 1. API Endpoints
- `/api/s?id={sourceId}` - Main endpoint matching the original protocol
- Supports both `mktnews-flash` and `cls-telegraph` source IDs
- Returns same response structure as original implementation

### 2. Supported Sources
- **MKTNews Flash**: Fetches market flash news from api.mktnews.net
- **Cailian Press (财联社)**: Fetches financial news from www.cls.cn

### 3. Data Storage
- MySQL database integration with 'study' database
- News items stored in 'news_items' table
- Automatic table creation via Hibernate

### 4. Response Format
Returns JSON with the same structure as the original:
```json
{
  "status": "success",
  "id": "mktnews-flash",
  "updatedTime": 1234567890123,
  "items": [
    {
      "id": "12345",
      "title": "News Title",
      "url": "https://...",
      "pubDate": 1234567890000,
      "extra": {
        "info": "Additional info",
        "hover": "Hover text"
      }
    }
  ]
}
```

## Technical Details

### Dependencies Used
- Spring Boot 3.x
- Spring Data JPA
- Hibernate
- MySQL Connector
- Jackson for JSON processing
- Lombok for reducing boilerplate code

### Architecture
- Controller: `NewsController` handles API requests
- Services: `MktNewsService` and `ClsService` handle data fetching
- Repository: `NewsItemRepository` manages database operations
- Entity: `NewsItem` represents the data model
- DTOs: `SourceResponse` and `NewsItemDto` for API responses

### Error Handling
- Graceful fallback to cached data when external APIs fail
- Comprehensive logging for debugging
- Proper exception handling to maintain service availability

## Database Schema
The application automatically creates the following table:
```sql
CREATE TABLE news_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  item_id VARCHAR(255),
  title TEXT,
  content TEXT,
  url VARCHAR(500),
  mobile_url VARCHAR(500),
  pub_date BIGINT,
  extra_info TEXT,
  important INT,
  source VARCHAR(100),
  created_at BIGINT
);
```

## Configuration
- Server runs on port 9082
- MySQL connection configured for 'study' database
- Hibernate automatically updates schema (ddl-auto=update)

## Testing
The implementation has been tested and:
- Responds to API requests with correct format
- Handles external API failures gracefully
- Stores data in MySQL database
- Follows the same protocol as the original TypeScript version