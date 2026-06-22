# Playbook: End-to-End Order Checkout

This playbook validates guest and premium checkout flows under varying shipping and promotional configurations.

## Test Profiles

> Each profile represents a distinct logical dataset to execute. You can toggle checkboxes to filter which profiles run.

### - [x] Profile: BaseUser
- **ID:** `GuestCheckout`
- **Credentials:** [Default credentials](fragments/default_user.yaml)
- **Role:** `customer`
- **Shipping Method:** `Ground`
- **Promo Code:** `FREESHIP`

### - [x] Profile: PremiumUser
- **ID:** `PremiumCheckout`
- **Inherits:** [Base User](#Profile-BaseUser)
- **Username:** `premium_user@xceptance.com`
- **Password (private):** `realSecretPassword` (mock: `mockPassword_abc`) <!-- Sensitive key marked via (private), mock value inline -->
- **Shipping Method:** `NextDay`
- **Promo Code:** `PREMIUM20`

## Steps

1. Navigate to home page

> Set up user session with profile credentials

2. [Login Flow](fragments/login.steps)

3. Select product from catalog matching role "${role}"

4. Add item to cart

> Complete the order placement and verify

5. [Checkout Flow](fragments/checkout.steps)

6. Verify order confirmation message is visible for "${username}"

> [!NOTE]
> This step may take up to 10 seconds on staging due to payment gateway latency
