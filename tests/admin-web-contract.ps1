<#
.SYNOPSIS
    Verifies that every Admin Web API contract holds against a running backend.

.DESCRIPTION
    The React client declares the response shapes in src/api/types.ts by hand, so
    the risk is drift: a renamed field or a moved route renders an empty screen
    rather than failing loudly. This script asserts each field the UI actually reads
    is present, and exercises the two mutations the console performs.

    Run the backend first, then:
      .\tests\admin-web-contract.ps1
#>

param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$AdminEmail = "admin@finai.local",
    [string]$AdminPassword = "Admin#12345"
)

$ErrorActionPreference = 'Stop'
$script:Passed = 0
$script:Failed = 0
$script:Results = New-Object System.Collections.ArrayList

function Write-Header {
    param([string]$Text)
    Write-Host ""
    Write-Host "=== $Text ===" -ForegroundColor Cyan
}

function Assert-True {
    param([string]$Name, [bool]$Condition, [string]$Detail = "")
    if ($Condition) {
        $script:Passed++
        $script:Results.Add([pscustomobject]@{ Test = $Name; Result = "PASS"; Detail = $Detail })
        Write-Host ("  PASS  {0} {1}" -f $Name, $Detail) -ForegroundColor Green
    } else {
        $script:Failed++
        $script:Results.Add([pscustomobject]@{ Test = $Name; Result = "FAIL"; Detail = $Detail })
        Write-Host ("  FAIL  {0} {1}" -f $Name, $Detail) -ForegroundColor Red
    }
}

function Assert-Equal {
    param([string]$Name, $Expected, $Actual)
    Assert-True -Name $Name -Condition ($Expected -eq $Actual) `
        -Detail ("expected={0} actual={1}" -f $Expected, $Actual)
}

# Invoke-WebRequest returns byte[] for non-text MIME types, which breaks
# ConvertFrom-Json; decode before parsing.
function ConvertTo-Object {
    param($Raw)
    if ($null -eq $Raw) { return $null }
    $text = $Raw
    if ($Raw -is [byte[]]) { $text = [System.Text.Encoding]::UTF8.GetString($Raw) }
    if ([string]::IsNullOrWhiteSpace($text)) { return $null }
    try { return $text | ConvertFrom-Json } catch { return $text }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token = $null,
        [switch]$Raw
    )
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{
        Method      = $Method
        Uri         = "$BaseUrl$Path"
        Headers     = $headers
        ContentType = "application/json"
        TimeoutSec  = 30
        ErrorAction = 'Stop'
    }
    if ($null -ne $Body) { $params["Body"] = ($Body | ConvertTo-Json -Depth 10 -Compress) }
    try {
        $response = Invoke-WebRequest @params
        if ($Raw) { return $response.Content }
        return [pscustomobject]@{
            Status  = [int]$response.StatusCode
            Content = ConvertTo-Object $response.Content
        }
    } catch {
        $status = 0
        $content = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $content = ConvertTo-Object $reader.ReadToEnd()
            } catch { }
        }
        return [pscustomobject]@{ Status = $status; Content = $content }
    }
}

function Assert-Field {
    param([string]$Name, $Object, [string]$Field)
    $present = $false
    if ($null -ne $Object -and $Object.PSObject.Properties.Name -contains $Field) {
        $present = $true
    }
    Assert-True -Name $Name -Condition $present -Detail ("field={0}" -f $Field)
}

# Jackson omits nulls entirely, so a nullable field is either present with a value
# or absent. What the client must be able to rely on is that it never arrives as an
# explicit JSON null, which is why the corresponding TypeScript type is optional.
function Assert-NoExplicitNull {
    param([string]$Name, $Object, [string]$Field)
    if ($null -eq $Object) {
        Assert-True -Name $Name -Condition $false -Detail "object was null"
        return
    }
    $property = $Object.PSObject.Properties[$Field]
    $ok = $true
    if ($null -ne $property -and $null -eq $property.Value) {
        $ok = $false
    }
    Assert-True -Name $Name -Condition $ok `
        -Detail ("field={0} is absent or set, never explicit null" -f $Field)
}

$stamp = (Get-Date).ToString("HHmmssfff")

# ---------------------------------------------------------------------------
Write-Header "Admin login"

$login = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{
    identifier = $AdminEmail
    password   = $AdminPassword
}
Assert-Equal "Admin can sign in" 200 $login.Status
Assert-Field "Login returns accessToken" $login.Content "accessToken"
Assert-Field "Login returns user object" $login.Content "user"
Assert-Field "Login returns user role" $login.Content.user "role"
Assert-Equal "Login role is ADMIN" "ADMIN" $login.Content.user.role

$token = $login.Content.accessToken

# ---------------------------------------------------------------------------
Write-Header "Dashboard (GET /api/admin/dashboard)"

