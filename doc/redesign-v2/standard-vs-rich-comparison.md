# LEAN vs. STANDARD vs. RICH DOM Context Comparison (Neodymium AI v2)

**Author:** AI-generated: Gemini 3.6 Flash  
**Organization:** Xceptance GmbH 2026  
**Document:** Comparison of `LEAN`, `STANDARD`, and `RICH` DOM extraction levels  

---

## 1. Executive Summary

In Neodymium AI's context escalation ladder:

$$\text{HINT} \longrightarrow \mathbf{LEAN} \longrightarrow \mathbf{STANDARD} \longrightarrow \mathbf{RICH} \longrightarrow \mathbf{VISUAL} \longrightarrow \mathbf{VISUAL\_LEAN} \longrightarrow \mathbf{VISUAL\_RICH}$$

- **`LEAN`**: Core interaction level. Includes interactive elements, headings, container skeleton, and concise text labels. Filters out long static paragraph copy (`<p>`/`blockquote` > 120 chars). Saves ~30% tokens.
- **`STANDARD`**: Full static text DOM level. Includes `LEAN` + **all static text copy** (`<p>` tags of any length, full descriptions, text spans, badges, and prices).
- **`RICH`**: Full metadata DOM level. Includes `STANDARD` + **full HTML metadata attributes** (`data-*`, `title`, `aria-describedby`) and expands the structural parent text disambiguation depth from **3 levels to 5 levels**.

---

## 2. Side-by-Side Code Comparison

### Sample Product Card HTML

```html
<div class="product-card" data-product-id="prod-101" data-category="tops">
  <div class="product-info">
    <span class="product-category">TOPS</span>
    <h3 class="product-title" title="Minimalist Oversized Hoodie - Organic Cotton">Minimalist Oversized Hoodie</h3>
    <p class="product-desc">Minimalist silhouettes engineered with sustainable organic textiles and precision craftsmanship in Berlin.</p>
    <p class="product-price" aria-describedby="price-disclaimer-101">$120.00 USD</p>
    <div class="size-selector">
      <button class="btn-size" data-sku="HOODIE-OPT-S">S</button>
      <button class="btn-size selected" data-sku="HOODIE-OPT-M">M</button>
      <button class="btn-size" data-sku="HOODIE-OPT-L">L</button>
    </div>
    <div class="product-actions">
      <button class="btn-quick-add" data-analytics="add-cart-top-101">ADD TO BAG</button>
    </div>
  </div>
</div>
```

---

### Comparison Carousel Matrix

