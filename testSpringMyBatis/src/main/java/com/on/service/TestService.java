package com.on.service;

import com.on.dao.TestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestService {

	@Autowired
	TestMapper testMapper; //注入的是接口，通过

	public String findAll() {
		return testMapper.findAll();
	}

}
