<#
.SYNOPSIS
    End-to-end and negative API tests for the Personal Finance AI System.

.DESCRIPTION
    Runs against a live backend (default http://localhost:8081) and asserts real
    HTTP status codes and response bodies. Every check prints PASS or FAIL with the
    observed value, so the output is evidence rather than a summary claim.

    Covers the five E2E flows from the project plan plus the negative test matrix:
      Flow 1  Register -> Login -> Income -> Expense -> Dashboard -> Statistics
      Flow 2  Login -> Create budget -> Expense -> usage changes -> threshold alert
      Flow 3  Login -> Ask AI -> grounded answer, no fabrication on empty data
      Flow 4  User feedback -> Admin triage -> User sees updated status
      Flow 5  Admin lock -> user login rejected -> unlock -> login works
    Negative: wrong password, duplicate email, weak password, bad email, empty and
    negative amount, invalid date, unknown ids, unauthorised access, cross-user
    access, normal user calling an admin route, malformed and forged JWT.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tests\e2e-api-tests.ps1
#>

param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$AdminEmail = "admin@finai.local",
    [string]$AdminPassword = "Admin#12345"
)

$ErrorActionPreference = 'Stop'
$script:Results = New-Object System.Collections.Generic.List[object]
$script:Passed = 0
$script:Failed = 0

function Write-Header($text) {
    Write-Host ""
    Write-Host "=== $text ===" -ForegroundColor Cyan
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
    $ok = ($Expected -eq $Actual)
    Assert-True -Name $Name -Condition $ok -Detail ("expected={0} actual={1}" -f $Expected, $Actual)
}

function ConvertTo-Object {
    # Invoke-WebRequest returns a byte[] for non-text MIME types such as
    # application/vnd.spring-boot.actuator.v3+json. Decode before parsing,
    # otherwise ConvertFrom-Json yields bytes and every field reads as null.
    param($Raw)
    if ($null -eq $Raw) { return $null }
    $text = $Raw
    if ($Raw -is [byte[]]) {
        $text = [System.Text.Encoding]::UTF8.GetString($Raw)
    }
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
    if ($null -ne $Body) {
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }
    try {
        $response = Invoke-WebRequest @params
        $content = ConvertTo-Object $response.Content
        return [pscustomobject]@{ Status = [int]$response.StatusCode; Content = $content }
    } catch {
        $status = 0
        $content = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $text = $reader.ReadToEnd()
                if ($text) { $content = ConvertTo-Object $text }
            } catch { }
        }
        return [pscustomobject]@{ Status = $status; Content = $content; Error = $_.Exception.Message }
    }
}

function New-Stamp { return (Get-Date).ToString("HHmmssfff") }

# ---------------------------------------------------------------------------
Write-Header "Preconditions"
$health = Invoke-Api -Method GET -Path "/actuator/health"
Assert-Equal "Backend health is UP" 200 $health.Status
Assert-Equal "Health status field" "UP" $health.Content.status

$stamp = New-Stamp
$userAEmail = "e2e.a.$stamp@example.com"
$userAUsername = "e2e_a_$stamp"
$userBEmail = "e2e.b.$stamp@example.com"
$userBUsername = "e2e_b_$stamp"
$password = "Passw0rd!23"

# ---------------------------------------------------------------------------
Write-Header "Flow 1 - Register, login, income, expense, dashboard, statistics"

# Negative: duplicate email is rejected before the user can rely on the account.
$first = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    email = $userAEmail; username = $userAUsername; password = $password; fullName = "E2E User A"
}
Assert-Equal "Register user A" 201 $first.Status
Assert-True "Register returns a bearer token" ($first.Content.accessToken -and $first.Content.accessToken.Length -gt 20)
$tokenA = $first.Content.accessToken
$userAId = $first.Content.user.id

$duplicate = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    email = $userAEmail; username = "other_$userAUsername"; password = $password
}
Assert-Equal "Duplicate email is rejected" 409 $duplicate.Status
Assert-Equal "Duplicate email error code" "EMAIL_ALREADY_EXISTS" $duplicate.Content.code

$dupUser = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    email = "e2e.dup.$stamp@example.com"; username = $userAUsername; password = $password
}
Assert-Equal "Duplicate username is rejected" 409 $dupUser.Status
Assert-Equal "Duplicate username error code" "USERNAME_ALREADY_EXISTS" $dupUser.Content.code

