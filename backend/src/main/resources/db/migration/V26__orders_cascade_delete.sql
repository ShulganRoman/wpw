ALTER TABLE orders
    DROP CONSTRAINT orders_dealer_id_fkey,
    ADD  CONSTRAINT orders_dealer_id_fkey
         FOREIGN KEY (dealer_id) REFERENCES dealers(id) ON DELETE CASCADE;
