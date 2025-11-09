# AI-Enhanced Customer Feedback Platform

A modern Spring Boot application that processes customer feedback with automated sentiment analysis and AI-powered insights using Google's Gemini AI.

## 🎯 Project Overview

This application demonstrates a complete feedback processing pipeline that:
- Accepts customer feedback through REST API or web forms
- Performs automated sentiment analysis using Stanford CoreNLP
- Enhances feedback with AI-generated categories and actionable insights via Google Gemini
- Provides a dashboard for viewing and analyzing feedback trends
- Supports both synchronous and asynchronous processing

## ✨ Key Features

- **Automated Sentiment Analysis**: Stanford CoreNLP integration for accurate sentiment detection
- **AI-Powered Insights**: Google Gemini AI categorizes feedback and generates actionable recommendations
- **Async Processing**: Non-blocking batch processing for handling multiple feedback entries
- **REST API**: Comprehensive RESTful endpoints for all operations
- **Web Dashboard**: Interactive UI for viewing feedback and analytics
- **Monitoring**: Spring Boot Actuator with health checks and metrics

## 🛠️ Technology Stack

- **Backend**: Java 25, Spring Boot 3.2.3
- **Build Tool**: Gradle 8.11.1
- **AI/ML**: Google Gemini AI, Stanford CoreNLP
- **Architecture**: RESTful API, Async Processing, Repository Pattern
- **Code Quality**: Lombok, MapStruct

## 📋 Prerequisites

- Java 25 or higher
- Gradle 8.11.1 or higher
- Google Gemini API Key ([Get one here](https://makersuite.google.com/app/apikey))

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/feedback-service.git
cd feedback-service
```

### 2. Configure API Key

**Option A: Using Environment Variables (Recommended)**

```bash
export GEMINI_API_KEY=your-actual-api-key-here
```

**Option B: Using Configuration File**

```bash
# Copy the template
cp application-template.yml src/main/resources/application.yml

# Edit the file and set your API key
# gemini:
#   api-key: ${GEMINI_API_KEY}
```

### 3. Build and Run

```bash
# Build the project
./gradlew clean build

# Run the application
./gradlew bootRun
```

The application will start at `http://localhost:8080`

## 📡 API Endpoints

### Feedback Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Web dashboard |
| GET | `/api/v1/feedback` | Get all enhanced feedback |
| GET | `/api/v1/feedback/summary` | Get feedback statistics |
| POST | `/api/v1/feedback/raw/api` | Process single feedback |
| POST | `/api/v1/feedback/batch` | Process multiple feedbacks |

### Example: Submit Feedback

```bash
curl -X POST http://localhost:8080/api/v1/feedback/raw/api \
  -H "Content-Type: application/json" \
  -d '{
    "customer": "John Doe",
    "department": "Electronics",
    "comment": "Great product quality and fast delivery!"
  }'
```

**Response:**
```json
{
  "id": 1699564800000,
  "customer": "John Doe",
  "department": "Electronics",
  "comment": "Great product quality and fast delivery!",
  "sentiment": "VERY_POSITIVE",
  "category": "Product Quality",
  "actionableInsight": "Continue maintaining high quality standards.",
  "enhancedAt": "2025-11-09T15:30:00"
}
```

## 🏗️ Project Structure

```
feedback-service/
├── src/main/java/com/retailstore/feedback/
│   ├── controller/      # REST controllers
│   ├── service/         # Business logic
│   ├── repository/      # Data access layer
│   ├── model/           # Domain models and DTOs
│   ├── mapper/          # MapStruct mappers
│   ├── config/          # Configuration classes
│   └── exception/       # Exception handlers
├── src/main/resources/
│   ├── templates/       # Thymeleaf templates
│   └── application.yml  # Configuration (not in repo)
├── application-template.yml  # Configuration template
└── build.gradle.kts     # Build configuration
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `GEMINI_API_KEY` | Google Gemini API key | Required |
| `SPRING_PROFILES_ACTIVE` | Active profile (dev/prod) | `dev` |
| `SERVER_PORT` | Server port | `8080` |
| `FEEDBACK_FILE_PATH` | Feedback data file path | `file:sentiment_feedback_output.txt` |

### Application Profiles

- **dev**: Development mode with detailed logging
- **prod**: Production mode with optimized logging

## 📊 Monitoring

Access Spring Boot Actuator endpoints:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Application info
curl http://localhost:8080/actuator/info
```

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## 🔒 Security Notes

- Never commit `application.yml` with real API keys
- Use environment variables for sensitive configuration
- The `.gitignore` file is configured to exclude sensitive files
- `application-template.yml` is safe to commit (contains no secrets)

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Acknowledgments

- Google Gemini AI for AI-powered insights
- Stanford CoreNLP for sentiment analysis
- Spring Boot framework

## 📧 Contact

For questions or feedback, please open an issue on GitHub.

---

**Note**: This is a portfolio/demonstration project showcasing integration of AI services, sentiment analysis, and modern Spring Boot development practices.
