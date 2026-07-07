# 图书馆智能服务系统

## 运行要求

| 软件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+（密码 123456） |
| Redis | 任意版本 |
| Nacos | 2.2+ |
| RocketMQ | 5.0+ |

## 一键启动

确保 MySQL 和 Redis 已运行，然后双击 **startup.bat**。

首次启动约需 **3-5 分钟**，后续启动约 1 分钟。

## 访问

| 页面 | 地址 |
|------|------|
| 登录 | http://localhost:5173/login |
| Nacos | http://localhost:8848/nacos |

## 测试账号

| 用户名 | 密码 | 角色 |
|-------|------|------|
| admin | 123456 | 管理员 |
| zhangsan | 123456 | 普通用户 |
| lisi | 123456 | 普通用户 |

## 手动启动

如果想逐个启动：

```bash
mvn install -N -q
mvn install -pl common -q

# 逐个启动微服务（每个新开一个终端）
mvn spring-boot:run -pl user-service
mvn spring-boot:run -pl seat-service
mvn spring-boot:run -pl reservation-service
mvn spring-boot:run -pl book-service
mvn spring-boot:run -pl borrowing-service
mvn spring-boot:run -pl notification-service
mvn spring-boot:run -pl gateway-service

# 启动前端
cd frontend
npm install
npx vite --host
```

## 特色功能

- **AI 智能推荐**：资源检索页 → 生成我的推荐（基于 DeepSeek）
- **扫码借书**：资源检索页 → 扫码借书/还书（拍照识别条码）
- **拍照入库**：图书管理 → 拍照识别（自动填充入库表单）
