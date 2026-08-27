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

def extract_execution_metrics(exec_data, test_class_name):
    title = exec_data.get("title") or exec_data.get("dataSet") or ""
    browser = exec_data.get("browser") or "Chrome"
    if isinstance(browser, list):
        browser = browser[0] if browser else "Chrome"

    metrics_obj = exec_data.get("metrics") if isinstance(exec_data.get("metrics"), dict) else {}

    total_steps = exec_data.get("totalStepsCount") or metrics_obj.get("totalSteps")
    failed_steps = exec_data.get("failedStepsCount") or metrics_obj.get("failedSteps")
    healed_steps = exec_data.get("healedStepsCount") or metrics_obj.get("healedSteps")

    blocks = exec_data.get("blocks")
    all_steps = []
    if isinstance(blocks, dict) and blocks:
        for key in ("before", "steps", "after"):
            arr = blocks.get(key)
            if isinstance(arr, list):
                all_steps.extend(arr)
    elif isinstance(exec_data.get("steps"), list):
        all_steps = exec_data.get("steps")
    elif isinstance(exec_data.get("llmCalls"), list):
        all_steps = exec_data.get("llmCalls")

    if total_steps is None or not isinstance(total_steps, int):
        total_steps = len(all_steps)

    if failed_steps is None or not isinstance(failed_steps, int):
        failed_steps = sum(1 for s in all_steps if isinstance(s, dict) and str(s.get("status", "")).lower() in ("failed", "error"))

    if healed_steps is None or not isinstance(healed_steps, int):
        healed_steps = sum(1 for s in all_steps if isinstance(s, dict) and (s.get("healed") or str(s.get("status", "")).lower() in ("healed", "succeeded-fixed", "fixed")))

    llm_calls = exec_data.get("llmCallsCount") or metrics_obj.get("totalLlmCalls")
    llm_tokens = exec_data.get("llmTotalTokens") or metrics_obj.get("totalTokens")
    llm_cost = exec_data.get("llmCost") or metrics_obj.get("estimatedCostUsd")

    c_calls, c_tokens, c_cost, c_duration = 0, 0, 0.0, 0
    for s in all_steps:
        if isinstance(s, dict):
            calls = s.get("llmCallsCount") or s.get("llmCalls")
            if isinstance(calls, list):
                c_calls += len(calls)
            elif isinstance(calls, (int, float, str)):
                try:
                    c_calls += int(calls)
                except (ValueError, TypeError):
                    c_calls += 1
            elif s.get("modelName") or s.get("totalTokens") or s.get("estimatedCostUsd") or s.get("inputTokens") or "stepIndex" in s:
                c_calls += 1

            toks = s.get("totalTokens") or s.get("tokens") or s.get("llmTotalTokens")
            if toks is not None:
                c_tokens += int(toks)
            else:
                c_tokens += int(s.get("inputTokens") or 0) + int(s.get("outputTokens") or 0)

            cost = s.get("estimatedCostUsd") or s.get("llmCost") or s.get("cost") or s.get("costUsd")
            if cost is not None:
                c_cost += float(cost)

            dur = s.get("durationMs") or s.get("duration")
            if dur:
                c_duration += int(dur)

    if llm_calls is None or llm_calls == 0:
        llm_calls = c_calls
    if llm_tokens is None or llm_tokens == 0:
        llm_tokens = c_tokens
    if llm_cost is None or llm_cost == 0.0:
        llm_cost = round(c_cost, 6)

    duration_val = exec_data.get("duration") if exec_data.get("duration") is not None else exec_data.get("durationMs")
    duration_ms = int(duration_val) if duration_val is not None else c_duration
    if duration_ms > 0:
        duration_fmt = exec_data.get("durationFormatted") or f"{duration_ms:,} ms"
    else:
        duration_fmt = "0 ms"

    time_ms, time_str = extract_execution_start_time(exec_data)
    date_formatted = ""
    time_formatted = ""
    timestamp_ms = 0
    if time_ms is not None:
        timestamp_ms = int(time_ms)
        try:
            dt = datetime.datetime.fromtimestamp(time_ms / 1000.0, tz=datetime.timezone.utc)
            date_formatted = dt.strftime("%Y-%m-%d")
            time_formatted = dt.strftime("%H:%M:%S")
        except Exception:
            pass

    junit_tags = exec_data.get("junitTags") or []
    test_method = exec_data.get("testMethod") or ""
    if not test_method and isinstance(junit_tags, list) and len(junit_tags) >= 2:
        tag1 = str(junit_tags[1]).strip()
        if tag1 and not tag1.startswith("Dataset:") and not tag1.startswith("Location:") and not tag1.startswith("Browser:") and tag1.lower() != test_class_name.lower():
            test_method = tag1
    if not test_method and exec_data.get("testFile") and "#" in str(exec_data.get("testFile")):
        test_file_str = str(exec_data.get("testFile"))
        method = test_file_str.split("#", 1)[1].strip()
        if method.lower() != "executetest":
            test_method = method

    if test_method:
        key = f"{test_class_name}#{test_method}#{title}#{browser}"
    else:
        key = f"{test_class_name}#{title}#{browser}"

    return key, {
        "id": exec_id,
        "testClass": test_class_name,
        "testMethod": test_method,
        "title": title,
        "location": exec_data.get("location") or exec_data.get("locale") or "Unknown",
        "browser": browser,
        "status": exec_data.get("status") or "passed-clean",
        "executionMode": exec_data.get("executionMode") or exec_data.get("mode") or "FORCE_RECORDING",
        "startTime": time_str or "",
        "dateFormatted": date_formatted,
        "timeFormatted": time_formatted,
        "timestampMs": timestamp_ms,
        "durationMs": duration_ms,
        "durationFormatted": duration_fmt,
        "totalStepsCount": total_steps,
        "failedStepsCount": failed_steps,
        "healedStepsCount": healed_steps,
        "llmCallsCount": llm_calls,
        "llmTotalTokens": llm_tokens,
        "llmCost": llm_cost,
        "bugs": exec_data.get("bugs") if exec_data.get("bugs") is not None else None
    }


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
    execution_metrics = {}
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

        # Extract execution metrics for instant "All Tests" view
        metric_key, metric_data = extract_execution_metrics(exec_data, test_class_name)
        execution_metrics[metric_key] = metric_data

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

    summary_counts["totalLlmCalls"] = sum(m.get("llmCallsCount", 0) for m in execution_metrics.values())
    summary_counts["totalLlmTokens"] = sum(m.get("llmTotalTokens", 0) for m in execution_metrics.values())
    summary_counts["totalLlmCost"] = round(sum(m.get("llmCost", 0.0) for m in execution_metrics.values()), 6)

    # Assemble clean run.json structure containing only executionMetrics
    run_json_data = {
        "executionMetrics": execution_metrics
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
