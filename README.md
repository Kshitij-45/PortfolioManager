# Portfolio Manager - End-to-End Setup

This application now runs as a single end-to-end system:

1. Frontend UI in [frontend/index.html](frontend/index.html)
2. Spring Boot backend APIs
3. Database persistence (H2 by default, MySQL via profile)

The frontend is loaded by Spring Boot using:

1. classpath static resources
2. project-root [frontend](frontend/) folder via `spring.web.resources.static-locations`

## 1. Prerequisites

1. Java 21
2. MySQL 8+ (only needed when using MySQL profile)
3. Windows: use `.\\mvnw.cmd`

## 2. Database Profiles

By default the app runs with `h2` profile, so backend starts even when MySQL credentials are unavailable.

1. Default profile: `h2` (file DB at `./data/portfolio-db`)
2. MySQL profile: `mysql` (uses environment variables)

## 3. Configure MySQL Connection (Optional)

When using `mysql` profile, the app reads DB configuration from environment variables.

1. `DB_URL` (default: `jdbc:mysql://localhost:3306/portfolio_db?createDatabaseIfNotExist=true`)
2. `DB_USERNAME` (default: `root`)
3. `DB_PASSWORD` (default: empty)

For your colleague's laptop, point these to her MySQL instance and run with mysql profile.

Example (CMD):

```bat
set DB_URL=jdbc:mysql://localhost:3306/portfolio_db?createDatabaseIfNotExist=true
set DB_USERNAME=root
set DB_PASSWORD=your_password_here
```

## 4. Database Tables

No manual SQL is required now. The app auto-creates:

1. `account_balance`
2. `portfolio`

on startup if they do not already exist.

## 5. Run The App

From project root:

Default (H2):

```bat
.\mvnw.cmd spring-boot:run
```

MySQL profile:

```bat
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=mysql
```

Then open:

1. `http://localhost:8080/`

## 6. API Endpoints Used By Frontend

1. `GET /api/portfolios`
2. `POST /api/portfolios`
3. `PUT /api/portfolios/{id}`
4. `DELETE /api/portfolios/{id}`
5. `GET /api/balance`
6. `POST /api/balance/add`
7. Price refresh endpoints:
	1. `GET /api/stocks/{symbol}/price`
	2. `GET /api/bonds/{symbol}/price`
	3. `GET /api/crypto/{symbol}/price`
	4. `GET /api/funds/{symbol}/nav`

## 7. What Is Integrated

1. Add money updates backend and database
2. Add holding creates backend portfolio record
3. Remove holding updates or deletes backend record
4. Stock/ETF purchase and sell flows update account balance in DB
5. Refresh prices pulls backend market data and persists updated prices
6. UI renders live data from backend (not local storage)