package com.academy.paybridge.customer.api;

import java.util.UUID;

/**
 * The only way other modules may talk to the customer module.
 */
public interface CustomerApi {

    CustomerView getById(UUID customerId);
}
