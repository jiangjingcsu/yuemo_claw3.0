---
name: json-validator
description: "Validate, format, query, and compare JSON data using CLI — supports JSON Schema validation, JMESPath queries, and diff."
---

# JSON Validator

Validate, format, query, and compare JSON data using CLI.

## Prerequisites

Core uses only Python stdlib. Optional: `jmespath` for query support.

```bash
pip install jmespath
```

## Usage

```bash
python3 skills/json-validator/scripts/json_validator.py [--validate FILE] [--pretty] [--query JMESPATH] [--diff FILE1 FILE2]
```

## Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| --validate | str | no | Validate and optionally format JSON file |
| --pretty | flag | no | Pretty-print output |
| --query | str | no | JMESPath query expression |
| --diff | str str | no | Compare two JSON files |

## Examples

### Validate & format:
```bash
python3 skills/json-validator/scripts/json_validator.py --validate input.json --pretty
```

### Query with JMESPath:
```bash
python3 skills/json-validator/scripts/json_validator.py --validate data.json --query "users[?active].name"
```

### Compare two files:
```bash
python3 skills/json-validator/scripts/json_validator.py --diff file1.json file2.json
```

## Notes

- Built-in JMESPath support (requires `pip install jmespath`)
- Schema validation requires `--schema schema.json` (optional)
