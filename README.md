# 📦 GestãoNexus

**Sistema de gestão de estoque multi-tenant com vitrine pública (landing page),
construído em Spring Boot.** Cada empresa cadastrada tem seu próprio catálogo de
produtos, controle de entradas/saídas e histórico de movimentações — isolados
por login, protegidos por autenticação JWT e senhas com hash BCrypt.

<p align="left">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.2.5"/>
  <img src="https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security"/>
  <img src="https://img.shields.io/badge/H2%20%2F%20MySQL-database-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="H2 / MySQL"/>
  <img src="https://img.shields.io/badge/PWA-ready-5A0FC8?style=for-the-badge&logo=pwa&logoColor=white" alt="PWA"/>
  <img src="https://img.shields.io/badge/license-MIT-blue?style=for-the-badge" alt="MIT license"/>
</p>

---

## 🖼️ Prints

| Dashboard | Vitrine pública | Login |
|:---:|:---:|:---:|
| ![Dashboard](docs/screenshots/dashboard.jpg) | ![Vitrine pública](docs/screenshots/loja.jpg) | ![Login](docs/screenshots/login.jpg) |

---

## ✨ Funcionalidades

- 🏢 **Multi-tenant** — cada empresa vê e gerencia apenas os próprios dados
- 🔐 **Autenticação JWT** com senhas armazenadas em hash BCrypt (nunca em texto puro)
- 📦 **Catálogo de produtos** com custo, preço de venda, variações de tamanho/cor e fotos
- 🔄 **Movimentações de estoque** — entradas, vendas e devoluções com histórico completo
- 💰 **Painel financeiro** — valor total em estoque e receita por período
- 🛍️ **Vitrine pública (`/loja.html`)** — landing page para os clientes finais da loja, com link direto pro WhatsApp
- 👑 **Painel administrativo** — cadastro e gestão das empresas (super-admin)
- 📱 **PWA instalável** — funciona offline e pode ser adicionado à tela inicial do celular
- 🛡️ **Segurança em produção** — sem stack trace exposto, cookies `httpOnly`/`secure`, segredos 100% via variável de ambiente

---

## 🏗️ Arquitetura

```
┌─────────────────────────────┐        ┌──────────────────────────────┐
│   Front-end (HTML/CSS/JS)   │  HTTP  │        Spring Boot API        │
│  index / login / admin /    │───────▶│  Controller → Service →       │
│  loja.html  (PWA)           │◀───────│  Repository (Spring Data JPA) │
└─────────────────────────────┘  JSON  └──────────────┬───────────────┘
                                                        │
                                          JwtFilter → SecurityConfig
                                                        │
                                                ┌───────▼────────┐
                                                │  H2 / MySQL DB  │
                                                └─────────────────┘
```

- **Controller** — expõe a API REST (`/api/**`)
- **Service** — regras de negócio (estoque, multi-tenant, cálculo de receita)
- **Repository** — Spring Data JPA
- **Security** — `JwtFilter` valida o token em cada requisição; `SecurityConfig` define as rotas públicas x protegidas
- **Front-end** — servido estaticamente pelo próprio Spring Boot (sem build separado)

---

## 🔒 Segurança — o que este projeto já resolve

| Prática | Como é aplicada aqui |
|---|---|
| Sem segredo em texto puro no código | `application.properties` só contém `${VARIAVEL_DE_AMBIENTE}` — sem valores padrão para senha/chave JWT |
| Falha rápida em vez de rodar inseguro | Se `APP_JWT_SECRET` ou `APP_ADMIN_SENHA` não forem definidos, a aplicação **não sobe** |
| Senhas com hash | BCrypt (custo 12), nunca texto puro, nunca logado |
| Tokens assinados | JWT (HS256) com expiração configurável |
| Isolamento multi-tenant | Toda consulta é filtrada pela empresa autenticada |
| Erros genéricos | Stack trace e mensagens internas nunca vazam na resposta HTTP |
| Segredos fora do Git | `.gitignore` bloqueia `.env`, `application-prod.properties`, banco de dados local e uploads |

