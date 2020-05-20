package com.on.dao;

import org.springframework.stereotype.Repository;

@Repository("testDao")
public class TestDao {

	Class<?> clazz;

//	public TestDao() {
//	}

	// 可以给构造方法进行初始化赋值
//	@ConstructorProperties({"testDao1","testDao2"})
//	public TestDao(TestDao1 testDao1, TestDao2 testDao2) {
//	}

	public TestDao(Class<?> clazz) {
		this.clazz = clazz;
	}

	public void query() {
		System.out.println("dao:" + clazz);
	}

}
