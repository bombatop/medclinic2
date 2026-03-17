# Reset admin password to default

If you need to restore the default admin account (`admin` / `admin`):

1. Delete the seeded admin user from the auth DB, then restart auth-service so the seeder recreates it:

```bash
docker exec medclinic-postgres-auth psql -U auth_user -d auth_service_db -c "DELETE FROM users WHERE username = 'admin';"
docker restart medclinic-auth-service
```

2. Wait for auth-service to be healthy; the default admin account will exist again with password `admin`.

Change this password after first login via `/auth/me/password`.
