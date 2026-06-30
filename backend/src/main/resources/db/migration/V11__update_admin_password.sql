-- ═══════════════════════════════════════════════════════════════
-- V11__update_admin_password.sql
-- Set the default admin password to the operator-chosen value.
-- Supersedes the dev-only 'admin123' seeded in V2.
-- ═══════════════════════════════════════════════════════════════
-- Hash: bcrypt (cost 12, matches BCryptPasswordEncoder(12)) of the chosen
-- password. A bcrypt hash is one-way + salted, so committing it does not
-- expose the plaintext. Only updates the seeded admin account.
--
-- NOTE: for stronger hygiene, drive this from an env var / secret instead of
-- a committed hash. Tracked as a follow-up.
-- ═══════════════════════════════════════════════════════════════

UPDATE users
SET password_hash = '$2b$12$YGCxMWMKyjfk4YkzZq8ZZO/ZjjtaLPyFFCNWi.wiIe0m1H7zaOqSa'
WHERE email = 'admin@example.com';
