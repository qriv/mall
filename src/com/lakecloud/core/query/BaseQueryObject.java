package com.lakecloud.core.query;

import org.springframework.web.servlet.ModelAndView;

/**
 * 
* <p>Title: BaseQueryObject.java</p>

* <p>Description:基础的查询对象类，包装了page信息和order信息 </p>

 */
public class BaseQueryObject extends QueryObject {

	public BaseQueryObject(String currentPage, ModelAndView mv, String orderBy,
			String orderType) {
		super(currentPage, mv, orderBy, orderType);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getQuery() {

		return super.getQuery();
	}
}
