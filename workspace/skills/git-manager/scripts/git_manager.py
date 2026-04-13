#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
git-manager: Git CLI wrapper for Claw agents
Usage: python main.py [--clone URL PATH] [--cd DIR] [--add PATTERN] [--commit MSG] [--push REMOTE BRANCH]
"""
import sys
import argparse
import subprocess
import os

def run_cmd(cmd, cwd=None):
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True, cwd=cwd)
        if result.returncode != 0:
            print(f"[ERROR] {cmd}\n{result.stderr}")
            return False
        print(f"[OK] {cmd}\n{result.stdout.strip()}")
        return True
    except Exception as e:
        print(f"[EXCEPTION] {cmd}: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(description="Git CLI Wrapper")
    parser.add_argument("--clone", nargs=2, metavar=("URL", "PATH"), help="Clone repo")
    parser.add_argument("--cd", help="Change to directory before commands")
    parser.add_argument("--add", help="Git add (e.g., \".\" or \"*.py\")")
    parser.add_argument("--commit", help="Git commit -m MESSAGE")
    parser.add_argument("--push", nargs=2, metavar=("REMOTE", "BRANCH"), help="Git push REMOTE BRANCH")
    parser.add_argument("--status", action="store_true", help="Git status")
    parser.add_argument("--diff", action="store_true", help="Git diff")
    parser.add_argument("--dry-run", action="store_true", help="Show commands without executing")

    args = parser.parse_args()

    if args.dry_run:
        print("[DRY RUN MODE ENABLED]")

    cwd = args.cd or "."

    if args.clone:
        url, path = args.clone
        cmd = f"git clone {url} {path}"
        if not args.dry_run:
            run_cmd(cmd)
        else:
            print(f"[DRY] {cmd}")

    if args.status:
        cmd = "git status"
        if not args.dry_run:
            run_cmd(cmd, cwd=cwd)
        else:
            print(f"[DRY] cd {cwd} && {cmd}")

    if args.diff:
        cmd = "git diff"
        if not args.dry_run:
            run_cmd(cmd, cwd=cwd)
        else:
            print(f"[DRY] cd {cwd} && {cmd}")

    if args.add:
        cmd = f"git add {args.add}"
        if not args.dry_run:
            run_cmd(cmd, cwd=cwd)
        else:
            print(f"[DRY] cd {cwd} && {cmd}")

    if args.commit:
        cmd = f"git commit -m \"{args.commit}\""
        if not args.dry_run:
            run_cmd(cmd, cwd=cwd)
        else:
            print(f"[DRY] cd {cwd} && {cmd}")

    if args.push:
        remote, branch = args.push
        cmd = f"git push {remote} {branch}"
        if not args.dry_run:
            run_cmd(cmd, cwd=cwd)
        else:
            print(f"[DRY] cd {cwd} && {cmd}")


if __name__ == "__main__":
    main()
