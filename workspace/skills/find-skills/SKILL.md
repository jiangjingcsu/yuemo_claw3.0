---
name: find-skills
description: Discover and explore skills from the skills.sh ecosystem. Search by keywords, categories, or popularity to find the right skills for your agent.
---

# Find Skills Skill

## Description

Skill ecosystem explorer — search the skills.sh skill library by keywords to discover and install needed capabilities.

## Purpose

- Search the skills.sh directory for relevant skills
- Browse by categories and popularity
- Get skill descriptions and usage information
- Help users find the right skills for their tasks

## Usage

### Search Parameters

- `query`: Search keywords or skill name
- `category`: Filter by category (optional)
  - `search`: Search & information retrieval
  - `agent`: Agent enhancement tools
  - `browser`: Browser automation
  - `code`: Code generation & development
  - `document`: Document processing
  - `image`: Image generation & editing
  - `productivity`: Productivity tools
- `limit`: Number of results to return (default: 10, max: 50)

### Example Usage

```json
{
  "query": "baidu search",
  "category": "search",
  "limit": 10
}
```

## Features

### Search Capabilities

- Keyword search across all skills
- Category-based filtering
- Popularity ranking
- Skill metadata extraction

### Skill Information

For each found skill, provides:
- Skill name
- Description
- Category
- Popularity score
- Installation instructions
- Required dependencies

## Integration

- Works with skills.sh directory
- Can install discovered skills via ClawHub
- Provides recommendations based on user context

## Notes

- Requires internet connection to access skills.sh
- Popularity data is updated regularly
- Some skills may require API keys or additional setup
