# Aura Report Manager

A standalone Spring Boot application providing a modern, clean, and interactive test report management hub built with **Bootstrap 5.3**, **HTMX 1.9**, and **Thymeleaf 3.x**.

---

## Technical Features

1. **Bootstrap 5.3 Framework**:
   - Clean, modern layout grid, cards, badges, buttons, navs, and tables.
   - Unified font size scaling (`--base-font-size: 0.875rem` / 14px).
2. **HTMX 1.9 Dynamic Fragment Swaps**:
   - Dynamic navigation and view switching via lightweight HTML fragment swaps (`hx-get`, `hx-target`, `hx-swap`).
3. **Unobtrusive Event System**:
   - Zero inline styles (`style="..."`) or inline JavaScript handlers (`onclick="..."`) in HTML templates.
4. **Spring Boot 3 / 4 Engine**:
   - Standalone Spring MVC & REST controller structure.

---

## How to Run

### Prerequisites
- JDK 21+
- Apache Maven 3.8+

### Commands

```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run
```

Once started, open **`http://localhost:8080/`** in your browser to access the Aura Report Manager!
