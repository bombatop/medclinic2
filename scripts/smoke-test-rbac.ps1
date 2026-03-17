# RBAC Smoke Tests - run against http://localhost:8080
$base = "http://localhost:8080/api"
$adminToken = $null
$doctorToken = $null
$receptionistToken = $null
$adminId = $null
$doctorUserId = $null
$doctorEmployeeId = $null
$receptionistUserId = $null
$clientId = $null
$appointmentId = $null

function Invoke-Api {
    param($Method, $Path, $Body = $null, $Token = $null, [switch]$NoAuth)
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token -and -not $NoAuth) { $headers["Authorization"] = "Bearer $Token" }
    $uri = "$base$Path"
    try {
        if ($Body) {
            $r = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -Body ($Body | ConvertTo-Json) -ErrorAction Stop
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

Write-Host "`n=== 1. Login as admin ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/login" -Body @{ username = "admin"; password = "admin" } -NoAuth
if (-not $r.ok) { Write-Host "FAIL: admin login - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$adminToken = $r.data.accessToken
Write-Host "OK: roles=$($r.data.roles -join ',') perms=$($r.data.permissions.Count)" -ForegroundColor Green

Write-Host "`n=== 2. Get users (admin) ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/users?page=0&size=20" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: get users - $($r.status)" -ForegroundColor Red; exit 1 }
$users = $r.data.content
$adminUser = $users | Where-Object { $_.username -eq "admin" } | Select-Object -First 1
$adminId = $adminUser.id
Write-Host "OK: $($users.Count) users" -ForegroundColor Green

Write-Host "`n=== 3. Get/Update user roles (admin) ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/users/$adminId/roles" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: get roles - $($r.status)" -ForegroundColor Red; exit 1 }
Write-Host "OK: admin roles=$($r.data.roles -join ',')" -ForegroundColor Green

$r = Invoke-Api -Method PUT -Path "/auth/auth/users/$adminId/roles" -Body @{ roles = @("ADMIN") } -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: update roles - $($r.status)" -ForegroundColor Red; exit 1 }
Write-Host "OK: roles updated" -ForegroundColor Green

Write-Host "`n=== 4. Create doctor user + employee ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/users" -Body @{
    username = "dr_smoke"
    password = "doctor123"
    firstName = "Smoke"
    lastName = "Doctor"
    email = "dr_smoke@test.local"
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

Write-Host "`n=== 5. Create receptionist user ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/users" -Body @{
    username = "rec_smoke"
    password = "rec123"
    firstName = "Smoke"
    lastName = "Receptionist"
    email = "rec_smoke@test.local"
    roles = @("RECEPTIONIST")
} -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: create receptionist - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
$receptionistUserId = $r.data.id
Write-Host "OK: receptionist user id=$receptionistUserId" -ForegroundColor Green

Write-Host "`n=== 6. Get or create client ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/main/clients?page=0&size=5" -Token $adminToken
if ($r.ok -and $r.data.content.Count -gt 0) {
    $clientId = $r.data.content[0].id
    Write-Host "OK: using existing client id=$clientId" -ForegroundColor Green
} else {
    $r = Invoke-Api -Method POST -Path "/main/clients" -Body @{
        firstName = "Smoke"
        lastName = "Client"
        phone = "+1234567890"
        email = "client_smoke@test.local"
    } -Token $adminToken
    if (-not $r.ok) { Write-Host "FAIL: create client - $($r.status) $($r.body)" -ForegroundColor Red; exit 1 }
    $clientId = $r.data.id
    Write-Host "OK: created client id=$clientId" -ForegroundColor Green
}

Write-Host "`n=== 7. Login as doctor ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/login" -Body @{ username = "dr_smoke"; password = "doctor123" } -NoAuth
if (-not $r.ok) { Write-Host "FAIL: doctor login - $($r.status)" -ForegroundColor Red; exit 1 }
$doctorToken = $r.data.accessToken
Write-Host "OK: roles=$($r.data.roles -join ',')" -ForegroundColor Green

Write-Host "`n=== 8. Doctor: list appointments (read_all) ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/main/appointments?page=0&size=5" -Token $doctorToken
if (-not $r.ok) { Write-Host "FAIL: doctor list appointments - $($r.status)" -ForegroundColor Red; exit 1 }
Write-Host "OK: doctor can list appointments" -ForegroundColor Green

Write-Host "`n=== 9. Doctor: create appointment for SELF ===" -ForegroundColor Cyan
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
Write-Host "OK: created appointment id=$appointmentId" -ForegroundColor Green

Write-Host "`n=== 10. Doctor: update status (own appointment) ===" -ForegroundColor Cyan
$r = Invoke-Api -Method PATCH -Path "/main/appointments/$appointmentId/status?status=IN_PROGRESS" -Token $doctorToken
if (-not $r.ok) { Write-Host "FAIL: doctor update status - $($r.status)" -ForegroundColor Red; exit 1 }
Write-Host "OK: doctor can update own status" -ForegroundColor Green

Write-Host "`n=== 11. Login as receptionist ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/auth/auth/login" -Body @{ username = "rec_smoke"; password = "rec123" } -NoAuth
if (-not $r.ok) { Write-Host "FAIL: receptionist login - $($r.status)" -ForegroundColor Red; exit 1 }
$receptionistToken = $r.data.accessToken
Write-Host "OK: roles=$($r.data.roles -join ',')" -ForegroundColor Green

Write-Host "`n=== 12. Receptionist: list appointments (read_only) ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/main/appointments?page=0&size=5" -Token $receptionistToken
if (-not $r.ok) { Write-Host "FAIL: receptionist list - $($r.status)" -ForegroundColor Red; exit 1 }
Write-Host "OK: receptionist can list appointments" -ForegroundColor Green

Write-Host "`n=== 13. Receptionist: create appointment (should DENY) ===" -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/main/appointments" -Body @{
    employeeId = $doctorEmployeeId
    clientId = $clientId
    startTime = $start
    endTime = $end
} -Token $receptionistToken
if ($r.ok) { Write-Host "FAIL: receptionist should NOT be able to create - got 200" -ForegroundColor Red; exit 1 }
Write-Host "OK: correctly denied (403)" -ForegroundColor Green

Write-Host "`n=== 14. Receptionist: update status (should DENY) ===" -ForegroundColor Cyan
$r = Invoke-Api -Method PATCH -Path "/main/appointments/$appointmentId/status?status=COMPLETED" -Token $receptionistToken
if ($r.ok) { Write-Host "FAIL: receptionist should NOT be able to update status - got 200" -ForegroundColor Red; exit 1 }
Write-Host "OK: correctly denied (403)" -ForegroundColor Green

Write-Host "`n=== 15. Admin: update doctor roles (add RECEPTIONIST) ===" -ForegroundColor Cyan
$r = Invoke-Api -Method PUT -Path "/auth/auth/users/$doctorUserId/roles" -Body @{ roles = @("DOCTOR", "RECEPTIONIST") } -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: update doctor roles - $($r.status)" -ForegroundColor Red; exit 1 }
Write-Host "OK: doctor now has DOCTOR,RECEPTIONIST" -ForegroundColor Green

Write-Host "`n=== 16. Verify role assignment persisted ===" -ForegroundColor Cyan
$r = Invoke-Api -Method GET -Path "/auth/auth/users/$doctorUserId/roles" -Token $adminToken
if (-not $r.ok) { Write-Host "FAIL: get roles - $($r.status)" -ForegroundColor Red; exit 1 }
$roles = $r.data.roles
if ($roles -notcontains "DOCTOR" -or $roles -notcontains "RECEPTIONIST") {
    Write-Host "FAIL: expected DOCTOR,RECEPTIONIST got $($roles -join ',')" -ForegroundColor Red
    exit 1
}
Write-Host "OK: roles=$($roles -join ',')" -ForegroundColor Green

Write-Host "`n=== All smoke tests PASSED ===" -ForegroundColor Green
