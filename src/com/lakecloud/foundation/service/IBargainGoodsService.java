﻿package com.lakecloud.foundation.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.lakecloud.core.query.support.IPageList;
import com.lakecloud.core.query.support.IQueryObject;

import com.lakecloud.foundation.domain.BargainGoods;

public interface IBargainGoodsService {
	/**
	 * 保存一个BargainGoods，如果保存成功返回true，否则返回false
	 * 
	 * @param instance
	 * @return 是否保存成功
	 */
	boolean save(BargainGoods instance);
	
	/**
	 * 根据一个ID得到BargainGoods
	 * 
	 * @param id
	 * @return
	 */
	BargainGoods getObjById(Long id);
	
	/**
	 * 删除一个BargainGoods
	 * 
	 * @param id
	 * @return
	 */
	boolean delete(Long id);
	
	/**
	 * 批量删除BargainGoods
	 * 
	 * @param ids
	 * @return
	 */
	boolean batchDelete(List<Serializable> ids);
	
	/**
	 * 通过一个查询对象得到BargainGoods
	 * 
	 * @param properties
	 * @return
	 */
	IPageList list(IQueryObject properties);
	
	/**
	 * 更新一个BargainGoods
	 * 
	 * @param id
	 *            需要更新的BargainGoods的id
	 * @param dir
	 *            需要更新的BargainGoods
	 */
	boolean update(BargainGoods instance);
	/**
	 * 
	 * @param query
	 * @param params
	 * @param begin
	 * @param max
	 * @return
	 */
	List<BargainGoods> query(String query, Map params, int begin, int max);
}
