# czh & hsj · 情侣 H5

个人情侣网页应用（类 Suki）：访客展示模式 + 双人密码正常模式。  
功能：纪念日、相册、问答、恋爱挑战（100 件小事 / 70 城市 / 100 票根）。

## 技术栈

- 后端：Spring Boot 3.3 + MyBatis-Plus + JWT + H2（默认）/ MySQL
- 前端：Vue 3 + Vite + Pinia + Vue Router
- 部署：Nginx 反代（见 `deploy/nginx.conf.example`）

## 本地运行

### 要求

- JDK **17+**（推荐 Temurin/OpenJDK 17；勿用与 Lombok 不兼容的过新 JDK 做编译）
- Node 18+
- Maven 3.9+

### 1. 启动后端

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home   # macOS Homebrew 示例
cd server
mvn spring-boot:run
```

默认：

- API：`http://localhost:8080`
- H2 文件库：`server/data/couple`
- 上传目录：`server/uploads`
- 账号（可在 `application.yml` 的 `couple.partners` 修改）：
  - `czh` / `czh123456`
  - `hsj` / `hsj123456`

### 2. 启动前端

```bash
cd web
npm install
npm run dev
```

浏览器打开 Vite 提示的地址（通常 `http://localhost:5173`）。  
开发代理已将 `/api`、`/uploads` 转到 `8080`。

### 3. 生产构建

```bash
cd web && npm run build
# 将 web/dist 部署到 Nginx 静态目录
cd server && mvn -DskipTests package
# 运行 java -jar target/couple-app-1.0.0.jar
```

## MySQL 模式（可选）

```bash
cd deploy && docker compose -f docker-compose.mysql.yml up -d
cd ../server && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

按需修改 `application-mysql.yml` 中的账号密码。

## 订阅号菜单接入

1. 购置域名并解析到服务器，配置 HTTPS（可用 Let’s Encrypt）。
2. 参考 `deploy/nginx.conf.example` 配置静态资源与 `/api`、`/uploads` 反代。
3. 微信订阅号后台 → 自定义菜单 → 跳转网页 → 填入 `https://你的域名/`。
4. **订阅号无法做网页授权 OpenID**；当前为密码登录，足够个人使用。

## 模式说明

| 模式 | 入口 | 能力 |
|------|------|------|
| 展示 | 任何人打开站点 | 恋爱天数、下一场纪念日、精选照片 |
| 正常 | 封面低调入口登录 | 首页 / 相册 / 问答 / 挑战 |

问答：双方都答完才互看。  
挑战：仅「我们」共同完成，不按个人记进度。

## 安全建议

- 上线前修改 `couple.jwt.secret` 与双方密码
- 不要把真实密码提交进 Git
- 限制服务器上传目录权限与磁盘容量

## 文档

- 设计：`docs/superpowers/specs/2026-07-27-couple-h5-design.md`
- 实现拆分：`docs/superpowers/plans/2026-07-27-couple-h5-implementation.md`
