# My Photography Project

A Spring Boot + Electron desktop application for managing photography journals, albums, and uploaded photos.

## Project overview

This project started as a Spring Boot web application and was later packaged as a Windows desktop application through Electron.

It is designed for photography diary management and includes photo upload, album management, user interaction, and desktop distribution support.

## Main features

- user registration and login
- photo upload and management
- album creation and browsing
- comments, likes, and notifications
- profile management
- desktop packaging for Windows

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

## Recommended for teachers

If you only need to review the source code, browse the following directories first:

- `src/main/java/`
- `src/main/resources/templates/`
- `src/main/resources/static/`
- `electron/`

If you want to run the project locally, follow the run instructions below.

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

After the backend starts, open:

```text
http://127.0.0.1:8080
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

## Related documents

- `CONFIGURATION.md`: local configuration and secret handling
- `DESKTOP.md`: desktop runtime and Windows packaging notes

## GitHub upload reference

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
