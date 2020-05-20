package com.on;

import com.on.application.TestSpringApplication;
import org.apache.catalina.LifecycleException;

public class TestSpringBoot {

	public static void main(String[] args) {
		try {
			TestSpringApplication.run();
		} catch (LifecycleException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
