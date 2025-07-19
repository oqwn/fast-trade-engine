# Fast Trade Engine - Full Stack Application

A high-performance electronic trading system with separate frontend and backend applications.

## 🏗️ Project Structure

```
fast-trade-engine/
├── frontend/               # React TypeScript frontend
│   ├── src/               # Source code
│   ├── public/            # Static assets
│   ├── package.json       # Node dependencies
│   └── Dockerfile         # Frontend container
├── backend/               # Java Spring Boot backend
│   ├── src/               # Source code
│   ├── pom.xml           # Maven dependencies
│   └── Dockerfile         # Backend container
├── docker-compose.yml     # Production orchestration
├── docker-compose.dev.yml # Development orchestration
├── Makefile              # Build automation
└── .github/              # CI/CD workflows
```

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Node.js 20+ and pnpm (for local frontend development)
- Java 17+ (for local backend development)
- Make (optional, for using Makefile commands)

### Using Make (Recommended)

```bash
# Install all dependencies
make install

# Start development environment
make dev

# Start production environment
make prod

# Run all tests
make test

# View logs
make logs
```

### Using Docker Compose

```bash
# Development environment with hot reload
docker-compose -f docker-compose.dev.yml up

# Production environment
docker-compose up -d

# Scale backend instances
docker-compose up -d --scale backend=3
```

### Manual Setup

#### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

#### Frontend

```bash
cd frontend
pnpm install
pnpm run dev
```

## 🌐 Service URLs

### Development

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **H2 Console**: http://localhost:8080/api/h2-console
- **Mailhog**: http://localhost:8025

### Production

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)

## 🏗️ Architecture Overview

### Frontend (React + TypeScript)

- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **API Client**: Axios with React Query
- **Real-time**: Socket.io Client
- **Charts**: Chart.js
- **Routing**: React Router v6

### Backend (Java + Spring Boot)

- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Database**: PostgreSQL (H2 for dev)
- **Cache**: Redis
- **Message Queue**: Disruptor
- **API**: RESTful + WebSocket
- **Monitoring**: Micrometer + Prometheus

## 📦 Key Features

### Trading Engine

- High-performance order matching
- Real-time market data streaming
- Multiple order types (Market, Limit)
- Price-time priority matching
- Circuit breakers and price limits

### Frontend Features

- Real-time order book visualization
- Interactive trading interface
- Portfolio management dashboard
- Live market data charts
- WebSocket notifications

### Infrastructure

- Containerized deployment
- Horizontal scaling support
- Health checks and monitoring
- Automated CI/CD pipeline
- Security scanning

## 🧪 Testing

### Run All Tests

```bash
make test
```

### Backend Tests

```bash
cd backend
mvn test                    # Unit tests
mvn verify                  # Integration tests
mvn jacoco:report          # Coverage report
```

### Frontend Tests

```bash
cd frontend
pnpm test                   # Unit tests
pnpm run test:coverage      # With coverage
pnpm run test:ui           # Interactive UI
```

## 📊 Monitoring & Observability

### Metrics

- Application metrics exposed at `/api/actuator/prometheus`
- Custom trading metrics (orders/sec, latency, etc.)
- JVM and system metrics

### Dashboards

1. Access Grafana at http://localhost:3001
2. Import provided dashboards from `backend/monitoring/grafana/dashboards/`
3. View real-time metrics and alerts

### Logs

```bash
# All services
make logs

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
```

## 🔧 Development

### Code Style

- Backend: Google Java Style Guide
- Frontend: ESLint + Prettier

### Hot Reload

Both frontend and backend support hot reload in development mode:

```bash
# Start with hot reload
make dev
```

### Debugging

#### Backend (Port 5005)

```bash
# Connect debugger to localhost:5005
docker-compose -f docker-compose.dev.yml up backend
```

#### Frontend

Use browser DevTools with React Developer Tools extension

## 🚢 Deployment

### Build Images

```bash
# Build all images
docker-compose build

# Build specific service
docker-compose build backend
docker-compose build frontend
```

### Production Deployment

```bash
# Deploy with Docker Compose
docker-compose up -d

# Deploy with Kubernetes
kubectl apply -f k8s/

# Deploy with Helm
helm install fast-trade-engine ./helm-chart
```

## 🔐 Security

- Non-root container execution
- Security scanning in CI/CD
- OWASP dependency checks
- Rate limiting on APIs
- Input validation
- CORS configuration

## 📚 Documentation

- **Architecture**: [architecture.md](architecture.md)
- **API Documentation**: http://localhost:8080/api/swagger-ui.html
- **Frontend Storybook**: Run `pnpm run storybook` in frontend/
- **CI/CD Guide**: [CI-CD-README.md](CI-CD-README.md)

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built with modern best practices
- Inspired by real-world trading systems
- Optimized for performance and scalability
