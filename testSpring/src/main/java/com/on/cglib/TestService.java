package com.on.cglib;

public class TestService {

	public void query() {
		System.out.println("query");
	}

	public void query2() {
		query();
		System.out.println("query2");
	}

}
