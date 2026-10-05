param()

$ErrorActionPreference = 'Continue'
$scriptFailed = $false

function Get-RequiredEnvironmentValue {
    param([Parameter(Mandatory = $true)][string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Falta la variable de entorno requerida: $Name"
    }
    return $value
}

function Invoke-DocumentPhase {
    param(
        [Parameter(Mandatory = $true)][string]$Phase,
        [Parameter(Mandatory = $true)][string]$ContainerName,
        [Parameter(Mandatory = $true)][string]$ImageName,
        [Parameter(Mandatory = $true)][hashtable]$Configuration
    )

    $arguments = @(
        'run', '--rm',
        '--name', $ContainerName,
        '--network', $Configuration.Network,
        '-e', "DOCUMENT_PHASE=$Phase",
        '-e', "DOCUMENT_S3_ENDPOINT=$($Configuration.Endpoint)",
        '-e', "DOCUMENT_S3_REGION=$($Configuration.Region)",
        '-e', "DOCUMENT_S3_ACCESS_KEY=$($Configuration.AccessKey)",
        '-e', "DOCUMENT_S3_SECRET_KEY=$($Configuration.SecretKey)",
        '-e', "DOCUMENT_S3_BUCKET=$($Configuration.Bucket)",
        '-e', "DOCUMENT_SESSION_ID=$($Configuration.SessionId)",
        $ImageName
    )

    & docker @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "La fase $Phase termino con codigo $LASTEXITCODE"
    }
}

function Test-ContainerAbsent {
    param([Parameter(Mandatory = $true)][string]$ContainerName)

    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        $containerId = docker ps -a --filter "name=^/$ContainerName$" --format '{{.ID}}'
        if ([string]::IsNullOrWhiteSpace(($containerId -join ''))) {
            return $true
        }
        Start-Sleep -Milliseconds 250
    }
    return $false
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$backendRoot = Join-Path $repositoryRoot 'backend/app'
$sessionId = [Guid]::NewGuid().ToString('N')
$shortSession = $sessionId.Substring(0, 12)
$imageName = "document-qa-doc-16-runner:$shortSession"
$containerA = "document-doc16-write-$shortSession"
$containerB = "document-doc16-verify-$shortSession"
$cleanupContainer = "document-doc16-cleanup-$shortSession"
$temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) "document-qa-doc-16-$sessionId"
$resolvedTempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$resolvedTemporaryRoot = [IO.Path]::GetFullPath($temporaryRoot)

if (!$resolvedTemporaryRoot.StartsWith($resolvedTempBase, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'El contexto temporal no esta dentro del directorio temporal esperado'
}

$configuration = @{
    Network = Get-RequiredEnvironmentValue 'DOCUMENT_DOCKER_NETWORK'
    Endpoint = Get-RequiredEnvironmentValue 'DOCUMENT_S3_ENDPOINT'
    Region = Get-RequiredEnvironmentValue 'DOCUMENT_S3_REGION'
    AccessKey = Get-RequiredEnvironmentValue 'DOCUMENT_S3_ACCESS_KEY'
    SecretKey = Get-RequiredEnvironmentValue 'DOCUMENT_S3_SECRET_KEY'
    Bucket = Get-RequiredEnvironmentValue 'DOCUMENT_S3_BUCKET'
    SessionId = $sessionId
}

$localStackContainer = Get-RequiredEnvironmentValue 'DOCUMENT_S3_CONTAINER'
$localStackStatus = docker inspect $localStackContainer --format '{{.State.Health.Status}}'
if ($LASTEXITCODE -ne 0 -or $localStackStatus -ne 'healthy') {
    throw 'El contenedor S3 configurado no esta healthy'
}

$networkName = docker network inspect $configuration.Network --format '{{.Name}}'
if ($LASTEXITCODE -ne 0 -or $networkName -ne $configuration.Network) {
    throw 'No existe la red Docker configurada'
}

try {
    New-Item -ItemType Directory -Path $temporaryRoot | Out-Null
    Copy-Item -LiteralPath (Join-Path $backendRoot '.mvn') -Destination $temporaryRoot -Recurse
    Copy-Item -LiteralPath (Join-Path $backendRoot 'mvnw') -Destination $temporaryRoot
    Copy-Item -LiteralPath (Join-Path $backendRoot 'pom.xml') -Destination $temporaryRoot
    New-Item -ItemType Directory -Path (Join-Path $temporaryRoot 'src/main') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $temporaryRoot 'src/test') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $backendRoot 'src/main/java') `
        -Destination (Join-Path $temporaryRoot 'src/main') -Recurse
    Copy-Item -LiteralPath (Join-Path $backendRoot 'src/test/java') `
        -Destination (Join-Path $temporaryRoot 'src/test') -Recurse

    $dockerfile = @'
FROM eclipse-temurin:21-jdk
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -DskipTests test-compile
ENTRYPOINT ["./mvnw", "-q", "-Dtest=DocumentContainerPersistenceIT", "test"]
'@
    Set-Content -LiteralPath (Join-Path $temporaryRoot 'Dockerfile') `
        -Value $dockerfile -Encoding Ascii

    docker build --tag $imageName --file (Join-Path $temporaryRoot 'Dockerfile') $temporaryRoot
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo construir la imagen operacional; codigo $LASTEXITCODE"
    }

    Write-Output "QA_DOC_16_ORCHESTRATOR session=$sessionId containerA=$containerA"
    Invoke-DocumentPhase -Phase 'WRITE' -ContainerName $containerA `
        -ImageName $imageName -Configuration $configuration

    if (!(Test-ContainerAbsent -ContainerName $containerA)) {
        throw "El contenedor A $containerA no fue destruido"
    }
    Write-Output "QA_DOC_16_ORCHESTRATOR containerA=$containerA destroyed=true"

    $recoveryTimer = [Diagnostics.Stopwatch]::StartNew()
    Invoke-DocumentPhase -Phase 'VERIFY' -ContainerName $containerB `
        -ImageName $imageName -Configuration $configuration
    $recoveryTimer.Stop()

    if (!(Test-ContainerAbsent -ContainerName $containerB)) {
        throw "El contenedor B $containerB no fue destruido al terminar"
    }
    Write-Output "QA_DOC_16_ORCHESTRATOR containerB=$containerB newContainer=true recoveryMilliseconds=$($recoveryTimer.ElapsedMilliseconds)"

    Invoke-DocumentPhase -Phase 'CLEANUP' -ContainerName $cleanupContainer `
        -ImageName $imageName -Configuration $configuration
    if (!(Test-ContainerAbsent -ContainerName $cleanupContainer)) {
        throw "El contenedor de cleanup $cleanupContainer no fue destruido"
    }

    Write-Output "QA_DOC_16_RESULT status=PASS session=$sessionId containerA=$containerA containerADestroyed=true containerB=$containerB newContainer=true recoveryMilliseconds=$($recoveryTimer.ElapsedMilliseconds) cleanup=true"
} catch {
    [Console]::Error.WriteLine("QA_DOC_16_ERROR $($_.Exception.Message)")
    $scriptFailed = $true
} finally {
    docker image inspect $imageName 1>$null 2>$null
    if ($LASTEXITCODE -eq 0) {
        docker image rm $imageName 1>$null 2>$null
    }
    if (Test-Path -LiteralPath $resolvedTemporaryRoot) {
        Remove-Item -LiteralPath $resolvedTemporaryRoot -Recurse -Force
    }
}

if ($scriptFailed) {
    exit 1
}
