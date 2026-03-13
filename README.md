## 问题溯源系统 - 后端（track-backend）

`track-backend` 是 **问题溯源系统** 的后端服务，基于 **Spring Boot 2.7** 与 **MyBatis-Plus** 实现，主要用于：

- 管理和加密保存多种数据源配置（MySQL、Oracle、OceanBase、Redis 等）
- 提供统一的 SQL 执行与结果查询接口
- 提供元数据（库 / 表 / 字段）查询能力，辅助问题排查与分析
- 提供系统用户与登录认证能力，保障接口安全

前端项目为 `track-frontend`，两者配合组成完整的问题溯源平台。

---

## 目录结构概览

仅列出关键目录和文件，完整结构请参见代码仓库：

- `src/main/java/com/track`
  - `TrackApplication`：Spring Boot 启动类
  - `controller`：对外 REST 接口层
  - `service` / `service.impl`：业务逻辑实现
  - `mapper`：MyBatis-Plus Mapper 接口
  - `entity` / `dto`：实体与传输对象
  - `config`：CORS、拦截器、Web 配置等
  - `common`：统一返回结果、全局异常处理、鉴权等
  - `util`：数据源工具、加解密工具等
- `src/main/resources`
  - `application.yml`：通用配置
  - `application-dev.yml` / `application-test.yml` / `application-prod.yml`：环境配置
  - `schema.sql`：初始化数据库表结构及默认数据
  - `log4j2-spring.xml`：日志配置
- `scripts/start.sh`：Linux 启动脚本（瘦 JAR + `lib/` 依赖方式）
- `pom.xml`：Maven 项目配置

数据库设计说明见仓库根目录 `doc/db设计.md`。

---

## 技术栈

- **语言**：Java 8
- **框架**：
  - Spring Boot 2.7.18
  - Spring Web、Spring JDBC
  - MyBatis-Plus 3.5.x
- **数据源与驱动**：
  - MySQL（`mysql-connector-j`）
  - Oracle（`ojdbc8` + `orai18n`）
  - OceanBase（`oceanbase-client`）
  - Redis（`jedis`，用于特定场景）
- **连接池**：HikariCP
- **日志**：Log4j2
- **构建工具**：Maven

---

## 环境要求

- JDK 8+
- Maven 3.6+
- 可访问的数据库实例（至少一个 MySQL/Oracle/OceanBase，用于系统库）
- （可选）Redis 服务

---

## 本地开发与运行

### 1. 克隆代码

```bash
git clone <your-github-repo-url>.git
cd track-backend
```

### 2. 配置数据库

1. 在目标数据库中新建一个库（例如 `track_db`）。
2. 执行初始化脚本：

```sql
-- 在目标库中执行
SOURCE src/main/resources/schema.sql;
```

> 该脚本会创建 `datasource_config` / `sys_user` / `common_sql` 等表，并插入默认管理员账号 `admin/admin`（MD5 存储）。

### 3. 配置应用参数

根据实际环境修改 `src/main/resources` 下的配置文件：

- `application.yml`：通用配置
- `application-dev.yml`：开发环境专用配置（如数据库连接、端口号）

典型需要关注的内容包括：

- 数据库连接地址、用户名、密码
- 日志输出路径
- CORS 与鉴权相关参数

### 4. 启动应用（开发模式）

使用 Maven 直接运行：

```bash
mvn spring-boot:run
```

或使用 IDE（IntelliJ IDEA / Eclipse）运行 `com.track.TrackApplication` 的 `main` 方法。

默认情况下，应用会运行在 `http://localhost:8080`，可配合前端 `track-frontend` 进行联调。

---

## 构建与部署

本项目使用 **瘦 JAR + 外部 lib 目录** 的部署方式：

### 1. 打包

```bash
mvn clean package
```

执行完成后，`target` 目录中会生成：

- `track-backend-1.0.0.jar`：应用主 JAR（不包含依赖）
- `lib/`：运行时依赖包集合（由 `maven-dependency-plugin` 复制）

### 2. 部署到服务器

将以下文件/目录上传至服务器同一目录（例如 `/opt/track-backend`）：

- `track-backend-1.0.0.jar`
- `lib/` 目录
- `scripts/start.sh`（建议赋予可执行权限）
- `application-*.yml`（如有需要可按环境覆盖部署）

### 3. Linux 启动脚本

在服务器上执行：

```bash
cd /opt/track-backend
chmod +x scripts/start.sh
./scripts/start.sh
```

脚本会使用如下方式启动：

- 使用 `java -cp "track-backend-1.0.0.jar:lib/*" com.track.TrackApplication`
- 以 `nohup` 后台方式运行，日志输出到 `nohup.out`

停止服务时，可通过 `ps -ef | grep TrackApplication` 查找进程并 `kill` 对应 PID。

---

## 主要功能说明（简要）

- **数据源管理**
  - 通过 `datasource_config` 表管理多种类型的数据源
  - 支持启用/禁用、额外连接参数等
- **动态 SQL 执行**
  - 根据配置的数据源执行 SQL，返回统一格式的数据结构
  - 支持常用 SQL 模板的保存、分类与复用（`common_sql` 表）
- **元数据查询**
  - 查询库、表、字段信息，辅助分析和问题排查
- **用户与权限**
  - `sys_user` 表管理系统用户与角色（ADMIN/USER）
  - 基于 Token 的登录鉴权（由前后端配合完成）

> 详细接口定义可通过查看 `controller` 包中各个 `Controller` 类注释与方法签名获得。

---

## 与前端联调

- 前端项目：`track-frontend`
- 建议在前端开发配置中将后端代理为 `http://localhost:8080` 或实际后端地址
- 生产环境中可通过 Nginx 反向代理，将 `/api` 等前缀转发到本服务

前端打包与发布流程详见仓库根目录 `doc/前端升级步骤.md`。

---

## 许可证

根据你计划开源的协议进行补充，例如：

- 若使用 MIT 协议，请在仓库根目录添加 `LICENSE` 文件，并在此处说明。

