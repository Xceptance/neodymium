#!/usr/bin/env python3
"""
Script to analyze test execution JSON files in a run directory and generate/update the run.json file.
Ensures that run.json 100% corresponds with the actual test execution files on disk.

Usage:
    python3 scripts/generate_run_json.py storage/runs/1049
    python3 scripts/generate_run_json.py --all
"""

import os
import sys
import json
import argparse
from pathlib import Path

def analyze_and_generate_run_json(run_dir_path):
    run_dir = Path(run_dir_path).resolve()
    if not run_dir.is_dir():
        print(f"Error: Directory {run_dir} does not exist.")
        return False

    run_id = run_dir.name
    existing_run_json_path = run_dir / "run.json"

    # Default metadata if run.json doesn't exist yet
    meta = {
        "runId": run_id,
        "batchName": "NA Integration STG" if run_id in ["1047", "1048", "1049"] else "EU Smoke Suite",
        "timestamp": "Today, 14:22:10",
        "duration": "1m 42s",
        "trigger": "Scheduled Cron",
        "environment": "Staging (us-west-2.shop.xceptance.com)",
        "locales": ["US"],
        "browsers": ["Chrome"],
        "threadsCount": 2
    }

    if existing_run_json_path.exists():
        try:
            with open(existing_run_json_path, "r", encoding="utf-8") as f:
                existing = json.load(f)
                for key in ["batchName", "timestamp", "duration", "trigger", "environment", "locales", "browsers", "threadsCount"]:
                    if key in existing:
                        meta[key] = existing[key]
        except Exception as e:
            print(f"Warning: Failed to read existing run.json in {run_dir}: {e}")

    # Discover all execution .json files in subdirectories
    execution_files = []
    for root, _, files in os.walk(run_dir):
        for file in files:
            if file.endswith(".json") and file != "run.json":
                full_path = Path(root) / file
                rel_path = full_path.relative_to(run_dir)
                execution_files.append((rel_path, full_path))

    if not execution_files:
        print(f"No execution JSON files found in {run_dir}")

    # Group executions by area and testClass
    areas_map = {}
    summary_counts = {
        "total": 0,
        "pass": 0,
        "fixed": 0,
        "known": 0,
        "unknown": 0,
        "ignored": 0
    }

    locales_set = set(meta.get("locales", []))
    browsers_set = set(meta.get("browsers", []))

    for rel_path, full_path in execution_files:
        parts = rel_path.parts
        if len(parts) >= 3:
            area_folder = parts[0]
            class_folder = parts[1]
            exec_filename = parts[2]
        elif len(parts) == 2:
            area_folder = parts[0]
            class_folder = "DefaultClass"
            exec_filename = parts[1]
        else:
            area_folder = "General"
            class_folder = "GeneralClass"
            exec_filename = parts[0]

        try:
            with open(full_path, "r", encoding="utf-8") as f:
                exec_data = json.load(f)
        except Exception as e:
            print(f"Error reading {full_path}: {e}")
            continue

        status = exec_data.get("status", "passed-clean")
        location = exec_data.get("location")
        browser = exec_data.get("browser")

        if location:
            locales_set.add(location)
        if browser:
            browsers_set.add(browser)

        # Update summary counts based on execution status
        summary_counts["total"] += 1
        if status == "passed-clean":
            summary_counts["pass"] += 1
        elif status == "succeeded-fixed":
            summary_counts["fixed"] += 1
        elif status == "failed-known":
            summary_counts["known"] += 1
        elif status == "failed-unknown":
            summary_counts["unknown"] += 1
        elif status == "ignored":
            summary_counts["ignored"] += 1
        else:
            summary_counts["pass"] += 1

        # Populate area/class hierarchy
        area_name = exec_data.get("areaName", area_folder)
        test_class_name = exec_data.get("testClass", class_folder)

        if area_folder not in areas_map:
            areas_map[area_folder] = {
                "areaName": area_name,
                "areaGroup": f"areaGroup{area_folder}",
                "folder": area_folder,
                "classes": {}
            }

        class_map = areas_map[area_folder]["classes"]
        if class_folder not in class_map:
            class_map[class_folder] = {
                "className": test_class_name,
                "classContainer": f"classContainer{class_folder}",
                "folder": class_folder,
                "executions": []
            }

        class_map[class_folder]["executions"].append(exec_filename)

    # Build final areas list
    areas_list = []
    for area_folder, area_info in areas_map.items():
        test_classes_list = []
        for class_folder, class_info in area_info["classes"].items():
            test_classes_list.append({
                "className": class_info["className"],
                "classContainer": class_info["classContainer"],
                "folder": class_info["folder"],
                "executions": sorted(class_info["executions"])
            })

        areas_list.append({
            "areaName": area_info["areaName"],
            "areaGroup": area_info["areaGroup"],
            "folder": area_info["folder"],
            "testClasses": test_classes_list
        })

    # Assemble complete run.json structure
    run_json_data = {
        "runId": meta["runId"],
        "batchName": meta["batchName"],
        "timestamp": meta["timestamp"],
        "duration": meta["duration"],
        "trigger": meta["trigger"],
        "environment": meta["environment"],
        "locales": sorted(list(locales_set)) if locales_set else ["US"],
        "browsers": sorted(list(browsers_set)) if browsers_set else ["Chrome"],
        "threadsCount": meta["threadsCount"],
        "summary": summary_counts,
        "areas": areas_list
    }

    # Write out run.json
    with open(existing_run_json_path, "w", encoding="utf-8") as f:
        json.dump(run_json_data, f, indent=2)

    print(f"Successfully generated {existing_run_json_path}:")
    print(f"  Total Executions: {summary_counts['total']} (Pass: {summary_counts['pass']}, Fixed: {summary_counts['fixed']}, Known: {summary_counts['known']}, Unknown: {summary_counts['unknown']}, Ignored: {summary_counts['ignored']})")
    print(f"  Areas: {len(areas_list)}")
    return True

def main():
    parser = argparse.ArgumentParser(description="Generate run.json for Aura run directories based on test execution files.")
    parser.add_argument("path", nargs="?", help="Path to a run directory (e.g., storage/runs/1049)")
    parser.add_argument("--all", action="store_true", help="Process all run directories in storage/runs/")

    args = parser.parse_args()

    if args.all or not args.path:
        base_runs_dir = Path("storage/runs").resolve()
        if not base_runs_dir.is_dir():
            print("Error: storage/runs directory does not exist.")
            sys.exit(1)

        count = 0
        for entry in sorted(base_runs_dir.iterdir()):
            if entry.is_dir():
                print(f"Processing run directory {entry.name}...")
                if analyze_and_generate_run_json(entry):
                    count += 1
        print(f"Finished processing {count} run directories.")
    else:
        analyze_and_generate_run_json(args.path)

if __name__ == "__main__":
    main()
