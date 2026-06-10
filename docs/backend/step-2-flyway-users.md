# Paso 2 - Flyway y tabla users

## Objetivo

Crear la primera migracion real del backend sin implementar todavia autenticacion ni endpoints.
La aplicacion debe arrancar con Flyway y JPA en modo `validate`.

## Contexto usado

Documentos revisados:

- `CareCircle MVP v0.1.pdf`
- `CareCircle - Modelo de Datos MVP.pdf`
- `CareCircle Roadmap.pdf`

Decisiones tomadas desde esos documentos:

- Supabase Auth es la fuente de identidad.
- CareCircle mantiene un usuario interno propio para permisos y datos de negocio.
- `globalRole` solo representa permisos globales de aplicacion.
- Roles familiares como `MAIN_CAREGIVER`, `COLLABORATOR` y `OBSERVER` pertenecen a miembros de un circulo.
- Consentimientos y documentos legales deben ser versionados.
- Auditoria MVP debe ser simple y orientada a trazabilidad, no a copiar datos privados.

## Archivos modificados

- `src/main/resources/application.yml`
- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/java/com/carecircle/api/users/entity/GlobalRole.java`
- `src/main/java/com/carecircle/api/users/entity/AccountStatus.java`

## Esquema creado

### users

Tabla interna de usuarios sincronizados desde Supabase Auth.

Campos clave:

- `id`
- `supabase_user_id`
- `email`
- `full_name`
- `global_role`
- `account_status`
- `email_verified`
- aceptaciones legales iniciales
- timestamps de creacion, actualizacion y ultimo login

### legal_documents

Tabla para documentos legales versionados:

- terminos
- politica de privacidad
- aviso no medico
- consentimiento de compania
- consentimiento para compartir datos con entidades

### consent_records

Tabla de aceptaciones de usuario sobre documentos legales concretos.
Guarda `legal_document_id` para saber que version exacta fue aceptada.

### audit_logs

Auditoria basica para acciones sensibles del MVP.
El campo `metadata` debe contener solo datos minimos.

## Como probar

```powershell
cd C:\carecircle-api
docker compose up -d postgres
.\mvnw.cmd clean test
```

Resultado esperado:

- Flyway aplica `V1__init_schema.sql`.
- JPA arranca con `ddl-auto: validate`.
- Maven termina con `BUILD SUCCESS`.

## Problemas comunes

### El puerto 5432 falla

Este proyecto usa PostgreSQL en `localhost:5433` porque hay otra instancia local en `5432`.

### V1 ya fue aplicada y se modifica despues

No editar migraciones ya aplicadas en entornos compartidos.
Durante desarrollo local se puede recrear la base si no hay datos importantes.
En cuanto el proyecto este compartido, cualquier cambio nuevo debe ir en `V2__...`.

### JPA validate falla

La entidad Java no coincide con el esquema SQL.
Corregir la entidad o crear una nueva migracion, segun el caso.

## Fuera de alcance

- No se implementa `/auth/me`.
- No se valida JWT todavia.
- No se crean circulos ni miembros.
- No se crean tareas, citas, medicacion ni check-ins.
