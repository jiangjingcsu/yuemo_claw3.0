# 智能体技能库 (Agent Skills)

这是一个多智能体系统的技能库，包含各种可重用的技能组件。

## 技能列表

### 1. baidu-search (百度搜索技能)
- **描述**: 使用百度 AI 搜索引擎进行网络搜索
- **功能**: 网页搜索、时间筛选、结果数量控制
- **文件**: `baidu-search/SKILL.md`
- **脚本**: `baidu-search/baidu_search.py`
- **依赖**: Python requests 库
- **环境变量**: `BAIDU_API_KEY`

### 2. find-skills (技能发现技能)
- **描述**: 发现和探索 skills.sh 生态系统中的技能
- **功能**: 按关键词、分类、流行度搜索技能
- **文件**: `find-skills/SKILL.md`

### 3. poetry-skill (诗歌创作技能)
- **描述**: 专业的诗歌创作助手
- **功能**: 唐诗、宋词、现代诗、藏头诗等
- **文件**: `poetry-skill/SKILL.md`

## 技能结构

每个技能文件夹包含：
- `SKILL.md`: 技能描述和元数据（标准格式）
- 可选的脚本文件（如 Python、Shell 等）

## 标准技能格式

```markdown
---
name: skill-name
description: A clear description of what this skill does
---

# Skill Name

## Description

[Skill description]

## Purpose

[Skill purpose and use cases]

## Usage

[Usage instructions]

## Features

[Feature list]

## Notes

[Additional notes]
```

## 使用说明

将技能文件夹复制到您的智能体系统技能目录中，即可使用相应的技能。
