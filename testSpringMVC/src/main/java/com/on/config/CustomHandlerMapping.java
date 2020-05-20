package com.on.config;

import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

public class CustomHandlerMapping implements HandlerMapping {
	@Override
	public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {

		request.getRequestURI().endsWith(".html");
//		HandlerExecutionChain handlerExecutionChain = new HandlerExecutionChain();


		return null;
	}
}
