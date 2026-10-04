-- Dev seed: one admin account for manual UAT and curl tests.
-- Runs only when spring.flyway.locations includes classpath:db/seed (dev profile only).
-- Password: admin1@example.com / Admin1Pass!
-- Only officials have accounts (L10, #18); racers are competitors, seeded in V1003.

insert into users (email, password_hash, first_name, last_name, created_at, updated_at)
values
    ('admin1@example.com', '$2b$10$xEdd1B0miFakxIY2TUpCbOzdjzMluN1h0QsGw2zGspn7yTyYmeBwa', 'Admin', 'One',   now(), now())
on conflict (email) do nothing;

insert into user_roles (user_id, role)
select id, 'ADMIN' from users where email = 'admin1@example.com'
on conflict do nothing;
