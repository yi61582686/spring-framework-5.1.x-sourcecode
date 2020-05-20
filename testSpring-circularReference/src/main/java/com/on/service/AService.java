package com.on.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("aService")
public class AService {

	@Autowired
	private BService bservice;

	public void run() {
		System.out.println("I'm a service");
	}

}