$weak = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    email = "e2e.weak.$stamp@example.com"; username = "weak_$stamp"; password = "short"
}
Assert-Equal "Weak password is rejected" 400 $weak.Status
Assert-Equal "Weak password error code" "VALIDATION_ERROR" $weak.Content.code

$badEmail = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    email = "not-an-email"; username = "bademail_$stamp"; password = $password
}
Assert-Equal "Malformed email is rejected" 400 $badEmail.Status

$login = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{
    identifier = $userAEmail; password = $password
}
Assert-Equal "Login with email" 200 $login.Status
$tokenA = $login.Content.accessToken

$loginByName = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{
    identifier = $userAUsername; password = $password
}
Assert-Equal "Login with username" 200 $loginByName.Status

$wrongPassword = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{
    identifier = $userAEmail; password = "WrongPassw0rd!"
}
Assert-Equal "Wrong password is rejected" 401 $wrongPassword.Status
Assert-Equal "Wrong password error code" "INVALID_CREDENTIALS" $wrongPassword.Content.code

$unknownUser = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{
    identifier = "nobody.$stamp@example.com"; password = $password
}
Assert-Equal "Unknown account is rejected" 401 $unknownUser.Status

$categories = Invoke-Api -Method GET -Path "/api/categories" -Token $tokenA
Assert-Equal "List categories" 200 $categories.Status
Assert-True "System categories are returned" ($categories.Content.Count -ge 10) ("count=" + $categories.Content.Count)
$expenseCat = ($categories.Content | Where-Object { $_.type -eq 'EXPENSE' -and $_.code -eq 'FOOD' })[0]
$incomeCat = ($categories.Content | Where-Object { $_.type -eq 'INCOME' -and $_.code -eq 'SALARY' })[0]
Assert-True "FOOD expense category available" ($null -ne $expenseCat)
Assert-True "SALARY income category available" ($null -ne $incomeCat)

$today = (Get-Date).ToString("yyyy-MM-dd")
$income = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $incomeCat.id; type = "INCOME"; amount = 15000000; note = "Monthly salary"; transactionDate = $today
}
Assert-Equal "Create income transaction" 201 $income.Status
Assert-Equal "Income amount echoed" 15000000 ([double]$income.Content.amount)

$expense = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; type = "EXPENSE"; amount = 2400000; note = "Groceries"; transactionDate = $today
}
Assert-Equal "Create expense transaction" 201 $expense.Status
$expenseId = $expense.Content.id

$dashboard = Invoke-Api -Method GET -Path "/api/dashboard" -Token $tokenA
Assert-Equal "Dashboard loads" 200 $dashboard.Status
Assert-Equal "Dashboard month income" 15000000 ([double]$dashboard.Content.monthIncome)
Assert-Equal "Dashboard month expense" 2400000 ([double]$dashboard.Content.monthExpense)
Assert-Equal "Dashboard month balance" 12600000 ([double]$dashboard.Content.monthBalance)
Assert-True "Dashboard lists recent transactions" ($dashboard.Content.recentTransactions.Count -ge 2)

$overview = Invoke-Api -Method GET -Path "/api/statistics/overview" -Token $tokenA
Assert-Equal "Statistics overview loads" 200 $overview.Status
Assert-Equal "Overview income matches transaction" 15000000 ([double]$overview.Content.totalIncome)

$byCategory = Invoke-Api -Method GET -Path "/api/statistics/by-category?type=EXPENSE" -Token $tokenA
Assert-Equal "Category breakdown loads" 200 $byCategory.Status
Assert-True "Category breakdown is non-empty" ($byCategory.Content.Count -ge 1)

$monthly = Invoke-Api -Method GET -Path "/api/statistics/monthly" -Token $tokenA
Assert-Equal "Monthly series loads" 200 $monthly.Status
Assert-Equal "Monthly series has 12 points" 12 $monthly.Content.Count

# ---------------------------------------------------------------------------
Write-Header "Negative - transaction validation"

$negativeAmount = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; type = "EXPENSE"; amount = -500; transactionDate = $today
}
Assert-Equal "Negative amount is rejected" 400 $negativeAmount.Status
Assert-Equal "Negative amount error code" "AMOUNT_MUST_BE_POSITIVE" $negativeAmount.Content.code

$zeroAmount = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; type = "EXPENSE"; amount = 0; transactionDate = $today
}
Assert-Equal "Zero amount is rejected" 400 $zeroAmount.Status

