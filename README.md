# Bond Risk Metrics Calculator

A Spring Boot application for calculating risk metrics for plain vanilla fixed-coupon bonds and bond portfolios.

## Overview

This application provides REST API endpoints to calculate key bond risk metrics including Yield to Maturity (YTM), Macaulay Duration, Modified Duration, and Portfolio-level weighted average duration. It's designed for analyzing interest rate risk in fixed-income securities.

## Features

- **Bond Management**: Load and store multiple bonds via JSON
- **Yield to Maturity (YTM)**: Calculate approximate YTM using closed-form approximation
- **Duration Metrics**:
  - Macaulay Duration (weighted average time to receive cash flows)
  - Modified Duration (price sensitivity to yield changes)
- **Portfolio Analysis**: Calculate weighted average duration across bond portfolios
- **In-Memory Database**: H2 database for quick prototyping and testing
- **Request Validation**: Jakarta validation for input data integrity

## Technologies

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA** - Database access
- **H2 Database** - In-memory database
- **Jakarta Validation** - Request validation
- **Jackson** - JSON processing with LocalDate support
- **JUnit 5** - Unit testing
- **Maven** - Build and dependency management

## Project Structure

```
src/
├── main/java/com/bonds/
│   ├── BondsCalculatorApplication.java    # Main Spring Boot application
│   ├── controller/
│   │   └── BondController.java            # REST API endpoints
│   ├── domain/
│   │   ├── Bond.java                      # Bond entity
│   │   └── Portfolio.java                 # Portfolio entity
│   ├── repository/
│   │   ├── BondRepository.java            # Bond data access
│   │   └── PortfolioRepository.java       # Portfolio data access
│   └── service/
│       └── RiskMetricsService.java        # Risk calculation logic
└── test/java/com/bonds/
    └── service/
        └── RiskMetricsServiceTest.java    # Comprehensive unit tests
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd Bonds
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### H2 Console

Access the H2 database console at: `http://localhost:8080/h2-console`

- **JDBC URL**: `jdbc:h2:mem:bondsdb`
- **Username**: `sa`
- **Password**: (leave empty)

## API Endpoints

### 1. Load Bonds

Load multiple bonds from JSON into the database.

**Endpoint**: `POST /bonds/load`

**Request Body**:
```json
[
  {
    "isin": "US1234567890",
    "maturityDate": "2030-12-31",
    "couponDates": ["2025-06-30", "2025-12-31", "2026-06-30", "2026-12-31"],
    "couponRate": 0.05,
    "faceValue": 1000,
    "marketPrice": 950
  }
]
```

**Response**: Returns the saved bonds with generated IDs

**Example**:
```bash
curl -X POST http://localhost:8080/bonds/load \
  -H "Content-Type: application/json" \
  -d '[
    {
      "isin": "US1234567890",
      "maturityDate": "2030-12-31",
      "couponDates": ["2025-06-30", "2025-12-31", "2026-06-30"],
      "couponRate": 0.05,
      "faceValue": 1000,
      "marketPrice": 950
    }
  ]'
```

### 2. Calculate Yield to Maturity

Calculate YTM for a specific bond.

**Endpoint**: `GET /bonds/{id}/ytm`

**Response**: YTM as a decimal (e.g., `0.0568` = 5.68%)

**Example**:
```bash
curl http://localhost:8080/bonds/1/ytm
```

**Response**:
```json
0.0568
```

### 3. Calculate Duration

Calculate both Macaulay and Modified Duration for a bond.

**Endpoint**: `GET /bonds/{id}/duration`

**Response**: Map containing both duration values

**Example**:
```bash
curl http://localhost:8080/bonds/1/duration
```

**Response**:
```json
{
  "macaulayDuration": 4.8765,
  "modifiedDuration": 4.6432
}
```

### 4. Calculate Portfolio Duration

Calculate weighted average duration for a portfolio.

**Endpoint**: `GET /portfolio/{id}/duration`

**Response**: Portfolio duration as a double