$dash = Invoke-Api -Method GET -Path "/api/admin/dashboard" -Token $token
Assert-Equal "Dashboard loads" 200 $dash.Status
foreach ($field in @(
    "totalUsers", "activeUsers", "lockedUsers", "deactivatedUsers",
    "newUsersLast7Days", "totalTransactions", "systemCategories",
    "personalCategories", "feedbackByStatus", "ai", "runtime"
)) {
    Assert-Field "Dashboard exposes $field" $dash.Content $field
}
Assert-Field "Dashboard ai block exposes engine" $dash.Content.ai "engine"
Assert-Field "Dashboard ai block exposes totalQuestions" $dash.Content.ai "totalQuestions"
Assert-Field "Dashboard runtime exposes uptimeSeconds" $dash.Content.runtime "uptimeSeconds"
Assert-Field "Dashboard runtime exposes uptimeSinceRestart" $dash.Content.runtime "uptimeSinceRestart"

# ---------------------------------------------------------------------------
Write-Header "Users (GET /api/admin/users)"

$users = Invoke-Api -Method GET -Path "/api/admin/users?page=0&size=20" -Token $token
Assert-Equal "User list loads" 200 $users.Status
Assert-Field "User page exposes content" $users.Content "content"
Assert-Field "User page exposes totalElements" $users.Content "totalElements"
Assert-Field "User page exposes totalPages" $users.Content "totalPages"
Assert-True "User list is non-empty" ($users.Content.content.Count -ge 1)

if ($users.Content.content.Count -ge 1) {
    $firstUser = $users.Content.content[0]
    foreach ($field in @("id", "email", "username", "role", "status",
        "transactionCount", "createdAt")) {
        Assert-Field "User row exposes $field" $firstUser $field
    }
    foreach ($field in @("fullName", "lastLoginAt")) {
        Assert-NoExplicitNull "User row never sends an explicit null for $field" $firstUser $field
    }
}

$search = Invoke-Api -Method GET -Path "/api/admin/users?search=$stamp&page=0&size=20" -Token $token
Assert-Equal "User search is accepted" 200 $search.Status

$filtered = Invoke-Api -Method GET -Path "/api/admin/users?status=LOCKED&page=0&size=20" -Token $token
Assert-Equal "User status filter is accepted" 200 $filtered.Status
Assert-True "Locked filter returns only LOCKED accounts" (
    @($filtered.Content.content | Where-Object { $_.status -ne 'LOCKED' }).Count -eq 0)

# ---------------------------------------------------------------------------
Write-Header "Categories (GET/POST/PATCH /api/admin/categories)"

$categories = Invoke-Api -Method GET -Path "/api/admin/categories" -Token $token
Assert-Equal "Category list loads" 200 $categories.Status
Assert-True "Category list is an array" ($categories.Content -is [array])
Assert-True "Category list is non-empty" ($categories.Content.Count -ge 10)

$firstCategory = $categories.Content[0]
foreach ($field in @("id", "name", "code", "type", "active", "usageCount")) {
    Assert-Field "Category row exposes $field" $firstCategory $field
}
foreach ($field in @("icon", "color")) {
    Assert-NoExplicitNull "Category row never sends an explicit null for $field" $firstCategory $field
}

$code = "CONTRACT_$stamp"
$created = Invoke-Api -Method POST -Path "/api/admin/categories" -Token $token -Body @{
    name = "Contract check"; code = $code; type = "EXPENSE"; icon = "check"; color = "#123456"
}
Assert-Equal "Category can be created" 200 $created.Status
Assert-Field "Created category exposes usageCount" $created.Content "usageCount"
Assert-Equal "New category is active" $true $created.Content.active
$categoryId = $created.Content.id

$duplicate = Invoke-Api -Method POST -Path "/api/admin/categories" -Token $token -Body @{
    name = "Contract check again"; code = $code; type = "EXPENSE"
}
Assert-Equal "Duplicate category code is rejected" 409 $duplicate.Status

$deactivated = Invoke-Api -Method PATCH -Path "/api/admin/categories/$categoryId" -Token $token -Body @{
    active = $false
}
Assert-Equal "Category can be deactivated" 200 $deactivated.Status
Assert-Equal "Category is now inactive" $false $deactivated.Content.active

$hidden = @($categories.Content | Where-Object { $_.id -eq $categoryId }).Count
Assert-Equal "Deactivated category is hidden from users" 0 $hidden

# ---------------------------------------------------------------------------
Write-Header "Feedback (GET/PATCH /api/admin/feedback)"

$feedback = Invoke-Api -Method GET -Path "/api/admin/feedback?page=0&size=20" -Token $token
Assert-Equal "Feedback list loads" 200 $feedback.Status
Assert-Field "Feedback page exposes content" $feedback.Content "content"
Assert-Field "Feedback page exposes totalElements" $feedback.Content "totalElements"