$emptyAmount = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; type = "EXPENSE"; transactionDate = $today
}
Assert-Equal "Missing amount is rejected" 400 $emptyAmount.Status

$badDate = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; type = "EXPENSE"; amount = 1000; transactionDate = "not-a-date"
}
Assert-Equal "Malformed date is rejected" 400 $badDate.Status

$futureDate = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; type = "EXPENSE"; amount = 1000; transactionDate = "2099-01-01"
}
Assert-Equal "Future date is rejected" 400 $futureDate.Status
Assert-Equal "Future date error code" "DATE_IN_FUTURE" $futureDate.Content.code

$mismatch = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $incomeCat.id; type = "EXPENSE"; amount = 1000; transactionDate = $today
}
Assert-Equal "Category type mismatch is rejected" 400 $mismatch.Status
Assert-Equal "Category mismatch error code" "CATEGORY_TYPE_MISMATCH" $mismatch.Content.code

$unknownCategory = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = 999999; type = "EXPENSE"; amount = 1000; transactionDate = $today
}
Assert-Equal "Unknown category is rejected" 400 $unknownCategory.Status

# ---------------------------------------------------------------------------
Write-Header "Negative - authentication and authorisation"

$noToken = Invoke-Api -Method GET -Path "/api/transactions"
Assert-Equal "No token is rejected" 401 $noToken.Status

$garbageToken = Invoke-Api -Method GET -Path "/api/transactions" -Token "not.a.real.token"
Assert-Equal "Malformed token is rejected" 401 $garbageToken.Status

# A structurally valid token signed with the wrong key must not be accepted.
$forged = Invoke-Api -Method GET -Path "/api/transactions" -Token `
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaXNzIjoxNTE2MjM5MDIyfQ.WrongSignatureValueThatIsLongEnough1234567890"
Assert-Equal "Forged token is rejected" 401 $forged.Status

$adminAsUser = Invoke-Api -Method GET -Path "/api/admin/users" -Token $tokenA
Assert-Equal "Normal user cannot call an admin route" 403 $adminAsUser.Status

# ---------------------------------------------------------------------------
Write-Header "Flow 5 (part 1) - cross-user isolation"

$userB = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    email = $userBEmail; username = $userBUsername; password = $password; fullName = "E2E User B"
}
Assert-Equal "Register user B" 201 $userB.Status
$tokenB = $userB.Content.accessToken

$bReadsA = Invoke-Api -Method GET -Path "/api/transactions/$expenseId" -Token $tokenB
Assert-Equal "User B cannot read user A's transaction" 404 $bReadsA.Status
Assert-Equal "Cross-user read error code" "TRANSACTION_NOT_FOUND" $bReadsA.Content.code

$bUpdatesA = Invoke-Api -Method PUT -Path "/api/transactions/$expenseId" -Token $tokenB -Body @{
    categoryId = $expenseCat.id; type = "EXPENSE"; amount = 1; transactionDate = $today
}
Assert-Equal "User B cannot update user A's transaction" 404 $bUpdatesA.Status

$bDeletesA = Invoke-Api -Method DELETE -Path "/api/transactions/$expenseId" -Token $tokenB
Assert-Equal "User B cannot delete user A's transaction" 404 $bDeletesA.Status

$bList = Invoke-Api -Method GET -Path "/api/transactions" -Token $tokenB
Assert-Equal "User B transaction list is empty" 0 $bList.Content.totalElements

$bDashboard = Invoke-Api -Method GET -Path "/api/dashboard" -Token $tokenB
Assert-Equal "User B dashboard month income is zero" 0 ([double]$bDashboard.Content.monthIncome)

# ---------------------------------------------------------------------------
Write-Header "Flow 3 (part 1) - AI refuses to answer without data"

$aiEmpty = Invoke-Api -Method POST -Path "/api/ai/chat" -Token $tokenB -Body @{
    message = "This month where did I spend the most?"
}
Assert-Equal "AI responds to a user with no data" 200 $aiEmpty.Status
Assert-True "AI refuses to invent data" ($aiEmpty.Content.grounded -eq $false) `
    ("grounded=" + $aiEmpty.Content.grounded)
Assert-True "AI states it lacks data" `
    ($aiEmpty.Content.answer -match "not have enough data|không có đủ dữ liệu") `
    ("answer=" + ($aiEmpty.Content.answer -replace "`n", " "))

# ---------------------------------------------------------------------------
Write-Header "Flow 2 - budget usage and threshold alert"

