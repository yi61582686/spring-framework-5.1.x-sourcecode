package com.on.dao;

import com.on.config.Select;

import java.util.List;

public interface TestMapper {

	@Select("select * from test_table") // 自定义的注解
	public String findAll();

}
