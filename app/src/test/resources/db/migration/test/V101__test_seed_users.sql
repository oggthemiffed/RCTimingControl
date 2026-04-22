-- Test seed: two racer accounts for integration tests that need ownership isolation.
-- Passwords: racer1@example.com / Racer1Pass!  |  racer2@example.com / Racer2Pass!

insert into users (email, password_hash, first_name, last_name, created_at, updated_at)
values
    ('racer1@example.com', '$2b$10$QWNPqLhXElyx9PhFCXKZsOWMudKPFrvHBdP.wnQa92WYoP5Trg9oe', 'Racer', 'One',  now(), now()),
    ('racer2@example.com', '$2b$10$16v.WcD0KklZyY5v0t/ncuVJYfZSnXZW4xs0zc.J7mmaEZBpdz7CG', 'Racer', 'Two',  now(), now())
on conflict (email) do nothing;

insert into user_roles (user_id, role)
select id, 'RACER' from users where email in ('racer1@example.com', 'racer2@example.com')
on conflict do nothing;
