package com.on.config;

import com.on.dao.TestDao1;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class TestImportSelector implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		/**
		 * 向Spring中注册TestDao1类
		 * 返回一个类的路径，然后Spring通过类的路径去获取到该类
		 */
		return new String[]{TestDao1.class.getName()};
	}

}
