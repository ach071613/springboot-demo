package com.bonds.service;

import com.bonds.domain.Bond;
import com.bonds.domain.Portfolio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskMetricsServiceTest {

    private RiskMetricsService riskMetricsService;

    @BeforeEach
    void setUp() {
        riskMetricsService = new RiskMetricsService();
    }

    // ========== YTM Tests ==========

    @Test
    void testCalculateYieldToMaturity_Success() {
        // Given: A bond trading at discount (market price < face value)
        Bond bond = createBond("US1234567890", LocalDate.now().plusYears(5),
                BigDecimal.valueOf(0.05), BigDecimal.valueOf(1000), BigDecimal.valueOf(950));

        // When
        BigDecimal ytm = riskMetricsService.calculateYieldToMaturity(bond);

        // Then: YTM should be higher than coupon rate when bond trades at discount
        assertNotNull(ytm);
        assertTrue(ytm.compareTo(BigDecimal.valueOf(0.05)) > 0,
                "YTM should be greater than coupon rate for discount bond");
    }

    @Test
    void testCalculateYieldToMaturity_BondAtPremium() {
        // Given: A bond trading at premium (market price > face value)
        Bond bond = createBond("US1234567890", LocalDate.now().plusYears(5),
                BigDecimal.valueOf(0.05), BigDecimal.valueOf(1000), BigDecimal.valueOf(1050));

        // When
        BigDecimal ytm = riskMetricsService.calculateYieldToMaturity(bond);

        // Then: YTM should be lower than coupon rate when bond trades at premium
        assertNotNull(ytm);
        assertTrue(ytm.compareTo(BigDecimal.valueOf(0.05)) < 0,
                "YTM should be less than coupon rate for premium bond");
    }

    @Test
    void testCalculateYieldToMaturity_BondAtPar() {
        // Given: A bond trading at par (market price = face value)
        Bond bond = createBond("US1234567890", LocalDate.now().plusYears(5),
                BigDecimal.valueOf(0.05), BigDecimal.valueOf(1000), BigDecimal.valueOf(1000));

        // When
        BigDecimal ytm = riskMetricsService.calculateYieldToMaturity(bond);

        // Then: YTM should be approximately equal to coupon rate
        assertNotNull(ytm);
        assertEquals(0.05, ytm.doubleValue(), 0.001,
                "YTM should equal coupon rate for bond at par");
    }

    @Test
    void testCalculateYieldToMaturity_MarketPriceNull() {
        // Given: A bond without market price
        Bond bond = createBond("US1234567890", LocalDate.now().plusYears(5),
                BigDecimal.valueOf(0.05), BigDecimal.valueOf(1000), null);

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.calculateYieldToMaturity(bond),
                "Should throw exception when market price is null");
    }

    @Test
    void testCalculateYieldToMaturity_BondAlreadyMatured() {
        // Given: A bond that has already matured
        Bond bond = createBond("US1234567890", LocalDate.now().minusDays(1),
                BigDecimal.valueOf(0.05), BigDecimal.valueOf(1000), BigDecimal.valueOf(950));

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.calculateYieldToMaturity(bond),
                "Should throw exception when bond has already matured");
    }

    @Test
    void testCalculateYieldToMaturity_ShortMaturity() {
        // Given: A bond with very short maturity (30 days)
        Bond bond = createBond("US1234567890", LocalDate.now().plusDays(30),
                BigDecimal.valueOf(0.05), BigDecimal.valueOf(1000), BigDecimal.valueOf(995));

        // When
        BigDecimal ytm = riskMetricsService.calculateYieldToMaturity(bond);

        // Then: Should calculate YTM even for short-term bonds
        assertNotNull(ytm);
        assertTrue(ytm.compareTo(BigDecimal.ZERO) > 0);
    }

    // ========== Macaulay Duration Tests ==========

    @Test
    void testCalculateMacaulayDuration_Success() {
        // Given: A bond with semi-annual coupons
        Bond bond = createBondWithCouponDates("US1234567890",
                LocalDate.now().plusYears(5),
                createSemiAnnualCouponDates(5),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(950));

        // When
        BigDecimal duration = riskMetricsService.calculateMacaulayDuration(bond);

        // Then: Duration should be positive and less than maturity
        assertNotNull(duration);
        assertTrue(duration.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(duration.compareTo(BigDecimal.valueOf(5)) < 0,
                "Duration should be less than time to maturity");
    }

    @Test
    void testCalculateMacaulayDuration_MarketPriceNull() {
        // Given: A bond without market price
        Bond bond = createBondWithCouponDates("US1234567890",
                LocalDate.now().plusYears(5),
                createSemiAnnualCouponDates(5),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(1000),
                null);

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.calculateMacaulayDuration(bond),
                "Should throw exception when market price is null");
    }

    @Test
    void testCalculateMacaulayDuration_NoCouponDates() {
        // Given: A bond without coupon dates
        Bond bond = createBond("US1234567890", LocalDate.now().plusYears(5),
                BigDecimal.valueOf(0.05), BigDecimal.valueOf(1000), BigDecimal.valueOf(950));
        bond.setCouponDates(new ArrayList<>());

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.calculateMacaulayDuration(bond),
                "Should throw exception when coupon dates are empty");
    }

    @Test
    void testCalculateMacaulayDuration_NullCouponDates() {
        // Given: A bond with null coupon dates
        Bond bond = createBond("US1234567890", LocalDate.now().plusYears(5),
                BigDecimal.valueOf(0.05), BigDecimal.valueOf(1000), BigDecimal.valueOf(950));
        bond.setCouponDates(null);

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.calculateMacaulayDuration(bond),
                "Should throw exception when coupon dates are null");
    }

    @Test
    void testCalculateMacaulayDuration_HigherCouponLowerDuration() {
        // Given: Two bonds with same maturity but different coupon rates
        Bond lowCouponBond = createBondWithCouponDates("US1234567890",
                LocalDate.now().plusYears(5),
                createSemiAnnualCouponDates(5),
                BigDecimal.valueOf(0.02),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(950));

        Bond highCouponBond = createBondWithCouponDates("US9876543210",
                LocalDate.now().plusYears(5),
                createSemiAnnualCouponDates(5),
                BigDecimal.valueOf(0.08),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1050));

        // When
        BigDecimal lowCouponDuration = riskMetricsService.calculateMacaulayDuration(lowCouponBond);
        BigDecimal highCouponDuration = riskMetricsService.calculateMacaulayDuration(highCouponBond);

        // Then: Higher coupon bond should have lower duration
        assertTrue(highCouponDuration.compareTo(lowCouponDuration) < 0,
                "Bond with higher coupon should have lower duration");
    }

    // ========== Modified Duration Tests ==========

    @Test
    void testCalculateModifiedDuration_Success() {
        // Given: A bond with semi-annual coupons
        Bond bond = createBondWithCouponDates("US1234567890",
                LocalDate.now().plusYears(5),
                createSemiAnnualCouponDates(5),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(950));

        // When
        BigDecimal modifiedDuration = riskMetricsService.calculateModifiedDuration(bond);
        BigDecimal macaulayDuration = riskMetricsService.calculateMacaulayDuration(bond);

        // Then: Modified duration should be less than Macaulay duration
        assertNotNull(modifiedDuration);
        assertTrue(modifiedDuration.compareTo(macaulayDuration) < 0,
                "Modified duration should be less than Macaulay duration");
    }

    @Test
    void testCalculateModifiedDuration_MarketPriceNull() {
        // Given: A bond without market price
        Bond bond = createBondWithCouponDates("US1234567890",
                LocalDate.now().plusYears(5),
                createSemiAnnualCouponDates(5),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(1000),
                null);

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.calculateModifiedDuration(bond),
                "Should throw exception when market price is null");
    }

    // ========== Portfolio Weighted Duration Tests ==========

    @Test
    void testCalculatePortfolioWeightedDuration_Success() {
        // Given: A portfolio with multiple bonds
        Portfolio portfolio = new Portfolio("Test Portfolio");

        Bond bond1 = createBondWithCouponDates("US1234567890",
                LocalDate.now().plusYears(3),
                createSemiAnnualCouponDates(3),
                BigDecimal.valueOf(0.04),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(500)); // 50% weight

        Bond bond2 = createBondWithCouponDates("US9876543210",
                LocalDate.now().plusYears(7),
                createSemiAnnualCouponDates(7),
                BigDecimal.valueOf(0.06),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(500)); // 50% weight

        portfolio.addBond(bond1);
        portfolio.addBond(bond2);

        // When
        BigDecimal portfolioDuration = riskMetricsService.calculatePortfolioWeightedDuration(portfolio);

        // Then: Portfolio duration should be between the two bond durations
        BigDecimal duration1 = riskMetricsService.calculateModifiedDuration(bond1);
        BigDecimal duration2 = riskMetricsService.calculateModifiedDuration(bond2);

        assertNotNull(portfolioDuration);
        assertTrue(portfolioDuration.compareTo(duration1) > 0);
        assertTrue(portfolioDuration.compareTo(duration2) < 0);
    }

    @Test
    void testCalculatePortfolioWeightedDuration_EmptyPortfolio() {
        // Given: An empty portfolio
        Portfolio portfolio = new Portfolio("Empty Portfolio");

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.calculatePortfolioWeightedDuration(portfolio),
                "Should throw exception for empty portfolio");
    }

    @Test
    void testCalculatePortfolioWeightedDuration_NullBondsList() {
        // Given: A portfolio with null bonds list
        Portfolio portfolio = new Portfolio("Test Portfolio");
        portfolio.setBonds(null);

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.calculatePortfolioWeightedDuration(portfolio),
                "Should throw exception when bonds list is null");
    }

    @Test
    void testCalculatePortfolioWeightedDuration_BondWithoutMarketPrice() {
        // Given: A portfolio with a bond missing market price
        Portfolio portfolio = new Portfolio("Test Portfolio");

        Bond bond = createBondWithCouponDates("US1234567890",
                LocalDate.now().plusYears(3),
                createSemiAnnualCouponDates(3),
                BigDecimal.valueOf(0.04),
                BigDecimal.valueOf(1000),
                null); // No market price

        portfolio.addBond(bond);

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.calculatePortfolioWeightedDuration(portfolio),
                "Should throw exception when bond lacks market price");
    }

    @Test
    void testCalculatePortfolioWeightedDuration_SingleBond() {
        // Given: A portfolio with a single bond
        Portfolio portfolio = new Portfolio("Single Bond Portfolio");

        Bond bond = createBondWithCouponDates("US1234567890",
                LocalDate.now().plusYears(5),
                createSemiAnnualCouponDates(5),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(950));

        portfolio.addBond(bond);

        // When
        BigDecimal portfolioDuration = riskMetricsService.calculatePortfolioWeightedDuration(portfolio);
        BigDecimal bondDuration = riskMetricsService.calculateModifiedDuration(bond);

        // Then: Portfolio duration should equal the single bond's duration
        assertEquals(bondDuration.doubleValue(), portfolioDuration.doubleValue(), 0.0001,
                "Single bond portfolio duration should equal bond duration");
    }

    // ========== JSON Deserialization Tests ==========

    @Test
    void testDeserializeBondsFromJson_Success() {
        // Given: Valid JSON string with bonds
        String json = """
                [
                  {
                    "isin": "US1234567890",
                    "maturityDate": "2030-12-31",
                    "couponDates": ["2025-06-30", "2025-12-31"],
                    "couponRate": 0.05,
                    "faceValue": 1000,
                    "marketPrice": 950
                  }
                ]
                """;

        // When
        List<Bond> bonds = riskMetricsService.deserializeBondsFromJson(json);

        // Then
        assertNotNull(bonds);
        assertEquals(1, bonds.size());
        Bond bond = bonds.get(0);
        assertEquals("US1234567890", bond.getIsin());
        assertEquals(LocalDate.of(2030, 12, 31), bond.getMaturityDate());
        assertEquals(0.05, bond.getCouponRate().doubleValue());
        assertEquals(1000, bond.getFaceValue().doubleValue());
        assertEquals(950, bond.getMarketPrice().doubleValue());
        assertEquals(2, bond.getCouponDates().size());
    }

    @Test
    void testDeserializeBondsFromJson_MultipleBonds() {
        // Given: JSON string with multiple bonds
        String json = """
                [
                  {
                    "isin": "US1234567890",
                    "maturityDate": "2030-12-31",
                    "couponDates": ["2025-06-30"],
                    "couponRate": 0.05,
                    "faceValue": 1000,
                    "marketPrice": 950
                  },
                  {
                    "isin": "US9876543210",
                    "maturityDate": "2035-06-30",
                    "couponDates": ["2025-12-31"],
                    "couponRate": 0.06,
                    "faceValue": 1000,
                    "marketPrice": 1050
                  }
                ]
                """;

        // When
        List<Bond> bonds = riskMetricsService.deserializeBondsFromJson(json);

        // Then
        assertNotNull(bonds);
        assertEquals(2, bonds.size());
    }

    @Test
    void testDeserializeBondsFromJson_EmptyArray() {
        // Given: Empty JSON array
        String json = "[]";

        // When
        List<Bond> bonds = riskMetricsService.deserializeBondsFromJson(json);

        // Then
        assertNotNull(bonds);
        assertEquals(0, bonds.size());
    }

    @Test
    void testDeserializeBondsFromJson_NullString() {
        // Given: Null JSON string
        String json = null;

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.deserializeBondsFromJson(json),
                "Should throw exception for null JSON string");
    }

    @Test
    void testDeserializeBondsFromJson_EmptyString() {
        // Given: Empty JSON string
        String json = "";

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.deserializeBondsFromJson(json),
                "Should throw exception for empty JSON string");
    }

    @Test
    void testDeserializeBondsFromJson_InvalidJson() {
        // Given: Invalid JSON format
        String json = "{ invalid json }";

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.deserializeBondsFromJson(json),
                "Should throw exception for invalid JSON format");
    }

    @Test
    void testDeserializeBondsFromJson_MalformedJson() {
        // Given: Malformed JSON (missing closing bracket)
        String json = """
                [
                  {
                    "isin": "US1234567890",
                    "maturityDate": "2030-12-31"
                """;

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> riskMetricsService.deserializeBondsFromJson(json),
                "Should throw exception for malformed JSON");
    }

    // ========== Helper Methods ==========

    private Bond createBond(String isin, LocalDate maturityDate, BigDecimal couponRate,
                            BigDecimal faceValue, BigDecimal marketPrice) {
        return new Bond(isin, maturityDate, new ArrayList<>(), couponRate, faceValue, marketPrice);
    }

    private Bond createBondWithCouponDates(String isin, LocalDate maturityDate,
                                           List<LocalDate> couponDates, BigDecimal couponRate,
                                           BigDecimal faceValue, BigDecimal marketPrice) {
        return new Bond(isin, maturityDate, couponDates, couponRate, faceValue, marketPrice);
    }

    private List<LocalDate> createSemiAnnualCouponDates(int years) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        for (int i = 1; i <= years * 2; i++) {
            dates.add(currentDate.plusMonths(6 * i));
        }

        return dates;
    }
}
