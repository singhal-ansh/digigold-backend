# FinGold - Backend

Spring Boot REST API with JWT authentication, PostgreSQL, and Razorpay payment integration.

## Quick Start

### Prerequisites
- Java 17+
- PostgreSQL running locally (port 5432)
- Create a database: `CREATE DATABASE fingold_db;`

### Run
```bash
# Build and run
./mvnw spring-boot:run

# Or build jar first
./mvnw package -DskipTests
java -jar target/*.jar
```

API will be available at `http://localhost:8080/api/v1`  
Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`

## Configuration (`src/main/resources/application.properties`)

| Key | Default | Notes |
|-----|---------|-------|
| `spring.datasource.password` | `1111` | Change to your Postgres password |
| `app.jwt.secret` | placeholder | Use `openssl rand -base64 64` to generate |
| `razorpay.key-id` | placeholder | Replace with your Razorpay test key |
| `razorpay.key-secret` | placeholder | Replace with your Razorpay test secret |

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | No | Register new user |
| POST | `/auth/login` | No | Login |
| POST | `/auth/refresh` | No | Refresh JWT |
| POST | `/auth/logout` | Yes | Logout |
| GET | `/gold-prices/live` | No | Live gold price |
| GET | `/wallet` | Yes | User wallet |
| POST | `/transactions/buy` | Yes | Initiate buy |
| POST | `/transactions/buy/verify-payment` | Yes | Verify Razorpay payment |
| POST | `/transactions/sell` | Yes | Sell gold |
| GET | `/transactions` | Yes | Transaction history |
| GET | `/users/me` | Yes | User profile |
| GET | `/admin/dashboard` | Admin | Admin stats |

## Default Admin Account
Created automatically by DataSeeder on first run:
- Email: `admin@fingold.com`
- Password: `Admin@1234`

Set a new gold price via `POST /gold-prices` (admin) before users can buy/sell.
