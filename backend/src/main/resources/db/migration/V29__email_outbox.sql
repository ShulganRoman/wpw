-- Outbox for outgoing email: every message is persisted before a send attempt,
-- so SMTP downtime does not lose order/status notifications.

CREATE TABLE email_outbox (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipients          TEXT         NOT NULL,
    subject             VARCHAR(500) NOT NULL,
    body                TEXT         NOT NULL,
    attachment          BYTEA,
    attachment_filename VARCHAR(500),
    attachment_mime     VARCHAR(200),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts            INT          NOT NULL DEFAULT 0,
    last_error          TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    next_attempt_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at             TIMESTAMPTZ,
    CONSTRAINT chk_email_outbox_status CHECK (status IN ('PENDING','SENT','FAILED'))
);

-- Dispatcher polls pending rows by next_attempt_at
CREATE INDEX idx_email_outbox_pending_due
    ON email_outbox(next_attempt_at)
    WHERE status = 'PENDING';

-- Admin UI lists FAILED first
CREATE INDEX idx_email_outbox_status_created
    ON email_outbox(status, created_at DESC);
