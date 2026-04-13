---
name: git-manager
description: "CLI tool for common Git operations: init, clone, commit, push, status, diff, and branch management — all from Python API or command line."
---

# Git Manager

CLI tool for common Git operations from Python API or command line.

## Prerequisites

```bash
pip install GitPython
```

Requires Git CLI installed and in PATH.

## Usage

```bash
python3 skills/git-manager/scripts/git_manager.py [--clone URL PATH] [--cd DIR] [--add PATTERN] [--commit MSG] [--push REMOTE BRANCH] [--status] [--diff]
```

## Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| --clone | str str | no | Clone repo (URL PATH) |
| --cd | str | no | Change to directory before commands |
| --add | str | no | Git add pattern (e.g., "." or "*.py") |
| --commit | str | no | Git commit message |
| --push | str str | no | Git push REMOTE BRANCH |
| --status | flag | no | Git status |
| --diff | flag | no | Git diff |
| --dry-run | flag | no | Show commands without executing |

## Examples

### Clone a repo:
```bash
python3 skills/git-manager/scripts/git_manager.py --clone https://github.com/user/repo.git ./repo
```

### Commit & push:
```bash
python3 skills/git-manager/scripts/git_manager.py --cd ./repo --add . --commit "auto-commit" --push origin main
```

### Show status & diff:
```bash
python3 skills/git-manager/scripts/git_manager.py --status --diff
```

## Notes

- Uses `GitPython` for safe, Python-native Git interaction
- All commands support `--dry-run` flag
