package com.on.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 这也是SpringMVC中Controller的一种实现方法，这样返回的内容是通过Response写回给客户端的。
 * 这种形式的Controller，在HandlerMapping的实现类中存放的形式是，路径+类名。如下：
 * Key：impl，Value：ControllerImpl。
 *
 * 每个类都用一个对应的HandlerMapping的实现类：BeanNameUrlHandlerMapping，来存放映射关系等信息。
 *
 * 使用时直接通过路径获取到对象，调用对象的handleRequest()方法即可。
 */
@Component("/handler")
public class ControllerImplHttpRequestHandler implements HttpRequestHandler {
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("HttpRequestHandler");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("handler：HttpRequestHandler");
		response.setContentType("text/html;charset=utf-8");
	}
}
