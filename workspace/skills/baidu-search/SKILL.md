---
name: baidu-search
description: Search the web using Baidu AI Search Engine (BDSE). Use for live information, documentation, or research topics.
---

# Baidu Search

Search the web via Baidu AI Search API for real-time information retrieval.

## Prerequisites

### API Key Configuration
This skill requires a **BAIDU_API_KEY** to be configured. See `references/apikey-fetch.md` for instructions on obtaining one.

## Usage

```bash
python3 skills/baidu-search/scripts/search.py '{"query":"人工智能最新发展","count":10}'
```

## Request Parameters

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| query | str | yes | - | Search query string |
| count | int | no | 10 | Number of results (1-50) |
| freshness | str | no | - | Time range filter: `pd`(past day), `pw`(past week), `pm`(past month), `py`(past year), or `YYYY-MM-DDtoYYYY-MM-DD` |

## Examples

```bash
python3 skills/baidu-search/scripts/search.py '{"query":"人工智能最新发展","count":10,"freshness":"pw"}'
```

## Response Format

Returns search results with:
- Title
- URL
- Other metadata

## Current Status

Fully functional. Requires active internet connection and BAIDU_API_KEY.