# A ticket is created through the public API so the admin contract can be
# exercised without depending on leftover data.
$testUser = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    email   = "contract.$stamp@example.com"
    username = "contract_$stamp"
    password = "Password1"
}
Assert-Equal "Contract user registers" 201 $testUser.Status
$userToken = $testUser.Content.accessToken

$ticket = Invoke-Api -Method POST -Path "/api/feedback" -Token $userToken -Body @{
    category = "UI"
    title    = "Contract check ticket"
    content  = "Submitted by the admin web contract test to exercise the triage route."
}
Assert-Equal "Feedback ticket is created" 201 $ticket.Status
$ticketId = $ticket.Content.id

$openTickets = Invoke-Api -Method GET -Path "/api/admin/feedback?status=OPEN&page=0&size=50" -Token $token
Assert-Equal "Open feedback filter loads" 200 $openTickets.Status
Assert-True "New ticket appears in the OPEN filter" (
    @($openTickets.Content.content | Where-Object { $_.id -eq $ticketId }).Count -eq 1)

$ticketRow = @($openTickets.Content.content | Where-Object { $_.id -eq $ticketId })[0]
foreach ($field in @("id", "userId", "userEmail", "title", "content", "category",
    "status", "createdAt", "updatedAt")) {
    Assert-Field "Feedback row exposes $field" $ticketRow $field
}
Assert-NoExplicitNull "Feedback row never sends an explicit null for adminReply" $ticketRow "adminReply"

$triaged = Invoke-Api -Method PATCH -Path "/api/admin/feedback/$ticketId" -Token $token -Body @{
    status = "RESOLVED"; adminReply = "Checked by the contract test."
}
Assert-Equal "Feedback can be triaged" 200 $triaged.Status
Assert-Equal "Ticket is RESOLVED" "RESOLVED" $triaged.Content.status
Assert-Equal "Admin reply is stored" "Checked by the contract test." $triaged.Content.adminReply

# ---------------------------------------------------------------------------
Write-Header "Audit trail (GET /api/admin/audit-logs)"

$audit = Invoke-Api -Method GET -Path "/api/admin/audit-logs?page=0&size=50" -Token $token
Assert-Equal "Audit trail loads" 200 $audit.Status
Assert-Field "Audit page exposes content" $audit.Content "content"
Assert-True "Audit trail has entries" ($audit.Content.content.Count -ge 1)

$auditRow = $audit.Content.content[0]
foreach ($field in @("id", "action", "result", "createdAt")) {
    Assert-Field "Audit row exposes $field" $auditRow $field
}
foreach ($field in @("adminId", "adminName", "targetType", "targetId", "detail", "ipAddress")) {
    Assert-NoExplicitNull "Audit row never sends an explicit null for $field" $auditRow $field
}

$actions = @($audit.Content.content | ForEach-Object { $_.action })
Assert-True "Audit trail records the category creation" ($actions -contains 'ADMIN_CREATE_CATEGORY')
Assert-True "Audit trail records the category update" ($actions -contains 'ADMIN_UPDATE_CATEGORY')
Assert-True "Audit trail records the feedback triage" ($actions -contains 'ADMIN_UPDATE_FEEDBACK')

# ---------------------------------------------------------------------------
Write-Header "System metrics (GET /api/admin/system)"

$system = Invoke-Api -Method GET -Path "/api/admin/system" -Token $token
Assert-Equal "System metrics load" 200 $system.Status
foreach ($field in @("startedAt", "uptimeSeconds", "uptimeSinceRestart",
    "unhandledErrorsSinceStartup", "jvmName", "availableProcessors")) {
    Assert-Field "System metrics expose $field" $system.Content $field
}

# ---------------------------------------------------------------------------
Write-Header "Authorisation"

$unauthenticated = Invoke-Api -Method GET -Path "/api/admin/dashboard"
Assert-Equal "Dashboard refuses an anonymous caller" 401 $unauthenticated.Status

$asUser = Invoke-Api -Method GET -Path "/api/admin/dashboard" -Token $userToken
Assert-Equal "Dashboard refuses a non-admin caller" 403 $asUser.Status

$usersRoute = Invoke-Api -Method GET -Path "/api/admin/users" -Token $userToken
Assert-Equal "User list refuses a non-admin caller" 403 $usersRoute.Status

# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
Write-Host ("  passed: {0}" -f $script:Passed)
Write-Host ("  failed: {0}" -f $script:Failed) -ForegroundColor $(if ($script:Failed) { 'Red' } else { 'Green' })

$stampFile = Get-Date -Format 'yyyyMMdd-HHmmss'
$resultsDir = Join-Path $PSScriptRoot 'results'
if (-not (Test-Path $resultsDir)) { New-Item -ItemType Directory -Path $resultsDir | Out-Null }
$csv = Join-Path $resultsDir "admin-web-contract-$stampFile.csv"
$script:Results | Export-Csv -Path $csv -NoTypeInformation -Encoding UTF8
Write-Host ("  results: {0}" -f $csv)

if ($script:Failed -gt 0) { exit 1 }