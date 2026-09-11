#!/usr/bin/env python3
"""
Populates full sets of test execution JSON files for runs 1042, 1043, 1044, 1047, 1048 based on templates from 1049,
ensuring every run directory has complete execution files.
"""

import os
import json
import shutil
from pathlib import Path

BASE_DIR = Path("storage/runs").resolve()
TEMPLATE_RUN = BASE_DIR / "1049"

RUN_SPECS = {
    "1042": {
        "pass": 8, "fixed": 1, "known": 1, "unknown": 0, "ignored": 2, "loc": "EU", "browser": "Firefox"
    },
    "1043": {
        "pass": 9, "fixed": 1, "known": 1, "unknown": 0, "ignored": 1, "loc": "EU", "browser": "Chrome"
    },
    "1044": {
        "pass": 10, "fixed": 1, "known": 1, "unknown": 0, "ignored": 0, "loc": "DE", "browser": "Edge"
    },
    "1045": {
        "pass": 7, "fixed": 2, "known": 1, "unknown": 1, "ignored": 1, "loc": "EU", "browser": "Chrome"
    },
    "1047": {
        "pass": 2, "fixed": 2, "known": 1, "unknown": 1, "ignored": 2, "loc": "US", "browser": "Chrome"
    },
    "1048": {
        "pass": 1, "fixed": 2, "known": 0, "unknown": 0, "ignored": 0, "loc": "US", "browser": "Chrome"
    },
    "1049": {
        "pass": 6, "fixed": 2, "known": 2, "unknown": 1, "ignored": 1, "loc": "US", "browser": "Chrome"
    }
}

TEST_TEMPLATES = [
    ("Checkout", "CheckoutProcessTest", "row-ds-1.json", "CheckoutProcessTest [CreditCard]", "tb-checkout-ds1"),
    ("Checkout", "CheckoutProcessTest", "row-ds-2.json", "CheckoutProcessTest [PayPal]", "tb-checkout-ds2"),
    ("Checkout", "CheckoutProcessTest", "row-ds-3.json", "CheckoutProcessTest [ApplePay]", "tb-checkout-ds3"),
    ("Checkout", "CheckoutGuestFlowTest", "row-guest-1.json", "CheckoutGuestFlowTest [Guest]", "tb-checkout-guest1"),
    ("Authentication", "AuthenticationTest", "row-auth-1.json", "AuthenticationTest [ValidLogin]", "tb-auth-1"),
    ("Authentication", "AuthenticationTest", "row-auth-2.json", "AuthenticationTest [RememberMe]", "tb-auth-2"),
    ("Authentication", "PasswordResetTest", "row-auth-3.json", "PasswordResetTest [ForgotPassword]", "tb-auth-3"),
    ("Search", "ProductSearchTest", "row-search-1.json", "ProductSearchTest [KeywordSearch]", "tb-search-1"),
    ("Search", "ProductFilterTest", "row-search-2.json", "ProductFilterTest [CategoryFilter]", "tb-search-2"),
    ("Cart", "CartManagementTest", "row-cart-1.json", "CartManagementTest [UpdateQuantity]", "tb-cart-1"),
    ("Cart", "CartManagementTest", "row-cart-2.json", "CartManagementTest [ApplyCoupon]", "tb-cart-2"),
    ("Cart", "CartManagementTest", "row-cart-3.json", "CartManagementTest [RemoveItem]", "tb-cart-3"),
]

def populate_run(run_id, spec):
    run_dir = BASE_DIR / run_id
    run_dir.mkdir(parents=True, exist_ok=True)

    status_sequence = (
        ["passed-clean"] * spec["pass"] +
        ["succeeded-fixed"] * spec["fixed"] +
        ["failed-known"] * spec["known"] +
        ["failed-unknown"] * spec["unknown"] +
        ["ignored"] * spec["ignored"]
    )

    total_needed = len(status_sequence)

    for i in range(total_needed):
        area_folder, class_folder, exec_filename, title, var_key = TEST_TEMPLATES[i % len(TEST_TEMPLATES)]
        status = status_sequence[i]

        dest_dir = run_dir / area_folder / class_folder
        dest_dir.mkdir(parents=True, exist_ok=True)

        exec_id = f"exec-{run_id}-{i+1}"
        file_name = exec_filename if i < len(TEST_TEMPLATES) else f"row-extra-{i+1}.json"
        dest_file = dest_dir / file_name

        bugs = ["BUG-4821"] if status in ["failed-known", "succeeded-fixed"] else []
        failure = "ZipCodeInvalidException: Provided ZIP '90210' failed verification API." if "failed" in status else "NONE"

        exec_data = {
            "id": exec_id,
            "title": f"{title} [{spec['loc']} {spec['browser']}]",
            "testClass": class_folder,
            "classContainer": f"classContainer{class_folder}",
            "areaGroup": f"areaGroup{area_folder}",
            "variationKey": var_key,
            "status": status,
            "bugs": bugs,
            "location": spec["loc"],
            "browser": spec["browser"],
            "engine": "Java",
            "isRetried": False,
            "retriedChip": "",
            "comment": f"Auto-generated execution for run {run_id}",
            "failure": failure,
            "areaName": area_folder
        }

        with open(dest_file, "w", encoding="utf-8") as f:
            json.dump(exec_data, f, indent=2)

    print(f"Populated {total_needed} execution files for run {run_id}")

def main():
    for run_id, spec in RUN_SPECS.items():
        populate_run(run_id, spec)

if __name__ == "__main__":
    main()
