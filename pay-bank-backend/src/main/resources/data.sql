-- Seed users for local development (idempotent). Passwords are stored as BCrypt hashes.
--   alias: ana    password: ana-pass
--   alias: maria  password: maria-pass
INSERT INTO users (alias, password) VALUES ('ana', '$2a$10$O/B0Ef.CpRSVMg/AcNrB3epGfGLU2QV/Vgm6NArOcoquRnN7EP.Sy') ON CONFLICT (alias) DO NOTHING;
INSERT INTO users (alias, password) VALUES ('maria', '$2a$10$P2CPsJ9lu4fdqcpe/bZJROs.fcOuUF.IHF4hvJ2pXBh2gB6FIUKCe') ON CONFLICT (alias) DO NOTHING;
