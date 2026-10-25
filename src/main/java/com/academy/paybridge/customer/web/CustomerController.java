package com.academy.paybridge.customer.web;

import com.academy.paybridge.customer.api.CustomerView;
import com.academy.paybridge.customer.domain.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    public record RegisterCustomerRequest(
            @NotBlank @Size(max = 50) String firstName,
            @NotBlank @Size(max = 50) String lastName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "^(\\+234|0)[789][01]\\d{8}$", message = "must be a Nigerian mobile number")
            String phoneNumber,
            @NotNull @Past LocalDate dateOfBirth) {
    }

    public record BvnRequest(@NotBlank @Pattern(regexp = "\\d{11}", message = "must be 11 digits") String bvn) {
    }

    public record NinRequest(@NotBlank @Pattern(regexp = "\\d{11}", message = "must be 11 digits") String nin) {
    }

    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerView register(@Valid @RequestBody RegisterCustomerRequest r) {
        return customers.register(r.firstName(), r.lastName(), r.email(), r.phoneNumber(), r.dateOfBirth());
    }

    @GetMapping("/{id}")
    public CustomerView get(@PathVariable UUID id) {
        return customers.getById(id);
    }

    @PostMapping("/{id}/kyc/bvn")
    public CustomerView verifyBvn(@PathVariable UUID id, @Valid @RequestBody BvnRequest request) {
        return customers.verifyBvn(id, request.bvn());
    }

    @PostMapping("/{id}/kyc/nin")
    public CustomerView verifyNin(@PathVariable UUID id, @Valid @RequestBody NinRequest request) {
        return customers.verifyNin(id, request.nin());
    }
}
