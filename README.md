# OrderFlux — E-Commerce Backend API

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![JWT](https://img.shields.io/badge/Auth-JWT-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

Production-grade e-commerce REST API built with Spring Boot 3,
featuring JWT authentication, email OTP verification,
product management, and order processing.

---

## Features

- JWT Authentication with refresh tokens
- Email OTP verification (Gmail SMTP)
- Product catalog with pagination, sorting, and search
- Order management with stock control
- Role-based access control (Customer / Admin)
- Soft delete for products
- Global exception handling
- Swagger/OpenAPI documentation
- BCrypt password hashing

---

## Tech Stack

| Layer          | Technology              |
|----------------|-------------------------|
| Language       | Java 21                 |
| Framework      | Spring Boot 3.5         |
| Database       | MySQL 8.0               |
| ORM            | Spring Data JPA/Hibernate|
| Security       | Spring Security + JWT   |
| Email          | Spring Mail (Gmail SMTP)|
| Documentation  | SpringDoc OpenAPI 2.x   |
| Build Tool     | Maven                   |

---

## Getting Started

### Prerequisites

- Java 21+
- MySQL 8.0+
- Maven 3.8+
- Gmail account with App Password

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/yourusername/orderflux-backend.git
cd orderflux-backend
```

**2. Create database**
```sql
CREATE DATABASE orderflux_db;
```

**3. Configure credentials**
```bash
cp src/main/resources/application.properties.example \
   src/main/resources/application-local.properties
```

Edit `application-local.properties` with your values:
```properties
spring.datasource.password=your_mysql_password
spring.mail.username=your_gmail@gmail.com
spring.mail.password=your_app_password
jwt.secret=your_jwt_secret
```

**4. Run the application**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## API Documentation

After startup, visit: http://localhost:8080/api/swagger-ui/index.html#/