````carousel
```html
<!-- LEAN DOM Capture (Excludes Long Paragraph Copy) -->
<div class="product-card" data-ai="xce869u2">
  <div class="product-info" data-ai="xc9g93c4">
    <span class="product-category" selector="span.product-category" data-ai="xc2nupq6">TOPS</span>
    <h3 class="product-title" selector="h3.product-title" data-ai="xceg4w4t">Minimalist Oversized Hoodie</h3>
    <p class="product-price" selector="p.product-price" data-ai="xc1j5c6g">$120.00 USD</p>
    <div class="size-selector" data-ai="xca712m9">
      <button class="btn-size" type="button" selector="button.btn-size:nth-of-type(1)" data-ai="xc99a81z">S</button>
      <button class="btn-size selected" type="button" selector="button.btn-size:nth-of-type(2)" data-ai="xc11b92x">M</button>
      <button class="btn-size" type="button" selector="button.btn-size:nth-of-type(3)" data-ai="xc44f99q">L</button>
    </div>
    <div class="product-actions" data-ai="xc5f2v4m">
      <button class="btn-quick-add" type="button" selector="button.btn-quick-add:nth-of-type(1)" data-ai="xce624f1">ADD TO BAG</button>
    </div>
  </div>
</div>
```
<!-- slide -->
```html
<!-- STANDARD DOM Capture (LEAN + Full Static Paragraph Copy) -->
<div class="product-card" data-ai="xce869u2">
  <div class="product-info" data-ai="xc9g93c4">
    <span class="product-category" selector="span.product-category" data-ai="xc2nupq6">TOPS</span>
    <h3 class="product-title" selector="h3.product-title" data-ai="xceg4w4t">Minimalist Oversized Hoodie</h3>
    <p class="product-desc" selector="p.product-desc" data-ai="xch294lm">Minimalist silhouettes engineered with sustainable organic textiles and precision craftsmanship in Berlin.</p>
    <p class="product-price" selector="p.product-price" data-ai="xc1j5c6g">$120.00 USD</p>
    <div class="size-selector" data-ai="xca712m9">
      <button class="btn-size" type="button" selector="button.btn-size:nth-of-type(1)" data-ai="xc99a81z">S</button>
      <button class="btn-size selected" type="button" selector="button.btn-size:nth-of-type(2)" data-ai="xc11b92x">M</button>
      <button class="btn-size" type="button" selector="button.btn-size:nth-of-type(3)" data-ai="xc44f99q">L</button>
    </div>
    <div class="product-actions" data-ai="xc5f2v4m">
      <button class="btn-quick-add" type="button" selector="button.btn-quick-add:nth-of-type(1)" data-ai="xce624f1">ADD TO BAG</button>
    </div>
  </div>
</div>
```
<!-- slide -->
```html
<!-- RICH DOM Capture (STANDARD + data-*, title, aria-describedby + 5-Level Parent Context) -->
<div class="product-card" data-product-id="prod-101" data-category="tops" data-ai="xce869u2">
  <div class="product-info" data-ai="xc9g93c4">
    <span class="product-category" selector="span.product-category" data-ai="xc2nupq6">TOPS</span>
    <h3 class="product-title" selector="h3.product-title" title="Minimalist Oversized Hoodie - Organic Cotton" data-ai="xceg4w4t">Minimalist Oversized Hoodie</h3>
    <p class="product-desc" selector="p.product-desc" data-ai="xch294lm">Minimalist silhouettes engineered with sustainable organic textiles and precision craftsmanship in Berlin.</p>
    <p class="product-price" selector="p.product-price" aria-describedby="price-disclaimer-101" data-ai="xc1j5c6g">$120.00 USD</p>
    <div class="size-selector" data-ai="xca712m9">
      <button class="btn-size" type="button" data-sku="HOODIE-OPT-S" selector="button.btn-size:nth-of-type(1)" data-ai="xc99a81z">S</button>
      <button class="btn-size selected" type="button" data-sku="HOODIE-OPT-M" selector="button.btn-size:nth-of-type(2)" data-ai="xc11b92x">M</button>
      <button class="btn-size" type="button" data-sku="HOODIE-OPT-L" selector="button.btn-size:nth-of-type(3)" data-ai="xc44f99q">L</button>
    </div>
    <div class="product-actions" data-ai="xc5f2v4m">
      <button class="btn-quick-add" type="button" data-analytics="add-cart-top-101" selector="button.btn-quick-add:nth-of-type(1)" data-parent-text="TOPS > Minimalist Oversized Hoodie > $120.00 USD > Size M" data-ai="xce624f1">ADD TO BAG</button>
    </div>
  </div>
</div>
```
````

---

## 3. Feature Breakdown

| Feature / Metadata | `LEAN` Mode | `STANDARD` Mode | `RICH` Mode |
| :--- | :---: | :---: | :---: |
| **Interactive Locators & Selectors** | ✅ Included | ✅ Included | ✅ Included |
| **Headings & Container Skeletons** | ✅ Included | ✅ Included | ✅ Included |
| **Short Text Labels (Buttons, Prices)** | ✅ Included | ✅ Included | ✅ Included |
| **Long Static Paragraph Copy (`<p>` > 120 chars)** | ❌ **Filtered** | ✅ **Included** | ✅ **Included** |
| **`title="..."` Tooltip Attributes** | ❌ Omitted | ❌ Omitted | ✅ **Captured** |
| **`aria-describedby="..."` Accessibility Link** | ❌ Omitted | ❌ Omitted | ✅ **Captured** |
| **`data-*="..."` Data Attributes** (`data-sku`, `data-testid`) | ❌ Omitted | ❌ Omitted | ✅ **Captured** |
| **Parent Disambiguation Text Context Depth** | Up to **3 levels** | Up to **3 levels** | Up to **5 levels** |
