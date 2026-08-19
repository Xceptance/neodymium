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
            if file.endswith(".json") and file not in ("run.json", "batch.json"):
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
            folder_area = parts[0]
            folder_class = parts[1]
        elif len(parts) == 2:
            folder_area = ""
            folder_class = parts[0]
        else:
            folder_area = ""
            folder_class = ""

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

        # Determine area/category and test class
        test_class_name = exec_data.get("testClass")
        if test_class_name and (test_class_name.endswith(".json") or test_class_name == full_path.name):
            test_class_name = None

        if not test_class_name or not test_class_name.strip() or test_class_name in ["GeneralClass", "DefaultClass"]:
            if folder_class:
                test_class_name = folder_class
            elif exec_data.get("testFile"):
                tf = exec_data.get("testFile")
                if "#" in tf:
                    tf = tf.split("#")[0]
                if "." in tf:
                    test_class_name = tf.split(".")[-1]
                else:
                    test_class_name = tf
            elif exec_data.get("testId"):
                test_class_name = exec_data.get("testId").replace(" ", "")
            else:
                test_class_name = "DefaultClass"
        else:
            test_class_name = test_class_name.strip()

        raw_area = exec_data.get("areaName") or exec_data.get("category")
        if raw_area and (raw_area == folder_class or raw_area == full_path.name or raw_area == test_class_name or raw_area == "General"):
            raw_area = None

        if not raw_area or raw_area.strip() in ["", "General"]:
            if folder_area and folder_area != "General":
                area_name = folder_area
            else:
                area_name = "Browsing (default)"
        else:
            area_name = raw_area.strip()

        exec_data["areaName"] = area_name
        exec_data["testClass"] = test_class_name

        # Ensure file is moved to target area and test class folder on disk if needed
        target_dir = run_dir / area_name / test_class_name
        target_file = target_dir / full_path.name

        if full_path.resolve() != target_file.resolve():
            target_dir.mkdir(parents=True, exist_ok=True)
            with open(target_file, "w", encoding="utf-8") as out_f:
                json.dump(exec_data, out_f, indent=2)
            try:
                old_parent = full_path.parent
                full_path.unlink()
                if old_parent.exists() and not any(old_parent.iterdir()):
                    old_parent.rmdir()
                    if old_parent.parent.exists() and old_parent.parent != run_dir and not any(old_parent.parent.iterdir()):
                        old_parent.parent.rmdir()
            except Exception:
                pass
            full_path = target_file

        area_folder = area_name
        class_folder = test_class_name
        exec_filename = full_path.name

        if area_folder not in areas_map:
            clean_group = area_folder.replace(" ", "").replace("(", "").replace(")", "").replace("@", "")
            areas_map[area_folder] = {
                "areaName": area_name,
                "areaGroup": f"areaGroup{clean_group}",
                "folder": area_folder,
                "classes": {}
            }

        class_map = areas_map[area_folder]["classes"]
        if class_folder not in class_map:
            clean_container = class_folder.replace(".", "").replace(" ", "")
            class_map[class_folder] = {
                "className": test_class_name,
                "classContainer": f"classContainer{clean_container}",
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
