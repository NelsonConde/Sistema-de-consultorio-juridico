param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$backendRoot = Join-Path $repositoryRoot 'backend/app'

# The caller supplies only the synthetic LocalStack account through the environment.
foreach ($name in @('DOCUMENT_S3_ENDPOINT', 'DOCUMENT_S3_REGION',
        'DOCUMENT_S3_ACCESS_KEY', 'DOCUMENT_S3_SECRET_KEY', 'DOCUMENT_S3_BUCKET',
        'DOCUMENT_QA_BRANCH', 'DOCUMENT_S3_CONTAINER')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Missing required environment variable: $name"
    }
}
if ($env:DOCUMENT_S3_ENDPOINT -ne 'http://localhost:4566' -or
    $env:DOCUMENT_S3_REGION -ne 'us-east-1' -or
    $env:DOCUMENT_S3_BUCKET -ne 'legal-documents' -or
    $env:DOCUMENT_S3_ACCESS_KEY -ne 'test' -or
    $env:DOCUMENT_S3_SECRET_KEY -ne 'test') {
    throw 'This procedure is restricted to the agreed local synthetic S3 environment'
}

$branch = git -C $repositoryRoot branch --show-current
if ($LASTEXITCODE -ne 0 -or $branch -ne $env:DOCUMENT_QA_BRANCH) {
    throw 'The configured QA branch is not active'
}
$containerHealth = docker inspect $env:DOCUMENT_S3_CONTAINER --format '{{.State.Health.Status}}'
if ($LASTEXITCODE -ne 0 -or $containerHealth -ne 'healthy') {
    throw 'The configured S3 container is not healthy'
}
$s3Health = Invoke-RestMethod -Uri 'http://localhost:4566/_localstack/health'
if ($s3Health.services.s3 -notin @('available', 'running')) {
    throw 'LocalStack S3 is not available'
}

Push-Location $backendRoot
try {
    & java -version
    if ($LASTEXITCODE -ne 0) { throw 'Java version check failed' }
    & .\mvnw.cmd -version
    if ($LASTEXITCODE -ne 0) { throw 'Maven Wrapper version check failed' }
    & .\mvnw.cmd '-DskipTests' test-compile
    if ($LASTEXITCODE -ne 0) { throw 'test-compile failed' }
    & .\mvnw.cmd '-Dtest=DocumentBackupRestoreIT' test
    if ($LASTEXITCODE -ne 0) { throw 'Operational backup/restore test failed' }
} finally {
    Pop-Location
}
