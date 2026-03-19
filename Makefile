.PHONY: help setup setup-backend local-backend local-frontend local-up test-backend lint-frontend docker-build docker-up docker-down docker-logs clean

help:
	@echo "Available targets:"
	@echo "  make setup          - Install frontend dependencies"
	@echo "  make setup-backend  - Download backend dependencies"
	@echo "  make local-backend  - Start backend locally (port 3000)"
	@echo "  make local-frontend - Start frontend locally (port 3001)"
	@echo "  make local-up       - Print local start instructions"
	@echo "  make test-backend   - Run backend tests"
	@echo "  make lint-frontend  - Run frontend lint"
	@echo "  make docker-build   - Build Docker images"
	@echo "  make docker-up      - Start full project in Docker"
	@echo "  make docker-down    - Stop Docker project"
	@echo "  make docker-logs    - Tail Docker logs"
	@echo "  make clean          - Stop Docker and remove volumes"

setup:
	cd frontend && npm install

setup-backend:
	cd backend && lein deps

local-backend:
	cd backend && lein ring server-headless

local-frontend:
	cd frontend && npm run dev

local-up:
	@echo "1) cd backend && cp .env.example .env && edit JWT_SECRET"
	@echo "2) make local-backend"
	@echo "3) make local-frontend"

test-backend:
	cd backend && lein midje

lint-frontend:
	cd frontend && npm run lint

docker-build:
	docker compose build

docker-up:
	docker compose up --build

docker-down:
	docker compose down

docker-logs:
	docker compose logs -f

clean:
	docker compose down -v
