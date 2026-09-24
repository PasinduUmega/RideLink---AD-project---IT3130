# 👤 RideLink — Account & Authentication Service

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-MongoDB-green.svg)](https://www.mongodb.com/)
[![Security](https://img.shields.io/badge/Security-Spring%20Security%20%2B%20JWT-blue.svg)](https://jwt.io/)

The **Account Service** is the central authentication and identity management microservice of the **RideLink** ride-hailing platform. It handles user onboarding, credential verification, cryptographic password hashing, JWT token issuance, and user profile lifecycle management.

---

## 🚀 Features

- **User Registration & Validation:** Automatic input validation (`@Email`, `@NotBlank`, password minimum length constraints).
- **Password Security:** Cryptographic one-way hashing using `BCryptPasswordEncoder`.
- **JWT Issuance:** Generates HMAC-signed JSON Web Tokens carrying `userId`, `email`, and `role` claims.
- **Stateless Authorization:** Enforces stateless session management (`SessionCreationPolicy.STATELESS`) with a custom `JwtAuthFilter`.
- **User CRUD Operations:** Full management for user details, role assignment, and account deactivation.

---

## 🛠️ Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.2.5
- **Modules:** Spring Web, Spring Security, Spring Data MongoDB, Spring Boot Actuator, Spring Validation
- **JWT Library:** JJWT (`io.jsonwebtoken 0.12.5`)
- **Database:** MongoDB
- **Build Tool:** Apache Maven

---

## ⚙️ Configuration & Ports

- **Port:** `8081`
- **Database:** `ridelink_accounts` (MongoDB collection: `users`)
- **Token Validity:** 24 Hours (`86400000 ms`)

---

## 🏃 Getting Started

### Prerequisites
- **JDK 17** installed and configured
- **Maven 3.8+** (or use your IDE's embedded Maven)
- **MongoDB** instance or connection string

### Build and Run

1. **Clone and navigate to the service directory:**
   ```bash
   cd account-service
