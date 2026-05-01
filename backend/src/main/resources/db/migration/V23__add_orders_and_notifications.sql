-- Orders
CREATE TABLE orders (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    dealer_id   UUID         NOT NULL REFERENCES dealers(id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    currency    VARCHAR(10)  NOT NULL DEFAULT 'USD',
    total       NUMERIC(14,2) NOT NULL DEFAULT 0,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  UUID,
    tool_no     VARCHAR(100) NOT NULL,
    name        VARCHAR(500) NOT NULL,
    qty         INT          NOT NULL CHECK (qty > 0),
    unit_price  NUMERIC(14,2),
    line_total  NUMERIC(14,2)
);

-- Notification email addresses for admin alerts
CREATE TABLE notification_emails (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL UNIQUE,
    active     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_dealer_id  ON orders(dealer_id);
CREATE INDEX idx_orders_status     ON orders(status);
CREATE INDEX idx_order_items_order ON order_items(order_id);
