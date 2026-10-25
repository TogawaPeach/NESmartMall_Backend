# NESmartMall · 经纬智选

![Java](https://img.shields.io/badge/Java-21-ED8B00)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.8-6DB33F)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.3-6DB33F)
![MyBatis](https://img.shields.io/badge/MyBatis-181717)
![MySQL](https://img.shields.io/badge/MySQL-4479A1)
![Redis](https://img.shields.io/badge/Redis-DC382D)
![Nacos](https://img.shields.io/badge/Nacos-2C9DD5)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600)
![Seata](https://img.shields.io/badge/Seata-2.6.0-00A7D0)

面向测绘工具销售的微服务商城后端，基于 Spring Boot 与 Spring Cloud 构建，涵盖用户认证、商品浏览、购物车、订单、秒杀及智能导购与客服服务。

## 核心功能

- **用户认证**：注册、登录、JWT 令牌签发与网关统一鉴权。
- **商品服务**：商品列表、分页搜索、详情查询，商家商品上下架与统计。
- **购物车**：商品加入、数量修改、移除与结算后的异步清理。
- **订单流程**：下单、列表与详情查询、模拟支付、取消、超时关闭及确认收货。
- **秒杀抢购**：Redis 缓存与 Lua 库存预扣、RabbitMQ 异步建单、Seata 分布式事务。
- **智能导购与客服**：独立的导购与客服接口，复用网关登录鉴权，通过 Nacos 服务发现与 WebClient 调用 Python Agent，并使用 SSE 逐段转发回答。

> 项目持续开发中。

## 项目结构

```text
NESmartMall
├── mall-gateway          # 统一入口与 JWT 鉴权                 8080
├── mall-auth-service     # 用户注册与登录                     8081
├── mall-product-service  # 商品、库存与秒杀商品查询           8082
├── mall-cart-service     # 购物车管理                         8083
├── mall-order-service    # 订单、模拟支付与秒杀下单           8084
├── mall-support-service  # 智能导购、客服与 Agent 流式转发     8085
├── mall-api              # Feign 接口与服务间 DTO
└── mall-common           # 统一响应与用户上下文
```

## 智能导购与客服

`mall-support-service` 统一承接导购与客服请求，按入口调用独立 Python 服务 `mall-agent-service` 的对应接口：

| 入口 | Gateway 对外接口 | Python 内部接口 |
| --- | --- | --- |
| 智能导购 | `POST /api/guide/turns/stream` | `POST /internal/guide/turns/stream` |
| 客服咨询 | `POST /api/support/turns/stream` | `POST /internal/support/turns/stream` |

前端携带 `Authorization: Bearer <token>`，以 JSON 发送本轮消息 `{"text":"用户的问题"}`。网关完成 JWT 校验并传递可信用户身份，客服服务向 Python 发送 `userId` 与本轮 `text`，通过 WebClient 接收 SSE，再由 Spring MVC 的 SseEmitter 向前端逐段输出。

```text
前端 → Gateway:8080 → mall-support-service:8085 → mall-agent-service:8090
前端 ← Gateway      ← SseEmitter               ← Python SSE
```

Python Agent 独立于本 Maven 工程运行，客服服务通过 Nacos 与 LoadBalancer 按服务名发现并调用实例。

## 本地运行

1. 准备 JDK 21、MySQL、Redis、Nacos、RabbitMQ 和 Seata，以及已有业务表。当前仓库未附完整建库脚本。
2. 在各服务的 `src/main/resources/application-local.yaml` 中配置数据库和中间件连接；认证与网关使用相同的 `JWT_SECRET`。本地配置文件已被 Git 忽略，需自行创建。
3. 在项目根目录打包：

   ```bash
   ./mvnw clean package -DskipTests
   ```

4. 在 IDE 中为网关、认证、商品、购物车、订单和客服六个应用启用 `local` profile，分别运行启动类；也可在各终端启动对应 JAR，例如：

   ```bash
   java -jar mall-order-service/target/mall-order-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
   ```

5. 使用导购与客服功能时，另行启动 Python Agent 服务，并将其以 `mall-agent-service` 注册到与客服服务相同的 Nacos namespace 和 group；本地默认端口为 `8090`。客服服务可通过 `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_GROUP`、`NACOS_USERNAME` 与 `NACOS_PASSWORD` 配置服务发现连接。

统一 API 入口：`http://localhost:8080`。修改服务端口时，同步调整网关中的目标地址；客服服务的网关目标可通过 `SUPPORT_SERVICE_URL` 覆盖。
