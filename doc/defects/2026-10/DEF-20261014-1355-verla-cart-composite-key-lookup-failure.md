# [DEF-20261014-1355] VÉRLA Mini-Cart Dropdown and Drawer Render Empty Due to Composite Key Lookup Failure

- **Status:** `Resolved`
- **Opened:** 2026-10-14 13:55
- **Closed:** 2026-10-14 14:00
- **Component:** `neodymium-test-server`
- **Scope:** `SUT & Test/Harness`
- **Symptom:** When adding items with a size selection (e.g. Size 32 pants) to the cart in any VÉRLA storefront flavor, the header cart badge incremented, but the mini-cart dropdown, cart drawer, and cart page rendered zero items with an empty item container and "$0.00" subtotal.
- **Root Cause:** Sized cart entries are stored in `Cart.items` with composite keys (`<id>:<size>`, e.g. `SKU-PNT-1000:32`). When `VerlaRenderer` iterates over `cart.items`, it queries `catalog.lookupProductById(entry.getKey())`. In `VerlaCatalog.java`, `lookupProductById` queried `productsById.get(id)` directly without stripping the `:size` discriminator and without fallback between product IDs and slugs. The lookup returned `null`, causing `VerlaRenderer` to skip every item.
- **Detection Gap ("What did we miss?"):** Existing E2E storefront tests (`VerlaTailwindStorefrontTest`, etc.) asserted that the cart counter badge incremented (`CART 1`), but did not assert on the contents of the rendered mini-cart dropdown, drawer rows, or subtotal calculation.
- **Resolution:** Updated `VerlaCatalog.getProductById`, `lookupProductById`, `getProductBySlug`, and `lookupProductBySlug` to strip `:size` discriminators and provide bidirectional ID/slug fallback.
- **Safety Net Added:** Added unit tests in `VerlaCatalogTest` validating composite key lookups (`id:size`), slug fallbacks, and null handling; added integration assertions in `JavalinVerlaServerTest.testCartOperations` asserting non-zero subtotal and item row presence in rendered cart dropdowns and cart pages.
