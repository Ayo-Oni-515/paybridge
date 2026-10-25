package com.academy.paybridge.fx.domain;

import com.academy.paybridge.fx.api.FxApi;
import com.academy.paybridge.fx.api.QuoteView;
import com.academy.paybridge.fx.client.RateProvider;
import com.academy.paybridge.fx.repository.FxQuoteRepository;
import com.academy.paybridge.fx.repository.FxSaleRepository;
import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.ledger.api.SystemAccounts;
import com.academy.paybridge.shared.exception.BusinessException;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class FxService implements FxApi {

    public record RateView(Currency from, Currency to, BigDecimal midRate, BigDecimal customerRate,
                           BigDecimal spreadPercent) {}

    public record PositionView(BigDecimal usdHeld, BigDecimal ngnPosition, BigDecimal ngnRevenue) {}

    private final FxQuoteRepository quotes;
    private final FxSaleRepository sales;
    private final RateProvider rates;
    private final LedgerApi ledger;
    private final FxProperties properties;
    private final Clock clock;

    public FxService(FxQuoteRepository quotes, FxSaleRepository sales, RateProvider rates, LedgerApi ledger,
                     FxProperties properties, Clock clock) {
        this.quotes = quotes;
        this.sales = sales;
        this.rates = rates;
        this.ledger = ledger;
        this.properties = properties;
        this.clock = clock;
    }

    public RateView currentRate(Currency from, Currency to) {
        BigDecimal mid = rates.midRate(from, to);
        return new RateView(from, to, mid, RateCalculator.customerRate(mid, properties.spreadPercent()),
                properties.spreadPercent());
    }

    // ------------------------------------------------------------------
    // Quotes: "$100 = NGN X, valid for 5 minutes"
    // ------------------------------------------------------------------
    @Transactional
    public QuoteView createQuote(Money source, Currency target) {
        if (!source.isPositive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero");
        }
        RateView rate = currentRate(source.currency(), target);
        Money targetAmount = Money.of(RateCalculator.convert(source.amount(), rate.customerRate()), target);

        Instant now = clock.instant();
        FxQuote quote = quotes.save(new FxQuote(source, targetAmount, rate.midRate(), rate.customerRate(),
                now, now.plus(properties.quoteTtl())));
        return quote.toView(now);
    }

    @Transactional(readOnly = true)
    public QuoteView getQuote(UUID quoteId) {
        return quotes.findById(quoteId).map(q -> q.toView(clock.instant()))
                .orElseThrow(() -> quoteNotFound(quoteId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public QuoteView consumeQuote(UUID quoteId, String usedBy) {
        FxQuote quote = quotes.findForUpdate(quoteId).orElseThrow(() -> quoteNotFound(quoteId));
        Instant now = clock.instant();
        if (quote.isUsed()) {
            throw new BusinessException(HttpStatus.CONFLICT, "QUOTE_ALREADY_USED", "Quote " + quoteId + " was already used");
        }
        if (quote.isExpired(now)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "QUOTE_EXPIRED",
                    "Quote expired at " + quote.getExpiresAt() + "; request a new one");
        }
        quote.markUsed(usedBy, now);
        return quote.toView(now);
    }

    // ------------------------------------------------------------------
    // Selling the dollars (FX desk). Two journals, one per currency.
    // ------------------------------------------------------------------
    @Transactional
    public FxSale.View sellUsd(String idempotencyKey, BigDecimal usdAmount, BigDecimal ngnPerUsd) {
        Optional<FxSale> existing = sales.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().toView();
        }

        Money usd = Money.of(usdAmount, Currency.USD);
        BigDecimal held = ledger.balanceOf(SystemAccounts.USD_FX_POSITION).amount();
        if (held.compareTo(usdAmount) < 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INSUFFICIENT_USD_POSITION",
                    "Cannot sell USD " + usdAmount + "; the FX position holds USD " + held);
        }
        Money ngn = Money.of(RateCalculator.convert(usdAmount, ngnPerUsd), Currency.NGN);

        FxSale sale = sales.save(new FxSale(idempotencyKey, usd, ngnPerUsd, ngn, clock.instant()));
        String ref = "fx-sale-" + sale.getId();

        ledger.move(ref + ":usd", SystemAccounts.USD_FX_POSITION, SystemAccounts.USD_FX_SOLD, usd,
                "Sold USD at " + ngnPerUsd);
        ledger.move(ref + ":ngn", SystemAccounts.NGN_FX_BOUGHT, SystemAccounts.NGN_FX_POSITION, ngn,
                "NGN received for USD sale");

        // Once every dollar is sold, whatever is left in the NGN position is the spread.
        // (With dollars still held we can't tell the profit yet, so we wait.)
        if (ledger.balanceOf(SystemAccounts.USD_FX_POSITION).amount().signum() == 0) {
            BigDecimal profit = ledger.balanceOf(SystemAccounts.NGN_FX_POSITION).amount();
            if (profit.signum() > 0) {
                ledger.move(ref + ":revenue", SystemAccounts.NGN_FX_POSITION, SystemAccounts.NGN_FX_REVENUE,
                        Money.of(profit, Currency.NGN), "FX spread earned");
            } else if (profit.signum() < 0) {
                ledger.move(ref + ":loss", SystemAccounts.NGN_FX_REVENUE, SystemAccounts.NGN_FX_POSITION,
                        Money.of(profit.negate(), Currency.NGN), "FX loss: sold below the rate we paid");
            }
            sale.recordRealisedProfit(profit);
        }
        return sale.toView();
    }

    @Transactional(readOnly = true)
    public PositionView position() {
        return new PositionView(
                ledger.balanceOf(SystemAccounts.USD_FX_POSITION).amount(),
                ledger.balanceOf(SystemAccounts.NGN_FX_POSITION).amount(),
                ledger.balanceOf(SystemAccounts.NGN_FX_REVENUE).amount());
    }

    private static BusinessException quoteNotFound(UUID id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "QUOTE_NOT_FOUND", "Quote not found: " + id);
    }
}
