# Loan Market

Full-stack loan marketplace demo with:
- **Backend**: Clojure + Ring + Datomic Local
- **Frontend**: Next.js + React + TypeScript + Tailwind

## Project Structure

- `backend` - API, authentication, role-based routes, Datomic schema/seed
- `frontend` - UI for login, profile, users admin, applications, and offers

## Requirements

For local (non-Docker) development:
- **Java 17+** (required by Clojure/Leiningen backend)
- **Leiningen** (`lein`) installed
- **Node.js 20+** and **npm** (for Next.js frontend)

For Docker-based run:
- **Docker Desktop** (or Docker Engine + Compose plugin)

Configuration requirements:
- `backend/.env` file with `JWT_SECRET` set
  - You can copy from `backend/.env.example`


## Running Locally

You can run the project with either:
- `Makefile` helpers (recommended)
- direct commands

### 1) Backend

```bash
cd backend
cp .env.example .env
# set JWT_SECRET in .env
lein ring server-headless
```

Backend runs on `http://localhost:3000` by default.

### 2) Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3001`.

## Running with Docker

### Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin)

### Start project in Docker

From project root:

```bash
make docker-up
```

Services:
- Frontend: `http://localhost:3001`
- Backend: `http://localhost:3000`

### Stop Docker project

```bash
make docker-down
```

To also remove persisted Datomic volume:

```bash
make clean
```

## Default Seed Users

- Admin - email: `admin@admin.com` / password: `adminPass`
- User - email: `jane@user.com` / password: `userPass`
- Bank - email: `otp@bank.com` / password: `bankPass`


## Roles

- `user`
  - submit credit applications
  - view own applications and received offers
  - update own profile
- `bank`
  - view applications
  - submit/update one offer per application
  - update own profile (name)
- `admin`
  - manage users (create/edit/delete)
  - view all applications and offers
  - delete applications
  - update own profile (name)

## Profile Model

Primary profile fields:
- `name`
- `email`
- `dateOfBirth`
- `maritalStatus` (`not married`, `married`, `divorced`, `other`)
- `yearsWorking`
- `industry`

Notes:
- For `admin` and `bank` profile screen, optional fields (`dateOfBirth`, `maritalStatus`, `yearsWorking`, `industry`) are hidden in UI.
- When profile `name` is updated, header display updates immediately.
## API and Backend Details

See `backend/README.md` for endpoint-level documentation and backend configuration details.

### Makefile Commands

From project root:

```bash
make help
make setup
make local-backend
make local-frontend
```

Other useful commands:

```bash
make test-backend
make lint-frontend
```
