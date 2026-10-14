package com.nemall.auth.mapper;

import com.nemall.auth.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    Account selectByAccount(@Param("account") String account);
}