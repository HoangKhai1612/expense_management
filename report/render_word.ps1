# Mo baocao.docx bang Word, cap nhat truong (muc luc, SEQ, PAGE),
# luu lai, xuat PDF de kiem tra truc quan, va bao cao so trang.
param(
    [string]$Docx = "baocao.docx",
    [switch]$Pdf
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$docxPath = Join-Path $root $Docx
if (-not (Test-Path $docxPath)) { throw "Khong tim thay $docxPath" }

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0

try {
    $doc = $word.Documents.Open($docxPath, $false, $false)

    # 1. Cap nhat truong trong toan bo tai lieu
    $null = $doc.Fields.Update()
    foreach ($story in $doc.StoryRanges) {
        $s = $story
        while ($null -ne $s) {
            $null = $s.Fields.Update()
            $s = $s.NextStoryRange
        }
    }

    # 2. Cap nhat rieng muc luc va danh muc
    foreach ($toc in $doc.TablesOfContents)  { $null = $toc.Update() }
    foreach ($tof in $doc.TablesOfFigures)   { $null = $tof.Update() }

    $doc.Repaginate()

    $pages  = $doc.ComputeStatistics(2)
    $words  = $doc.ComputeStatistics(0)
    $tables = $doc.Tables.Count
    $inl    = $doc.InlineShapes.Count
    $shp    = $doc.Shapes.Count
    $fields = $doc.Fields.Count

    # 3. Kiem tra loi hien thi cua truong
    $errs = @()
    foreach ($f in $doc.Fields) {
        $r = $f.Result.Text
        if ($r -and $r -match "Error!|Loi!|Khong tim thay") {
            $errs += $f.Code.Text.Trim() + " => " + $r
        }
    }

    $doc.Save()
    if ($Pdf) {
        $pdfPath = Join-Path $root "report\validation\baocao.pdf"
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $pdfPath) | Out-Null
        $doc.ExportAsFixedFormat($pdfPath, 17)
        Write-Output "PDF             : $pdfPath"
    }

    Write-Output "SO TRANG        : $pages"
    Write-Output "SO TU           : $words"
    Write-Output "SO BANG         : $tables"
    Write-Output "SO HINH NOI     : $inl"
    Write-Output "SO HINH TROI    : $shp"
    Write-Output "SO TRUONG       : $fields"
    Write-Output "SO TRUONG LOI   : $($errs.Count)"
    foreach ($e in $errs) { Write-Output "   $e" }

    $doc.Close($false)
}
finally {
    $word.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
}
