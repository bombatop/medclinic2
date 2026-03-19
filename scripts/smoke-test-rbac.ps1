# RBAC Smoke Tests - run against http://localhost:8080
$base = "http://localhost:8080/api"
$adminToken = $null
$doctorToken = $null
$receptionistToken = $null
$adminRefreshToken = $null
$adminId = $null
$doctorUserId = $null
$doctorEmployeeId = $null
$receptionistUserId = $null
$clientId = $null
$appointmentId = $null
$customRoleId = $null
$customRoleCode = "SMOKE_" + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

function Invoke-Api {
    param($Method, $Path, $Body = $null, $Token = $null, [switch]$NoAuth)
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token -and -not $NoAuth) { $headers["Authorization"] = "Bearer $Token" }
    $uri = "$base$Path"
    try {
        if ($Body) {
            $r = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -Body ($Body | ConvertTo-Json -Depth 8) -ErrorAction Stop
        } else {
            $r = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ErrorAction Stop
        }
        return @{ ok = $true; data = $r }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $body = $null
        try { $stream = $_.Exception.Response.GetResponseStream(); $reader = New-Object System.IO.StreamReader($stream); $body = $reader.ReadToEnd() } catch {}
        return @{ ok = $false; status = $status; body = $body }
    }
}

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$doctorUsername = "dr_smoke_$suffix"
$receptionistUsername = "rec_smoke_$suffix"

