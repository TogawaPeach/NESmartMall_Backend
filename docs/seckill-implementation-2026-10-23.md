# 秒杀下单后端实现与 MQ 并发控制交接

更新时间：2026-10-23（北京时间）

## 1. 本轮交付与最新决定

秒杀基本下单链路已实现，用户反馈已完成 5 件库存的抢购测试，数据库和订单结果正常。本轮交付是当前可运行的基本流程，不代表历史需求中所有生产可靠性要求均已实现。

后端目录：`/Users/lightfull/NESmartMall_After/NESmartMall`；GitHub 仓库为 `TogawaPeach/NESmartMall_Backend`，提交目标为已有 `main` 分支。用户本轮明确授权更新 Require、提交并推送后端代码。Require 目录本身没有 Git 仓库，交接另存副本至后端 `docs/seckill-implementation-2026-10-23.md`。

最新消费方案：移除 Redisson `RRateLimiter`、`order:seckill:consume:rate` key 的初始化和每秒 50 次额度等待；使用 RabbitMQ 消费并发控制。Redis 仍用于库存预扣，本次移除的是 Redis 限速器，不是整个 Redis 依赖。

## 2. 接口与执行链路

接口：`POST /api/orders/seckill`，走网关 Bearer 鉴权。用户 ID 来自服务端 `UserContext`，请求不传价格、用户 ID 或购买数量。

```json
{
  "productId": "26",
  "recipient": {
    "name": "测试收货人",
    "phone": "测试手机号",
    "address": "测试地址"
  }
}
```

每次固定购买一件。当前 DTO/Controller 尚未增加收货字段 Bean Validation；请求示例是结构说明，不代表校验已经齐备。

```text
HTTP 请求
  → SeckillOrderServiceImpl 读取服务端用户身份
  → Redis Lua 校验秒杀标记、活动时间、剩余库存，原子预扣一件
  → RabbitMQ 持久消息，等待 publisher confirm，检查 returned message
  → HTTP 返回 code=200、message=购买成功、data=null

RabbitMQ 秒杀队列
  → SeckillOrderListener 同步处理
  → SeckillOrderCreateServiceImpl 经 Spring 代理开启 Seata/本地事务
  → 查询数据库商品摘要，写订单和固定一件的订单明细，调用商品服务扣数据库库存
  → 成功返回后由 Spring 容器确认消费；失败走既有重试策略
```

HTTP 的 200 实际表示已预扣并确认 MQ 投递，不表示订单已落库；当前接口不返回订单号，也没有独立受理结果查询接口。后续前端、测试断言和文案需按这一语义处理。

## 3. Redis、MQ 与事务实现

- 预扣脚本：`mall-order-service/src/main/resources/lua/seckill-reserve.lua`。
- 当前直接修改秒杀详情缓存 `product:seckill:{detail:商品ID}:data` 的 `data.stock`；要求缓存已经存在，不在下单入口自动查库初始化。
- Lua 读取 `isSeckill`、起止时间和整数库存；使用订单服务本地 `LocalDateTime` 判断开始时间包含、结束时间不包含，成功减一并保留原物理 TTL。各服务应使用一致的北京时间配置。
- 消息交换机：`mall.order.exchange`；routing key：`order.seckill`；队列：`mall.order.seckill.queue`。
- 消息包含 `requestId`、服务端 `userId`、`productId`、固定 `quantity=1` 和收货信息。消息 ID 每次 HTTP 提交新生成；同一消息消费重试时保持不变。
- 发送采用持久消息和持久队列，等待 confirm 最多 3 秒，并检查 mandatory 路由退回。持久化和 confirm 不等同于跨 Redis/MQ 的原子提交或故障恢复。
- 订单服务以 `StringCodec` 使用 Redisson；地址、密码、库号来自 `app.redis.*`，与商品缓存所在 Redis 配置保持一致；真实凭据只保留环境变量或被忽略的本地文件。
- 落库服务同时使用 `@GlobalTransactional` 和 `@Transactional`。数据库扣库存仍检查上架、未删除、合法价格、正数量和足量库存，并增加销量。每笔成功订单仍逐笔更新数据库。

## 4. 消费并发与失败行为

秒杀监听器明确配置 `concurrency = "1"`；现有 YAML 为 `prefetch: 1`、`acknowledge-mode: auto`。监听器不把创建任务转交其他线程池，处理成功后容器发送 ACK，再接收下一条。

这是每个订单服务实例一个消费者：一个实例最多同时处理一笔，三个实例最多同时处理三笔。没有全局单活消费者约束，也没有固定每秒 N 条的速率保证；实际吞吐由完整订单事务耗时决定。交换机负责路由，不按每秒固定条数投递。