$transportCat = ($categories.Content | Where-Object { $_.code -eq 'TRANSPORT' })[0]
$monthStart = (Get-Date).ToString("yyyy-MM-01")

$budget = Invoke-Api -Method POST -Path "/api/budgets" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; amount = 3000000; periodType = "MONTHLY"; periodStart = $monthStart
}
Assert-Equal "Create budget" 201 $budget.Status
$budgetId = $budget.Content.id
# The 2,400,000 FOOD expense from Flow 1 already falls inside this period, so the
# budget must report it immediately rather than starting from a stale zero.
Assert-Equal "Budget usage reflects existing transactions at creation" 80 ([double]$budget.Content.usagePercentage)
Assert-Equal "Budget status reflects usage at creation" "WARNING" $budget.Content.status

# A budget on a category with no spending proves the SAFE branch and the 0% baseline.
$transportBudget = Invoke-Api -Method POST -Path "/api/budgets" -Token $tokenA -Body @{
    categoryId = $transportCat.id; amount = 1000000; periodType = "MONTHLY"; periodStart = $monthStart
}
Assert-Equal "Create a second budget on an untouched category" 201 $transportBudget.Status
Assert-Equal "Budget usage is zero when nothing has been spent" 0 ([double]$transportBudget.Content.usagePercentage)
Assert-Equal "Budget status starts SAFE" "SAFE" $transportBudget.Content.status
Assert-Equal "Budget remaining equals the limit when unused" 1000000 ([double]$transportBudget.Content.remainingAmount)

$dupeBudget = Invoke-Api -Method POST -Path "/api/budgets" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; amount = 5000000; periodType = "MONTHLY"; periodStart = $monthStart
}
Assert-Equal "Duplicate budget period is rejected" 409 $dupeBudget.Status
Assert-Equal "Duplicate budget error code" "BUDGET_ALREADY_EXISTS" $dupeBudget.Content.code

$incomeBudget = Invoke-Api -Method POST -Path "/api/budgets" -Token $tokenA -Body @{
    categoryId = $incomeCat.id; amount = 1000000; periodType = "MONTHLY"; periodStart = $monthStart
}
Assert-Equal "Budget on an INCOME category is rejected" 400 $incomeBudget.Status

# 2,400,000 of 3,000,000 = 80.00% -> exactly the configured warning threshold.
$currentBudgets = Invoke-Api -Method GET -Path "/api/budgets/current" -Token $tokenA
Assert-Equal "Budget usage recomputed from transactions" 80 ([double]$currentBudgets.Content[0].usagePercentage)
Assert-Equal "Budget status becomes WARNING at the threshold" "WARNING" $currentBudgets.Content[0].status
Assert-Equal "Budget remaining amount" 600000 ([double]$currentBudgets.Content[0].remainingAmount)

$unread = Invoke-Api -Method GET -Path "/api/notifications/unread" -Token $tokenA
Assert-True "Warning notification was raised" ($unread.Content.Count -ge 1) ("count=" + $unread.Content.Count)
$alert = $unread.Content[0]
Assert-Equal "Notification is a BUDGET_WARNING" "BUDGET_WARNING" $alert.type
Assert-Equal "Notification level is WARNING" "WARNING" $alert.level

# Re-evaluating must not spam duplicates.
$null = Invoke-Api -Method GET -Path "/api/budgets/current" -Token $tokenA
$null = Invoke-Api -Method GET -Path "/api/budgets/current" -Token $tokenA
$unreadAgain = Invoke-Api -Method GET -Path "/api/notifications/unread" -Token $tokenA
# @() forces an array so .Count is defined even when a single item is returned.
$warningCount = @($unreadAgain.Content | Where-Object { $_.type -eq 'BUDGET_WARNING' }).Count
Assert-Equal "Warning alert is not duplicated" 1 $warningCount

# Cross the exceeded threshold: push to 3,600,000 of 3,000,000 = 120%.
$over = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; type = "EXPENSE"; amount = 1200000; note = "Bulk purchase"; transactionDate = $today
}
Assert-Equal "Add expense that exceeds the budget" 201 $over.Status

$afterExceed = Invoke-Api -Method GET -Path "/api/budgets/current" -Token $tokenA
Assert-Equal "Budget usage exceeds 100 percent" 120 ([double]$afterExceed.Content[0].usagePercentage)
Assert-Equal "Budget status becomes EXCEEDED" "EXCEEDED" $afterExceed.Content[0].status
Assert-Equal "Budget remaining is negative" -600000 ([double]$afterExceed.Content[0].remainingAmount)