---

## 🚀 Como rodar localmente

### Pré-requisitos
- Java 21+
- Maven 3.8+ (ou use o `mvn` já instalado)

### 1. Clone o repositório
```bash
git clone https://github.com/SEU-USUARIO/SEU-REPOSITORIO.git
cd SEU-REPOSITORIO
```

### 2. Configure as variáveis de ambiente
Veja todos os nomes e exemplos em [`application.properties.example`](./application.properties.example).
No mínimo, defina:

```bash
# Linux / macOS
export APP_JWT_SECRET=$(openssl rand -base64 32)
export APP_ADMIN_SENHA="uma-senha-forte-aqui"
```

```powershell
# Windows PowerShell
$env:APP_JWT_SECRET = [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
$env:APP_ADMIN_SENHA = "uma-senha-forte-aqui"
```

### 3. Rode o projeto
```bash
mvn spring-boot:run
```

### 4. Acesse
```
http://localhost:8080          → login
http://localhost:8080/loja.html → vitrine pública
```

---

## ☁️ Indo para produção (banco real)

Por padrão o projeto usa **H2** (arquivo local, ótimo para começar). Para produção,
aponte para um banco real (ex.: MySQL) só com variáveis de ambiente — nenhuma
alteração de código necessária:

```bash
export DB_URL="jdbc:mysql://SEU-HOST:3306/gestaonexus"
export DB_USERNAME="usuario_do_banco"
export DB_PASSWORD="senha-do-banco"
export DB_DRIVER="com.mysql.cj.jdbc.Driver"
```

Adicione a dependência do driver no `pom.xml` (veja comentário em
[`application.properties.example`](./application.properties.example)) e pronto.

---

## 📚 Endpoints principais da API

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/auth/login` | Autentica e devolve o JWT |
| `GET` | `/api/produtos` | Lista os produtos da empresa logada |
| `GET` | `/api/produtos?busca=termo` | Busca por nome |
| `GET` | `/api/produtos/valor-total` | Valor total em estoque |
| `POST` | `/api/produtos` | Cadastra produto |
| `PUT` | `/api/produtos/{id}` | Edita produto |
| `PATCH` | `/api/produtos/{id}/qtd` | Ajusta quantidade |
| `POST` | `/api/produtos/{id}/imagens` | Upload de foto do produto |
| `GET` | `/api/movimentacoes` | Histórico de movimentações |
| `POST` | `/api/movimentacoes` | Registra venda / entrada / devolução |
| `GET` | `/api/movimentacoes/receita` | Receita por período |
| `GET` | `/api/loja/produtos` | Catálogo público (vitrine) |
| `GET` | `/api/admin/empresas` | *(admin)* Lista empresas cadastradas |

---

## 🗂️ Estrutura do projeto

```
GestãoNexus/
├── pom.xml
├── README.md
├── application.properties.example
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/estoque/
    │   │   ├── controller/     → API REST
    │   │   ├── service/        → Regras de negócio
    │   │   ├── repository/     → Spring Data JPA
    │   │   ├── model/          → Entidades JPA
    │   │   ├── security/       → JWT + Spring Security
    │   │   └── exception/      → Tratamento global de erros
    │   └── resources/
    │       ├── application.properties
    │       └── static/         → Front-end (index, login, admin, loja, PWA)
    └── test/
```

---

## 🛠️ Stack

Java 21 · Spring Boot 3.2 · Spring Security · Spring Data JPA · H2 / MySQL ·
JJWT · BCrypt · HTML/CSS/JS vanilla · PWA (manifest + service worker)

---

## 📄 Licença

Distribuído sob a licença MIT. Veja [`LICENSE`](./LICENSE) para mais detalhes.

---

<p align="center">Feito com 💚 para a <strong>PAFANNY</strong></p>
