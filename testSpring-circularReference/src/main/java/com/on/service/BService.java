package com.on.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BService {

	@Autowired
	private AService aService;

	public void run() {
		System.out.println("I'm b service");
	}

}
