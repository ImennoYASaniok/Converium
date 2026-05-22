param(
  [Parameter(Mandatory = $true)]
  [string]$MsgFile
)

$Msg = Get-Content -Raw -Path $MsgFile

# Format: type(module): description
# type: fix|feat|refactor|perf|docs|test|chore|build|ci
# module: backend|frontend|db|infra|deps|config|core|multi
$Regex = '^(fix|feat|refactor|perf|docs|test|chore|build|ci)\((backend|frontend|db|infra|deps|config|core|multi)\): .+$'

if ($Msg -match $Regex) {
  exit 0
}

Write-Error "Invalid commit message format. Expected: type(module): description"
Write-Error "Example: feat(frontend): добавил страницу входа в систему"
exit 1
