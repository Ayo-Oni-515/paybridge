package com.academy.paybridge.fx.web;

import com.academy.paybridge.fx.api.QuoteView;
import com.academy.paybridge.fx.domain.FxSale;
import com.academy.paybridge.fx.domain.FxService;
import com.academy.paybridge.shared.exception.BusinessException;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/fx")
public class FxController {

    public record QuoteRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal sourceAmount,
            @NotNull Currency sourceCurrency,
            @NotNull Currency targetCurrency) {
    }

    public record SaleRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal usdAmount,
            @NotNull @DecimalMin("0.000001") @Digits(integer = 13, fraction = 6) BigDecimal ngnPerUsd) {
    }

    private final FxService fx;

    public FxController(FxService fx) {
        this.fx = fx;
    }

    /** GET /api/fx/rates/USD/NGN */
    @GetMapping("/rates/{from}/{to}")
    public FxService.RateView rate(@PathVariable Currency from, @PathVariable Currency to) {
        return fx.currentRate(from, to);
    }

    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteView quote(@Valid @RequestBody QuoteRequest request) {
        return fx.createQuote(Money.of(request.sourceAmount(), request.sourceCurrency()), request.targetCurrency());
    }

    @GetMapping("/quotes/{id}")
    public QuoteView getQuote(@PathVariable UUID id) {
        return fx.getQuote(id);
    }

    /** Treasury: sell collected dollars to a bank / FX desk. */
    @PostMapping("/sales")
    public FxSale.View sell(@RequestHeader("Idempotency-Key") String idempotencyKey,
                            @Valid @RequestBody SaleRequest request) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 64) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key header is required (max 64 characters)");
        }
        return fx.sellUsd(idempotencyKey, request.usdAmount(), request.ngnPerUsd());
    }

    @GetMapping("/position")
    public FxService.PositionView position() {
        return fx.position();
    }
}
