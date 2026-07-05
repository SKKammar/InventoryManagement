# 📦 Full-Stack Inventory Management System

![Spring Boot](https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Render](https://img.shields.io/badge/Render-%46E3B7.svg?style=for-the-badge&logo=render&logoColor=white)

A secure, high-performance inventory tracking and management system. Built with a decoupled architecture featuring a Spring Boot REST API, a responsive React frontend, and a cloud-native PostgreSQL database via Supabase.

## 🚀 Live Demo

**Experience the application live:** [https://inventory-frontend-mdvc.onrender.com](https://inventory-frontend-mdvc.onrender.com)

> **Test Credentials:**
> * **Admin:** `admin` / `admin123`
> * **User:** `user` / `user123`

---

## ✨ Key Features

* **Secure Authentication:** Implementation of industry-standard JWT (JSON Web Tokens) with cross-origin `httpOnly`, `Secure`, and `SameSite=None` cookie management.
* **Role-Based Access Control (RBAC):** Distinct permissions for Admin and Standard User accounts.
* **Silent Token Refresh:** Automated background token regeneration via Axios interceptors for a seamless user experience.
* **Inventory Tracking:** Real-time visibility into products, stock levels, and active orders.
* **Cloud-Native Database:** Optimized for serverless and pooled database connections using Supavisor (SNI enabled).
* **Responsive UI:** Clean, modern interface built with Tailwind CSS and Vite.

---

## 🛠️ Technology Stack

### **Frontend**
* **Framework:** React.js
* **Build Tool:** Vite
* **Styling:** Tailwind CSS
* **Networking:** Axios
* **State Management:** React Context API
* **Hosting:** Render (Static Site)

### **Backend**
* **Core:** Java 17, Spring Boot 3.2.0
* **Security:** Spring Security, JWT
* **Data Access:** Spring Data JPA / Hibernate
* **Hosting:** Render (Web Service)

### **Database**
* **Engine:** PostgreSQL
* **Provider:** Supabase (with transaction pooling on port 6543)

---

## 🔒 Security Architecture Highlights

This application implements robust, production-grade security measures:
1. **Cookie-Based JWTs:** Tokens are never stored in `localStorage`. They are delivered via secure, HTTP-only cookies to prevent Cross-Site Scripting (XSS) attacks.
2. **Cross-Origin Resource Sharing (CORS):** Strictly configured to only accept API requests from verified frontend origins.
3. **Password Cryptography:** All user passwords are encrypted at rest using BCrypt hashing.
4. **Environment Isolation:** Database credentials and signing keys are injected exclusively via secure environment variables.

---

## 💻 Local Development Setup

To run this project locally, you will need Java 17+, Node.js, and Yarn installed on your machine.

### 1. Clone the Repository
```bash
git clone [https://github.com/SKKammar/InventoryManagement.git](https://github.com/SKKammar/InventoryManagement.git)

```

### 2. Configure Backend Environment

Navigate to the backend directory and create an `application.properties` or `.env` file with the following variables:

```properties
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://<your-db-host>:6543/postgres?sslmode=verify-full&prepareThreshold=0
SPRING_DATASOURCE_USERNAME=your_db_username
SPRING_DATASOURCE_PASSWORD=your_db_password

# JWT Security
APP_JWT_SECRET=YourSuperSecretKeyThatIsAtLeast64CharactersLong1234567890!
SERVER_PORT=8080

```

*Run the Spring Boot application using Maven or your preferred IDE.*

### 3. Configure Frontend Environment

Navigate to the frontend directory and create a `.env` file:

```env
VITE_API_BASE_URL=http://localhost:8080/api

```

### 4. Install and Run Frontend

```bash
# Install dependencies
yarn install

# Start the Vite development server
yarn dev

```

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://www.google.com/search?q=https://github.com/SKKammar/InventoryManagement/issues).

```

```