Write-Host "`n=== 1. Login as admin ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/login" -Body @{ username = "admin"; password = "admin" } -NoAuth
if (-not $r.ok) { Write-Host "FAIL: admin login - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$adminToken = $r.data.accessToken
$adminRefreshToken = $r.data.refreshToken
Write-Host "OK: roles=$($r.data.roles -join ',') perms=$($r.data.permissions.Count)" -ForegroundColor Green

Write-Host "`n=== 2. Refresh token cannot be used as access token ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/me" -Token $adminRefreshToken
if ($r.ok) { Write-Host "FAIL: refresh token unexpectedly authorized" -ForegroundColor Red; exit 1 }
Write-Host "OK: refresh token denied ($($r.status))" -ForegroundColor Green

Write-Host "`n=== 3. Get users (admin) ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/users?page=0&size=20" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: get users - $($r.status)" -ForegroundColor Red; exit 1 }
$users = $r.data.content
$adminUser = $users | Where-Object { $_.username -eq "admin" } | Select-Object -First 1
$adminId = $adminUser.id
Write-Host "OK: $($users.Count) users" -ForegroundColor Green

Write-Host "`n=== 4. List baseline RBAC roles ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/rbac/roles?page=0&size=50" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: list roles - $($r.status)" -ForegroundColor Red; exit 1 }
if (($r.data.content | Where-Object { $_.code -eq "ADMIN" }).Count -eq 0) { Write-Host "FAIL: ADMIN role missing" -ForegroundColor Red; exit 1 }
Write-Host "OK: baseline roles listed" -ForegroundColor Green

Write-Host "`n=== 5. List permission catalog ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/rbac/permissions" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: list permissions - $($r.status)" -ForegroundColor Red; exit 1 }
if (($r.data | Where-Object { $_.code -eq "appointment.read_all" }).Count -eq 0) { Write-Host "FAIL: appointment.read_all missing" -ForegroundColor Red; exit 1 }
Write-Host "OK: permission catalog available" -ForegroundColor Green

Write-Host "`n=== 6. Create custom role ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/rbac/roles" -Body @{
    code = $customRoleCode
    name = "Smoke Custom"
    description = "Smoke test custom role"
    active = $true
} -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: create role - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$customRoleId = $r.data.id
Write-Host "OK: custom role id=$customRoleId code=$customRoleCode" -ForegroundColor Green

Write-Host "`n=== 7. Set custom role permissions ===" -ForegroundColor Cyan
$r = Invoke-Api -Method PUT -Path "/auth/auth/rbac/roles/$customRoleId/permissions" -Body @{
    permissions = @("appointment.read_all", "employee.read_all")
} -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: set role permissions - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
if ($r.data.permissions.Count -ne 2) { Write-Host "FAIL: expected 2 permissions on custom role" -ForegroundColor Red; exit 1 }
Write-Host "OK: custom role permissions updated" -ForegroundColor Green

Write-Host "`n=== 8. Verify role permissions persisted ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/rbac/roles/$customRoleId/permissions" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: get role permissions - $($r.status)" -ForegroundColor Red; exit 1 }
if ($r.data.permissions -notcontains "appointment.read_all") { Write-Host "FAIL: expected appointment.read_all" -ForegroundColor Red; exit 1 }
Write-Host "OK: role permission matrix persisted" -ForegroundColor Green

Write-Host "`n=== 9. Get/Update admin user roles ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/users/$adminId/roles" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: get admin roles - $($r.status)" -ForegroundColor Red; exit 1 }
$r = Invoke-Api -Method PUT -Path "/auth/auth/users/$adminId/roles" -Body @{ roles = @("ADMIN") } -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: normalize admin roles - $($r.status)" -ForegroundColor Red; exit 1 }
Write-Host "OK: admin user roles endpoint works" -ForegroundColor Green

Write-Host "`n=== 10. Create doctor user + employee ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/users" -Body @{
    username = $doctorUsername
    password = "doctor123"
    firstName = "Smoke"
    lastName = "Doctor"
    email = "$doctorUsername@test.local"
    roles = @("DOCTOR")
} -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: create doctor user - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$doctorUserId = $r.data.id
Write-Host "OK: doctor user id=$doctorUserId" -ForegroundColor Green

$r = Invoke-Api -Method POST -Path "/main/employees" -Body @{
    authUserId = $doctorUserId
    firstName = "Smoke"
    lastName = "Doctor"
    specialization = "General"
} -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: create employee - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$doctorEmployeeId = $r.data.id
Write-Host "OK: doctor employee id=$doctorEmployeeId" -ForegroundColor Green

Write-Host "`n=== 11. Create receptionist user ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/users" -Body @{
    username = $receptionistUsername
    password = "rec123"
    firstName = "Smoke"
    lastName = "Receptionist"
    email = "$receptionistUsername@test.local"
    roles = @("RECEPTIONIST")
} -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: create receptionist - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$receptionistUserId = $r.data.id
Write-Host "OK: receptionist user id=$receptionistUserId" -ForegroundColor Green

Write-Host "`n=== 12. Get or create client ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/main/clients?page=0&size=5" -Token $adminToken
if ($r.ok -and $r.data.content.Count -gt 0) {
    $clientId = $r.data.content[0].id
    Write-Host "OK: using existing client id=$clientId" -ForegroundColor Green
} else {
    $r = Invoke-Api -Method POST -Path "/main/clients" -Body @{
        firstName = "Smoke"
        lastName = "Client"
        phone = "+1234567890"
        email = "client_smoke_$suffix@test.local"
    } -Token $adminToken
    if (-not $r.ok) { Write-Host "FAIL: create client - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
    $clientId = $r.data.id
    Write-Host "OK: created client id=$clientId" -ForegroundColor Green
}

Write-Host "`n=== 13. Login as doctor ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/login" -Body @{ username = $doctorUsername; password = "doctor123" } -NoAuth
if (-not $r.ok) { Write-Host "FAIL: doctor login - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$doctorToken = $r.data.accessToken
Write-Host "OK: doctor login roles=$($r.data.roles -join ',')" -ForegroundColor Green

Write-Host "`n=== 14. Doctor: create and update own appointment ===" -ForegroundColor Cyan
$start = (Get-Date).AddHours(2).ToString("yyyy-MM-ddTHH:mm:ss")
$end = (Get-Date).AddHours(3).ToString("yyyy-MM-ddTHH:mm:ss")
$r = Invoke-Api -Method POST -Path "/main/appointments" -Body @{
    employeeId = $doctorEmployeeId
    clientId = $clientId
    startTime = $start
    endTime = $end
    notes = "Smoke test"
} -Token $doctorToken
if (-not $r.ok) { Write-Host "FAIL: doctor create self - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$appointmentId = $r.data.id
$r = Invoke-Api -Method PATCH -Path "/main/appointments/$appointmentId/status?status=IN_PROGRESS" -Token $doctorToken
if (-not $r.ok) { Write-Host "FAIL: doctor status update - $($r.status)" -ForegroundColor Red; exit 1 }
Write-Host "OK: doctor self permissions enforced" -ForegroundColor Green

Write-Host "`n=== 15. Login as receptionist ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/login" -Body @{ username = $receptionistUsername; password = "rec123" } -NoAuth
if (-not $r.ok) { Write-Host "FAIL: receptionist login - $($r.status)" -ForegroundColor Red; exit 1 }
$receptionistToken = $r.data.accessToken

Write-Host "`n=== 16. Receptionist: read allowed, write denied ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/main/appointments?page=0&size=5" -Token $receptionistToken
if (-not $r.ok) { Write-Host "FAIL: receptionist read - $($r.status)" -ForegroundColor Red; exit 1 }
$r = Invoke-Api -Method POST -Path "/main/appointments" -Body @{
    employeeId = $doctorEmployeeId
    clientId = $clientId
    startTime = $start
    endTime = $end
} -Token $receptionistToken
if ($r.ok) { Write-Host "FAIL: receptionist write unexpectedly allowed" -ForegroundColor Red; exit 1 }
Write-Host "OK: receptionist has read-only behavior" -ForegroundColor Green

Write-Host "`n=== 17. Assign custom role to receptionist user ===" -ForegroundColor Cyan
$r = Invoke-Api -Method PUT -Path "/auth/auth/users/$receptionistUserId/roles" -Body @{ roles = @("RECEPTIONIST", $customRoleCode) } -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: assign custom role - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$r = Invoke-Api -Method GET -Path "/auth/auth/users/$receptionistUserId/roles" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: get updated user roles - $($r.status)" -ForegroundColor Red; exit 1 }
if ($r.data.roles -notcontains $customRoleCode) { Write-Host "FAIL: custom role assignment missing" -ForegroundColor Red; exit 1 }
Write-Host "OK: user-role assignment accepts custom roles" -ForegroundColor Green

Write-Host "`n=== 18. RBAC audit endpoint should return events ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/rbac/audit?page=0&size=20" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: audit query - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
if ($r.data.content.Count -lt 1) { Write-Host "FAIL: expected at least one RBAC audit entry" -ForegroundColor Red; exit 1 }
Write-Host "OK: audit logs query works" -ForegroundColor Green

Write-Host "`n=== All enterprise RBAC smoke tests PASSED ===" -ForegroundColor Green
