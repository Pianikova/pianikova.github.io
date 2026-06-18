param(
    [Parameter(Position = 0)]
    [string] $OldVersion,

    [Parameter(Position = 1)]
    [string] $NewVersion
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OldVersion) -or [string]::IsNullOrWhiteSpace($NewVersion)) {
    throw "Usage: version.cmd <old_version> <new_version>"
}

if ($OldVersion -notmatch '^\d+\.\d+\.\d+$') {
    throw "Old version must match N.N.N, got '$OldVersion'."
}

if ($NewVersion -notmatch '^\d+\.\d+\.\d+$') {
    throw "New version must match N.N.N, got '$NewVersion'."
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootPrefix = (Get-Item -LiteralPath $root).FullName.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

function Get-RelativePath {
    param([string] $Path)

    $fullPath = (Get-Item -LiteralPath $Path).FullName
    if ($fullPath.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $fullPath.Substring($rootPrefix.Length)
    }

    return $fullPath
}

function Test-ExcludedPath {
    param([string] $Path)

    $relativePath = Get-RelativePath $Path
    $parts = $relativePath -split '[\\/]'
    return $parts -contains ".git" -or
        $parts -contains ".metadata" -or
        $parts -contains ".idea" -or
        $parts -contains "target"
}

function Update-File {
    param(
        [System.IO.FileInfo] $File,
        [scriptblock] $Update
    )

    if (Test-ExcludedPath $File.FullName) {
        return $null
    }

    $oldContent = [System.IO.File]::ReadAllText($File.FullName)
    $newContent = & $Update $oldContent

    if ($newContent -ne $oldContent) {
        [System.IO.File]::WriteAllText($File.FullName, $newContent, $utf8NoBom)
        return Get-RelativePath $File.FullName
    }

    return $null
}

$oldEscaped = [regex]::Escape($OldVersion)
$oldSnapshotEscaped = [regex]::Escape("$OldVersion-SNAPSHOT")
$oldQualifierEscaped = [regex]::Escape("$OldVersion.qualifier")
$oldLicenseEscaped = [regex]::Escape("$OldVersion.v1")

$changedFiles = New-Object System.Collections.Generic.List[string]

Get-ChildItem -Path $root -Recurse -File -Filter "pom.xml" |
    ForEach-Object {
        $changed = Update-File $_ {
            param([string] $content)

            $content = [regex]::Replace(
                $content,
                "(<version>\s*)$oldSnapshotEscaped(\s*</version>)",
                "`${1}$NewVersion-SNAPSHOT`${2}")

            return [regex]::Replace(
                $content,
                "(<version>\s*)$oldLicenseEscaped(\s*</version>)",
                "`${1}$NewVersion.v1`${2}")
        }

        if ($null -ne $changed) {
            $changedFiles.Add($changed)
        }
    }

Get-ChildItem -Path $root -Recurse -File -Filter "feature.xml" |
    ForEach-Object {
        $changed = Update-File $_ {
            param([string] $content)

            $content = [regex]::Replace(
                $content,
                "(version\s*=\s*"")$oldQualifierEscaped("")",
                "`${1}$NewVersion.qualifier`${2}")

            $content = [regex]::Replace(
                $content,
                "(version\s*=\s*"")$oldLicenseEscaped("")",
                "`${1}$NewVersion.v1`${2}")

            $content = [regex]::Replace(
                $content,
                "(license-feature-version\s*=\s*"")$oldLicenseEscaped("")",
                "`${1}$NewVersion.v1`${2}")

            return [regex]::Replace(
                $content,
                "(<import\b[^>]*\bversion\s*=\s*"")$oldEscaped("")",
                "`${1}$NewVersion`${2}")
        }

        if ($null -ne $changed) {
            $changedFiles.Add($changed)
        }
    }

Get-ChildItem -Path $root -Recurse -File -Filter "MANIFEST.MF" |
    ForEach-Object {
        $changed = Update-File $_ {
            param([string] $content)

            $content = [regex]::Replace(
                $content,
                "(Bundle-Version:\s*)$oldQualifierEscaped",
                "`${1}$NewVersion.qualifier")

            $content = [regex]::Replace(
                $content,
                "(bundle-version\s*=\s*""\[)$oldEscaped(,)",
                "`${1}$NewVersion`${2}")

            $content = [regex]::Replace(
                $content,
                "(bundle-version\s*=\s*"")$oldEscaped("")",
                "`${1}$NewVersion`${2}")

            return [regex]::Replace(
                $content,
                "(;version\s*=\s*"")$oldEscaped("")",
                "`${1}$NewVersion`${2}")
        }

        if ($null -ne $changed) {
            $changedFiles.Add($changed)
        }
    }

if ($changedFiles.Count -eq 0) {
    Write-Host "No files changed."
    exit 0
}

Write-Host "Updated version $OldVersion -> $NewVersion in $($changedFiles.Count) file(s):"
$changedFiles | Sort-Object | ForEach-Object {
    Write-Host "  $_"
}
