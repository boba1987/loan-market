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
    data_loader.clj  # CSV bank data
    interest.clj     # Interest calculations
  auth/
    core.clj         # JWT, login, role middleware
  routes/
    public.clj       # /, /api/login, /api/admin/users
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
   (d/q '[:find ?username ?role :where [?e :user/username ?username] [?e :user/role ?role]] db-value)
   ```
   You should see `#{["bank" "bank"] ["user" "user"]}`. If you still see `[]`, ensure `DATOMIC_STORAGE_DIR` is in `.env`, restart the app, hit it once, then start a **new** REPL.

   **Note:** Datomic Local allows only one process to connect to the same storage at a time. If the app is running, the REPL will get "File .../.lock is in use". Either **stop the app** and then use the REPL, or use the admin endpoint below while the app is running.

6. **While the app is running:** `GET /api/admin/users` returns the users in the app's DB as JSON (no second connection needed).

## Default seed users

When the database is empty, the app seeds two users:

| Role | Username | Password  |
|------|----------|-----------|
| Bank | `bank`   | `bankPass` |
| User | `user`   | `userPass` |

These are for local development only. Change or disable them in production.

## API

- **Public**
  - `GET /` – Hello world.
  - `POST /api/login` – Body `{"username":"...","password":"..."}`. Returns `{"token":"...","role":"user"|"bank"}`.
  - `GET /api/admin/users` – Returns `{"users":[{"username":"...","role":"..."}]}` from the DB (for inspecting data while the app runs).
- **Authenticated (User)** – Send `Authorization: Bearer <token>`.
  - `GET /api/user/me` – Current user info.
  - `POST /api/user/credit-applications` – Submit a credit application (authenticated). `dateOfBirth` must be `YYYY-MM-DD`.
  - `GET /api/user/credit-applications?page=1&pageSize=20` – List your credit applications with offset pagination.
- **Authenticated (Bank)** – Same header, role must be `bank`.
  - `GET /api/bank/me` – Current bank user info.
  - `GET /api/bank/credit-applications?page=1&pageSize=20` – List all credit applications with offset pagination.
