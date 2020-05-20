package com.on.service;

import com.on.dao.TestDao;
import com.on.dao.TestDao1;
import com.on.dao.TestDao2;
import com.on.dao.TestDao3;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// 动态获取不同的Dao
@Service("testDaoService")
public class TestDaoService implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	public void query(String daoName) {
		if(daoName.equals("testDao")) {
			TestDao dao = (TestDao) applicationContext.getBean(daoName);
			dao.query();
		} else if(daoName.equals("com.on.dao.TestDao1")) {
			TestDao1 dao = (TestDao1) applicationContext.getBean(daoName);
			dao.query();
		} else if(daoName.equals("testDao2")) {
			TestDao2 dao = (TestDao2) applicationContext.getBean(daoName);
			dao.query();
		} else if(daoName.equals("testDao3")) {
			TestDao3 dao = (TestDao3) applicationContext.getBean(daoName);
			dao.query();
		}

	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
