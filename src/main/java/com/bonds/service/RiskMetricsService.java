package com.bonds.service;

import com.bonds.domain.Bond;
import com.bonds.domain.Portfolio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class RiskMetricsService {

    private final ObjectMapper objectMapper;

    public RiskMetricsService() {
        this.objectMapper = new ObjectMapper();
        // Register JavaTimeModule to handle LocalDate serialization/deserialization
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Calculates the Yield to Maturity (YTM) for a bond using an approximation formula.
     *
     * Approximation formula:
     * YTM ≈ [Annual Coupon + (Face Value - Market Price) / Years to Maturity] / [(Face Value + Market Price) / 2]
     *
     * @param bond The bond to calculate YTM for
     * @return The approximate YTM as a decimal (e.g., 0.05 for 5%)
     * @throws IllegalArgumentException if market price is not set or bond data is invalid
     */
    public BigDecimal calculateYieldToMaturity(Bond bond) {
        if (bond.getMarketPrice() == null) {
            throw new IllegalArgumentException("Market price must be set to calculate YTM");
        }

        if (bond.getMaturityDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Bond has already matured");
        }

        BigDecimal faceValue = bond.getFaceValue();
        BigDecimal marketPrice = bond.getMarketPrice();
        BigDecimal couponRate = bond.getCouponRate();

        // Calculate years to maturity
        long daysToMaturity = ChronoUnit.DAYS.between(LocalDate.now(), bond.getMaturityDate());
        BigDecimal yearsToMaturity = BigDecimal.valueOf(daysToMaturity)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

        if (yearsToMaturity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid years to maturity");
        }

        // Calculate annual coupon payment
        BigDecimal annualCoupon = faceValue.multiply(couponRate);

        // Apply approximation formula:
        // YTM ≈ [Annual Coupon + (Face Value - Market Price) / Years] / [(Face Value + Market Price) / 2]

        BigDecimal capitalGainPerYear = faceValue.subtract(marketPrice)
                .divide(yearsToMaturity, 10, RoundingMode.HALF_UP);

        BigDecimal numerator = annualCoupon.add(capitalGainPerYear);

        BigDecimal denominator = faceValue.add(marketPrice)
                .divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);

        BigDecimal ytm = numerator.divide(denominator, 10, RoundingMode.HALF_UP);

        return ytm;
    }

    /**
     * Calculates the Macaulay Duration for a bond.
     *
     * Macaulay Duration is the weighted average time to receive all cash flows,
     * where weights are the present values of each cash flow.
     *
     * Formula: Duration = Σ[t × PV(CF_t)] / Price
     * where t is time in years, CF_t is cash flow at time t, PV is present value
     *
     * @param bond The bond to calculate duration for
     * @return The Macaulay Duration in years
     * @throws IllegalArgumentException if market price is not set or bond data is invalid
     */
    public BigDecimal calculateMacaulayDuration(Bond bond) {
        if (bond.getMarketPrice() == null) {
            throw new IllegalArgumentException("Market price must be set to calculate duration");
        }

        if (bond.getCouponDates() == null || bond.getCouponDates().isEmpty()) {
            throw new IllegalArgumentException("Coupon dates must be defined to calculate duration");
        }

        BigDecimal ytm = calculateYieldToMaturity(bond);
        BigDecimal faceValue = bond.getFaceValue();
        BigDecimal couponRate = bond.getCouponRate();
        LocalDate today = LocalDate.now();

        // Determine payment frequency from coupon dates
        int paymentsPerYear = determinePaymentFrequency(bond);
        BigDecimal couponPayment = faceValue.multiply(couponRate)
                .divide(BigDecimal.valueOf(paymentsPerYear), 10, RoundingMode.HALF_UP);

        BigDecimal weightedPV = BigDecimal.ZERO;
        BigDecimal totalPV = BigDecimal.ZERO;

        // Calculate present value of coupon payments
        for (LocalDate couponDate : bond.getCouponDates()) {
            if (couponDate.isAfter(today)) {
                long daysFromNow = ChronoUnit.DAYS.between(today, couponDate);
                BigDecimal yearsFromNow = BigDecimal.valueOf(daysFromNow)
                        .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

                // Calculate discount factor: 1 / (1 + ytm)^t
                BigDecimal discountFactor = BigDecimal.ONE.add(ytm)
                        .pow(yearsFromNow.intValue())
                        .setScale(10, RoundingMode.HALF_UP);

                // For fractional years, use approximation
                if (yearsFromNow.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                    discountFactor = calculateDiscountFactor(ytm, yearsFromNow);
                }

                BigDecimal presentValue = couponPayment.divide(discountFactor, 10, RoundingMode.HALF_UP);

                weightedPV = weightedPV.add(yearsFromNow.multiply(presentValue));
                totalPV = totalPV.add(presentValue);
            }
        }

        // Add present value of principal payment at maturity
        long daysToMaturity = ChronoUnit.DAYS.between(today, bond.getMaturityDate());
        BigDecimal yearsToMaturity = BigDecimal.valueOf(daysToMaturity)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

        BigDecimal discountFactorMaturity = calculateDiscountFactor(ytm, yearsToMaturity);
        BigDecimal principalPV = faceValue.divide(discountFactorMaturity, 10, RoundingMode.HALF_UP);

        weightedPV = weightedPV.add(yearsToMaturity.multiply(principalPV));
        totalPV = totalPV.add(principalPV);

        // Macaulay Duration = Sum of weighted PVs / Total PV (bond price)
        BigDecimal macaulayDuration = weightedPV.divide(totalPV, 10, RoundingMode.HALF_UP);

        return macaulayDuration;
    }

    /**
     * Calculates the Modified Duration for a bond.
     *
     * Modified Duration measures the percentage change in bond price for a 1% change in yield.
     * It adjusts Macaulay Duration for the yield level and payment frequency.
     *
     * Formula: Modified Duration = Macaulay Duration / (1 + YTM/frequency)
     *
     * @param bond The bond to calculate modified duration for
     * @return The Modified Duration
     * @throws IllegalArgumentException if market price is not set or bond data is invalid
     */
    public BigDecimal calculateModifiedDuration(Bond bond) {
        BigDecimal macaulayDuration = calculateMacaulayDuration(bond);
        BigDecimal ytm = calculateYieldToMaturity(bond);
        int paymentsPerYear = determinePaymentFrequency(bond);

        // Modified Duration = Macaulay Duration / (1 + YTM/frequency)
        BigDecimal ytmPerPeriod = ytm.divide(BigDecimal.valueOf(paymentsPerYear), 10, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.add(ytmPerPeriod);

        BigDecimal modifiedDuration = macaulayDuration.divide(divisor, 10, RoundingMode.HALF_UP);

        return modifiedDuration;
    }

    /**
     * Determines the payment frequency (payments per year) from coupon dates.
     * Defaults to 2 (semi-annual) if cannot be determined.
     */
    private int determinePaymentFrequency(Bond bond) {
        if (bond.getCouponDates() == null || bond.getCouponDates().size() < 2) {
            return 2; // Default to semi-annual
        }

        // Count coupon payments in first year to determine frequency
        LocalDate firstDate = bond.getCouponDates().get(0);
        LocalDate oneYearLater = firstDate.plusYears(1);
        long paymentsInYear = bond.getCouponDates().stream()
                .filter(date -> !date.isBefore(firstDate) && date.isBefore(oneYearLater))
                .count();

        return paymentsInYear > 0 ? (int) paymentsInYear : 2;
    }

    /**
     * Calculates discount factor (1 + r)^t for fractional time periods.
     * Uses approximation: (1 + r)^t ≈ e^(t * ln(1 + r))
     */
    private BigDecimal calculateDiscountFactor(BigDecimal rate, BigDecimal years) {
        if (years.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }

        double r = rate.doubleValue();
        double t = years.doubleValue();
        double discountFactor = Math.pow(1 + r, t);

        return BigDecimal.valueOf(discountFactor).setScale(10, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the Portfolio-level weighted average Modified Duration.
     *
     * Each bond's duration is weighted by its proportion of the total portfolio market value.
     * Formula: Portfolio Duration = Σ(Weight_i × Duration_i)
     * where Weight_i = Market Value of Bond_i / Total Portfolio Market Value
     *
     * This metric indicates the approximate percentage change in the portfolio's value
     * for a 1% change in interest rates.
     *
     * @param portfolio The portfolio containing bonds
     * @return The weighted average Modified Duration for the portfolio
     * @throws IllegalArgumentException if portfolio is empty or bonds lack market prices
     */
    public BigDecimal calculatePortfolioWeightedDuration(Portfolio portfolio) {
        if (portfolio.getBonds() == null || portfolio.getBonds().isEmpty()) {
            throw new IllegalArgumentException("Portfolio must contain at least one bond");
        }

        // Calculate total portfolio market value
        BigDecimal totalPortfolioValue = BigDecimal.ZERO;
        for (Bond bond : portfolio.getBonds()) {
            if (bond.getMarketPrice() == null) {
                throw new IllegalArgumentException("All bonds must have market prices set to calculate portfolio duration");
            }
            totalPortfolioValue = totalPortfolioValue.add(bond.getMarketPrice());
        }

        if (totalPortfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Total portfolio value cannot be zero");
        }

        // Calculate weighted duration
        BigDecimal weightedDuration = BigDecimal.ZERO;
        for (Bond bond : portfolio.getBonds()) {
            // Calculate bond's weight in portfolio
            BigDecimal bondWeight = bond.getMarketPrice()
                    .divide(totalPortfolioValue, 10, RoundingMode.HALF_UP);

            // Calculate bond's modified duration
            BigDecimal bondDuration = calculateModifiedDuration(bond);

            // Add weighted duration contribution
            BigDecimal weightedContribution = bondWeight.multiply(bondDuration);
            weightedDuration = weightedDuration.add(weightedContribution);
        }

        return weightedDuration;
    }

    /**
     * Deserializes a JSON string into a list of Bond objects.
     *
     * Expected JSON format:
     * [
     *   {
     *     "isin": "US1234567890",
     *     "maturityDate": "2030-12-31",
     *     "couponDates": ["2025-06-30", "2025-12-31", "2026-06-30"],
     *     "couponRate": 0.05,
     *     "faceValue": 1000,
     *     "marketPrice": 950
     *   },
     *   ...
     * ]
     *
     * @param jsonString The JSON string containing an array of bond objects
     * @return A list of Bond objects
     * @throws IllegalArgumentException if JSON is invalid or cannot be parsed
     */
    public List<Bond> deserializeBondsFromJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }

        try {
            Bond[] bondsArray = objectMapper.readValue(jsonString, Bond[].class);
            return Arrays.asList(bondsArray);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse JSON string: " + e.getMessage(), e);
        }
    }
}
