# BankFlow

Plataforma bancária em microserviços com Spring Boot. O cliente fala com o **API Gateway**; autenticação, contas e (em breve) transferências ficam em serviços separados.

## Stack

| Camada | Tecnologia |
|--------|------------|
| Runtime | Java 21, Spring Boot 4.1, Spring Cloud 2025.1 |
| Build | Maven (wrapper `mvnw` / `mvnw.cmd`) |
| Dados | PostgreSQL 16, Redis 7 |
| Mensageria | RabbitMQ 3.13 (infra pronta; uso de negócio ainda não) |
| Auth | JWT (HS256), senha com Argon2 |
| Docs | springdoc OpenAPI / Swagger UI |

## Arquitetura

```text
Cliente (Postman / front)
        │
        ▼
  api-gateway :8080
   ├─ JWT na borda → injeta X-User-Id / X-User-Role
   ├─ Rate limit (Redis)
   └─ Rotas:
        /auth/**      → auth-service      :8084
        /accounts/**  → account-service   :8081
        /transfers/** → transfer-service  :8082
```

| Serviço | Porta | Status |
|---------|-------|--------|
| `api-gateway` | 8080 | JWT, rate limit, roteamento, Swagger |
| `auth-service` | 8084 | Register, login, `/me`; cria conta no registro |
| `account-service` | 8081 | Criar conta, consultar conta e saldo |
| `transfer-service` | 8082 | Skeleton (só health) |

No **registro**, o `auth-service` chama o `account-service` via HTTP (`RestClient`). Se a criação da conta falhar, o usuário é removido e a API responde `503`.

## Pré-requisitos

- JDK 21+
- Docker + Docker Compose
- Maven Wrapper (já no repositório)

## Subir a infraestrutura

Na raiz do projeto:

```powershell
docker compose up -d
```

Sobe:

| Container | Portas | Credenciais padrão |
|-----------|--------|--------------------|
| Postgres | `5432` | user `bankflow` / pass `postgres` |
| Redis | `6379` | — |
| RabbitMQ | `5672`, management `15672` | `bankflow` / `bankflow` |

Bancos criados no init: `auth`, `accounts`, `transfers`.

## Variáveis de ambiente

O `auth-service` **exige** datasource via env (sem default no YAML):

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/auth"
$env:DB_USERNAME = "bankflow"
$env:DB_PASSWORD = "postgres"
```

Úteis (opcionais em local):

| Variável | Default típico | Onde |
|----------|----------------|------|
| `JWT_SECRET` | chave de dev no YAML | gateway + auth (**mesmo valor** nos dois) |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | gateway + auth |
| `ACCOUNT_SERVICE_URI` | `http://localhost:8081` | auth (client) e gateway (rota) |
| `AUTH_SERVICE_URI` | `http://localhost:8084` | gateway |
| `TRANSFER_SERVICE_URI` | `http://localhost:8082` | gateway |

O `account-service` já tem defaults de DB apontando para `accounts`.

## Rodar os serviços

Em terminais separados (PowerShell), na raiz ou em cada módulo:

```powershell
# Infra já deve estar up
cd C:\Projects\BankFlow

# account-service (o auth depende dele no register)
cd account-service
..\mvnw spring-boot:run

# auth-service (com DB_URL etc. definidos)
cd ..\auth-service
..\mvnw spring-boot:run

# api-gateway
cd ..\api-gateway
..\mvnw spring-boot:run
```

`transfer-service` é opcional por enquanto.

## API (via gateway)

Base: `http://localhost:8080`

### Públicas (sem token)

| Método | Path | Descrição |
|--------|------|-----------|
| `POST` | `/auth/register` | Cria usuário + conta padrão |
| `POST` | `/auth/login` | Retorna JWT |
| `GET` | `/actuator/health` | Health do gateway |

Swagger do gateway: `http://localhost:8080/swagger-ui.html`

### Protegidas (`Authorization: Bearer <token>`)

O gateway valida o JWT e **injeta** `X-User-Id` (não envie esse header pelo Postman no fluxo normal).

| Método | Path | Descrição |
|--------|------|-----------|
| `GET` | `/auth/me` | Usuário autenticado |
| `POST` | `/accounts` | Abrir conta |
| `GET` | `/accounts/{id}` | Detalhe da conta |
| `GET` | `/accounts/{id}/balance` | Saldo |

### Fluxo rápido no Postman

1. `POST http://localhost:8080/auth/register`  
   Body: `{ "email": "a@b.com", "password": "123456" }`
2. `POST http://localhost:8080/auth/login` (mesmo body) → copiar `token`
3. Demais requests com header:  
   `Authorization: Bearer <token>`

## Testes

```powershell
cd C:\Projects\BankFlow
.\mvnw test
```

Ou por módulo:

```powershell
.\mvnw -pl auth-service,account-service,api-gateway test
```

## Estrutura do repositório

```text
bankflow/
├── api-gateway/
├── auth-service/
├── account-service/
├── transfer-service/
├── docker/postgres/     # init dos databases
├── docker-compose.yml
├── pom.xml              # parent multi-módulo
└── mvnw / mvnw.cmd
```

## Status atual

- **Pronto:** gateway (JWT + rate limit), auth completo, account API + provisionamento no register  
- **Pendente:** domínio de transferências, uso de RabbitMQ, README operacional de deploy (Dockerfile raiz ainda é stub)

## Licença / notas

Projeto em desenvolvimento (`0.0.1-SNAPSHOT`). Segredos JWT e senhas do compose são para **ambiente local**; em produção use variáveis fortes e não comite credenciais.
