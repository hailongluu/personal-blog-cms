-- V7: Newsletter send audit log
-- Tracks every newsletter broadcast attempt (subject, recipient count, success/failure tally).

CREATE TABLE newsletter_send_log (
    id              BIGSERIAL PRIMARY KEY,
    subject         VARCHAR(200) NOT NULL,
    body_html       TEXT         NOT NULL,
    recipient_count INTEGER      NOT NULL DEFAULT 0,
    success_count   INTEGER      NOT NULL DEFAULT 0,
    failure_count   INTEGER      NOT NULL DEFAULT 0,
    sent_by         BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    sent_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_newsletter_send_log_sent_at ON newsletter_send_log(sent_at DESC);
