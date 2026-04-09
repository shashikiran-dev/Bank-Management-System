# 💳 SecureBank – Full Stack Bank Management System

## 🚀 Overview

**SecureBank** is a full-stack banking application that simulates real-world banking operations including account management, secure authentication, transactions, loan processing, and administrative controls.

Built using **Spring Boot, MySQL, and modern JavaScript**, the system demonstrates scalable backend architecture, secure authentication mechanisms, and responsive UI design.

---

## 🌟 Key Highlights

* 🔐 Secure authentication with **BCrypt + OTP verification**
* 💰 Real-time banking operations (Deposit, Withdraw, Transfer)
* 🏦 Loan management system with EMI calculation
* 📊 Admin dashboard with full system control
* 📄 PDF bank statement generation
* ⚙️ Transaction limits & business rule enforcement

---

## 🧑‍💻 Features

### 🔐 Authentication & Security

* User Registration & Login
* Password hashing using BCrypt
* Email-based OTP verification
* Transaction PIN security
* Account lock/unlock functionality

---

### 💰 Banking Operations

* Deposit & Withdraw funds
* Transfer money between accounts
* View transaction history
* Undo last transaction

---

### 🏦 Loan Management

* Apply for loans
* EMI calculation (dynamic)
* Loan status tracking (Pending / Approved / Rejected)
* Admin loan approval system

---

### 📊 Admin Dashboard

* View all users and accounts
* Monitor transactions
* Lock/Unlock accounts
* Process loan requests
* View system-wide analytics

---

### 📄 Additional Features

* PDF statement generation (iText)
* Transaction limits & minimum balance enforcement
* Responsive UI with modern design
* Real-time notifications (OTP/logs)

---

## 🛠️ Tech Stack

### 🔹 Backend

* Java 21
* Spring Boot 3.x
* Spring Data JPA
* Hibernate
* Maven

### 🔹 Frontend

* HTML5
* CSS3
* JavaScript (Vanilla)

### 🔹 Database

* MySQL

### 🔹 Tools & Libraries

* Swagger UI
* iText (PDF generation)
* Spring Mail (OTP)
* Git & GitHub

---

## 🏗️ Architecture

```text
Frontend (HTML/CSS/JS)
        ↓
REST APIs (Spring Boot Controllers)
        ↓
Service Layer (Business Logic)
        ↓
Repository Layer (JPA)
        ↓
MySQL Database
```

---

## ⚙️ Setup Instructions

### 1️⃣ Clone Repository

```bash
git clone https://github.com/your-username/Bank-Management-System.git
cd Bank-Management-System
```

---

### 2️⃣ Configure Database

```sql
CREATE DATABASE bank_db;
```

Update `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bank_db
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

---

### 3️⃣ Run Backend

```bash
cd bank-api
mvn spring-boot:run
```

---

### 4️⃣ Run Frontend

Open the file:

```text
index.html
```

---

### 5️⃣ Access API Documentation

```text
http://localhost:8082/swagger-ui/index.html
```

---

## 📂 Project Structure

```text
bank-management-system/
│
├── bank-api/                # Spring Boot Backend
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── config/
│
├── core-engine/             # Business Logic Module
│
└── frontend/                # UI (HTML, CSS, JS)
```

---

## 🔐 Security Features

* BCrypt password hashing
* OTP-based login verification
* Transaction PIN protection
* Account freeze mechanism
* Server-side validation

---

## 📈 Future Enhancements

* JWT-based authentication
* Mobile application (Android)
* AI-based fraud detection
* Multi-account support
* Email/SMS notifications
* Dashboard analytics with charts

---

## 👨‍💻 Author

**Shashi Kiran**
🎓 Computer Science Student | Full Stack Developer

---

## ⭐ Support

If you found this project useful:

👉 Give it a ⭐ on GitHub
👉 Share with others

---

## 📌 Note

This project is developed for **learning and demonstration purposes**, simulating real-world banking workflows.
