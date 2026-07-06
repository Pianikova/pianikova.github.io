<#
.SYNOPSIS
    Updates Require-Bundle version range upper bounds in plugin manifests
    to match bundle versions from a p2 repository.

.DESCRIPTION
    Scans all bundles/*/META-INF/MANIFEST.MF and tests/*/META-INF/MANIFEST.MF,
    compares every Require-Bundle version range against the bundle versions found
    in <RepoPath>\plugins. If a bundle version in the repository is not covered by
    the range's upper bound, the upper bound is raised to the next major version
    (e.g. repository version 18.0.0 -> upper bound 19.0.0).

    Bundles that are not present in the repository (own project bundles, bundles
    coming from other p2 repositories) are left untouched. The dependency set is
    discovered from the manifests on every run, so it may change freely.

.PARAMETER RepoPath
    Path to the p2 repository (a directory containing a 'plugins' subfolder),
    e.g. D:\Downloads\repo

.PARAMETER ProjectRoot
    Root of the project. Defaults to the directory containing this script.

.PARAMETER DryRun
    Report what would change without modifying any files.

.EXAMPLE
    .\upgrade_deps.ps1 -RepoPath D:\Downloads\repo
    .\upgrade_deps.ps1 D:\Downloads\repo -DryRun
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string] $RepoPath,

    [string] $ProjectRoot,

    [switch] $DryRun
)

$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
}

$pluginsDir = Join-Path $RepoPath 'plugins'
if (-not (Test-Path $pluginsDir)) {
    throw "Plugins folder not found: $pluginsDir"
}

function Parse-OsgiVersion {
    param([string] $Value)

    $parts = $Value.Split('.')
    $major = [int]$parts[0]
    $minor = 0
    $micro = 0
    if ($parts.Length -gt 1 -and $parts[1] -match '^\d+$') { $minor = [int]$parts[1] }
    if ($parts.Length -gt 2 -and $parts[2] -match '^\d+$') { $micro = [int]$parts[2] }
    return [version]::new($major, $minor, $micro)
}

# 1. Collect bundle versions from the repository (max version per symbolic name).
$repoVersions = @{}
Get-ChildItem $pluginsDir -Filter '*.jar' | ForEach-Object {
    if ($_.BaseName -match '^(.+?)_(\d+\.\d+\.\d+(\.[\w\-]+)?)$') {
        $name = $Matches[1]
        $version = Parse-OsgiVersion $Matches[2]
        if (-not $repoVersions.ContainsKey($name) -or $repoVersions[$name] -lt $version) {
            $repoVersions[$name] = $version
        }
    }
}
Write-Host "Repository bundles found: $($repoVersions.Count)"

# 2. Discover project manifests.
$manifests = @()
foreach ($group in 'bundles', 'tests') {
    $groupDir = Join-Path $ProjectRoot $group
    if (-not (Test-Path $groupDir)) { continue }
    Get-ChildItem $groupDir -Directory | ForEach-Object {
        $manifest = Join-Path $_.FullName 'META-INF\MANIFEST.MF'
        if (Test-Path $manifest) { $manifests += $manifest }
    }
}
if (-not $manifests) {
    throw "No MANIFEST.MF files found under $ProjectRoot\bundles or $ProjectRoot\tests"
}

# 3. Check and update ranges.
$changes = @()
$warnings = @()

foreach ($manifest in $manifests) {
    $module = Split-Path (Split-Path (Split-Path $manifest -Parent) -Parent) -Leaf
    $raw = [System.IO.File]::ReadAllText($manifest)
    $updated = $raw

    # Unfold manifest continuation lines for parsing only; edits target the raw text.
    $unfolded = $raw -replace "`r`n ", '' -replace "`n ", ''
    $requireLine = ($unfolded -split "`r?`n") |
        Where-Object { $_ -match '^Require-Bundle:' } |
        Select-Object -First 1
    if (-not $requireLine) { continue }

    $entries = [regex]::Split(
        ($requireLine -replace '^Require-Bundle:\s*', ''),
        ',(?=(?:[^"]*"[^"]*")*[^"]*$)') # commas outside quotes

    foreach ($entry in $entries) {
        $entry = $entry.Trim()
        if ($entry -notmatch '^([\w\.\-]+)') { continue }
        $bundle = $Matches[1]

        # Only closed ranges have an upper bound to maintain.
        if ($entry -notmatch 'bundle-version="([\[\(])\s*([\d\.]+)\s*,\s*([\d\.]+)\s*([\)\]])"') { continue }
        $lowBracket = $Matches[1]
        $low = $Matches[2]
        $high = $Matches[3]
        $highBracket = $Matches[4]

        $repoVersion = $repoVersions[$bundle]
        if ($null -eq $repoVersion) { continue } # not in this repository, managed elsewhere

        if ($repoVersion -lt (Parse-OsgiVersion $low)) {
            $warnings += "${module}: $bundle repository version $repoVersion is below lower bound $low - check manually"
            continue
        }

        $highVersion = Parse-OsgiVersion $high
        $covered = if ($highBracket -eq ']') { $repoVersion -le $highVersion } else { $repoVersion -lt $highVersion }
        if ($covered) { continue }

        # Raise the upper bound to the next major of the repository version, exclusive.
        $newHigh = '{0}.0.0' -f ($repoVersion.Major + 1)
        $oldAttribute = 'bundle-version="{0}{1},{2}{3}"' -f $lowBracket, $low, $high, $highBracket
        $newAttribute = 'bundle-version="{0}{1},{2})"' -f $lowBracket, $low, $newHigh

        # Replace only within this bundle's entry; tolerate a manifest line fold
        # between the bundle name and the attribute.
        $pattern = '(?<=' + [regex]::Escape($bundle) + ';(\r?\n )?)' + [regex]::Escape($oldAttribute)
        $replaced = [regex]::Replace($updated, $pattern, { param($m) $newAttribute })
        if ($replaced -eq $updated) {
            $warnings += "${module}: could not locate '$bundle;$oldAttribute' in raw manifest (line folding?) - update manually"
            continue
        }
        $updated = $replaced

        $changes += [pscustomobject]@{
            Module      = $module
            Bundle      = $bundle
            OldRange    = '{0}{1},{2}{3}' -f $lowBracket, $low, $high, $highBracket
            NewRange    = '{0}{1},{2})' -f $lowBracket, $low, $newHigh
            RepoVersion = $repoVersion.ToString()
        }
    }

    if ($updated -ne $raw -and -not $DryRun) {
        [System.IO.File]::WriteAllText($manifest, $updated, $utf8NoBom)
    }
}

# 4. Report.
if ($changes) {
    $action = if ($DryRun) { 'Ranges to update (dry run)' } else { 'Updated ranges' }
    Write-Host ''
    Write-Host "=== $action ==="
    $changes | Format-Table Module, Bundle, OldRange, NewRange, RepoVersion -AutoSize |
        Out-String -Width 250 | Write-Host
} else {
    Write-Host 'All version ranges already cover the repository versions - nothing to update.'
}

if ($warnings) {
    Write-Host '=== Warnings ==='
    $warnings | ForEach-Object { Write-Warning $_ }
    exit 1
}
