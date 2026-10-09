-- ==============================================================================
-- XPENSE — DATABASE SCHEMA FOR SUPABASE (PostgreSQL)
--
-- Supabase is used as hosted PostgreSQL only (ADR-003). The Spring Boot API is the
-- only client of these tables and the only authority for sign-in (JWT + BCrypt), so
-- there are no links to Supabase Auth (auth.users) here.
--
-- Column names and types mirror the JPA entities in backend/src/main/java/com/xpense/model.
-- IDs are text because the API generates prefixed ids such as "user-…" and "wallet-…".
--
-- How to use:
--   1. Supabase dashboard → SQL Editor → paste this file → Run.
--   2. Put the connection details in backend/.env (see backend/.env.example).
--   3. Start the API with run-backend.bat (it switches to the "postgres" profile).
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- OPTIONAL RESET: only if you previously ran the old Supabase-Auth version of this
-- file (UUID ids referencing auth.users). This deletes all Xpense data.
-- ------------------------------------------------------------------------------
-- DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
-- DROP FUNCTION IF EXISTS public.handle_new_user();
-- DROP TABLE IF EXISTS public.transactions, public.budgets, public.savings_goals,
--                      public.wallets, public.profiles CASCADE;

-- 1. PROFILES — one row per account, holds the total balance
CREATE TABLE IF NOT EXISTS public.profiles (
    id              VARCHAR(64)   PRIMARY KEY,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    full_name       VARCHAR(255)  DEFAULT '',
    avatar_url      VARCHAR(255),
    student_id      VARCHAR(255)  DEFAULT '',
    university      VARCHAR(255),
    semester        VARCHAR(255),
    role            VARCHAR(255)  DEFAULT 'user',
    account_type    VARCHAR(255)  DEFAULT 'Student Account',
    currency        VARCHAR(255)  DEFAULT 'INR',
    currency_symbol VARCHAR(255)  DEFAULT '₹',
    total_balance   NUMERIC(12,2) DEFAULT 0.00,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     DEFAULT now()
);

-- 2. WALLETS — the "budgets" students see (money set aside for one purpose)
CREATE TABLE IF NOT EXISTS public.wallets (
    id              VARCHAR(64)   PRIMARY KEY,
    user_id         VARCHAR(64)   NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    name            VARCHAR(255)  NOT NULL,
    category        VARCHAR(255)  NOT NULL,
    balance         NUMERIC(12,2) DEFAULT 0.00,
    budget_limit    NUMERIC(12,2) DEFAULT 1000.00,
    icon            VARCHAR(255)  DEFAULT 'wallet',
    color           VARCHAR(255)  DEFAULT '#3B82F6',
    cycle_days_left INTEGER       DEFAULT 30,
    daily_avg       NUMERIC(10,2) DEFAULT 0.00,
    status          VARCHAR(255)  DEFAULT 'Good',
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     DEFAULT now(),
    CONSTRAINT wallets_balance_non_negative CHECK (balance >= 0)
);

-- 3. TRANSACTIONS — money in and money out. Deleting a wallet keeps its history.
CREATE TABLE IF NOT EXISTS public.transactions (
    id             VARCHAR(64)   PRIMARY KEY,
    user_id        VARCHAR(64)   NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    wallet_id      VARCHAR(64)   REFERENCES public.wallets(id) ON DELETE SET NULL,
    wallet_name    VARCHAR(255),
    title          VARCHAR(255)  NOT NULL,
    amount         NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    type           VARCHAR(32)   NOT NULL CHECK (type IN ('income', 'expense', 'transfer')),
    category       VARCHAR(64)   DEFAULT 'General',
    recipient      VARCHAR(255),
    merchant       VARCHAR(255),
    payment_method VARCHAR(255)  DEFAULT 'UPI',
    status         VARCHAR(32)   DEFAULT 'completed',
    note           VARCHAR(500),
    date           TIMESTAMP     NOT NULL DEFAULT now(),
    created_at     TIMESTAMP     NOT NULL DEFAULT now()
);

-- 4. BUDGETS — monthly spending limit per category (used for 80% / 100% alerts)
CREATE TABLE IF NOT EXISTS public.budgets (
    id           VARCHAR(64)   PRIMARY KEY,
    user_id      VARCHAR(64)   NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    category     VARCHAR(64)   NOT NULL,
    limit_amount NUMERIC(12,2) NOT NULL,
    spent_amount NUMERIC(12,2) DEFAULT 0.00,
    period       VARCHAR(32)   DEFAULT 'monthly',
    start_date   DATE          DEFAULT CURRENT_DATE,
    end_date     DATE,
    created_at   TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP     DEFAULT now()
);

-- 5. SAVINGS GOALS
CREATE TABLE IF NOT EXISTS public.savings_goals (
    id             VARCHAR(64)   PRIMARY KEY,
    user_id        VARCHAR(64)   NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title          VARCHAR(255)  NOT NULL,
    target_amount  NUMERIC(12,2) NOT NULL,
    current_amount NUMERIC(12,2) DEFAULT 0.00,
    target_date    DATE,
    icon           VARCHAR(255)  DEFAULT 'target',
    category       VARCHAR(255)  DEFAULT 'Savings',
    status         VARCHAR(32)   DEFAULT 'in_progress',
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP     DEFAULT now()
);

-- Indexes for the queries the API runs most (ARCHITECTURE §6)
CREATE INDEX IF NOT EXISTS idx_wallets_user              ON public.wallets (user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_date    ON public.transactions (user_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_wallet       ON public.transactions (wallet_id);
CREATE INDEX IF NOT EXISTS idx_goals_user                ON public.savings_goals (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_budgets_user_cat_period ON public.budgets (user_id, category, period);

-- Row Level Security as defense in depth.
-- RLS is ON with no policies, so Supabase's public API keys (anon / authenticated)
-- cannot read or write anything. The Spring Boot API connects as the database
-- owner, which bypasses RLS, and enforces per-user ownership itself.
ALTER TABLE public.profiles      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wallets       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.budgets       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.savings_goals ENABLE ROW LEVEL SECURITY;
