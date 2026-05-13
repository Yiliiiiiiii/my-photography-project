# Windows Desktop Build

This project uses Electron as a Windows desktop shell and keeps Spring Boot as the local backend.

## Runtime design

- Electron starts the Spring Boot jar in the background.
- Spring Boot runs with the `win` profile on `127.0.0.1:18080`.
- The `win` profile uses an embedded H2 file database.
- Database files are stored under the desktop executable directory: `data/`.
- Uploaded photos are stored under the desktop executable directory: `photos/`.
- If an `application.properties` file is found beside the executable, or the development Web config exists under `src/main/resources/application.properties`, the desktop app reuses its `myapp.upload-dir` value for photos.
- The desktop app never switches back to the Web MySQL database automatically; it still uses H2.
- When a Web `application.properties` is found and the desktop H2 database has no business data yet, the app attempts a one-time migration from the Web MySQL database into desktop H2.

## Development run

```powershell
npm install
npm run desktop:dev
```

`desktop:dev` builds the backend jar and opens the Electron window.

## Windows installer

```powershell
npm install
npm run desktop:dist
```

`desktop:dist` builds the Spring Boot jar, creates a small Java runtime with `jlink`, and then runs `electron-builder`.

Requirements:

- Node.js
- Maven
- JDK 17 or newer
- `JAVA_HOME` pointing to the JDK before running `desktop:dist`

## Backend-only check

```powershell
mvn clean package -DskipTests
java -jar target/my-photography-project-0.0.1-SNAPSHOT.jar --spring.profiles.active=win
```

Then open:

```text
http://127.0.0.1:18080
```

## Data location rule

The desktop shell passes runtime paths to Spring Boot when it starts the jar.

Priority:

1. Database: always `<exe directory>/data`.
2. Photos: use `myapp.upload-dir` from a detected Web `application.properties` when available.
3. Photos fallback: `<exe directory>/photos`.

Migration rule:

1. If no Web `application.properties` is found, the desktop app starts as a fresh install.
2. If a Web `application.properties` is found and desktop H2 is empty, the app reads its `spring.datasource.*` MySQL settings and copies table data into H2.
3. If desktop H2 already has users/photos/albums/comments, migration is skipped to avoid duplicate data.

This means the desktop build and the original Web project share the same code and pages, but the desktop build keeps its own H2 database after migration.
