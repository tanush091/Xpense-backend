# Xpense — Personal Finance REST API Backend

<p align="center">
  <strong>High-performance, secure Spring Boot REST API for personal finance, multi-wallet budgeting, savings goals, and real-time expense tracking.</strong>
</p>

---

## 🚀 Overview

**Xpense Backend** is a modular RESTful API built with **Spring Boot 3.3.5** and **Java 21**. It powers the Xpense financial dashboard with secure JWT authentication, multi-wallet account balance tracking, envelope budgeting, transaction ledgers, gamified savings goals, and analytics.

Designed for frictionless local development and scalable production:
- **Zero-config local development**: Runs out of the box using an embedded **H2 database** with web console at `/h2-console`.
- **Production & Cloud ready**: Seamlessly connects to hosted **PostgreSQL** or **Supabase** via Hikari connection pooling.

---

## 🛠️ Tech Stack & Architecture

- **Language & Framework**: Java 21, Spring Boot 3.3.5
- **Security**: Spring Security 6, JWT (io.jsonwebtoken 0.12.6), BCrypt hashing
- **Persistence**: Spring Data JPA, Hibernate ORM
- **Databases**:
  - H2 (In-memory, default profile)
  - PostgreSQL / Supabase (`postgres` profile)
- **JSON Serialization**: Jackson with `SNAKE_CASE` naming convention for seamless frontend interoperability
- **Connection Pool**: HikariCP

```
com.xpense/
├── config/             # Security, CORS, and sample data initializers
├── controller/         # REST Controllers (Auth, Wallets, Budgets, Goals, Transactions, etc.)
├── dto/                # Request & response payloads (DTOs)
├── exception/          # Global exception handler & custom exceptions
├── model/              # JPA domain entities (User, Wallet, Budget, Transaction, SavingsGoal)
├── repository/         # Spring Data JPA repositories
├── security/           # JWT authentication filter and token providers
└── service/            # Core business logic and transaction management
```

---

## 📑 API Endpoints Reference

### 1. Authentication (`/api/auth`)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user profile with initial balances |
| `POST` | `/api/auth/login` | Authenticate credentials and receive a signed JWT token |
| `GET` | `/api/auth/me` | Retrieve the authenticated user's profile |

### 2. Wallets & Accounts (`/api/wallets`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/wallets` | List all wallets for the current user |
| `POST` | `/api/wallets` | Create a new budget wallet |
| `GET` | `/api/wallets/{id}` | Get wallet details and balance |
| `POST` | `/api/wallets/{id}/topup` | Add funds to a wallet |
| `POST` | `/api/wallets/transfer` | Transfer funds between two user wallets |
| `DELETE`| `/api/wallets/{id}` | Remove a wallet |

### 3. Category Budgets (`/api/budgets`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/budgets` | Fetch monthly envelope budgets and spending status |
| `POST` | `/api/budgets` | Create or update a category budget limit |
| `DELETE`| `/api/budgets/{id}` | Delete a budget envelope |

### 4. Savings Goals (`/api/goals`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/goals` | List all savings targets and progress metrics |
| `POST` | `/api/goals` | Create a new savings goal |
| `POST` | `/api/goals/{id}/deposit` | Deposit funds towards a goal |
| `POST` | `/api/goals/{id}/withdraw`| Withdraw funds from a goal |
| `DELETE`| `/api/goals/{id}` | Delete a savings goal |

### 5. Transactions (`/api/transactions`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/transactions` | Search and filter transactions (category, wallet, date) |
| `POST` | `/api/transactions` | Log an expense, income, or transfer |
| `GET` | `/api/transactions/{id}` | Retrieve transaction details |
| `DELETE`| `/api/transactions/{id}` | Delete a transaction |

### 6. Analytics & Reports (`/api/analytics`, `/api/reports`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/analytics/summary` | Cashflow overview (total income, total expense, net savings) |
| `GET` | `/api/analytics/categories` | Spending breakdown grouped by category |
| `GET` | `/api/reports/monthly` | Generate monthly financial report summary |

---

## ⚙️ Getting Started

### Prerequisites
- **Java JDK 21** (or later)
- **Maven 3.8+** (or use your local maven installation)

### 1. Zero-Config Local Setup (H2 Database)
Simply clone and run:

```bash
git clone https://github.com/tanush091/Xpense-backend.git
cd Xpense-backend

mvn spring-boot:run
```

- API Server will start on: `http://localhost:8080`
- H2 Web Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:xpensedb`
  - Username: `sa`
  - Password: *(leave blank)*

---

### 2. Connecting to PostgreSQL / Supabase

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```
2. Open `.env` and fill in your database credentials:
   ```ini
   SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
   SPRING_DATASOURCE_USERNAME=postgres.<your-project-ref>
   SPRING_DATASOURCE_PASSWORD=<your-database-password>
   APP_JWT_SECRET=<your-32-character-secret-key>
   ```
3. Run the application with the `postgres` Spring profile active:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=postgres
   ```

---

## 🧪 Testing & Building

Run the automated test suite:
```bash
mvn test
```

Package the production executable JAR:
```bash
mvn clean package -DskipTests
```
The resulting JAR will be generated in `target/xpense-backend-1.0.0.jar`.

Run the production JAR:
```bash
java -jar target/xpense-backend-1.0.0.jar
```

---

## 🔒 Security Configuration

- **CORS**: Configured in `CorsConfig.java` to permit requests from `http://localhost:5173`, `http://localhost:3000`, and `http://127.0.0.1:5173`.
- **Stateless Sessions**: JWT bearer tokens are validated per-request via `JwtAuthenticationFilter`.
- **Password Protection**: Passwords hashed using Spring Security's BCrypt with salt rounds.

---

## 📄 License

This project is licensed under the MIT License.
