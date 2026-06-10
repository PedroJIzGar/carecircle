# Paso 3 - User domain

## Objetivo

Crear el dominio Java minimo para usuarios internos sincronizados desde Supabase Auth.
Este paso no valida JWT y no expone endpoints.

## Decision tecnica

`UserService` recibe `SupabaseUserClaims` como DTO normalizado, no un `Jwt` de Spring Security.
Esto mantiene el modulo `users` separado de la infraestructura de seguridad.

En el Paso 4, el modulo de seguridad sera responsable de:

- validar el JWT de Supabase;
- extraer `sub`, `email`, nombre, avatar y estado de email;
- construir `SupabaseUserClaims`;
- llamar a `UserService`.

## Archivos creados

- `src/main/java/com/carecircle/api/auth/dto/SupabaseUserClaims.java`
- `src/main/java/com/carecircle/api/users/entity/User.java`
- `src/main/java/com/carecircle/api/users/repository/UserRepository.java`
- `src/main/java/com/carecircle/api/users/dto/UserResponse.java`
- `src/main/java/com/carecircle/api/users/mapper/UserMapper.java`
- `src/main/java/com/carecircle/api/users/service/UserService.java`

## Responsabilidades

### User

Entidad JPA que representa el usuario interno de CareCircle.
No guarda contrasenas ni material de autenticacion.

### UserRepository

Acceso a persistencia por:

- `id` interno;
- `supabaseUserId`;
- `email`.

### UserResponse

DTO seguro para exponer el usuario interno en endpoints autenticados.

### UserMapper

Mapper MapStruct para convertir entidad a DTO.

### UserService

Contiene `findOrCreateFromSupabaseClaims`.

Comportamiento esperado:

- valida que existan `supabaseUserId` y `email`;
- busca usuario por `supabaseUserId`;
- si no existe, crea usuario interno;
- sincroniza email, nombre, avatar y email verified;
- actualiza `lastLoginAt`;
- devuelve `UserResponse`.

## Reglas importantes

- `globalRole` es rol global de aplicacion.
- Roles familiares como `MAIN_CAREGIVER`, `COLLABORATOR` y `OBSERVER` pertenecen a `CircleMember`.
- Supabase Auth es la fuente de identidad.
- CareCircle no guarda passwords.
- El servicio asume que el JWT ya fue validado antes de recibir claims.

## Como probar

```powershell
cd C:\carecircle-api
docker compose up -d postgres
.\mvnw.cmd clean test
```

Resultado esperado:

- MapStruct genera `UserMapperImpl`.
- Hibernate valida la entidad `User` contra la tabla `users`.
- Maven termina con `BUILD SUCCESS`.

## Fuera de alcance

- No se implementa `/api/auth/me`.
- No se configura OAuth2 Resource Server JWT todavia.
- No se leen tokens reales de Supabase todavia.
- No se crean circulos ni miembros.
