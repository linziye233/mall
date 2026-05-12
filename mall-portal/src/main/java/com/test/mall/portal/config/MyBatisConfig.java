package com.test.mall.portal.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis配置类
 * Created by tuling on 2019/4/8.
 */
@Configuration
@EnableTransactionManagement
@MapperScan({"com.test.mall.mapper","com.test.mall.portal.dao"})
public class MyBatisConfig {
}
