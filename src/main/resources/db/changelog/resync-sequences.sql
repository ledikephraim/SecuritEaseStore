-- data.sql seeds customer and order rows with explicit IDs, which bypasses their identity
-- sequences entirely. On a fresh database, that leaves customer_id_seq/order_id_seq stuck at
-- their start value (1), so the very first INSERT without an explicit id collides with a
-- seeded row. Resync both sequences to the current max seeded id.
SELECT setval('customer_id_seq', (SELECT MAX(id) FROM customer));
SELECT setval('order_id_seq', (SELECT MAX(id) FROM "order"));
