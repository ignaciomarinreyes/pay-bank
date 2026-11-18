-- Seed data for local development (idempotent; PostgreSQL syntax). Passwords are stored as BCrypt hashes.
--   alias: ana    password: ana-pass
--   alias: maria  password: maria-pass
--   alias: luis   password: luis-pass
INSERT INTO users (alias, password) VALUES ('ana', '$2a$10$O/B0Ef.CpRSVMg/AcNrB3epGfGLU2QV/Vgm6NArOcoquRnN7EP.Sy') ON CONFLICT (alias) DO NOTHING;
INSERT INTO users (alias, password) VALUES ('maria', '$2a$10$P2CPsJ9lu4fdqcpe/bZJROs.fcOuUF.IHF4hvJ2pXBh2gB6FIUKCe') ON CONFLICT (alias) DO NOTHING;
INSERT INTO users (alias, password) VALUES ('luis', '$2a$10$brYLUGbGJKmtO4vPFIEeOerolCLqfYM2G5jwBVy4b8LBHPOCkh7Ue') ON CONFLICT (alias) DO NOTHING;

-- One account per user with an initial balance of 1000.00
INSERT INTO accounts (user_id, balance)
SELECT id, 1000.00 FROM users WHERE alias IN ('ana', 'maria', 'luis')
ON CONFLICT (user_id) DO NOTHING;

-- Sample payments (identified by their date, before 2026-11-05): 30 between ana (pays on odd days)
-- and maria (pays on even days), every 5th one without concept.
INSERT INTO payments (origin_account_id, destination_account_id, amount, concept, created_at)
SELECT CASE WHEN g % 2 = 1 THEN a.id ELSE m.id END,
       CASE WHEN g % 2 = 1 THEN m.id ELSE a.id END,
       (10 + g)::numeric(19, 2),
       CASE WHEN g % 5 = 0 THEN NULL ELSE 'Pago de ejemplo ' || g END,
       TIMESTAMPTZ '2026-10-01 10:00:00+00' + g * INTERVAL '1 day'
FROM generate_series(1, 30) g,
     (SELECT acc.id FROM accounts acc JOIN users u ON u.id = acc.user_id WHERE u.alias = 'ana') a,
     (SELECT acc.id FROM accounts acc JOIN users u ON u.id = acc.user_id WHERE u.alias = 'maria') m
WHERE NOT EXISTS (SELECT 1 FROM payments WHERE created_at < TIMESTAMPTZ '2026-11-05 00:00:00+00');

-- One sample payment from maria to luis
INSERT INTO payments (origin_account_id, destination_account_id, amount, concept, created_at)
SELECT m.id, l.id, 20.00, 'Cena', TIMESTAMPTZ '2026-11-04 12:00:00+00'
FROM (SELECT acc.id FROM accounts acc JOIN users u ON u.id = acc.user_id WHERE u.alias = 'maria') m,
     (SELECT acc.id FROM accounts acc JOIN users u ON u.id = acc.user_id WHERE u.alias = 'luis') l
WHERE NOT EXISTS (SELECT 1 FROM payments WHERE created_at = TIMESTAMPTZ '2026-11-04 12:00:00+00');

-- Two movements per payment that has none (a charge and a credit), with the balance after each one
INSERT INTO movements (account_id, payment_id, type, amount, balance_after, created_at)
SELECT account_id, payment_id, type, amount,
       1000.00 + SUM(delta) OVER (PARTITION BY account_id ORDER BY created_at, payment_id),
       created_at
FROM (
    SELECT p.origin_account_id AS account_id, p.id AS payment_id, 'CHARGE' AS type, p.amount,
           -p.amount AS delta, p.created_at
    FROM payments p
    WHERE NOT EXISTS (SELECT 1 FROM movements mv WHERE mv.payment_id = p.id)
    UNION ALL
    SELECT p.destination_account_id, p.id, 'CREDIT', p.amount, p.amount, p.created_at
    FROM payments p
    WHERE NOT EXISTS (SELECT 1 FROM movements mv WHERE mv.payment_id = p.id)
) pending;

-- The balance of each account is the balance after its latest movement
UPDATE accounts a
SET balance = latest.balance_after
FROM (SELECT DISTINCT ON (account_id) account_id, balance_after
      FROM movements
      ORDER BY account_id, created_at DESC, id DESC) latest
WHERE a.id = latest.account_id;
