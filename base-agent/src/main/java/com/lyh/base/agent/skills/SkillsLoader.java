package com.lyh.base.agent.skills;

import com.lyh.base.agent.domain.FunctionTool;
import com.lyh.base.agent.skills.model.Skill;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.aspectj.weaver.tools.cache.SimpleCacheFactory.path;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/6 21:14
 */
public class SkillsLoader {
    private static final String SKILL_MD = "skill.md";
    private static List<Skill> systemSkills = null;

    /**
     * 双检锁实现的单例 保证只加载一次
     *
     * @return
     */
    public List<Skill> loadOnce() {
        if (systemSkills == null) {
            synchronized (SkillsLoader.class) {
                if (systemSkills == null) {
                    systemSkills = load();
                }
            }
        }
        return systemSkills;
    }

    /**
     * 加载anthropic格式的skill
     *
     * @return
     */
    private List<Skill> load() {
        List<Skill> skills = new ArrayList<>();
        for (String path : getSkillPaths()) {
            skills.addAll(loadOnePath(path));
        }
        return skills;
    }

    private List<Skill> loadOnePath(String path) {
        List<Skill> skills = new ArrayList<Skill>();
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            return skills;
        }
        for (File f : files) {
            //非文件夹跳过
            if (!f.isDirectory()) {
                continue;
            }
            File[] skillFiles = f.listFiles();
            if (skillFiles == null) {
                continue;
            }
            for (File sf : skillFiles) {
                if (sf.isFile() && sf.getName().toLowerCase().endsWith(SKILL_MD)) {
                    Skill skill = readFromSkillMd(sf);
                    if (skill != null) {
                        skills.add(skill);
                    }
                }
            }
        }
        return skills;
    }

    private Skill readFromSkillMd(File sf) {
        //读取其中的SKILL.md的元信息
        try {
            String content = Files.readString(sf.toPath());
            String[] split = content.split("---", 3);
            if (split.length < 2) {
                throw new RuntimeException("非标准的skill格式");
            }
            String[] headers = split[1].trim().split("\n");
            String skillName = null;
            String skillDesc = null;
            String skillContent = split[2];
            for (String header : headers) {
                String[] kv = header.split(":", 2);
                if (kv.length < 2) {
                    continue;
                }
                String key = kv[0].trim();
                if ("name".equals(key)) {
                    skillName = kv[1].trim();

                } else if ("description".equals(key)) {
                    skillDesc = kv[1].trim();
                }
            }
            if (skillName != null && skillDesc != null) {
                return new Skill(skillName, skillDesc, skillContent);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private List<String> getSkillPaths() {
        List<String> paths = new ArrayList<>();
        String userName = System.getProperty("user.name");
        String agentSkillPath = String.format("C:\\Users\\%s\\.agents\\skills", userName);
        paths.add(agentSkillPath);
        return paths;
    }
}
