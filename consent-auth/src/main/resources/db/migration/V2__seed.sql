-- Customers (demo plaintext passwords)
INSERT INTO customer (customer_id, username, password, display_name) VALUES
 ('alice', 'alice', 'pw', 'Alice Account-Holder'),
 ('bob',   'bob',   'pw', 'Bob Account-Holder');

-- Pre-registered TPP client: MohanaTPP
INSERT INTO registered_client (client_id, client_secret, redirect_uri, allowed_scopes) VALUES
 ('mohana-tpp', 'mohana-secret', 'http://localhost:8080/callback', 'accounts payments');
