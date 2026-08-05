# Portfolio Manager ER Diagram

```mermaid
erDiagram
    PORTFOLIO {
        INT id PK
        VARCHAR symbol
        VARCHAR company_name
        VARCHAR asset_type
        INT quantity
        DECIMAL buy_price
        DECIMAL current_price
        DATE purchase_date
    }

    PORTFOLIO_HISTORY {
        INT id PK
        INT portfolio_id FK
        VARCHAR symbol
        DATE recorded_date
        DECIMAL buy_price
        DECIMAL current_price
        INT quantity
        DECIMAL profit
    }

    ACCOUNT_BALANCE {
        INT id PK
        DECIMAL available_balance
    }

    PORTFOLIO ||--o{ PORTFOLIO_HISTORY : records
```

## Notes

- account_balance currently stores a single seeded row (id = 1) for app-level available balance.
- portfolio_history.portfolio_id references portfolio.id in application logic.
- The relationship shown is one portfolio holding to many history snapshots.
