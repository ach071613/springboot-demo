package com.bonds.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bonds")
public class Bond {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "ISIN is required")
    @Column(unique = true, nullable = false)
    private String isin;

    @NotNull(message = "Maturity date is required")
    @Column(nullable = false)
    private LocalDate maturityDate;

    @ElementCollection
    @CollectionTable(name = "coupon_dates", joinColumns = @JoinColumn(name = "bond_id"))
    @Column(name = "coupon_date")
    private List<LocalDate> couponDates = new ArrayList<>();

    @NotNull(message = "Coupon rate is required")
    @Positive(message = "Coupon rate must be positive")
    @Column(nullable = false)
    private BigDecimal couponRate;

    @NotNull(message = "Face value is required")
    @Positive(message = "Face value must be positive")
    @Column(nullable = false)
    private BigDecimal faceValue;

    @Positive(message = "Market price must be positive")
    private BigDecimal marketPrice;

    public Bond() {
    }

    public Bond(String isin, LocalDate maturityDate, List<LocalDate> couponDates,
                BigDecimal couponRate, BigDecimal faceValue, BigDecimal marketPrice) {
        this.isin = isin;
        this.maturityDate = maturityDate;
        this.couponDates = couponDates != null ? couponDates : new ArrayList<>();
        this.couponRate = couponRate;
        this.faceValue = faceValue;
        this.marketPrice = marketPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public List<LocalDate> getCouponDates() {
        return couponDates;
    }

    public void setCouponDates(List<LocalDate> couponDates) {
        this.couponDates = couponDates;
    }

    public BigDecimal getCouponRate() {
        return couponRate;
    }

    public void setCouponRate(BigDecimal couponRate) {
        this.couponRate = couponRate;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public void setFaceValue(BigDecimal faceValue) {
        this.faceValue = faceValue;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(BigDecimal marketPrice) {
        this.marketPrice = marketPrice;
    }
}
