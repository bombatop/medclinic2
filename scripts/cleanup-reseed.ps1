# Cleanup DB and reseed (no smoke tests)
# Drops public schema in auth + main DBs, restarts services so Hibernate recreates and seeders run.

$ErrorActionPreference = "Stop"

Write-Host "`n=== Cleanup DB and reseed ===" -ForegroundColor Cyan

# Auth DB
Write-Host "Dropping auth DB schema..." -ForegroundColor Yellow
docker exec medclinic-postgres-auth psql -U auth_user -d auth_service_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO auth_user; GRANT ALL ON SCHEMA public TO public;"
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL: auth DB cleanup" -ForegroundColor Red; exit 1 }
Write-Host "OK: auth schema dropped" -ForegroundColor Green

# Main DB
Write-Host "Dropping main DB schema..." -ForegroundColor Yellow
docker exec medclinic-postgres-main psql -U main_user -d main_service_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO main_user; GRANT ALL ON SCHEMA public TO public;"
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL: main DB cleanup" -ForegroundColor Red; exit 1 }
Write-Host "OK: main schema dropped" -ForegroundColor Green

# Restart services so Hibernate recreates schema and seeders run
Write-Host "Restarting auth-service and main-service..." -ForegroundColor Yellow
docker restart medclinic-auth-service medclinic-main-service
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL: restart services" -ForegroundColor Red; exit 1 }
Write-Host "OK: services restarted" -ForegroundColor Green

Write-Host "`nDone. Wait ~30s for services to be healthy; RBAC + admin will be reseeded." -ForegroundColor Cyan
