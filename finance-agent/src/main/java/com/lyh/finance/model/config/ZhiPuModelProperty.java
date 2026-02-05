package com.lyh.finance.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Data
@ConfigurationProperties(prefix = "model.zhipu")
public class ZhiPuModelProperty extends ModelProperty {
}
