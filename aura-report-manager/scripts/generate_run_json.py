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

import datetime

def extract_execution_start_time(exec_data):
    """
    Extracts start time timestamp (in ms) and original/formatted timestamp string from an execution dict.
    Checks 'startTime', 'startTimeMs', 'timestamp', 'startDate', 'time', 'createdAt'.
    Returns tuple: (time_ms: float or None, formatted_str: str or None)
    """
    candidates = ["startTime", "startTimeMs", "timestamp", "startDate", "time", "createdAt"]
    val = None
    for key in candidates:
        if key in exec_data and exec_data[key] is not None and str(exec_data[key]).strip() != "":
            val = exec_data[key]
            break

    if val is None:
        return None, None

    if isinstance(val, (int, float)):
        ms = float(val)
        if ms < 1e11:
            ms *= 1000.0
        try:
            dt = datetime.datetime.fromtimestamp(ms / 1000.0, tz=datetime.timezone.utc)
            formatted = dt.strftime("%Y-%m-%d %H:%M:%S")
        except Exception:
            formatted = str(val)
        return ms, formatted

    val_str = str(val).strip()

    try:
        num = float(val_str)
        if num < 1e11:
            num *= 1000.0
        dt = datetime.datetime.fromtimestamp(num / 1000.0, tz=datetime.timezone.utc)
        return num, dt.strftime("%Y-%m-%d %H:%M:%S")
    except ValueError:
        pass

    for fmt in [
        "%Y-%m-%dT%H:%M:%S.%f%z",
        "%Y-%m-%dT%H:%M:%S%z",
        "%Y-%m-%dT%H:%M:%S.%f",
        "%Y-%m-%dT%H:%M:%S",
        "%Y-%m-%d %H:%M:%S",
        "%Y-%m-%d %H:%M:%S.%f",
        "%Y/%m/%d %H:%M:%S",
        "%Y%m%d_%H%M%S"
    ]:
        try:
            dt = datetime.datetime.strptime(val_str.replace("Z", "+0000"), fmt)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=datetime.timezone.utc)
            ms = dt.timestamp() * 1000.0
            return ms, val_str
        except ValueError:
            pass

    return None, val_str

def extract_locales_from_execution(exec_data):
    locs = []
    for key in ("locale", "location"):
        val = exec_data.get(key)
        if val:
            if isinstance(val, list):
                locs.extend(val)
            elif isinstance(val, str):
                locs.append(val)

    for b_key in ("localDataBindings", "dataBindings"):
        bindings = exec_data.get(b_key)
        if isinstance(bindings, dict):
            for key in ("locale", "location"):
                val = bindings.get(key)
                if val:
                    if isinstance(val, list):
                        locs.extend(val)
                    elif isinstance(val, str):
                        locs.append(val)

    result = []
    for loc in locs:
        if loc is not None:
            s = str(loc).strip()
            if s and s.lower() != "unknown":
                result.append(s)
    return result

def extract_browsers_from_execution(exec_data):
    browsers = []
    val = exec_data.get("browser")
    if val:
        if isinstance(val, list):
            browsers.extend(val)
        elif isinstance(val, str):
            browsers.append(val)

    for b_key in ("localDataBindings", "dataBindings"):
        bindings = exec_data.get(b_key)
        if isinstance(bindings, dict):
            val = bindings.get("browser")
            if val:
                if isinstance(val, list):
                    browsers.extend(val)
                elif isinstance(val, str):
                    browsers.append(val)

    result = []
    for b in browsers:
        if b is not None:
            s = str(b).strip()
            if s and s.lower() != "unknown":
                result.append(s)
    return result

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
        "batchName": "Unknown",
        "timestamp": "Recently",
        "duration": "0s",
        "trigger": "Unknown",
        "environment": "Unknown",
        "locales": ["Unknown"],
        "browsers": ["Chrome"],
        "threadsCount": 1
    }

    if existing_run_json_path.exists():
        try:
            with open(existing_run_json_path, "r", encoding="utf-8") as f:
                existing = json.load(f)
                for key in ["batchName", "timestamp", "startTime", "duration", "trigger", "environment", "locales", "browsers", "threadsCount"]:
                    if key in existing:
                        val = existing[key]
                        if key in ("timestamp", "startTime") and str(val).strip().lower() == "recently":
                            continue
                        meta[key] = val
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

    locales_set = set()
    browsers_set = set()

    earliest_ms = None
    earliest_time_str = None

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

        time_ms, time_str = extract_execution_start_time(exec_data)
        if time_ms is not None:
            if earliest_ms is None or time_ms < earliest_ms:
                earliest_ms = time_ms
                earliest_time_str = time_str
        elif time_str is not None and earliest_time_str is None:
            earliest_time_str = time_str

        status = exec_data.get("status", "passed-clean")
        found_locales = extract_locales_from_execution(exec_data)
        for loc in found_locales:
            locales_set.add(loc)

        found_browsers = extract_browsers_from_execution(exec_data)
        for b in found_browsers:
            browsers_set.add(b)

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

    if earliest_time_str:
        meta["timestamp"] = earliest_time_str
        meta["startTime"] = earliest_time_str

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
        "startTime": meta.get("startTime", meta["timestamp"]),
        "duration": meta["duration"],
        "trigger": meta["trigger"],
        "environment": meta["environment"],
        "locales": sorted(list(locales_set)) if locales_set else (meta.get("locales") if meta.get("locales") else ["Unknown"]),
        "browsers": sorted(list(browsers_set)) if browsers_set else (meta.get("browsers") if meta.get("browsers") else ["Chrome"]),
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
