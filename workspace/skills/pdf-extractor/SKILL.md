---
name: pdf-extractor
description: Extract text, metadata, and page count from PDF files — supports both native (searchable) PDFs and scanned documents (OCR-ready).
---

# PDF Extractor

Extract text, metadata, and page count from PDF files.

## Prerequisites

```bash
pip install PyPDF2
```

## Usage

```bash
python3 skills/pdf-extractor/scripts/extract_pdf.py [--meta] [--output OUTFILE] <input.pdf>
```

## Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| input | str | yes | Input PDF file path |
| --meta | flag | no | Show metadata only |
| --output | str | no | Output text file (default: stdout) |

## Examples

### Extract all text:
```bash
python3 skills/pdf-extractor/scripts/extract_pdf.py input.pdf
```

### Show metadata only:
```bash
python3 skills/pdf-extractor/scripts/extract_pdf.py --meta input.pdf
```

### Output to file:
```bash
python3 skills/pdf-extractor/scripts/extract_pdf.py --output output.txt input.pdf
```

## Notes

- Native PDFs: Full text extraction supported
- Scanned PDFs: Requires OCR setup (pdf2image + pytesseract)
- Does not support PDF/A or password-protected files by default
