# ============================================
# Nacos 配置导入说明
# ============================================
#
# 以下配置需要在 Nacos 控制台手动导入：
# 访问 http://127.0.0.1:8848/nacos （默认账号密码：nacos/nacos）
#
# 1. seata.properties
#    - Data ID: seata.properties
#    - Group: SEATA_GROUP
#    - 格式: Properties
#    - 内容: 见 seata.properties 文件
#
# 2. 各微服务配置（可选，如果已在 application.yml 中配置）
#    - 如需使用 Nacos 配置中心动态管理，可将 application.yml 中的
#      数据库连接、Redis 配置等敏感信息迁移到 Nacos 配置中心
#
# ============================================
# 快速开始步骤：
# ============================================
#
# 1. 启动 MySQL 8.0
#    - 执行 sql/ 目录下的三个 SQL 脚本创建数据库和表
#
# 2. 启动 Redis
#    - 默认 127.0.0.1:6379
#
# 3. 启动 Nacos (standalone 模式)
#    - Windows: startup.cmd -m standalone
#    - 访问 http://127.0.0.1:8848/nacos
#    - 导入 seata.properties 到配置中心
#
# 4. 启动 Seata Server（可选，如果需要分布式事务）
#    - 下载 Seata Server 1.6.1
#    - 修改 registry.conf 注册到 Nacos
#    - 启动: seata-server.bat
#
# 5. 在 IDEA 中按顺序启动微服务：
#    a. UserApplication (8081)
#    b. SeatApplication (8082)
#    c. ReservationApplication (8083)
#    d. GatewayApplication (8080)
#
# 6. 启动前端
#    cd frontend
#    npm install
#    npm run dev
#
# 7. 访问前端: http://localhost:5173
#    API 网关: http://localhost:8080