$unreadExceed = Invoke-Api -Method GET -Path "/api/notifications/unread" -Token $tokenA
$exceededCount = @($unreadExceed.Content | Where-Object { $_.type -eq 'BUDGET_EXCEEDED' }).Count
Assert-Equal "Critical notification was raised" 1 $exceededCount

# ---------------------------------------------------------------------------
Write-Header "Flow 3 (part 2) - AI grounded in real data"

$aiSpending = Invoke-Api -Method POST -Path "/api/ai/chat" -Token $tokenA -Body @{
    message = "This month, where did I spend the most?"
}
Assert-Equal "AI answers the spending question" 200 $aiSpending.Status
Assert-True "AI answer is grounded in data" ($aiSpending.Content.grounded -eq $true)
Assert-True "AI answer contains a FACT section" ($aiSpending.Content.answer -match "FACT")
Assert-True "AI answer contains a SUGGESTION section" ($aiSpending.Content.answer -match "SUGGESTION")
Assert-True "AI cites the actual top category" ($aiSpending.Content.answer -match "Food") `
    ("answer=" + ($aiSpending.Content.answer -replace "`n", " "))
Assert-True "AI persisted the verified facts" ($aiSpending.Content.facts.Count -ge 3)
Assert-Equal "AI detected the spending intent" "SPENDING_ANALYSIS" $aiSpending.Content.intent

# The AI's persisted facts must carry the exact stored total, not a formatted echo.
# Comparing against the `facts` list avoids locale/format ambiguity entirely.
$actualExpense = [double](Invoke-Api -Method GET -Path "/api/statistics/overview" -Token $tokenA).Content.totalExpense
$expenseFact = @($aiSpending.Content.facts | Where-Object { $_ -like "totalExpense=*" })
Assert-True "AI recorded a totalExpense fact" ($expenseFact.Count -ge 1) `
    ("facts=" + ($aiSpending.Content.facts -join ", "))
Assert-True "AI expense fact equals the stored expense total" `
    ($expenseFact[0] -eq ("totalExpense=" + $actualExpense.ToString("0.00"))) `
    ("aiFact=" + $expenseFact[0] + " dbTotalExpense=" + $actualExpense.ToString("0.00"))

$topFact = @($aiSpending.Content.facts | Where-Object { $_ -like "topCategory=*" })
Assert-True "AI recorded the top category fact" ($topFact.Count -ge 1)

$aiBudget = Invoke-Api -Method POST -Path "/api/ai/chat" -Token $tokenA -Body @{
    message = "Am I over budget this month?"
}
Assert-Equal "AI answers the budget question" 200 $aiBudget.Status
Assert-True "AI confirms the budget breach from data" ($aiBudget.Content.answer -match "EXCEEDED|VƯỢT") `
    ("answer=" + ($aiBudget.Content.answer -replace "`n", " "))
Assert-Equal "AI detected the budget intent" "BUDGET_STATUS" $aiBudget.Content.intent

$aiCompare = Invoke-Api -Method POST -Path "/api/ai/chat" -Token $tokenA -Body @{
    message = "How does this month compare with last month?"
}
Assert-Equal "AI answers the comparison question" 200 $aiCompare.Status
Assert-True "AI refuses to compare with an empty previous month" `
    ($aiCompare.Content.grounded -eq $false) `
    ("grounded=" + $aiCompare.Content.grounded)

$aiUnknown = Invoke-Api -Method POST -Path "/api/ai/chat" -Token $tokenA -Body @{
    message = "What is the airspeed velocity of an unladen swallow?"
}
Assert-Equal "AI handles an off-topic question" 200 $aiUnknown.Status
Assert-True "Off-topic question is not classified" `
    ($aiUnknown.Content.intent -eq "UNRECOGNISED") `
    ("intent=" + $aiUnknown.Content.intent)

$emptyQuestion = Invoke-Api -Method POST -Path "/api/ai/chat" -Token $tokenA -Body @{ message = "   " }
Assert-Equal "Empty AI question is rejected" 400 $emptyQuestion.Status

$conversationId = $aiSpending.Content.conversationId
$transcript = Invoke-Api -Method GET -Path "/api/ai/conversations/$conversationId/messages" -Token $tokenA
Assert-Equal "AI transcript is readable" 200 $transcript.Status
Assert-True "Transcript has both turns" ($transcript.Content.Count -ge 2) ("messages=" + $transcript.Content.Count)

