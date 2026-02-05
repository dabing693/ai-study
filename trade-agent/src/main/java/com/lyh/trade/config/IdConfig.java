package com.lyh.trade.config;

import com.baomidou.mybatisplus.core.toolkit.Sequence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Configuration
public class IdConfig {
    @Bean
    public Sequence sequence() {
        // workerId：机器ID，可以使用服务器编号，范围为0-31
        // datacenterId：数据中心ID，可以使用机器IP地址最后一段数字，范围为0-31
        return new Sequence(1, 1);
    }
}