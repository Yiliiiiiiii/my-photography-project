# 摄影日记管理系统

这是一个基于 Spring Boot 和 Electron 的摄影日记管理应用，支持以网页形式运行，也支持打包为 Windows 桌面应用。

## 项目简介

本项目最初是一个 Spring Boot Web 应用，后续通过 Electron 封装为桌面端程序。

系统主要面向摄影作品管理与展示，包含照片上传、相册管理、用户交互以及桌面端发布能力。

## 主要功能

- 用户注册、登录与权限控制
- 照片上传、编辑与管理
- 相册创建、浏览与分类展示
- 评论、点赞与通知功能
- 用户个人资料管理
- Windows 桌面端打包与运行

## 技术栈

- Java 17
- Spring Boot
- Maven Wrapper
- Electron
- Node.js

## 项目结构

- `src/`：后端源码、页面模板、静态资源与测试代码
- `electron/`：Electron 桌面端入口代码
- `scripts/`：桌面打包辅助脚本
- `build/`：桌面应用图标等资源文件
- `pom.xml`：后端 Maven 构建配置
- `package.json`：Electron 相关依赖与打包配置

## 给老师查看的建议

如果只需要查看主要源码，建议优先阅读以下目录：

- `src/main/java/`
- `src/main/resources/templates/`
- `src/main/resources/static/`
- `electron/`

如果需要运行项目，可参考下方运行说明。

## GitHub 仓库中包含的内容

本仓库主要用于保存源代码与构建配置。

会上传的内容：

- 项目源代码
- 前端模板与静态资源
- Electron 桌面端代码
- Maven 与 npm 构建配置
- 项目说明文档

不会上传的内容：

- `node_modules/`
- `target/`
- `dist/`
- `desktop-runtime/`
- 本地日志文件
- 运行时上传文件目录 `src/main/resources/static/uploads/`
- 本地私有配置，如 `src/main/resources/application-local.properties`

## 后端运行方式

在项目根目录执行：

```powershell
.\mvnw.cmd spring-boot:run
```

或者先打包再运行：

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target/my-photography-project-0.0.1-SNAPSHOT.jar
```

后端启动后，可在浏览器访问：

```text
http://127.0.0.1:8080
```

## 桌面端开发运行

先安装 Electron 相关依赖：

```powershell
npm install
```

再启动桌面应用：

```powershell
npm run desktop:dev
```

## 桌面端安装包构建

生成 Windows 安装包：

```powershell
npm run desktop:dist
```

运行与打包前建议准备好以下环境：

- Node.js
- JDK 17 或更高版本
- Maven
- 已正确配置 `JAVA_HOME`

## 本地配置

安全默认配置位于 `src/main/resources/application.properties`。

更多说明请参考 `CONFIGURATION.md`。

## 相关文档

- `CONFIGURATION.md`：本地配置与敏感信息处理说明
- `DESKTOP.md`：桌面端运行机制与 Windows 打包说明