$bReadsAConversation = Invoke-Api -Method GET -Path "/api/ai/conversations/$conversationId/messages" -Token $tokenB
Assert-Equal "User B cannot read user A's AI transcript" 404 $bReadsAConversation.Status

# ---------------------------------------------------------------------------
Write-Header "Flow 4 - feedback round trip"

$feedback = Invoke-Api -Method POST -Path "/api/feedback" -Token $tokenA -Body @{
    title = "Chart labels are hard to read"; content = "The monthly bar chart labels overlap on a small screen."; category = "UI"
}
Assert-Equal "User submits feedback" 201 $feedback.Status
$feedbackId = $feedback.Content.id
Assert-Equal "New feedback starts OPEN" "OPEN" $feedback.Content.status

$shortFeedback = Invoke-Api -Method POST -Path "/api/feedback" -Token $tokenA -Body @{
    title = "x"; content = "short"; category = "BUG"
}
Assert-Equal "Too-short feedback is rejected" 400 $shortFeedback.Status

$adminLogin = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{
    identifier = $AdminEmail; password = $AdminPassword
}
Assert-Equal "Admin login" 200 $adminLogin.Status
$tokenAdmin = $adminLogin.Content.accessToken
Assert-Equal "Admin role is assigned" "ADMIN" $adminLogin.Content.user.role

# Audit rows persist across runs, so measure a delta rather than an absolute count.
$auditBaseline = Invoke-Api -Method GET -Path "/api/admin/audit-logs?size=100" -Token $tokenAdmin
$auditCountBefore = $auditBaseline.Content.totalElements

$adminFeedback = Invoke-Api -Method GET -Path "/api/admin/feedback?status=OPEN" -Token $tokenAdmin
Assert-Equal "Admin lists open feedback" 200 $adminFeedback.Status
Assert-True "Submitted feedback appears for admin" ($adminFeedback.Content.totalElements -ge 1)

$triaged = Invoke-Api -Method PATCH -Path "/api/admin/feedback/$feedbackId" -Token $tokenAdmin -Body @{
    status = "RESOLVED"; adminReply = "Fixed in the next release."
}
Assert-Equal "Admin updates feedback status" 200 $triaged.Status
Assert-Equal "Feedback is RESOLVED" "RESOLVED" $triaged.Content.status
Assert-Equal "Admin reply is stored" "Fixed in the next release." $triaged.Content.adminReply

$userSees = Invoke-Api -Method GET -Path "/api/feedback/$feedbackId" -Token $tokenA
Assert-Equal "User sees the updated status" "RESOLVED" $userSees.Content.status
Assert-Equal "User sees the admin reply" "Fixed in the next release." $userSees.Content.adminReply

$bReadsAFeedback = Invoke-Api -Method GET -Path "/api/feedback/$feedbackId" -Token $tokenB
Assert-Equal "User B cannot read user A's feedback" 404 $bReadsAFeedback.Status

# ---------------------------------------------------------------------------
Write-Header "Admin dashboard and category management"

$adminDashboard = Invoke-Api -Method GET -Path "/api/admin/dashboard" -Token $tokenAdmin
Assert-Equal "Admin dashboard loads" 200 $adminDashboard.Status
Assert-True "Dashboard reports total users" ($adminDashboard.Content.totalUsers -ge 3) `
    ("totalUsers=" + $adminDashboard.Content.totalUsers)
Assert-True "Dashboard reports transactions" ($adminDashboard.Content.totalTransactions -ge 3) `
    ("transactions=" + $adminDashboard.Content.totalTransactions)
Assert-True "Dashboard reports AI questions" ($adminDashboard.Content.ai.totalQuestions -ge 4) `
    ("aiQuestions=" + $adminDashboard.Content.ai.totalQuestions)
Assert-True "Dashboard labels runtime counters as since-restart" `
    ($adminDashboard.Content.runtime.uptimeSinceRestart -eq $true)

$adminCategories = Invoke-Api -Method GET -Path "/api/admin/categories" -Token $tokenAdmin
Assert-Equal "Admin lists system categories" 200 $adminCategories.Status
Assert-True "System categories present" ($adminCategories.Content.Count -ge 10)

