# Cart Service Skeleton Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan in the current workspace. Steps use checkbox syntax for tracking.

**Goal:** 建立可独立启动、可打包并接入网关的购物车微服务骨架。

**Architecture:** 新模块仅依赖公共库和 Web MVC，预留现有业务分层。网关固定地址转发并复用 JWT 校验，购物车业务后续实现。

**Tech Stack:** Java 21、Spring Boot 4.0.8、Spring Cloud 2025.1.3、Maven Wrapper。

**Spec:** ../specs/2026-10-15-cart-service-skeleton-design.md

## Global Constraints

- 服务名 mall-cart-service，端口 CART_SERVICE_PORT 默认 8083。
- 网关路径 /api/cart/**，目标 CART_SERVICE_URL 默认 http://localhost:8083。
- 不引入数据库或中间件，不提供购物车业务接口。
- 直接修改当前工作区，保留改动供用户查看。

## Review Focus

- 缺少或无效令牌必须返回 401。
- 基础路径及子路径均能转发，查询参数和请求体保留。
- /api/cartography 不匹配购物车路由。
- 现有路由测试使用真实测试 JWT，不依赖本地密钥。
- 新服务无外部配置也能启动，未实现接口返回 404。

### Task 1: 网关路由与登录边界

**Files:** 修改 mall-gateway/src/main/resources/application.yaml 和 mall-gateway/src/test/java/com/nemall/gateway/GatewayRoutesTests.java。

**Interfaces:** 消费现有网关 JWT 校验，产出 /api/cart/** 到 CART_SERVICE_URL 的路径保持路由。

- [x] 修正测试 JWT 夹具，新增购物车基础路径、子路径、匿名、无效令牌及相邻路径测试。
- [x] 运行网关测试，确认购物车转发测试因路由缺失返回 404。
- [x] 配置 cart-service 路由，重跑网关测试，预期全部通过。

### Task 2: 服务骨架与文档

**Files:** 新建 mall-cart-service/pom.xml、启动类、分层包、资源配置及 CartApplicationTests；修改根 pom.xml 和 README.md。

**Interfaces:** 独立启动入口 com.nemall.cart.CartApplication；服务配置名及端口与 Task 1 对齐。

- [x] 添加模块 POM、聚合注册和启动测试，先确认启动入口缺失导致测试失败。
- [x] 添加启动类、配置、分层包与资源占位文件，预期新服务独立启动测试通过。
- [x] 更新 README，运行整个 Maven 工程 clean verify，预期全部测试与打包通过。
- [x] 以 CART_SERVICE_PORT 覆盖端口启动可执行 JAR，确认启动及 HTTP 404 后停止验证进程。

## 验证记录

- 网关路由添加前：9 项测试中仅 2 项购物车转发测试失败，实际返回 404。
- 网关路由添加后：9 项全部通过。
- 购物车启动入口添加前：启动测试因找不到 Spring Boot 配置失败。
- 全工程 clean verify：12 项测试通过，所有模块打包成功；使用 Java 21、离线缓存和 README 中的测试专用数据库/JWT 配置。
- JAR 独立启动：清除数据库/JWT/外部 Spring 配置环境变量，以 CART_SERVICE_PORT=50334 启动成功，GET /api/cart 返回 404，验证进程已停止。
- 当前环境的 Maven 缓存文件锁不可写，验证命令额外使用 -Daether.syncContext.named.factory=rwlock-local；HTTP 集成测试在允许回环端口监听的执行环境运行。
