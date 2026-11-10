/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.server.verla.model;

import java.util.List;

/**
 * Customer order representation in the VÉRLA storefront.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class Order
{
    public final String orderNumber;
    public final String email;
    public final List<OrderItem> items;
    public final double subtotal;
    public final double shipping;
    public final double tax;
    public final double total;
    public final String status;
    public final Address shippingAddress;
    public final Card paymentCard;
    public final String discountApplied;
    public final boolean freeGiftAdded;

    public Order(final String orderNumber, final String email, final List<OrderItem> items, final double subtotal,
                 final double shipping, final double tax, final double total, final String status,
                 final Address shippingAddress, final Card paymentCard, final String discountApplied, final boolean freeGiftAdded)
    {
        this.orderNumber = orderNumber;
        this.email = email;
        this.items = items;
        this.subtotal = subtotal;
        this.shipping = shipping;
        this.tax = tax;
        this.total = total;
        this.status = status;
        this.shippingAddress = shippingAddress;
        this.paymentCard = paymentCard;
        this.discountApplied = discountApplied;
        this.freeGiftAdded = freeGiftAdded;
    }
}
