# NESmartMall 后端

在原有 Spring Boot 项目上整理的 Maven 聚合工程。技术基线为 Java 21、Spring Boot 4.0.8、Spring Cloud 2025.1.3，保留原 Maven Wrapper。

## 模块

```text
NESmartMall/
├── pom.xml                   # mall-parent，管理模块、依赖版本与构建配置
├── .mvn/、mvnw、mvnw.cmd      # 整个后端共用的 Maven Wrapper
├── mall-common/              # 普通公共库 JAR，没有启动类
├── mall-auth-service/        # AuthApplication，账号认证和令牌签发
├── mall-product-service/     # ProductApplication，商品及商家商品管理
├── mall-cart-service/        # CartApplication，购物车微服务基础骨架
└── mall-gateway/             # GatewayApplication，统一 HTTP 入口
```

原 `NeSmartMallApplication` 和启动测试已迁入商品模块，并改为 `ProductApplication`、`ProductApplicationTests`。根工程不再存放应用源码，也不生成可执行 JAR。

`mall-common` 提供统一响应体 `Result<T>`，并预留 exception、util 包；它不依赖 Web、数据库或 Spring Boot 打包插件。认证与商品服务使用 Web MVC、MyBatis 和 MySQL；购物车骨架只引入公共库和 Web MVC，Lombok 继承父工程。网关使用 Spring Cloud Gateway Server Web MVC，不引入数据库驱动。

业务服务统一预留 controller、service、mapper、entity、dto、vo、security、config、exception 包；Mapper XML 和数据库迁移分别放在各自服务的 `src/main/resources/mapper`、`db/migration`。购物车目录目前只作预留，未启用数据库或迁移工具。启动类位于各服务根包，默认扫描本服务的子包。公共库若以后加入 Spring 组件，需要显式导入相关配置，避免扩大扫描范围而混入其他服务。

## 导入与构建

在 IntelliJ IDEA 中重新加载根 `pom.xml` 的 Maven 项目，将 Project SDK 和 Maven Runner JRE 设置为 Java 21。四个服务分别运行自己的启动类。

在项目根目录执行：

```bash
./mvnw clean verify

# 只构建商品服务及其公共依赖
./mvnw -pl mall-product-service -am clean package

# 只构建购物车服务及其公共依赖，无需数据库或 JWT 配置
./mvnw -pl mall-cart-service -am clean package
```

如果本机默认 Java 版本不是 21，macOS 可以在执行命令前选择 Java 21：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## 独立启动

完成构建后，在四个终端分别启动：

```bash
java -jar mall-auth-service/target/mall-auth-service-0.0.1-SNAPSHOT.jar
java -jar mall-product-service/target/mall-product-service-0.0.1-SNAPSHOT.jar
java -jar mall-cart-service/target/mall-cart-service-0.0.1-SNAPSHOT.jar
java -jar mall-gateway/target/mall-gateway-0.0.1-SNAPSHOT.jar
```

| 服务 | 默认端口 | 端口环境变量 |
| --- | --- | --- |
| mall-gateway | 8080 | GATEWAY_PORT |
| mall-auth-service | 8081 | AUTH_SERVICE_PORT |
| mall-product-service | 8082 | PRODUCT_SERVICE_PORT |
| mall-cart-service | 8083 | CART_SERVICE_PORT |

网关当前配置以下路由，保留请求路径：

| 请求路径 | 默认目标 |
| --- | --- |
| /api/auth/** | http://localhost:8081 |
| /api/products/** | http://localhost:8082 |
| /api/merchant/** | http://localhost:8082 |
| /api/cart/** | http://localhost:8083 |

通过 `AUTH_SERVICE_URL`、`PRODUCT_SERVICE_URL`、`CART_SERVICE_URL` 环境变量可以覆盖网关目标地址。单独改变服务端口时，需要同步改变对应网关目标地址。

## 当前范围

认证服务已有注册、登录和 JWT 签发；商品服务已有商品列表、分页搜索及详情接口。网关已有 JWT 签名、时间与过期声明校验，精确放行登录、注册和首页商品查询；角色授权、可信身份转发及业务资源权限仍需后续接入。商家通配路由不代表商家业务接口已经实现。

购物车本次只完成独立模块、启动类、分层目录、基础配置、可执行 JAR 及网关路由。没有购物车增删改查接口或数据表，访问 `/api/cart` 暂时返回 404；通过网关访问时，未登录或令牌无效先返回 401。骨架可在没有数据库、Redis 和 JWT 配置时独立启动。后续业务再接入持久化、可信当前用户及商品服务调用。

认证和商品服务需要 MySQL 数据源及对应表，数据库连接和凭据通过环境变量或被忽略的本地配置提供。网关与认证服务须配置同一 `JWT_SECRET`；购物车骨架和网关不需要数据库连接。

校验包含认证、商品应用上下文启动，购物车独立 HTTP 启动，以及网关通过真实 HTTP 向测试上游转发请求；网关测试覆盖目标服务、请求路径、查询参数、有效 Bearer 请求头、PATCH 方法和请求体，以及购物车匿名/无效令牌的 401 和相邻路径的 404。

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

认证与商品服务的数据库连接通过 `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 环境变量提供，不在版本库中保存真实凭据。启动前须配置这三个变量；认证与网关还需配置同一 `JWT_SECRET`。

本机也可以在各服务的 `src/main/resources/application-local.yaml` 中配置本地参数，并通过 `SPRING_PROFILES_ACTIVE=local` 激活。本地配置文件已被 `.gitignore` 排除；新克隆的项目需自行创建需要的本地配置或设置环境变量。购物车骨架不要求创建本地配置文件。

`.env` 文件、本地或 secrets 配置、IDE 数据源信息、私钥和证书存储、日志、数据库文件及构建产物均不提交。Spring Boot 不会自动读取 `.env`，如使用该文件，需要自行将变量加载到启动进程。
