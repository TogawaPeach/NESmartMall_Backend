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
└── mall-gateway/             # GatewayApplication，统一 HTTP 入口
```

原 `NeSmartMallApplication` 和启动测试已迁入商品模块，并改为 `ProductApplication`、`ProductApplicationTests`。根工程不再存放应用源码，也不生成可执行 JAR。

`mall-common` 目前预留 response、exception、util 包，公共类型在接口约定确定后添加；它不依赖 Web、数据库或 Spring Boot 打包插件。认证与商品服务分别声明 WebMVC、MySQL Driver、Lombok 依赖；网关使用 Spring Cloud Gateway Server Web MVC，不引入数据库驱动。

业务服务统一预留 controller、service、mapper、entity、dto、vo、security、config、exception 包；Mapper XML 和数据库迁移分别放在各自服务的 `src/main/resources/mapper`、`db/migration`。启动类位于各服务根包，默认扫描本服务的子包。公共库若以后加入 Spring 组件，需要显式导入相关配置，避免扩大扫描范围而混入其他服务。

## 导入与构建

在 IntelliJ IDEA 中重新加载根 `pom.xml` 的 Maven 项目，将 Project SDK 和 Maven Runner JRE 设置为 Java 21。三个服务分别运行自己的启动类。

在项目根目录执行：

```bash
./mvnw clean verify

# 只构建商品服务及其公共依赖
./mvnw -pl mall-product-service -am clean package
```

如果本机默认 Java 版本不是 21，macOS 可以在执行命令前选择 Java 21：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## 独立启动

完成构建后，在三个终端分别启动：

```bash
java -jar mall-auth-service/target/mall-auth-service-0.0.1-SNAPSHOT.jar
java -jar mall-product-service/target/mall-product-service-0.0.1-SNAPSHOT.jar
java -jar mall-gateway/target/mall-gateway-0.0.1-SNAPSHOT.jar
```

| 服务 | 默认端口 | 端口环境变量 |
| --- | --- | --- |
| mall-gateway | 8080 | GATEWAY_PORT |
| mall-auth-service | 8081 | AUTH_SERVICE_PORT |
| mall-product-service | 8082 | PRODUCT_SERVICE_PORT |

网关当前配置以下路由，保留请求路径：

| 请求路径 | 默认目标 |
| --- | --- |
| /api/auth/** | http://localhost:8081 |
| /api/products/** | http://localhost:8082 |
| /api/merchant/** | http://localhost:8082 |

通过 `AUTH_SERVICE_URL`、`PRODUCT_SERVICE_URL` 环境变量可以覆盖网关目标地址。单独改变服务端口时，需要同步改变对应网关目标地址。

## 当前范围

本次完成工程结构、独立启动入口、基础配置、打包及网关路由。登录、JWT 签发与校验、商家权限和商品业务接口尚未实现；当前路由不是鉴权功能，也不代表商家接口已可用。业务接口未实现时访问相应路径返回 404。

商品服务已引入 MyBatis 并配置 MySQL 数据源，数据库连接和凭据通过环境变量或被忽略的本地配置提供（见下方说明）。当前尚未提供建表迁移，执行商品查询需要可用的数据库和对应表。认证服务和网关不需要数据库连接。

校验包含认证和商品应用上下文启动，以及网关通过真实 HTTP 向测试上游转发请求；网关测试覆盖目标服务、请求路径、查询参数、Bearer 请求头、PATCH 方法和请求体。

## 官方参考

- [Spring Cloud 与 Spring Boot 版本对应关系](https://spring.io/projects/spring-cloud/)
- [Spring Cloud Gateway Server Web MVC](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/starter.html)
- [Spring Boot Maven 打包插件](https://docs.spring.io/spring-boot/maven-plugin/using.html)

## 本地配置与敏感信息

商品服务的数据库连接通过 `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 环境变量提供，不在版本库中保存真实凭据。启动前须配置这三个变量。

本机也可以在 `mall-product-service/src/main/resources/application-local.yaml` 中配置数据源，并通过 `SPRING_PROFILES_ACTIVE=local` 激活。本地配置文件已被 `.gitignore` 排除；新克隆的项目需自行创建该文件或设置环境变量。

`.env` 文件、本地或 secrets 配置、IDE 数据源信息、私钥和证书存储、日志、数据库文件及构建产物均不提交。Spring Boot 不会自动读取 `.env`，如使用该文件，需要自行将变量加载到启动进程。
