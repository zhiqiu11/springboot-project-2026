package com.example;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication//是@Configuration、@EnableAutoConfiguration、@ComponentScan三个注解的组合，作用为启动自动配置、组件扫描和配置类
@MapperScan("com.example.mapper")
public class SpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootApplication.class, args);//启动springboot项目
        System.out.println("B站搜：程序员青戈  https://space.bilibili.com/402779077");
        System.out.println("公众号：程序员青戈");
    }

}
