# Spring Boot 项目 - 毕设脚手架 2026

这是一个基于Spring Boot的全栈开发项目，包含后端API服务和前端Vue界面。

## 项目结构

- `springboot/` - 后端Spring Boot应用程序
- `vue/` - 前端Vue.js应用程序

## 技术栈

### 后端技术
- Spring Boot 3.3.1
- MyBatis
- MySQL
- Maven

### 前端技术
- Vue.js
- Element Plus
- Vite

## 环境要求

- Java 17+
- Node.js
- MySQL 8.0+

## 配置说明

项目使用环境变量来管理敏感信息。请在部署时设置以下环境变量：

- `DB_USERNAME` - 数据库用户名
- `DB_PASSWORD` - 数据库密码
- `DB_URL` - 数据库连接URL

或者，在运行环境中创建相应的配置文件覆盖默认配置。

## 项目启动

### 后端启动

1. 导入数据库表结构
2. 配置数据库连接信息
3. 运行Spring Boot应用

### 前端启动

1. 进入`vue/`目录
2. 安装依赖：`npm install`
3. 启动开发服务器：`npm run dev`

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。