**Example**:
```bash
curl http://localhost:8080/portfolio/1/duration
```

**Response**:
```json
5.0234
```

## Bond Concepts

### Yield to Maturity (YTM)

The internal rate of return on a bond if held to maturity. The application uses an approximation formula:

```
YTM ≈ [Annual Coupon + (Face Value - Market Price) / Years] / [(Face Value + Market Price) / 2]
```

**Key Points**:
- Bond at **discount** (price < face): YTM > coupon rate
- Bond at **par** (price = face): YTM ≈ coupon rate
- Bond at **premium** (price > face): YTM < coupon rate

### Macaulay Duration

Weighted average time to receive all cash flows (in years). Useful for understanding when you'll recover your investment.

**Formula**: `Duration = Σ[t × PV(CF_t)] / Price`

**Interpretation**: A duration of 5 years means you'll recover your investment, on average, in 5 years.

### Modified Duration

Measures the percentage price change for a 1% change in yield.

**Formula**: `Modified Duration = Macaulay Duration / (1 + YTM/frequency)`

**Interpretation**: If modified duration = 5:
- 1% yield increase → ~5% price decrease
- 1% yield decrease → ~5% price increase

### Portfolio Weighted Duration

Average duration across all bonds, weighted by market value.

**Formula**: `Portfolio Duration = Σ(Weight_i × Duration_i)`

where `Weight_i = Bond Market Value_i / Total Portfolio Value`

**Interpretation**: Overall interest rate sensitivity of the entire portfolio.

## Testing

The project includes comprehensive unit tests covering:

- Normal calculation scenarios
- Edge cases (null values, empty lists, expired bonds)
- Financial principles (discount/premium bonds, duration relationships)
- JSON deserialization (valid and invalid inputs)

Run tests:
```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=RiskMetricsServiceTest
```

**Test Coverage**: 25 unit tests across all service methods

## Data Model

### Bond Entity

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated primary key |
| isin | String | International Securities Identification Number (unique) |
| maturityDate | LocalDate | Date when bond matures |
| couponDates | List<LocalDate> | Payment dates for coupons |
| couponRate | BigDecimal | Annual coupon rate (e.g., 0.05 = 5%) |
| faceValue | BigDecimal | Par value of the bond |
| marketPrice | BigDecimal | Current market price |

### Portfolio Entity

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated primary key |
| name | String | Portfolio name |
| bonds | List<Bond> | Collection of bonds in portfolio |

## Configuration

Default configuration in `src/main/resources/application.properties`:

```properties
# H2 Database
spring.datasource.url=jdbc:h2:mem:bondsdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## Error Handling

The API returns appropriate HTTP status codes:

- **200 OK**: Successful calculation
- **400 Bad Request**: Invalid input (missing market price, invalid JSON, etc.)
- **404 Not Found**: Bond or portfolio not found
- **500 Internal Server Error**: Unexpected server error

Error responses include descriptive messages:
```json
"Error calculating YTM: Market price must be set to calculate YTM"
```

## Limitations & Assumptions

1. **YTM Calculation**: Uses approximation formula, not exact iterative solution
2. **Plain Vanilla Bonds**: Only supports fixed-rate bonds with regular coupon payments
3. **No Call/Put Features**: Does not handle callable or putable bonds
4. **Day Count Convention**: Uses simple 365-day year convention
5. **Payment Frequency**: Inferred from coupon dates, defaults to semi-annual

## Future Enhancements

- [ ] Convexity calculation for better price sensitivity estimates
- [ ] Exact YTM calculation using Newton-Raphson method
- [ ] Support for zero-coupon bonds
- [ ] Multiple day-count conventions (30/360, Actual/Actual, etc.)
- [ ] Bond pricing given YTM
- [ ] REST endpoints for portfolio CRUD operations
- [ ] Integration tests for controller layer
- [ ] Persistent database (PostgreSQL, MySQL)

## License

This project is for educational purposes.

## Contact

For questions or issues, please open an issue in the repository.
