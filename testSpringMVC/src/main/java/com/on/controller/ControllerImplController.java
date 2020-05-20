package com.on.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 这也是SpringMVC中Controller的一种实现方法，这样只能返回视图。
 * 这种形式的Controller，在HandlerMapping的实现类中存放的形式是：路径+类名
 * Key：impl，Value：ControllerImpl
 *
 * 每个类都用一个对应的HandlerMapping的实现类：BeanNameUrlHandlerMapping，来存放映射关系等信息。
 *
 * 使用时直接通过路径获取到对象，调用对象的handleRequest()方法即可。
 */
@Component("/controller")
public class ControllerImplController implements Controller {

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		System.out.println("Controller");
		return null;
	}

}
