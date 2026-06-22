# Spring Boot 项目 - 毕设脚手架 2026

这是一个基于Spring Boot的全栈开发项目，包含后端API服务和前端Vue界面。

## 项目结构

- `springboot/` - 后端Spring Boot应用程序
- `vue/` - 前端Vue.js应用程序

## 技术栈

### 后端技术
- Spring Boot 3.3.1
- MyBatis
- MySQL
- Maven

### 前端技术
- Vue.js
- Element Plus
- Vite

## 环境要求

- Java 17+
- Node.js
- MySQL 8.0+

## 配置说明

项目使用环境变量来管理敏感信息。请在部署时设置以下环境变量：

- `DB_USERNAME` - 数据库用户名
- `DB_PASSWORD` - 数据库密码
- `DB_URL` - 数据库连接URL

或者，在运行环境中创建相应的配置文件覆盖默认配置。

## 项目启动

### 后端启动

1. 导入数据库表结构
2. 配置数据库连接信息
3. 运行Spring Boot应用

### 前端启动

1. 进入`vue/`目录
2. 安装依赖：`npm install`
3. 启动开发服务器：`npm run dev`

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。

## 功能特性

### 用户认证
- 使用 Sa-Token 实现登录/注册拦截，除白名单路径外所有接口需登录
- 白名单：`/login`, `/register`, `/files/**`, `/goods/**`, `/error`, `/connections`

### 秒杀
- 接口限流：`RateLimiter` 限制每秒 10 个请求
- 库存扣减：Redis 原子操作防超卖，兜底回滚
- 防重复下单：用户+商品级别 Redis `SET NX` 防重
- 库存预热：启动时所有秒杀商品的 flashNum 写入 Redis
- 分布式锁：用户级别 `SETNX + TTL` 保证单用户串行
- Lock Key 格式：`lock:seckill:{userId}`

### 团购
- 分布式锁：用户级别 `SETNX + TTL` 保证单用户串行
- Lock Key 格式：`lock:group:{userId}`
- 团长开团：`group:order:{orderId}` Hash（goodsId, targetNum, currentNum, status）
- 参团原子递增：`HINCRBY` 无锁竞争，满员则状态切 SUCCESS
- TTL 自动过期：默认 3600s，超过 Redis 自动清理
- 团购时间校验：下单时校验 goods.groupTime 是否已过
- 定时任务：超时未成团的"拼团中"订单退款、解库存

### 商品
- 字段含：hasFlash / flashTime / flashNum / flashPrice / hasGroup / groupTime / groupPrice
- 秒杀倒计时：GoodsService.calculateLastTime() 计算 flashTime 剩余秒数写入 goods.maxTime
- 团购倒计时：GoodsService.calculateLastTime() 计算 groupTime 剩余秒数写入 goods.maxTime
- 缓存策略：Cache-Aside（查询先读 Redis，更新/删除时清缓存）

### Redis 工具
- `RedisUtils` 封装：String 读写、Hash、Set NX、TTL、分布式锁 tryLock/releaseLock
- 序列化：Jackson2JsonRedisSerializer（JSON 序列化，对象直接存取）

### 订单
- 订单编号自动生成：`OrderUtils.fillOrderBaseInfo()` 统一设置
- 订单状态枚举：`OrderStatusEnum`（待支付 / 待接单 / 拼团中 / 已取消等）

## 数据库变更

### 已新增字段（goods 表）
| 字段 | 类型 | 说明 |
|---|---|---|
| has_flash | VARCHAR(10) | 是否秒杀（是/否） |
| flash_time | VARCHAR(20) | 秒杀结束时间 |
| flash_num | INT | 秒杀剩余名额 |
| flash_price | DECIMAL | 秒杀价格 |
| has_group | VARCHAR(10) | 是否团购（是/否） |
| group_time | VARCHAR(20) | 团购结束时间 |
| group_price | DECIMAL | 团购价格 |

## WebConfig 排除路径说明
为屏蔽 IDEA Dashboard 的持续性探测请求（/connections）及 Tomcat 默认错误页转发，在 Sa-Token 拦截器中添加了排除路径 `/connections` 和 `/error`，避免日志中频繁出现无意义的拦截报错。

## 可优化修改目标

1. 将每日为你推荐设置为通过redis缓存内部用户查询相关分类次数，设置第二天推荐的目标
2. 最新上架和热销商品也设置为通过redis缓存，热销商品和最新上架是到一定时间段才更新，不过时间少点，默认是1小时
3. 团购人数 targetNum 目前硬编码为 2，可从商品表或前端传入
4. 秒杀锁库存与业务数据库写入目前不是单个原子事务，极端场景可能出现 Redis 扣了但 DB 写失败的情况，可引入 MQ 最终一致性
