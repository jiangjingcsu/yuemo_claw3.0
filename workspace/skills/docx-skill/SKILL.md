---
name: docx-skill
description: A Python-based skill for processing Microsoft Word (.docx) documents — read, extract, modify and generate DOCX files.
---

# DOCX Skill

A Python-based skill for processing Microsoft Word (.docx) documents.

## Prerequisites

```bash
pip install python-docx
```

## Usage

```bash
python3 skills/docx-skill/scripts/process_docx.py input.docx
```

## Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| input | str | yes | Input .docx file path |

## Examples

### Extract all text:
```bash
python3 skills/docx-skill/scripts/process_docx.py input.docx
```

## Notes

- Supports .docx only (not .doc)
- Does not support password-protected or complex embedded objects
- Requires python-docx >= 0.8.11
