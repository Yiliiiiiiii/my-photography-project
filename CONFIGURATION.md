# Configuration Notes

## Safe defaults

`src/main/resources/application.properties` now contains only safe defaults.
Secrets and machine-specific paths should come from one of these places:

1. Environment variables.
2. An untracked `src/main/resources/application-local.properties` file.

## Recommended local setup

1. Copy `src/main/resources/application-local.example.properties` to `src/main/resources/application-local.properties`.
2. Replace the placeholder database password and API key.
3. Adjust `myapp.upload-dir` to a path that exists on your machine.

## Environment variables

The main properties file supports these variables:

- `SERVER_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `DB_PASSWORD` (legacy alias, still supported)
- `SPRING_DATASOURCE_DRIVER_CLASS_NAME`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`
- `SPRING_JPA_SHOW_SQL`
- `SPRING_JPA_FORMAT_SQL`
- `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE`
- `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE`
- `MYAPP_UPLOAD_DIR`
- `UPLOAD_DIR` (legacy alias, still supported)
- `MYAPP_AVATAR_UPLOAD_DIR`
- `ALIBABA_API_KEY`

