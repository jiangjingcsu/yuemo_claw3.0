#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
pdf-extractor: Extract text & metadata from PDF files
Usage: python extract_pdf.py [--meta] [--output OUTFILE] <input.pdf>
"""
import sys
import argparse
from PyPDF2 import PdfReader


def extract_text(pdf_path):
    try:
        reader = PdfReader(pdf_path)
        text = ""
        for page in reader.pages:
            text += page.extract_text() or ""
        return text.strip()
    except Exception as e:
        print(f"Error reading PDF: {e}")
        return None


def get_metadata(pdf_path):
    try:
        reader = PdfReader(pdf_path)
        info = reader.metadata
        meta_dict = {
            "pages": len(reader.pages),
            "author": info.get("/Author", "N/A"),
            "title": info.get("/Title", "N/A"),
            "creator": info.get("/Creator", "N/A"),
            "producer": info.get("/Producer", "N/A"),
            "creation_date": info.get("/CreationDate", "N/A")
        }
        return meta_dict
    except Exception as e:
        print(f"Error reading metadata: {e}")
        return None


def main():
    parser = argparse.ArgumentParser(description="Extract text/metadata from PDF")
    parser.add_argument("input", help="Input PDF file path")
    parser.add_argument("--meta", action="store_true", help="Show metadata only")
    parser.add_argument("--output", help="Output text file (default: stdout)")
    args = parser.parse_args()

    if args.meta:
        meta = get_metadata(args.input)
        if meta:
            for k, v in meta.items():
                print(f"{k}: {v}")
    else:
        text = extract_text(args.input)
        if text is not None:
            if args.output:
                with open(args.output, "w", encoding="utf-8") as f:
                    f.write(text)
                print(f"✓ Text saved to {args.output}")
            else:
                print(text)


if __name__ == "__main__":
    main()
