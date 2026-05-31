# Guest Checkout with Fixed Product (Happy Path)

A new guest user adds the fixed product **"Grizzly Bear"** to the cart, proceeds to checkout, enters shipping and billing details, provides credit card payment, reviews the order with exact expected financial calculations, and successfully places it.

## Metadata

- **Test ID:** TC_CHK_001_Fixed
- **Version:** 1.2
- **Software Version:** >= 1.0.0
- **Domains:** Checkout, Cart
- **Priority:** 🔴 Critical
- **Status:** 👀 To Be Reviewed
- **Execution Type:** Manual
- **Suite:** 🚀 Smoke, 🔄 Regression, 🧪 Full
- **Tags:** `guest`, `checkout`, `happy-path`, `fixed-product`
- **Author:** Antigravity (AI) (2026-05-20)

## Preconditions

- The Posters Demo Store is running.
- The user is not logged in.
- The browser cart is empty.

---

## Test Data (Common Constants)

These values are constant across all test profiles.

### Customer Details
| Field | Value |
| :--- | :--- |
| First Name | `John` |
| Last Name | `Doe` |
| Company | `Acme Corp` |
| Address | `123 Main St` |
| City | `Austin` |
| State | `Texas` |
| Zip | `78701` |
| Country | `United States` |
| Country | `United States` |
| Card Number (sensitive) | `4111111111111111` (mock: `4000123456789010`) | <!-- Sensitive key marked via (sensitive), mock value inline -->
| Name on Card | `John Doe` |
| Expiry (MM/YY) | `12/30` |
| CVV (sensitive) | `123` (mock: `999`) |                                 <!-- Sensitive key marked via (sensitive), mock value inline -->

### Product Selection
- **Size:** `16x12`
- **Finish:** `Matte` (JP: `マット`)

---

## Test Profiles (Logical Scenarios)

> Each checked profile represents a distinct logical dataset to execute. You can toggle checkboxes to filter which profiles run.

| Run | ID | Locale | Product Name | Subtotal | Shipping | Tax Rate | Expected Tax | Expected Total |
| :---: | :--- | :---: | :--- | :--- | :--- | :---: | :--- | :--- |
| - [x] | `Guest-US` | `en-US` | `Grizzly Bear` | `$17.00` | `$7.00` | `6.00%` | `$1.44` | `$25.44` |
| - [x] | `Guest-GB` | `en-GB` | `Grizzly Bear` | `£13.43` | `£7.00` | `6.00%` | `£1.23` | `£21.66` |
| - [x] | `Guest-DE` | `de-DE` | `Grizzlybär` | `14,96 €` | `7,00 €` | `6,00%` | `1,32 €` | `23,28 €` |
| - [x] | `Guest-SE` | `sv-SE` | `Grizzlybjörn` | `185,30 kr` | `7,00 kr` | `6,00%` | `11,54 kr` | `203,84 kr` |
| - [ ] | `Guest-JP` | `ja-JP` | `ハイイログマ` | `￥17` | `￥7` | `6.00%` | `￥1` | `￥25` |

---

## Steps

### 1. Search and Select the Product
- **Action:** Open the home page, search for the product name (`${productName}`) in the search bar. Click the product to open its detail page.
- **Verify:** The product detail page is displayed.

### 2. Add Fixed Product to Cart
- **Action:** Select size `16x12` and finish `Matte` (or localized equivalent), then click "Add to Cart".
- **Verify:** The mini-cart icon updates to show 1 item.

### 3. Navigate to Checkout
- **Action:** Open the cart page and click "Checkout".
- **Verify:** The user is redirected to the Shipping Address page (`/checkout/shippingAddress`).
- **Verify (tax rate & subtotal):** The cart summary displays the exact expected subtotal (`${subtotal}`), and the tax rate is displayed in the exact format **`6.00%`** (or `6,00 %`).

### 4. Enter Shipping and Payment Details
- **Action:** Complete shipping, billing, and billing-same-as-shipping address entry. Enter credit card details.
- **Verify:** The user reaches the order review page.

### 5. Review Order and Verify Exact Totals
- **Action:** Review all displayed information in the order summary panel.
- **Verify:**
  - The order items table lists **`${productName}`** with size `16x12`, finish `Matte`, quantity `1`.
  - The Subtotal, Shipping, Tax, and Total match **exactly** the values: `${subtotal}`, `${shipping}`, `${expectedTax}`, and `${expectedTotal}`.
  - A "Place Order" button (`#btn-place-order`) is visible.

### 6. Place Order
- **Action:** Click "Place Order".
- **Verify:**
  - The user is redirected to the Order Confirmation page.
  - The order summary panel displays the correct Subtotal, Tax, Shipping, and Total paid, matching **exactly** the expected profile values.
