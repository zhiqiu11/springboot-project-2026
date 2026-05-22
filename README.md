# 为了安全起见，我们创建一个示例配置文件说明如何管理敏感信息
# 实际部署时，请不要提交包含真实数据库凭证的配置文件

# application.yml - 公共配置
server:
  port: 9090

# 数据库配置 - 使用环境变量
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME:root}  # 如果未设置环境变量，则默认为root
    password: ${DB_PASSWORD:123456}  # 如果未设置环境变量，则默认为123456
    url: ${DB_URL:jdbc:mysql://localhost:3306/code2026?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false&serverTimezone=GMT%2b8&allowPublicKeyRetrieval=true}
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

# 配置mybatis实体和xml映射
mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true #转换java和sql对应字段的不同要求

fileBaseUrl: http://localhost:9090