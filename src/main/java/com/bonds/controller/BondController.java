package com.bonds.controller;

import com.bonds.domain.Bond;
import com.bonds.domain.Portfolio;
import com.bonds.repository.BondRepository;
import com.bonds.repository.PortfolioRepository;
import com.bonds.service.RiskMetricsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/bonds")
public class BondController {

    private final RiskMetricsService riskMetricsService;
    private final BondRepository bondRepository;
    private final PortfolioRepository portfolioRepository;

    public BondController(RiskMetricsService riskMetricsService, BondRepository bondRepository,
                          PortfolioRepository portfolioRepository) {
        this.riskMetricsService = riskMetricsService;
        this.bondRepository = bondRepository;
        this.portfolioRepository = portfolioRepository;
    }

    /**
     * Loads multiple bonds from a JSON string.
     *
     * Endpoint: POST /bonds/load
     *
     * Expected request body: JSON string array of bond objects
     * Example:
     * [
     *   {
     *     "isin": "US1234567890",
     *     "maturityDate": "2030-12-31",
     *     "couponDates": ["2025-06-30", "2025-12-31", "2026-06-30"],
     *     "couponRate": 0.05,
     *     "faceValue": 1000,
     *     "marketPrice": 950
     *   }
     * ]
     *
     * @param jsonString The JSON string containing bond data
     * @return ResponseEntity containing the list of loaded bonds
     */
    @PostMapping("/load")
    public ResponseEntity<?> loadBonds(@RequestBody String jsonString) {
        try {
            List<Bond> bonds = riskMetricsService.deserializeBondsFromJson(jsonString);
            List<Bond> savedBonds = bondRepository.saveAll(bonds);
            return ResponseEntity.ok(savedBonds);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error loading bonds: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Calculates Yield to Maturity (YTM) for a specific bond.
     *
     * Endpoint: GET /bonds/{id}/ytm
     *
     * @param id The ID of the bond
     * @return ResponseEntity containing the YTM value as a decimal (e.g., 0.05 for 5%)
     */
    @GetMapping("/{id}/ytm")
    public ResponseEntity<?> calculateYieldToMaturity(@PathVariable Long id) {
        try {
            Optional<Bond> bondOptional = bondRepository.findById(id);

            if (bondOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Bond with ID " + id + " not found");
            }

            Bond bond = bondOptional.get();
            BigDecimal ytm = riskMetricsService.calculateYieldToMaturity(bond);

            return ResponseEntity.ok(ytm);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error calculating YTM: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Calculates both Macaulay Duration and Modified Duration for a specific bond.
     *
     * Endpoint: GET /bonds/{id}/duration
     *
     * @param id The ID of the bond
     * @return ResponseEntity containing a map with "macaulayDuration" and "modifiedDuration" keys
     */
    @GetMapping("/{id}/duration")
    public ResponseEntity<?> calculateDuration(@PathVariable Long id) {
        try {
            Optional<Bond> bondOptional = bondRepository.findById(id);

            if (bondOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Bond with ID " + id + " not found");
            }

            Bond bond = bondOptional.get();
            BigDecimal macaulayDuration = riskMetricsService.calculateMacaulayDuration(bond);
            BigDecimal modifiedDuration = riskMetricsService.calculateModifiedDuration(bond);

            Map<String, Double> durationResult = new HashMap<>();
            durationResult.put("macaulayDuration", macaulayDuration.doubleValue());
            durationResult.put("modifiedDuration", modifiedDuration.doubleValue());

            return ResponseEntity.ok(durationResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error calculating duration: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Calculates the portfolio-level weighted average duration.
     *
     * Endpoint: GET /portfolio/{id}/duration
     *
     * The weighted average duration indicates the approximate percentage change
     * in the portfolio's value for a 1% change in interest rates.
     *
     * @param id The ID of the portfolio
     * @return ResponseEntity containing the weighted average duration as a Double
     */
    @GetMapping("/portfolio/{id}/duration")
    public ResponseEntity<?> calculatePortfolioDuration(@PathVariable Long id) {
        try {
            Optional<Portfolio> portfolioOptional = portfolioRepository.findById(id);

            if (portfolioOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Portfolio with ID " + id + " not found");
            }

            Portfolio portfolio = portfolioOptional.get();
            BigDecimal portfolioDuration = riskMetricsService.calculatePortfolioWeightedDuration(portfolio);

            return ResponseEntity.ok(portfolioDuration.doubleValue());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error calculating portfolio duration: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }
}
