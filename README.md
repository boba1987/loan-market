# Loan Market

Full-stack loan marketplace demo with:
- **Backend**: Clojure + Ring + Datomic Local
- **Frontend**: Next.js + React + TypeScript + Tailwind

## Project Structure

- `backend` - API, authentication, role-based routes, Datomic schema/seed
- `frontend` - UI for login, profile, users admin, applications, and offers

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

## Running Locally

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

## Default Seed Users

- `admin@admin.com` / `adminPass`
- `jane@user.com` / `userPass`
- `otp@bank.com` / `bankPass`

## API and Backend Details

See `backend/README.md` for endpoint-level documentation and backend configuration details.
