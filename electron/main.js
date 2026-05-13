const { app, BrowserWindow, dialog } = require("electron");
const { spawn } = require("child_process");
const fs = require("fs");
const http = require("http");
const net = require("net");
const path = require("path");

const DEFAULT_PORT = Number(process.env.MY_PHOTOGRAPHY_PORT || 18080);
const APP_NAME = "\u671d\u82b1\u5915\u62fe";
const SAFE_USER_AGENT =
  "ZhaoHuaXiShi/0.0.1 (Windows Desktop) Electron";

let backendProcess = null;
let mainWindow = null;

function appendDesktopLog(message) {
  try {
    const logPath = path.join(resolvePortableRoot(), "desktop.log");
    fs.appendFileSync(logPath, `${new Date().toISOString()} ${message}\n`, "utf8");
  } catch (error) {
    console.error(error);
  }
}

function appendBackendLog(message) {
  try {
    const logPath = path.join(resolvePortableRoot(), "backend.log");
    fs.appendFileSync(logPath, `${new Date().toISOString()} ${message}\n`, "utf8");
  } catch (error) {
    console.error(error);
  }
}

function normalizeForSpring(filePath) {
  return filePath.replace(/\\/g, "/");
}

function resolvePortableRoot() {
  if (app.isPackaged) {
    return path.dirname(process.execPath);
  }

  return app.getAppPath();
}

function resolveBackendJar() {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, "backend", "app.jar");
  }

  return path.join(
    app.getAppPath(),
    "target",
    "my-photography-project-0.0.1-SNAPSHOT.jar"
  );
}

function resolveJavaCommand() {
  const javaExe = process.platform === "win32" ? "java.exe" : "java";
  const packagedJava = path.join(process.resourcesPath, "runtime", "bin", javaExe);
  const devJava = path.join(app.getAppPath(), "desktop-runtime", "bin", javaExe);

  if (app.isPackaged && fs.existsSync(packagedJava)) {
    return packagedJava;
  }

  if (!app.isPackaged && fs.existsSync(devJava)) {
    return devJava;
  }

  return javaExe;
}

function isPortAvailable(port) {
  return new Promise((resolve) => {
    const server = net.createServer();

    server.once("error", () => {
      resolve(false);
    });

    server.once("listening", () => {
      server.close(() => resolve(true));
    });

    server.listen(port, "127.0.0.1");
  });
}

async function resolveAvailablePort(startPort = DEFAULT_PORT) {
  for (let port = startPort; port < startPort + 30; port++) {
    if (await isPortAvailable(port)) {
      return port;
    }
  }

  throw new Error(`No available local port found from ${startPort} to ${startPort + 29}.`);
}

function parsePropertiesFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {};
  }

  const properties = {};
  const text = fs.readFileSync(filePath, "utf8");

  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();

    if (!line || line.startsWith("#") || line.startsWith("!")) {
      continue;
    }

    const separatorIndex = line.search(/[:=]/);
    if (separatorIndex === -1) {
      continue;
    }

    const key = line.slice(0, separatorIndex).trim();
    const value = line.slice(separatorIndex + 1).trim();
    properties[key] = value;
  }

  return properties;
}

function resolveWebUploadDir() {
  const webConfigPath = resolveWebConfigPath();

  if (!webConfigPath) {
    return null;
  }

  const uploadDir = parsePropertiesFile(webConfigPath)["myapp.upload-dir"];

  if (uploadDir && !uploadDir.includes("${") && fs.existsSync(uploadDir)) {
    return uploadDir;
  }

  return null;
}

function resolveWebConfigPath() {
  const portableRoot = resolvePortableRoot();
  const candidates = [
    path.join(portableRoot, "application.properties"),
    path.join(app.getAppPath(), "application.properties"),
    path.join(app.getAppPath(), "src", "main", "resources", "application.properties")
  ];

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }

  return null;
}

function resolveDesktopRuntimePaths() {
  const portableRoot = resolvePortableRoot();
  const webConfigPath = resolveWebConfigPath();
  const webUploadDir = resolveWebUploadDir();

  return {
    dataDir: path.join(portableRoot, "data"),
    uploadDir: webUploadDir || path.join(portableRoot, "photos"),
    webConfigPath,
    usingWebUploadDir: Boolean(webUploadDir)
  };
}

function waitForBackend(port, timeoutMs = 120000) {
  const startedAt = Date.now();
  const healthUrl = `http://127.0.0.1:${port}/actuator/health`;

  return new Promise((resolve, reject) => {
    const poll = () => {
      const req = http.get(healthUrl, (res) => {
        res.resume();
        if (res.statusCode && res.statusCode >= 200 && res.statusCode < 500) {
          resolve();
          return;
        }
        retry();
      });

      req.on("error", retry);
      req.setTimeout(1500, () => {
        req.destroy();
        retry();
      });
    };

    const retry = () => {
      if (Date.now() - startedAt > timeoutMs) {
        reject(new Error("Spring Boot backend did not become ready in time."));
        return;
      }
      setTimeout(poll, 700);
    };

    poll();
  });
}

