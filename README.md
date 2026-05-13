# My Photography Project

A Spring Boot + Electron desktop application for managing photography journals, albums, and uploaded photos.

## Tech stack

- Java 17
- Spring Boot
- Maven Wrapper
- Electron
- Node.js

## Project structure

- `src/`: Spring Boot backend, templates, static assets, and tests
- `electron/`: Electron desktop shell entrypoint
- `scripts/`: helper scripts for desktop packaging
- `build/`: desktop packaging assets such as app icons
- `pom.xml`: backend build configuration
- `package.json`: desktop packaging configuration

## What is tracked in GitHub

This repository is intended to store source code and build configuration only.

Included:

- application source code
- frontend templates and static assets
- Electron shell code
- Maven and npm configuration
- documentation

Ignored:

- `node_modules/`
- `target/`
- `dist/`
- `desktop-runtime/`
- local logs
- uploaded runtime files under `src/main/resources/static/uploads/`
- local machine configuration such as `src/main/resources/application-local.properties`

## Backend run

Use the Maven wrapper from the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

Or package the backend first:

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target/my-photography-project-0.0.1-SNAPSHOT.jar
```

## Desktop development run

Install desktop dependencies:

```powershell
npm install
```

Start the desktop app:

```powershell
npm run desktop:dev
```

## Desktop installer build

Create the Windows installer:

```powershell
npm run desktop:dist
```

Requirements:

- Node.js
- JDK 17 or newer
- Maven
- `JAVA_HOME` configured

## Local configuration

Safe defaults are stored in `src/main/resources/application.properties`.

Do not commit machine-specific secrets. Put local overrides in:

- `src/main/resources/application-local.properties`
- environment variables

See `CONFIGURATION.md` for more details.

## Recommended GitHub upload steps

From this project directory:

```powershell
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

If the remote already exists, skip the `git remote add origin ...` step and use:

```powershell
git remote set-url origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```
