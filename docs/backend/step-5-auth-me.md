# Paso 5 - Feature /auth/me

## Objetivo

Crear el primer endpoint funcional de autenticacion del backend:

```text
GET /api/auth/me
```

El endpoint requiere un Bearer token valido emitido por Supabase Auth.

## Flujo

1. El cliente inicia sesion o se registra en Supabase.
2. El cliente envia el access token al backend:

```text
Authorization: Bearer <supabase_access_token>
```

3. Spring Security valida firma, issuer, expiracion y audience.
4. `CurrentUserProvider` extrae claims normalizados.
5. `UserService.findOrCreateFromSupabaseClaims` busca o crea el usuario interno.
6. El backend devuelve `UserResponse`.

## Archivos creados

- `src/main/java/com/carecircle/api/auth/controller/AuthController.java`
- `src/main/java/com/carecircle/api/shared/config/OpenApiConfig.java`
- `src/main/java/com/carecircle/api/shared/exception/ApiErrorResponse.java`
- `src/main/java/com/carecircle/api/shared/exception/GlobalExceptionHandler.java`
- `src/test/java/com/carecircle/api/auth/controller/AuthControllerTests.java`

## Contrato de respuesta

```json
{
  "id": "internal-user-id",
  "supabaseUserId": "supabase-sub",
  "fullName": "Care User",
  "email": "user@example.com",
  "globalRole": "USER",
  "accountStatus": "ACTIVE",
  "emailVerified": true,
  "createdAt": "2026-06-10T19:00:00Z",
  "updatedAt": "2026-06-10T19:00:00Z",
  "lastLoginAt": "2026-06-10T19:00:00Z"
}
```

## Como probar con JWT real

Configura `.env` con valores reales de Supabase:

```text
SUPABASE_PROJECT_URL=https://<project-ref>.supabase.co
SUPABASE_ISSUER=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWKS_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_AUDIENCE=authenticated
```

Arranca:

```powershell
cd C:\carecircle-api
docker compose up -d postgres
.\scripts\run-local.ps1
```

Prueba:

```powershell
curl.exe -i `
  -H "Authorization: Bearer <supabase_access_token>" `
  http://localhost:8080/api/auth/me
```

## Resultados esperados

Sin token:

```text
401 Unauthorized
```

Con token valido:

```text
200 OK
```

Y debe existir una fila en `users` con:

- `supabase_user_id` igual al claim `sub`;
- `email` igual al claim `email`;
- `global_role = USER`;
- `account_status = ACTIVE`;
- `last_login_at` informado.

## Problemas comunes

### 401 Unauthorized

Revisar:

- el token es access token, no refresh token;
- el token no esta expirado;
- `SUPABASE_ISSUER` coincide exactamente;
- `SUPABASE_JWKS_URI` apunta al proyecto correcto;
- `SUPABASE_AUDIENCE=authenticated`;
- el proyecto Supabase usa signing keys compatibles con JWKS.

### 400 Bad Request

El JWT fue aceptado por seguridad, pero falta algun claim necesario para crear usuario interno.
El backend requiere `sub` y `email`.

### 409 Conflict

Existe conflicto de unicidad en base de datos, normalmente por email o `supabase_user_id`.
Revisar datos existentes en `users`.

## Fuera de alcance

- No hay login propio.
- No se guardan contrasenas.
- No se crean circulos.
- No se crean miembros ni roles familiares.