$catCode = "PETS_$stamp"
$newCategory = Invoke-Api -Method POST -Path "/api/admin/categories" -Token $tokenAdmin -Body @{
    name = "Pets"; code = $catCode; type = "EXPENSE"; icon = "pets"; color = "#8BC34A"
}
Assert-Equal "Admin creates a system category" 200 $newCategory.Status
$newCategoryId = $newCategory.Content.id

$dupeCategory = Invoke-Api -Method POST -Path "/api/admin/categories" -Token $tokenAdmin -Body @{
    name = "Pets again"; code = $catCode; type = "EXPENSE"
}
Assert-Equal "Duplicate system category code is rejected" 409 $dupeCategory.Status

$userTriesAdminCategory = Invoke-Api -Method POST -Path "/api/admin/categories" -Token $tokenA -Body @{
    name = "Sneaky"; code = "SNEAKY"; type = "EXPENSE"
}
Assert-Equal "Normal user cannot create a system category" 403 $userTriesAdminCategory.Status

# An admin deactivating a system category must not silently rewrite user history.
$deactivated = Invoke-Api -Method PATCH -Path "/api/admin/categories/$newCategoryId" -Token $tokenAdmin -Body @{
    active = $false
}
Assert-Equal "Admin deactivates a system category" 200 $deactivated.Status
Assert-Equal "Category is now inactive" $false $deactivated.Content.active

$userView = Invoke-Api -Method GET -Path "/api/categories" -Token $tokenA
$visible = ($userView.Content | Where-Object { $_.id -eq $newCategoryId }).Count
Assert-Equal "Inactive system category is hidden from users" 0 $visible

$existingHistory = Invoke-Api -Method GET -Path "/api/transactions/$expenseId" -Token $tokenA
Assert-Equal "Existing transactions survive category deactivation" 200 $existingHistory.Status

$audit = Invoke-Api -Method GET -Path "/api/admin/audit-logs?size=100" -Token $tokenAdmin
Assert-Equal "Audit trail is readable" 200 $audit.Status
# Audit rows persist between runs, so compare the delta against the baseline taken
# before the feedback triage. Three privileged mutations happen in between:
# feedback triage, category create and category deactivate.
$auditDelta = $audit.Content.totalElements - $auditCountBefore
Assert-Equal "Audit trail recorded the three admin mutations so far" 3 $auditDelta
$actions = @($audit.Content.content | ForEach-Object { $_.action })
Assert-True "Audit trail contains the feedback action" ($actions -contains 'ADMIN_UPDATE_FEEDBACK') `
    ("actions=" + ($actions -join ", "))
Assert-True "Audit trail contains the category create action" ($actions -contains 'ADMIN_CREATE_CATEGORY')
Assert-True "Audit trail contains the category update action" ($actions -contains 'ADMIN_UPDATE_CATEGORY')

# ---------------------------------------------------------------------------
Write-Header "Flow 5 (part 2) - lock, login rejected, unlock, login restored"

$lock = Invoke-Api -Method PATCH -Path "/api/admin/users/$userAId/status" -Token $tokenAdmin -Body @{
    status = "LOCKED"; reason = "E2E test lock"
}
Assert-Equal "Admin locks the user" 200 $lock.Status
Assert-Equal "User status is LOCKED" "LOCKED" $lock.Content.status

$lockedLogin = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{
    identifier = $userAEmail; password = $password
}
Assert-Equal "Locked user cannot log in" 403 $lockedLogin.Status
Assert-Equal "Locked login error code" "ACCOUNT_LOCKED" $lockedLogin.Content.code

# A token minted before the lock must stop working immediately.
$staleTokenCall = Invoke-Api -Method GET -Path "/api/transactions" -Token $tokenA
Assert-Equal "Pre-lock token is rejected after locking" 401 $staleTokenCall.Status

$selfLock = Invoke-Api -Method PATCH -Path "/api/admin/users/$($adminLogin.Content.user.id)/status" -Token $tokenAdmin -Body @{
    status = "LOCKED"
}
Assert-Equal "Admin cannot lock their own account" 400 $selfLock.Status
Assert-Equal "Self-lock error code" "CANNOT_MODIFY_SELF" $selfLock.Content.code

$unlock = Invoke-Api -Method PATCH -Path "/api/admin/users/$userAId/status" -Token $tokenAdmin -Body @{
    status = "ACTIVE"
}
Assert-Equal "Admin unlocks the user" 200 $unlock.Status
Assert-Equal "User status is ACTIVE again" "ACTIVE" $unlock.Content.status

$auditAfterLock = Invoke-Api -Method GET -Path "/api/admin/audit-logs" -Token $tokenAdmin
$lockActions = @($auditAfterLock.Content.content | ForEach-Object { $_.action })
Assert-True "Lock action was audited" ($lockActions -contains 'ADMIN_LOCK_USER') `
    ("actions=" + ($lockActions -join ", "))
