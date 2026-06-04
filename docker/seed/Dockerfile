FROM postgres:16-alpine
COPY docker/seed/seed.sql /seed.sql
# libpq env vars (PGHOST/PGUSER/PGPASSWORD/PGDATABASE) supplied by compose.
# -v ON_ERROR_STOP=1 makes psql exit non-zero if the seed SQL raises an exception,
# so compose 'service_completed_successfully' correctly reflects seed success/failure.
ENTRYPOINT ["sh", "-c", "psql -v ON_ERROR_STOP=1 -f /seed.sql"]
