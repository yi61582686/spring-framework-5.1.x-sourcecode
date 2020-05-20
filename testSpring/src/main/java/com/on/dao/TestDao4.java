package com.on.dao;

import com.on.service.TestDaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class TestDao4 {
	
	private String name;
	
	@Autowired
	TestDaoService testDaoService;
	
	public TestDao4() {
	
	}
	
	public TestDao4(TestDao1 testDao1) {
	
	}
	
	public void query() {
		System.out.println(testDaoService.toString());
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
}
