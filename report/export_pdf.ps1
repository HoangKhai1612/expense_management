param(
    [string]$Docx = "baocao.docx",
    [string]$Pdf = "report-validation\baocao.pdf"
)
$ErrorActionPreference = "Stop"
$root = "C:\Users\Administrator\AndroidStudioProjects\expense_management"
$src = Join-Path $root $Docx
$dst = Join-Path $root $Pdf
New-Item -ItemType Directory -Force -Path (Split-Path $dst) | Out-Null
if (Test-Path $dst) { Remove-Item $dst -Force }

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $doc = $word.Documents.Open($src, $false, $true)
    # Thu cap nhat truong truoc, roi luu xuat PDF qua SaveAs2 (17 = wdFormatPDF).
    $doc.SaveAs2($dst, 17)
    Write-Output "PDF: $dst"
    $doc.Close($false)
} finally {
    $word.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
}
if (Test-Path $dst) {
    Write-Output ("Kich thuoc PDF: {0:N0} byte" -f (Get-Item $dst).Length)
} else {
    Write-Output "PDF khong duoc tao"
}
