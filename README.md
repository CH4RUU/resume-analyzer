# Resume Analyzer

A full-stack resume scoring tool built with Java 17, Spring Boot 4, Spring Data JPA and MySQL.
Upload a resume (PDF, DOCX or TXT), optionally paste a job description, and get an instant score
breakdown, matched/missing skills, and improvement suggestions. A minimal HTML/CSS/JS frontend is
served directly by Spring Boot — no separate frontend build step required.

## Stack

- Java 17
- Spring Boot 4 (Web, Data JPA, Validation)
- MySQL (via `mysql-connector-j`)
- Apache PDFBox / Apache POI for resume text extraction
- Plain HTML/CSS/JS frontend (`src/main/resources/static`)
- H2 in-memory database for tests

## How scoring works

`ResumeAnalyzer` extracts text from the uploaded file and computes five sub-scores:

- **Keywords** — overlap between skills found in the resume and skills found in the job description
  (or breadth of recognized skills if no job description is given)
- **Sections** — presence of standard resume sections (Experience, Education, Skills, ...)
- **Contact Info** — email, phone number, LinkedIn/GitHub presence
- **Impact** — use of strong action verbs and quantified achievements (numbers/percentages)
- **Format** — resume length and bullet-point usage

These combine into a weighted `overallScore` (0-100), plus a list of matched skills, missing
skills, and human-readable suggestions. The skill vocabulary lives in `SkillCatalog`.

## Optional AI feedback (Claude API)

On top of the free rule-based scoring, `POST /api/analyses/{id}/ai-feedback` can generate a
narrative review (strongest points, weaknesses, concrete edits) using the Claude API. It's
opt-in and costs money per call, so it's disabled by default.

To enable it, set an environment variable before starting the app:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
./mvnw spring-boot:run
```

Without the key set, the endpoint returns `503 Service Unavailable` with a clear message instead
of failing — the rest of the app (scoring, history, everything else) works exactly the same
either way. The model used is configurable via `anthropic.model` in `application.properties`
(defaults to `claude-sonnet-5`).

## Prerequisites

- JDK 17
- MySQL server running locally (or update `application.properties` to point elsewhere)

## Configure the database

Edit `src/main/resources/application.properties` if your MySQL credentials differ:

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/resume_analyzer?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=password
```

### This machine's local MySQL

This machine already had a MySQL instance running system-wide on port 3306 (installed via the
official installer, managed by `/Library/LaunchDaemons/com.oracle.oss.mysql.mysqld.plist`) whose
root password wasn't known. Rather than touch that instance, a second, dedicated MySQL 9.0.1
server was installed at `/Users/charu/mysql/server` and configured to run on **port 3307**
instead, with `root` password `password`. That's why the datasource URL above points at 3307, not
the default 3306.

Start it:

```bash
/Users/charu/mysql/server/bin/mysqld \
  --datadir=/Users/charu/mysql/server/data \
  --basedir=/Users/charu/mysql/server \
  --socket=/Users/charu/mysql/server/mysql.sock \
  --pid-file=/Users/charu/mysql/server/mysqld.pid \
  --port=3307 \
  --mysqlx-port=33061 &
```

Stop it:

```bash
/Users/charu/mysql/server/bin/mysqladmin -u root -ppassword --socket=/Users/charu/mysql/server/mysql.sock shutdown
```

The database `resume_analyzer` and its tables are created automatically on startup
(`spring.jpa.hibernate.ddl-auto=update`).

## Run

```bash
./mvnw spring-boot:run
```

Then open **http://localhost:8080** in your browser.

## Run tests

Tests use an in-memory H2 database, so no MySQL connection is needed:

```bash
./mvnw test
```

## API

| Method | Path                  | Description                                      |
|--------|-----------------------|---------------------------------------------------|
| POST   | `/api/analyses`       | Multipart upload: `resume` file + `jobDescription` (optional) |
| GET    | `/api/analyses`       | List past analyses (most recent first)            |
| GET    | `/api/analyses/{id}`  | Get one analysis in full detail                    |
| DELETE | `/api/analyses/{id}`  | Delete an analysis                                  |
| POST   | `/api/analyses/{id}/ai-feedback` | Generate AI feedback via Claude (needs `ANTHROPIC_API_KEY`); form param `jobDescription` (optional) |

## Deploying for free

The app is containerized (`Dockerfile`) and reads all datasource/API-key config from environment
variables (see `application.properties`), so it can run on any Docker host without code changes.
This guide uses two free services: **Render** (runs the container) and **Clever Cloud** (hosts the
MySQL database) — both have a genuinely free tier that doesn't expire or require a credit card.

### 1. Push this repo to GitHub

Render deploys straight from a GitHub repo.

### 2. Create a free MySQL database on Clever Cloud

1. Sign up at [clever-cloud.com](https://www.clever-cloud.com/) (free, no card required).
2. Create a new application → **Add-on** → **MySQL** → pick the free **DEV** plan.
3. Once provisioned, open the add-on's "Information" tab and copy the host, port, database name,
   user, and password.

### 3. Create a free Web Service on Render

1. Sign up at [render.com](https://render.com/) and connect your GitHub account.
2. **New +** → **Blueprint**, point it at this repo — it will pick up `render.yaml` automatically
   and create a free Docker web service.
   (Alternatively: **New +** → **Web Service** → select the repo → runtime **Docker**.)
3. In the service's **Environment** tab, set:
   - `SPRING_DATASOURCE_URL` = `jdbc:mysql://<clever-cloud-host>:<port>/<db-name>?useSSL=false&allowPublicKeyRetrieval=true`
   - `SPRING_DATASOURCE_USERNAME` = the Clever Cloud MySQL user
   - `SPRING_DATASOURCE_PASSWORD` = the Clever Cloud MySQL password
   - `ANTHROPIC_API_KEY` = (optional) your Anthropic key, only if you want the AI feedback endpoint live
4. Deploy. Render builds the `Dockerfile` and starts the container; `PORT` is injected
   automatically and the app already binds to it (`server.port=${PORT:8080}`).

### Free-tier caveats

- Render's free web service **spins down after ~15 minutes of inactivity** — the first request
  after idling takes 30-60s to wake back up. Fine for a portfolio/demo project, not for something
  that needs to always be instantly responsive.
- Clever Cloud's free MySQL "DEV" plan has limited storage (a few hundred MB) — plenty for resume
  text and scores, not meant for heavy production traffic.
- If either provider changes its free-tier terms, the same `Dockerfile` and env-var-driven config
  work unmodified on Railway, Fly.io, or any other container host — only the environment variables
  need to be set on the new platform.
