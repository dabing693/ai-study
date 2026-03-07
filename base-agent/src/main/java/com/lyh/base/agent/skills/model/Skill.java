package com.lyh.base.agent.skills.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/6 21:50
 */
@Data
@AllArgsConstructor
public class Skill {
    private String name;
    private String description;
    private String content;
}