function startBackend(port) {
  const jarPath = resolveBackendJar();
  const runtimePaths = resolveDesktopRuntimePaths();

  if (!fs.existsSync(jarPath)) {
    throw new Error(
      `Backend jar not found:\n${jarPath}\n\nRun npm run desktop:jar first.`
    );
  }

  backendProcess = spawn(
    resolveJavaCommand(),
    [
      "-jar",
      jarPath,
      "--spring.profiles.active=win",
      `--server.port=${port}`,
      `--spring.datasource.url=jdbc:h2:file:${normalizeForSpring(runtimePaths.dataDir)}/my-photography;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE`,
      `--myapp.upload-dir=${normalizeForSpring(runtimePaths.uploadDir)}`,
      `--myapp.avatar-upload-dir=${normalizeForSpring(path.join(runtimePaths.uploadDir, "avatars"))}`,
      runtimePaths.webConfigPath
        ? `--myapp.web-config-path=${normalizeForSpring(runtimePaths.webConfigPath)}`
        : "--myapp.web-config-path="
    ],
    {
      cwd: path.dirname(jarPath),
      env: {
        ...process.env,
        MY_PHOTOGRAPHY_PORT: String(port),
        MY_PHOTOGRAPHY_DATA_DIR: normalizeForSpring(runtimePaths.dataDir),
        MY_PHOTOGRAPHY_UPLOAD_DIR: normalizeForSpring(runtimePaths.uploadDir)
      },
      windowsHide: true
    }
  );

  console.log(`[desktop] port: ${port}`);
  console.log(`[desktop] data dir: ${runtimePaths.dataDir}`);
  console.log(`[desktop] web config: ${runtimePaths.webConfigPath || "(not found)"}`);
  console.log(
    `[desktop] upload dir: ${runtimePaths.uploadDir}` +
      (runtimePaths.usingWebUploadDir ? " (from application.properties)" : "")
  );
  appendDesktopLog(`port: ${port}`);
  appendDesktopLog(`data dir: ${runtimePaths.dataDir}`);
  appendDesktopLog(`web config: ${runtimePaths.webConfigPath || "(not found)"}`);
  appendDesktopLog(
    `upload dir: ${runtimePaths.uploadDir}` +
      (runtimePaths.usingWebUploadDir ? " (from application.properties)" : "")
  );

  backendProcess.stdout.on("data", (data) => {
    console.log(`[backend] ${data}`);
    appendBackendLog(data.toString());
  });

  backendProcess.stderr.on("data", (data) => {
    console.error(`[backend] ${data}`);
    appendBackendLog(data.toString());
  });

  backendProcess.on("exit", (code) => {
    if (code !== 0 && mainWindow) {
      console.error(`Backend exited with code ${code}`);
    }
    appendBackendLog(`Backend exited with code ${code}`);
    backendProcess = null;
  });
}

function createMainWindow(port) {
  const iconPath = app.isPackaged
    ? path.join(process.resourcesPath, "icon.ico")
    : path.join(app.getAppPath(), "build", "icon.ico");
  const startUrl = `http://127.0.0.1:${port}`;

  mainWindow = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 1024,
    minHeight: 680,
    title: APP_NAME,
    icon: iconPath,
    autoHideMenuBar: true,
    backgroundColor: "#111111",
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  mainWindow.webContents.setUserAgent(SAFE_USER_AGENT);
  mainWindow.loadURL(startUrl, {
    userAgent: SAFE_USER_AGENT
  });

  mainWindow.on("closed", () => {
    mainWindow = null;
  });
}

function stopBackend() {
  if (backendProcess) {
    backendProcess.kill();
    backendProcess = null;
  }
}

const hasLock = app.requestSingleInstanceLock();

if (!hasLock) {
  app.quit();
} else {
  app.on("second-instance", () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) {
        mainWindow.restore();
      }
      mainWindow.focus();
    }
  });

  app.whenReady().then(async () => {
    try {
      app.setName(APP_NAME);
      const port = await resolveAvailablePort();
      startBackend(port);
      await waitForBackend(port);
      createMainWindow(port);
    } catch (error) {
      dialog.showErrorBox(`${APP_NAME} failed to start`, error.message);
      app.quit();
    }
  });

  app.on("window-all-closed", () => {
    stopBackend();
    app.quit();
  });

  app.on("before-quit", stopBackend);
}