既有重试配置为首次失败后再重试 3 次，间隔为 1 秒、2 秒、2 秒，最多执行 4 次；重试期间该消费者暂时不能处理后续消息。耗尽后拒绝且不重新入队，代码没有为秒杀队列配置死信恢复或 Redis 预扣补偿。

## 5. 联调与压测记录

- 用户反馈：配置 1000 个 JMeter 线程、Ramp-Up 1 秒、循环一次，抢购 5 件库存的本轮测试正常，数据库结果正常。这是用户反馈，不是助手重复执行的端到端验收。
- 1000 线程不等于实测 1000 QPS，也不保证所有请求在同一时刻执行；没有取得可证明入口容量上限或建单 TPS 上限的报告。
- Apifox 截图阶段数据为总请求 5091、85.77 请求/秒、平均 8ms、P95 20ms、P99 77ms、失败率 99.98%。失败原因未逐项验证，不能把这组以失败请求为主的数据当作成功建单性能。
- 前一轮异常日志中，`ProductClient.deductStock` 返回 HTTP 409，随后 Seata 回滚，消息重试耗尽；没有本轮新订单。只读核对商品 26 的数据库库存实际为 0，最后更新时间早于异常；15 笔待付款订单由用户确认为上一轮测试产生。不能误记为超卖，用户已撤回超卖判断。
- 复测时先停止压测并确认无在途消息，再统一初始化专用测试商品的数据库与 Redis 库存，确认数据库修改已提交。不要在活跃排队期间直接重置库存。
- 本轮先前已使用 Java 21 完成订单模块及公共依赖的编译；提交前在独立临时 Redis、虚拟数据库连接、关闭实际 MQ 消费/服务注册/Seata 的环境执行一次工作区全量测试：45 项中 44 项通过、1 项失败。商品服务 22 项（含 11 项真实临时 Redis 集成测试）、网关 21 项、认证启动 1 项通过；`CartApplicationTests.startsWithoutInfrastructureAndReturnsNotFoundForUnimplementedCartApi` 仍期望 404，实际返回 500，未修改该购物车用例。订单模块当前没有专用自动化用例，不能据此宣称异步建单端到端测试已覆盖。
- 用户随后明确“不用做测试，我做了，很正常”，本轮不再追加测试；以用户已完成的业务联调反馈记录功能完成情况。临时 Redis 已关闭，测试未使用业务 Redis 或实际数据库。构建/测试不等同于生产中间件、全链路容量或故障恢复验收。

## 6. 后续边界

以下是当前代码实际缺口，不能沿用旧需求文档的目标描述将其标记为已完成：

1. Redis 预扣与 MQ 发送没有可靠待投递记录；发送失败、结果未知或进程退出可能占用名额且没有创建任务。最终消费失败也没有幂等释放名额机制。
2. `requestId` 未参与数据库幂等竞争；每次消费尝试新生成订单号。消息重复投递或提交后 ACK 丢失的重复建单风险尚未治理，也未实现每人限购。
3. 展示缓存和交易库存尚未分离；逻辑过期重建或缓存失效可能覆盖/重置预扣库存，不能将单轮测试成功视为该场景已安全。
4. 普通下单/购物车结算尚未增加后端拒绝秒杀商品的统一规则；HTTP 字段校验、可信购买资格、排队超时和容量上限也未补齐。
5. 秒杀落库服务没有调用普通下单的支付超时消息调度；不能宣称秒杀订单已完整接入自动超时关单。取消/超时后的 Redis 名额释放及恢复核对仍未实现。
6. 没有独立受理状态查询、完整断言和故障注入验证，也未测得企业级容量上限。

上述缺口保留为后续工作，本轮不擅自扩展实现、迁移数据库、重置业务库存或执行新的压力测试。

## 7. 提交范围

提交秒杀专用 DTO、Service、事务创建服务、Listener、Lua、Redisson 配置、Controller 入口、MQ 队列绑定、订单明细 Mapper 和对应依赖/Redis 配置，附本交接副本及 README 入口。

工作区原有 `ProductDetailData` 类型调整、普通详情查询删除过滤条件调整、秒杀展示缓存逻辑有效期从 120 秒调到 12000 秒，以及 `performance/` 首页压测文件不属于本次提交，保留本地。GitHub 提交中的商品缓存策略仍沿用已提交的 120 秒基线。

## 8. 后续开发顺序（用户确认）

2026-10-23 用户确认传统业务面向消费者端的功能已完成；这是用户对当前项目阶段的确认，不表示本轮重新验证了全部消费者端功能。

后续先开发商家端，再开发 Agent 客服。客服技术方向明确为 **Python + LangChain**。本轮仅记录顺序和技术选择，尚未开始商家端或 Python 服务实现，也未确定客服模型、工具权限、知识库、存储、部署方式及 Java/Python 通信契约；这些细节留待对应阶段结合现有代码确定。
