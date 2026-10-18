# NESmartMall 后端

在原有 Spring Boot 项目上整理的 Maven 聚合工程。技术基线为 Java 21、Spring Boot 4.0.8、Spring Cloud 2025.1.3，保留原 Maven Wrapper。

## 模块

```text
NESmartMall/
├── pom.xml                   # mall-parent，管理模块、依赖版本与构建配置
├── .mvn/、mvnw、mvnw.cmd      # 整个后端共用的 Maven Wrapper
├── mall-common/              # 普通公共库 JAR，没有启动类
├── mall-api/                 # 服务间 Feign Client 与 DTO 公共契约
├── mall-auth-service/        # AuthApplication，账号认证和令牌签发
├── mall-product-service/     # ProductApplication，商品及商家商品管理
├── mall-cart-service/        # CartApplication，购物车查询、加入、数量修改与删除
├── mall-order-service/       # OrderApplication，当前用户订单列表查询
└── mall-gateway/             # GatewayApplication，统一 HTTP 入口
```

原 `NeSmartMallApplication` 和启动测试已迁入商品模块，并改为 `ProductApplication`、`ProductApplicationTests`。根工程不再存放应用源码，也不生成可执行 JAR。

`mall-common` 提供统一响应体 `Result<T>` 和用户上下文，并预留 exception、util 包；它不依赖 Web、数据库或 Spring Boot 打包插件。认证、商品、购物车与订单服务使用 Web MVC、MyBatis 和 MySQL，Lombok 继承父工程。网关使用 Spring Cloud Gateway Server Web MVC，不引入数据库驱动。

业务服务统一预留 controller、service、service.impl、mapper、entity、dto、vo、security、config、exception 包；Mapper XML 和数据库迁移分别放在各自服务的 `src/main/resources/mapper`、`db/migration`。订单已配置 Mapper XML 查询，未启用数据库迁移工具。启动类位于各服务根包，默认扫描本服务的子包；订单 Mapper 使用 `@Mapper`，由 MyBatis starter 自动扫描。公共库若以后加入 Spring 组件，需要显式导入相关配置，避免扩大扫描范围而混入其他服务。

## 导入与构建

在 IntelliJ IDEA 中重新加载根 `pom.xml` 的 Maven 项目，将 Project SDK 和 Maven Runner JRE 设置为 Java 21。五个服务分别运行自己的启动类。

在项目根目录执行：

```bash
./mvnw clean verify

# 只构建商品服务及其公共依赖
./mvnw -pl mall-product-service -am clean package

# 只构建购物车服务及其公共依赖
./mvnw -pl mall-cart-service -am clean package

# 只构建订单服务及其公共依赖
./mvnw -pl mall-order-service -am clean package
```

如果本机默认 Java 版本不是 21，macOS 可以在执行命令前选择 Java 21：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## 独立启动

完成构建后，在五个终端分别启动：

```bash
java -jar mall-auth-service/target/mall-auth-service-0.0.1-SNAPSHOT.jar
java -jar mall-product-service/target/mall-product-service-0.0.1-SNAPSHOT.jar
java -jar mall-cart-service/target/mall-cart-service-0.0.1-SNAPSHOT.jar
java -jar mall-order-service/target/mall-order-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -jar mall-gateway/target/mall-gateway-0.0.1-SNAPSHOT.jar
```

| 服务 | 默认端口 | 端口环境变量 |
| --- | --- | --- |
| mall-gateway | 8080 | GATEWAY_PORT |
| mall-auth-service | 8081 | AUTH_SERVICE_PORT |
| mall-product-service | 8082 | PRODUCT_SERVICE_PORT |
| mall-cart-service | 8083 | CART_SERVICE_PORT |
| mall-order-service | 8084 | ORDER_SERVICE_PORT |

网关当前配置以下路由，保留请求路径：

