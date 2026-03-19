Online loan marketplace that helps users compare different credit offers and find the most suitable loan from partnered financial institutions.

## Source layout

```
src/loan_market/
  core.clj           # Entry point (-main)
  handler.clj        # Ring app composition
  config.clj         # Port, JWT, Datomic config
  db/
    core.clj         # Datomic connection, schema
    seed.clj         # Seed users when DB is empty
  domain/
    user.clj         # User entity (auth, CRUD)
    bank.clj         # Bank/banking domain logic
    interest.clj     # Interest calculations
  auth/
    core.clj         # JWT, login, role middleware
  routes/
    public.clj       # /, /api/login
    admin.clj        # /api/admin/* (role: admin)
    user.clj         # /api/user/* (role: user)
    bank.clj         # /api/bank/* (role: bank)
```

## Running locally

1. Copy `.env.example` to `.env` and set `JWT_SECRET` (required).
2. (Optional) Set `DATOMIC_STORAGE_DIR` to an absolute path (e.g. `DATOMIC_STORAGE_DIR=/tmp/loan-market-datomic`) for persistent storage. If unset, the app uses in-memory storage (data is lost on restart).
3. Start the server:

```bash
# Option A: run the main entry point (recommended)
lein run

# Option B: run via lein-ring (uses project.clj :ring config)
lein ring server-headless
```

The server listens on `PORT` (default `3000`). Try: `curl http://localhost:3000/`.

### Querying the same DB from the REPL

The app and the REPL are separate processes. To query the **same** database the app uses:

1. **Add to `.env`** (uncommented): `DATOMIC_STORAGE_DIR=/tmp/loan-market-datomic` (or another absolute path). Create the dir if needed: `mkdir -p /tmp/loan-market-datomic`.
2. **Restart the app** so it picks up the env. Hit it once (e.g. `curl http://localhost:3000/`) so it creates and seeds the DB.
3. In **another terminal**, from the **project root**: `lein repl`.
4. In the REPL, check storage (if this is `nil`, you're on in-memory and won't see the app's data):
   ```clojure
   (require '[loan-market.config :as config])
   (config/storage-dir)   ; => "/tmp/loan-market-datomic" or nil
   ```
5. Then connect and query:
   ```clojure
   (require '[datomic.client.api :as d] '[loan-market.db.core :as db])
   (def conn (db/connect))
   (def db-value (d/db conn))
   (d/q '[:find ?email ?role :where [?e :user/email ?email] [?e :user/role ?role]] db-value)
   ```
   You should see `#{["otp@bank.com" "bank"] ["jane@user.com" "user"] ["admin@admin.com" "admin"]}`. If you still see `[]`, ensure `DATOMIC_STORAGE_DIR` is in `.env`, restart the app, hit it once, then start a **new** REPL.

   **Note:** Datomic Local allows only one process to connect to the same storage at a time. If the app is running, the REPL will get "File .../.lock is in use". Either **stop the app** and then use the REPL, or use the admin endpoint below while the app is running.

6. **While the app is running:** `GET /api/admin/users` returns the users in the app's DB as JSON (send an admin JWT; no second connection needed). You can optionally filter by role using `GET /api/admin/users?role=admin`.

## Default Seed Users

On startup, the app seeds any missing default users (bank, user, admin):

| Role | Email | Password |
|------|-------|----------|
| Bank | `otp@bank.com` | `bankPass` |
| User | `jane@user.com` | `userPass` |
| Admin | `admin@admin.com` | `adminPass` |

These are for local development only. Change or disable them in production.

## API

- **Public**
  - `GET /` – Hello world.
  - `POST /api/login` – Body `{"email":"...","password":"..."}`. Returns `{"token":"...","role":"user"|"bank"|"admin","name":"...","email":"..."}`.
- **Authenticated (User)** – Send `Authorization: Bearer <token>`.
  - `GET /api/user/me` – Current user info. Includes `role`, `name`, `email`, and user profile fields: `dateOfBirth`, `married`, `yearsWorking`, `industry`.
  - `POST /api/user/credit-applications` – Submit a credit application (authenticated). `dateOfBirth` must be `YYYY-MM-DD`.
  - `GET /api/user/credit-applications?page=1&pageSize=20` – List your credit applications with offset pagination.
- **Authenticated (Admin)** – Same header, role must be `admin`.
  - `GET /api/admin/users` – Returns `{"users":[{"id":123,"email":"...","name":"...","role":"...","dateOfBirth":"...","married":true,"yearsWorking":7,"industry":"Software"}]}`. Optional query param: `?role=admin` (or `bank` / `user`).
  - `POST /api/admin/users` – Body `{"email":"...","password":"...","role":"user"|"bank"|"admin","name":"..."}` (optionally also `dateOfBirth`,`married`,`yearsWorking`,`industry`). Creates a new user.
  - `PUT /api/admin/users/:id` – Body may include `{"email":"...","password":"...","role":"...","name":"..."}`
    (optionally also `dateOfBirth`,`married`,`yearsWorking`,`industry`). Updates the user.
  - `DELETE /api/admin/users/:id` – Deletes the user by Datomic user id (`:id`).
  - `GET /api/admin/credit-applications?page=1&pageSize=20` – List all credit applications with offset pagination. Each item includes:
    - `offers`: an array of offers across all banks, e.g. `{"bankName":"OTP Bank","bankEmail":"otp@bank.com","interestRate":4.25,"repaymentPeriod":60}` (empty if no offers yet).
  - `DELETE /api/admin/credit-applications/:id` – Deletes a credit application.
- **Authenticated (Bank)** – Same header, role must be `bank`.
  - `GET /api/bank/me` – Current bank user info.
  - `GET /api/bank/credit-applications?page=1&pageSize=20` – List all credit applications with offset pagination. Each item includes:
    - `interestRate` and `repaymentPeriod` only for offers submitted by the calling bank (omitted if the bank hasn't offered yet).
  - `POST /api/bank/credit-applications/:id/offer` – Body `{"interestRate":4.25,"repaymentPeriod":60}`. Submits/overwrites the calling bank's offer for the given credit application. Response: `{"id": <id>, "offered": true}`.
