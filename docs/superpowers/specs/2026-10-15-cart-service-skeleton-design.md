# 购物车微服务基础骨架

## 目标与现状

用户要求先查看整个微服务工程，再直接修改文件建立购物车微服务骨架。
当前工程包含 mall-common、mall-auth-service、mall-product-service 和 mall-gateway。
认证和商品业务已有实现；网关使用固定地址路由并校验受保护请求的 JWT。

## 本次设计

- 新建独立的 mall-cart-service，父工程将其纳入 Maven 聚合构建。
- 沿用 Java 21、Spring Boot 4.0.8、Spring Cloud 2025.1.3。
- 启动类为 com.nemall.cart.CartApplication，只扫描本服务根包。
- 依赖 mall-common、Web MVC 和现有测试 starter；Lombok 继承父工程。
- 服务名 mall-cart-service，端口由 CART_SERVICE_PORT 配置，默认 8083。
- 预留 controller、service、service.impl、mapper、entity、dto、vo、security、config、exception 包，以及 mapper 和 db/migration 资源目录。
- 网关将 /api/cart/** 原路径转发到 CART_SERVICE_URL，默认 http://localhost:8083。
- 购物车不加入公开白名单，沿用网关现有 JWT 校验。

## 范围边界

骨架不提供购物车业务接口，因此直连 /api/cart 返回 404；这不代表查询到了空购物车。
本次不确定购物车表、数量合并规则或 SKU 模型，不引入数据库、Redis、Nacos、Feign、MQ 或事务中间件。
后续实现业务时，再按项目已有 MyBatis/XML 约定接入持久化，并完成可信身份来源和资源归属校验。
当前网关尚未实现可信身份转发，不能把客户端身份头直接作为当前用户。

## 验收

1. 新服务能在没有数据库或 JWT 配置的情况下启动并打包为可执行 JAR。
2. 网关对已登录购物车请求保留路径、查询参数、方法、请求体和 Bearer 请求头。
3. 无令牌或无效令牌访问购物车得到 401；相邻非购物车路径不会匹配购物车路由。
4. 原认证、商品和商家路由验证通过。
5. README 记录模块、端口、环境变量、启动方法与未实现边界。