| 请求路径 | 默认目标 |
| --- | --- |
| /api/auth/** | http://localhost:8081 |
| /api/products/** | http://localhost:8082 |
| /api/merchant/** | http://localhost:8082 |
| /api/cart/** | http://localhost:8083 |
| /api/orders/** | http://localhost:8084 |

通过 `AUTH_SERVICE_URL`、`PRODUCT_SERVICE_URL`、`CART_SERVICE_URL`、`ORDER_SERVICE_URL` 环境变量可以覆盖网关目标地址。单独改变服务端口时，需要同步改变对应网关目标地址。

## 当前范围

认证服务已有注册、登录和 JWT 签发；商品服务已有商品列表、分页搜索及详情接口。网关已有 JWT 签名、时间与过期声明校验，精确放行登录、注册和首页商品查询，并清除外部身份头后转发已验证的账户身份；角色授权及其他业务资源权限按服务继续接入。商家通配路由不代表商家业务接口已经实现。

购物车已有查询、加入、数量修改与删除代码，使用当前用户约束数据库操作，通过 Nacos、Feign/HC5 批量读取商品；GET 查询此前已由用户确认联调正常，写接口的运行验收见本地 Require。

订单默认端口 8084，已写入 `GET /api/orders` 列表查询代码，支持可选英文 status，不分页，返回 `Result<List<OrderVO>>`。先按当前用户查询 mall_order，再按订单 ID 批量查询 order_item，在内存中组装 items；非空列表两次业务 SQL，空列表一次。当前列表对外仅返回 orderNo、status、createdAt、payableAmountFen、items；OrderVO 的内部 id 通过 @JsonIgnore 隐藏，仅供后端关联商品明细，尚未补齐前端原完整订单契约中的支付、收货等字段，真实前后端联调待完成。详情、下单、支付、取消和收货接口尚未实现，不创建或执行数据库表 SQL，不接入 Nacos、Feign、Redis、MQ 或 Seata。

网关订单路径继续经过现有 JWT 校验；订单 UserInfoFilter 恢复 user-info/user-role 并在 finally 中清理 UserContext，订单主查询始终限定 user_id。沿用当前仅对外暴露网关的部署约定，订单不重复验 JWT 或额外认证内部来源。缺失或错误的内部用户 ID 作为身份传递链路错误抛出，不在订单服务重复返回登录 401。本次未编译、测试、打包、启动或真实联调，不能据此认为运行验收通过；异常 Result 处理尚待补齐。

认证、商品、购物车和订单服务需要 MySQL 数据源及对应表，数据库连接和凭据通过环境变量或被忽略的本地配置提供。网关与认证服务须配置同一 `JWT_SECRET`；网关不需要数据库连接。

已有测试包含认证、商品、购物车应用启动，以及网关向测试上游转发请求；未新增订单自动化用例，本次未运行任何测试，历史购物车骨架验收不代表当前版本或订单模块已通过验证。

完整构建如未配置本地环境，可使用只用于测试的配置运行现有启动测试；这些测试不执行数据库查询，不代表数据库业务联调通过：

```bash
./mvnw clean verify \
  -Djwt.secret=local-build-test-secret \
  -Dspring.datasource.url=jdbc:mysql://127.0.0.1:1/mall_test \
  -Dspring.datasource.username=test \
  -Dspring.datasource.password=test
```

## 官方参考

- [Spring Cloud 与 Spring Boot 版本对应关系](https://spring.io/projects/spring-cloud/)
- [Spring Cloud Gateway Server Web MVC](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/starter.html)
- [Spring Boot Maven 打包插件](https://docs.spring.io/spring-boot/maven-plugin/using.html)

## 本地配置与敏感信息

认证、商品与购物车服务的数据库连接通过 `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 环境变量提供，不在版本库中保存真实凭据。启动前须配置这三个变量；认证与网关还需配置同一 `JWT_SECRET`。

本机也可以在各服务的 `src/main/resources/application-local.yaml` 中配置本地参数，并通过 `SPRING_PROFILES_ACTIVE=local` 或 `--spring.profiles.active=local` 激活。本地配置文件已被 `.gitignore` 排除；新克隆的项目需自行创建需要的本地配置或设置环境变量。

订单的数据源 URL、账号和密码仅配置在本机 `mall-order-service/src/main/resources/application-local.yaml`，沿用现有共用库连接默认值，可分别通过 ORDER_MYSQL_URL、ORDER_MYSQL_USERNAME、ORDER_MYSQL_PASSWORD 覆盖。公共 application.yaml 只保留服务名、端口及 Mapper XML 路径。订单 DATETIME 按 UTC 存取，本地 JDBC URL 已设置 `connectionTimeZone=%2B00:00`、`forceConnectionTimeZoneToSession=true`、`preserveInstants=true`；覆盖 URL 时须保留这些参数，确保读取 Instant 和数据库会话不使用默认时区。

`.env` 文件、本地或 secrets 配置、IDE 数据源信息、私钥和证书存储、日志、数据库文件及构建产物均不提交。Spring Boot 不会自动读取 `.env`，如使用该文件，需要自行将变量加载到启动进程。
