# Local environment

## Objetivo

Mantener los valores reales de configuracion fuera de `application.yml` y fuera de Git.

## Archivos

- `.env.example`: plantilla versionable.
- `.env`: configuracion local ignorada por Git.
- `scripts/run-local.ps1`: carga `.env` en variables de entorno del proceso y arranca Spring Boot.

## Configuracion requerida

```text
SERVER_PORT=8080

DB_HOST=localhost
DB_PORT=5433
DB_NAME=carecircle
DB_USERNAME=carecircle
DB_PASSWORD=carecircle

SUPABASE_PROJECT_URL=https://your-project-ref.supabase.co
SUPABASE_ISSUER=https://your-project-ref.supabase.co/auth/v1
SUPABASE_JWKS_URI=https://your-project-ref.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_AUDIENCE=authenticated
```

## Como arrancar localmente

```powershell
cd C:\carecircle-api
docker compose up -d postgres
.\scripts\run-local.ps1
```

## Notas

`application.yml` importa `.env` con `spring.config.import=optional:file:.env[.properties]`.
Aunque el import sea optional, las variables son obligatorias porque los placeholders no tienen valores por defecto.

Docker Compose tambien carga `.env` automaticamente desde la raiz del proyecto.

El script `run-local.ps1` sigue siendo util porque carga `.env` en el proceso antes de ejecutar `mvnw.cmd spring-boot:run`.

No subir `.env` a Git.
Solo `.env.example` debe quedar versionado.
