#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
json-validator: CLI for JSON validation, formatting and querying
Usage: python main.py [--validate FILE] [--pretty] [--query JMESPATH] [--diff A B]
"""
import sys
import json
import argparse
import subprocess

try:
    import jmespath
except ImportError:
    jmespath = None


def load_json(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        print(f"Error loading {path}: {e}")
        return None


def validate_and_pretty(data, pretty=False):
    if pretty:
        return json.dumps(data, indent=2, ensure_ascii=False)
    else:
        return json.dumps(data, separators=(",", ":"), ensure_ascii=False)


def query_data(data, query):
    if not jmespath:
        print("[WARN] jmespath not installed. Install with: pip install jmespath")
        return None
    try:
        result = jmespath.search(query, data)
        return json.dumps(result, indent=2, ensure_ascii=False) if isinstance(result, (dict, list)) else str(result)
    except Exception as e:
        print(f"Query error: {e}")
        return None


def diff_json(path1, path2):
    try:
        a = load_json(path1)
        b = load_json(path2)
        if a is None or b is None:
            return
        # Use system diff if available, else simple string diff
        try:
            result = subprocess.run(["diff", "-u", path1, path2], capture_output=True, text=True)
            print(result.stdout or "No differences.")
        except:
            print("Semantic diff not available. Compare manually.")
    except Exception as e:
        print(f"Diff error: {e}")


def main():
    parser = argparse.ArgumentParser(description="JSON Validator & Tool")
    parser.add_argument("--validate", help="Validate and optionally format JSON file")
    parser.add_argument("--pretty", action="store_true", help="Pretty-print output")
    parser.add_argument("--query", help="JMESPath query expression")
    parser.add_argument("--diff", nargs=2, metavar=("FILE1", "FILE2"), help="Compare two JSON files")

    args = parser.parse_args()

    if args.validate:
        data = load_json(args.validate)
        if data is not None:
            print(validate_and_pretty(data, args.pretty))

    if args.query and args.validate:
        data = load_json(args.validate)
        if data is not None:
            res = query_data(data, args.query)
            if res:
                print(res)

    if args.diff:
        diff_json(args.diff[0], args.diff[1])


if __name__ == "__main__":
    main()
