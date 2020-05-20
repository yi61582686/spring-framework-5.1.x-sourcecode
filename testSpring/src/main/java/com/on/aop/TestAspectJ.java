package com.on.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class TestAspectJ {

	@Pointcut("execution(* com.on.service..*.*(..))") // service包及其子包
//	@Pointcut("execution(* com.on.service.*.*(..))") // 只是service包下的类
	public void pointCut(){}
	
	@Before("pointCut()")
	public void befroreAdvice() {
		System.out.println("------before advice------");
	}


}
