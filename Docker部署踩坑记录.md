# Docker 部署踩坑记录

> 项目：code2026（Vue 3 + Spring Boot 3.3.1 + MySQL + Redis + Sa-Token）
> 部署方式：Docker Compose 容器化（app + mysql + redis 三容器）
> 环境：Windows + Docker Desktop（WSL2 backend）
> 日期：2026-09

---

## 一、最终目录结构

```
code2026/
├── docker-compose.yml          # 三服务编排
├── .env                        # DEEPSEEK_API_KEY（已被 .gitignore 忽略）
├── init/
│   └── code2026.sql            # 数据库导出（11 张表）
├── files/                      # 用户上传目录，挂载到容器 /app/files
├── springboot/
│   ├── Dockerfile              # 单阶段：本地打包 + 容器运行
│   ├── .dockerignore
│   ├── pom.xml
│   └── target/*.jar            # mvn package 产物（fat jar）
└── vue/                        # 前端本地 dev server（未容器化）
```

**部署链路**

```
浏览器 → Vue dev server (宿主机 5173)
       → Spring Boot (容器 app:9090，映射宿主机 9090)
       → MySQL / Redis (容器，仅内部网络)
```

---

## 二、实际踩到的坑

### 坑 1：mysqldump 在中文路径下写入失败

**现象**

```
mysqldump: Can't create/write to file
'D:\...\??????小????????2026??目???旨?\code2026.sql'
(OS errno 2 - No such file or directory)
```

路径里的中文全部变成了 `?`。

**排查过程**

- 报错是**文件**错误而不是**连接**错误 → 说明数据库连接是通的，问题出在写文件
- 错误码 `errno 2` = 找不到文件 → 目录层面的问题
- 中文变 `?` → 编码层面的问题

**根因（两个问题叠加）**

1. **cmd 的代码页是 GBK，而 mysqldump 按另一种编码解析命令行参数** → 中文字符转换失败，变成 `?`
2. **`init` 目录不存在** —— mysqldump 只负责写文件，**不会自动创建父目录**

**解决**

- 导出到**纯英文路径**：`D:\Program Files\Projects\springBoot_projects\init\code2026.sql`
- 先手动 `mkdir` 建目录

**经验**：Windows 命令行工具遇到中文路径容易出编码问题，**构建/部署相关路径尽量用英文**。

---

### 坑 2：`cd /d` 在 PowerShell 里报错

**现象**

```
Set-Location : 找不到接受实际参数"..."的位置形式参数。
```

**根因**：`/d` 是 **cmd.exe** 的参数（用于跨盘符切换），**PowerShell 不支持**。

**解决**

| Shell | 提示符 | 切换目录写法 |
|---|---|---|
| cmd | `D:\>` | `cd /d "D:\path"` |
| PowerShell | `PS D:\>` | `cd "D:\path"` |

PowerShell 的 `cd` 本身就支持跨盘符，不需要 `/d`。

**经验**：先看提示符区分 shell，两者的命令不通用。

---

### 坑 3：`docker compose up` 报 `no configuration file provided`

**现象**

```
PS ...\code2026> docker compose up -d
no configuration file provided: not found
```

但 `docker-compose.yml` 明明存在 —— 在 `springboot/` 目录里。

**根因（两层）**

1. `docker compose` 只在**当前目录**查找配置文件，**不会进子目录找**
2. 更关键：**compose 里的相对路径以"compose 文件所在目录"为基准**。文件放在 `springboot/` 时，所有相对路径全部解析错误：

| compose 里写的 | 实际解析成 | 结果 |
|---|---|---|
| `build: ./springboot` | `springboot/springboot` | ❌ 不存在 |
| `./init/code2026.sql` | `springboot/init/...` | ❌ 不存在 |
| `./files` | `springboot/files` | ❌ 不存在 |

**解决**：把 `docker-compose.yml` 和 `.env` 移到**项目根目录**（即相对路径的基准目录）。

**经验**：compose 文件的位置决定了所有相对路径的基准，**放在项目根**是标准做法。

---

### 坑 4：Spring Boot 官方 Dockerfile 示例的版本号

**现象**：官方文档的 Dockerfile 示例第一行是

```dockerfile
FROM bellsoft/liberica-openjre-debian:24-cds
```

`24` 是 **Java 24**，而本项目是 **Java 21**。

**问题**：直接照抄会导致运行时版本与项目不一致（Java 24 的 JVM 虽然能跑 Java 21 的字节码，但不规范，被问到也说不清）。

**解决**：把两处 `FROM` 都换成 `eclipse-temurin:21-jre`。

**经验**：官方文档永远用**最新版本**做示例，**版本号必须替换成自己项目的**；只抄结构和思路。

---

## 三、提前规避的坑（设计阶段就处理了）

### 坑 5：容器里的 `localhost` 指向容器自己

**背景**：`application.yml` 里写的是

```yaml
url: jdbc:mysql://localhost:3306/code2026?...
redis:
  host: localhost
```

**为什么在容器里会失败**：**容器有独立的网络命名空间，`localhost` 指容器自己**。app 容器里没有 MySQL，所以连不上。

**解决（不改代码）**：用环境变量覆盖，把 `localhost` 改成 **compose 的服务名**

