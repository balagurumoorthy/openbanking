-- Sample customers: alice (GB-ALICE-001 current, GB-ALICE-002 savings), bob (GB-BOB-001 current)
INSERT INTO account (account_id, customer_id, status, currency, account_type, account_sub_type, nickname, identification, name) VALUES
 ('GB-ALICE-001', 'alice', 'Enabled', 'GBP', 'Personal', 'CurrentAccount', 'Everyday',  '40123412345678', 'Alice Current'),
 ('GB-ALICE-002', 'alice', 'Enabled', 'GBP', 'Personal', 'Savings',        'Rainy Day', '40123487654321', 'Alice Savings'),
 ('GB-BOB-001',   'bob',   'Enabled', 'GBP', 'Personal', 'CurrentAccount', 'Main',      '40555512340000', 'Bob Current');

INSERT INTO balance (id, account_id, amount, currency, credit_debit_indicator, type, date_time) VALUES
 (1, 'GB-ALICE-001', 2540.18, 'GBP', 'Credit', 'InterimAvailable', '2026-06-01T09:00:00Z'),
 (2, 'GB-ALICE-002', 8800.00, 'GBP', 'Credit', 'InterimAvailable', '2026-06-01T09:00:00Z'),
 (3, 'GB-BOB-001',   1250.75, 'GBP', 'Credit', 'InterimAvailable', '2026-06-01T09:00:00Z');

INSERT INTO transaction (id, account_id, credit_debit_indicator, status, amount, currency, booking_date_time, transaction_information) VALUES
 (1, 'GB-ALICE-001', 'Debit',  'Booked', 42.50,  'GBP', '2026-05-28T12:30:00Z', 'COFFEE SHOP LONDON'),
 (2, 'GB-ALICE-001', 'Credit', 'Booked', 2100.00,'GBP', '2026-05-25T08:00:00Z', 'SALARY ACME LTD'),
 (3, 'GB-ALICE-001', 'Debit',  'Booked', 75.00,  'GBP', '2026-05-24T19:10:00Z', 'GROCERIES'),
 (4, 'GB-ALICE-002', 'Credit', 'Booked', 200.00, 'GBP', '2026-05-20T08:00:00Z', 'TRANSFER FROM CURRENT'),
 (5, 'GB-BOB-001',   'Debit',  'Booked', 30.00,  'GBP', '2026-05-27T14:00:00Z', 'BOOKSTORE');
