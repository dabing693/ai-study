package com.lyh.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyh.finance.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM sys_user WHERE email = #{email}")
    User selectByEmail(String email);
}
