package com.on.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 这种形式的Controller，在HandlerMapping的实现类中存放的形式是：路径+方法名。如下：
 * Key：index，Value：test()；
 * Key：index2，Value：test2()。
 *
 * 每个这样的类都用一个对应的HandlerMapping的实现类：RequestMappingHandlerMapping，来存放映射关系等信息。
 *
 * 使用时通过请求路径找到对应的方法，再通过反射去执行对象的方法。
 */
@Controller
public class ControllerByAnnotation {

	@RequestMapping("/index")
	@ResponseBody
	public Map<String, String> test1() {
		Map<String, String> map = new HashMap();
		map.put("state", "200");
		System.out.println("Annotation：index");
		return map;
	}

	@RequestMapping("/index2")
	@ResponseBody
	public Map<String, String> test2() {
		Map<String, String> map = new HashMap();
		map.put("state", "300");
		System.out.println("Annotation：index2");
		return map;
	}

}
