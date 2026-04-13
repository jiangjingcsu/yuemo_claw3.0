---
name: browser-automator
description: "High-level CLI tool for browser automation — wraps built-in browserNavigate, browserType, browserClick, browserGetContent into unified commands."
---

# Browser Automator

High-level CLI tool for browser automation — wraps built-in browser tools into unified commands.

## Usage

```bash
python3 skills/browser-automator/scripts/browser_automator.py --navigate https://example.com --get-title
```

## Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| --navigate | str | no | Navigate to URL |
| --type | str str | no | Type text into element (SELECTOR TEXT) |
| --click | str | no | Click element by CSS selector |
| --get-title | flag | no | Get page title |
| --get-content | flag | no | Get full HTML content |
| --save-html | str | no | Save HTML to file |
| --timeout | int | no | Page load timeout in seconds (default: 10) |

## Examples

### Go to page & get title:
```bash
python3 skills/browser-automator/scripts/browser_automator.py --navigate https://example.com --get-title
```

### Fill login form & submit:
```bash
python3 skills/browser-automator/scripts/browser_automator.py --navigate https://site.com/login --type "#username" "user123" --type "#password" "pass456" --click "button[type=submit]"
```

### Save full page HTML:
```bash
python3 skills/browser-automator/scripts/browser_automator.py --navigate https://news.ycombinator.com --save-html page.html
```

## Notes

- All operations are synchronous and blocking
- Uses persistent browser session (no new tabs per command)
- Selector syntax: CSS selectors only