```yaml
environment:
  SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/code2026?..."
  SPRING_DATA_REDIS_HOST: redis
```

Docker Compose 内置 DNS，同一网络内的容器可以用**服务名**互相寻址。

**经验**：容器间通信用**服务名**，不用 `localhost`。这是容器网络的第一课。

---

### 坑 6：环境变量名的「连字符陷阱」

**背景**：要用环境变量覆盖 `spring.ai.openai.api-key`。

| 写法 | 结果 |
|---|---|
| `SPRING_AI_OPENAI_API_KEY` | ❌ 直觉写法，**不生效** |
| `SPRING_AI_OPENAI_APIKEY` | ✅ 正确 |

**根因**：Spring Boot 的 **Relaxed Binding** 转换规则

1. 点 `.` → 下划线 `_`
2. **连字符 `-` 直接删除**（不是替换成下划线）
3. 全部大写

所以 `api-key` → `apikey`，而不是 `api_key`。

**为什么危险**：写错**不会报错**，只是环境变量"静默失效"，直到真正调用 AI 服务时才失败 —— 排查成本极高。

**验证方法**

```powershell
docker exec code2026-app env | findstr SPRING
```

确认变量真的注入进容器了。

---

### 坑 7：容器 MySQL 版本必须 ≥ 8.0

**背景**：本机 MySQL 是 **9.7**，容器里想用 8.0，担心 dump 文件不兼容。

**排查**：在 dump 文件里搜索版本相关语法

```sql
/*!80016 DEFAULT ENCRYPTION='N' */   -- 条件注释，需 MySQL >= 8.0.16 才执行
PRIMARY KEY (`id` DESC)               -- 降序索引，需 MySQL >= 8.0
`score` double(10,1)                  -- 浮点显示宽度，8.0.17 起已弃用
```

**结论**：容器必须 **MySQL 8.0+**，`mysql:5.7` 会直接失败。

**经验**：迁移数据库前先检查 dump 里的版本相关语法；`/*!NNNNN ... */` 是 MySQL 的**条件注释**，数字是版本号（只有 ≥ 该版本才执行）。

---

### 坑 8：端口冲突 —— 本机 MySQL 占着 3306

**背景**：本机装了 MySQL 9.7 且正在运行，占用 3306 端口。

**如果 compose 里写**

```yaml
ports:
  - "3306:3306"
```

启动会报 `port is already allocated`。

**解决**：**不映射 MySQL / Redis 的端口到宿主机**。

因为 app 容器和 mysql 容器在**同一个 compose 网络**里，用服务名 `mysql:3306` 直接通信即可，**根本不需要经宿主机转发**。

（如果要在宿主机用 Navicat 连容器数据库调试，才映射到不冲突的端口，例如 `3307:3306`。）

**经验**：容器间通信走 Docker 内部网络，**只有需要从宿主机访问时才映射端口**。

---

### 坑 9：`depends_on` ≠ 依赖已就绪

**问题**：`depends_on` 只保证**启动顺序**（先启动 mysql 再启动 app），**不保证 MySQL 已经初始化完成、可以接受连接**。

MySQL 首次启动要建库、导入 188 KB 数据，耗时十几秒。这期间 app 启动会因连不上数据库而崩溃。

**解决**：给 MySQL 加 `healthcheck`，app 用 `condition: service_healthy`

```yaml
mysql:
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-uroot", "-p123456"]
    interval: 5s
    timeout: 5s
    retries: 20
    start_period: 30s

app:
  depends_on:
    mysql:
      condition: service_healthy
    redis:
      condition: service_started
```

**经验**：`depends_on` 管**顺序**，`healthcheck` 管**就绪**，两个都要。

---

### 坑 10：初始化 SQL 只在首次启动执行

**规则**：MySQL 官方镜像只在**数据目录为空**（即首次启动）时，才执行 `/docker-entrypoint-initdb.d/` 里的脚本。

**踩法**：第一次 `up` 失败后，只执行 `docker compose down`（**不带 `-v`**）再 `up` → **init 脚本不会重跑**，会误以为"SQL 没生效"，其实是被跳过了。

**解决**

```powershell
docker compose down -v     # -v 删除 volume → 数据目录清空
docker compose up -d       # 这时 init 脚本才会重新执行
```

**经验**：`down` 保留 volume（数据在），`down -v` 删除 volume（数据没，init 会重跑）。

---

## 四、只记 3 条的话

1. **容器的 `localhost` 是容器自己** → 服务间通信用服务名
2. **compose 文件的目录 = 所有相对路径的基准** → 放项目根
3. **volume 决定数据生死** → `down` 保留，`down -v` 删除

---

## 五、配套材料

| 材料 | 回答面试的哪一问 |
|---|---|
| 部署流程（Dockerfile + docker-compose.yml） | "你上线部署过吗？" |
| 本文（踩坑记录） | "遇到过什么问题？怎么定位和解决的？" |
| 数据卷持久化验证报告（文件 / 数据库两份） | "你怎么确认部署是对的？" |
| 秒杀压测记录（`seckill-press-test/压榨总结与修复记录.md`） | "高并发验证过吗？" |