Assert-True "Unlock action was audited" ($lockActions -contains 'ADMIN_UNLOCK_USER')
Assert-True "Refused self-lock was audited as a denial" ($lockActions -contains 'ADMIN_DENIED')

$relogin = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{
    identifier = $userAEmail; password = $password
}
Assert-Equal "Unlocked user can log in again" 200 $relogin.Status
$tokenA = $relogin.Content.accessToken

# ---------------------------------------------------------------------------
Write-Header "Edit and delete transaction"

$update = Invoke-Api -Method PUT -Path "/api/transactions/$expenseId" -Token $tokenA -Body @{
    categoryId = $expenseCat.id; type = "EXPENSE"; amount = 2500000; note = "Groceries corrected"; transactionDate = $today
}
Assert-Equal "Transaction is updated" 200 $update.Status
Assert-Equal "Updated amount is stored" 2500000 ([double]$update.Content.amount)
Assert-Equal "Updated note is stored" "Groceries corrected" $update.Content.note

$toDelete = Invoke-Api -Method POST -Path "/api/transactions" -Token $tokenA -Body @{
    categoryId = $transportCat.id; type = "EXPENSE"; amount = 45000; note = "Taxi"; transactionDate = $today
}
Assert-Equal "Create a transaction to delete" 201 $toDelete.Status
$deleteId = $toDelete.Content.id

$delete = Invoke-Api -Method DELETE -Path "/api/transactions/$deleteId" -Token $tokenA
Assert-Equal "Transaction is deleted" 204 $delete.Status

$deleted = Invoke-Api -Method GET -Path "/api/transactions/$deleteId" -Token $tokenA
Assert-Equal "Deleted transaction is gone" 404 $deleted.Status

$deleteUnknown = Invoke-Api -Method DELETE -Path "/api/transactions/999999" -Token $tokenA
Assert-Equal "Deleting an unknown id returns 404" 404 $deleteUnknown.Status

# ---------------------------------------------------------------------------
Write-Header "Empty state and profile"

$profile = Invoke-Api -Method GET -Path "/api/users/me" -Token $tokenA
Assert-Equal "Profile loads" 200 $profile.Status
Assert-Equal "Profile email matches" $userAEmail $profile.Content.email

$profileUpdate = Invoke-Api -Method PATCH -Path "/api/users/me" -Token $tokenA -Body @{
    fullName = "Renamed User"; phone = "+84 901234567"
}
Assert-Equal "Profile update succeeds" 200 $profileUpdate.Status
Assert-Equal "Full name is updated" "Renamed User" $profileUpdate.Content.fullName

$wrongCurrent = Invoke-Api -Method POST -Path "/api/users/me/password" -Token $tokenA -Body @{
    currentPassword = "WrongPassw0rd!"; newPassword = "NewPassw0rd!1"
}
Assert-Equal "Changing password with a wrong current one is rejected" 400 $wrongCurrent.Status

$unknownRoute = Invoke-Api -Method GET -Path "/api/does-not-exist" -Token $tokenA
Assert-True "Unknown route returns 404" ($unknownRoute.Status -eq 404) ("status=" + $unknownRoute.Status)

$openapi = Invoke-Api -Method GET -Path "/v3/api-docs"
Assert-Equal "OpenAPI document is served" 200 $openapi.Status

# ---------------------------------------------------------------------------
Write-Header "Summary"
Write-Host ("  passed: {0}" -f $script:Passed) -ForegroundColor Green
Write-Host ("  failed: {0}" -f $script:Failed) -ForegroundColor $(if ($script:Failed -gt 0) { 'Red' } else { 'Green' })

$resultsPath = Join-Path $PSScriptRoot 'results'
New-Item -ItemType Directory -Force -Path $resultsPath | Out-Null
$stampFile = Get-Date -Format 'yyyyMMdd-HHmmss'
$csv = Join-Path $resultsPath "e2e-$stampFile.csv"
$script:Results | Export-Csv -Path $csv -NoTypeInformation -Encoding utf8
Write-Host "  results: $csv"

if ($script:Failed -gt 0) {
    exit 1
}
exit 0
