# Paso 4 - Security con Supabase JWT

## Objetivo

Configurar Spring Security como OAuth2 Resource Server para validar JWT Bearer emitidos por Supabase Auth.
Este paso no crea todavia `/api/auth/me`.

## Decision tecnica

CareCircle valida tokens de Supabase en el backend y no implementa login propio.

La configuracion usa:

- `issuer`;
- `jwks-uri`;
- `audience`;
- converter JWT sin authorities de negocio;
- extractor de claims normalizado para el dominio `users`.

Los permisos de negocio no salen del JWT.
`globalRole` y los futuros roles de circulo se leen desde la base de datos.

## Variables de entorno

Los valores reales se configuran en `.env`.
`application.yml` no contiene defaults para estas variables.

```text
SUPABASE_PROJECT_URL=https://<project-ref>.supabase.co
SUPABASE_ISSUER=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWKS_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_AUDIENCE=authenticated
```

## JWKS y signing keys

Supabase expone JWKS para proyectos con signing keys asimetricas.
El endpoint esperado es:

```text
https://project-id.supabase.co/auth/v1/.well-known/jwks.json
```

Si el proyecto sigue usando el legacy JWT secret simetrico, esta validacion con JWKS no sera suficiente.
La recomendacion para este backend es usar signing keys asimetricas en Supabase.
El proyecto actual emite access tokens con `alg=ES256`, por lo que el decoder confia explicitamente en `SignatureAlgorithm.ES256`.

## Archivos creados

- `src/main/java/com/carecircle/api/auth/security/SupabaseJwtProperties.java`
- `src/main/java/com/carecircle/api/auth/security/SupabaseAudienceValidator.java`
- `src/main/java/com/carecircle/api/auth/security/SupabaseJwtAuthenticationConverter.java`
- `src/main/java/com/carecircle/api/auth/security/SupabaseJwtConfiguration.java`
- `src/main/java/com/carecircle/api/auth/security/SupabaseJwtClaimsExtractor.java`
- `src/main/java/com/carecircle/api/auth/security/CurrentUserProvider.java`

## Archivos modificados

- `src/main/java/com/carecircle/api/shared/config/SecurityConfig.java`

## Como probar localmente

```powershell
cd C:\carecircle-api
docker compose up -d postgres
.\mvnw.cmd clean test
.\scripts\run-local.ps1
```

Sin token:

```powershell
curl.exe -i http://localhost:8080/api/auth/me
```

Resultado esperado en este paso:

- `401 Unauthorized`;
- header `WWW-Authenticate: Bearer`.

Con token real se probara en el Paso 5, cuando exista `/api/auth/me`.

## Problemas comunes

### 401 con token real

Revisar:

- `SUPABASE_ISSUER`;
- `SUPABASE_JWKS_URI`;
- que el token no este expirado;
- que el token sea un access token, no refresh token;
- que el proyecto use signing keys compatibles con JWKS.

### Audience invalida

El backend espera `SUPABASE_AUDIENCE=authenticated`.
Si el token trae otro `aud`, ajustar la variable solo si esta justificado.

### El backend arranca pero los tokens fallan

Comprueba que `.env` tiene valores reales de Supabase.
Los placeholders de `.env.example` solo documentan el formato.

## Fuera de alcance

- No se crea `/api/auth/me`.
- No se sincroniza usuario interno todavia.
- No se crean endpoints de login.
- No se guardan contrasenas.
