package com.lakecloud.manage.seller.action;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.lakecloud.core.annotation.SecurityMapping;
import com.lakecloud.core.domain.virtual.SysMap;
import com.lakecloud.core.mv.JModelAndView;
import com.lakecloud.core.query.support.IPageList;
import com.lakecloud.core.security.support.SecurityUserHolder;
import com.lakecloud.core.tools.CommUtil;
import com.lakecloud.core.tools.WebForm;
import com.lakecloud.foundation.domain.GoldLog;
import com.lakecloud.foundation.domain.GoldRecord;
import com.lakecloud.foundation.domain.Payment;
import com.lakecloud.foundation.domain.PredepositLog;
import com.lakecloud.foundation.domain.User;
import com.lakecloud.foundation.domain.query.GoldLogQueryObject;
import com.lakecloud.foundation.domain.query.GoldRecordQueryObject;
import com.lakecloud.foundation.service.IGoldLogService;
import com.lakecloud.foundation.service.IGoldRecordService;
import com.lakecloud.foundation.service.IPaymentService;
import com.lakecloud.foundation.service.IPredepositLogService;
import com.lakecloud.foundation.service.ISysConfigService;
import com.lakecloud.foundation.service.IUserConfigService;
import com.lakecloud.foundation.service.IUserService;
import com.lakecloud.pay.tools.PayTools;

/**
 * 
* <p>Title: GoldSellerAction.java</p>

* <p>Description: 卖家金币控制器</p>

* <p>Copyright: Copyright (c) 2014</p>

* <p>Company: 江苏太湖云计算信息技术股份有限公司 www.chinacloud.net</p>

* @author erikzhang

* @date 2014-5-8

* @version LakeCloud_C2C 1.4
 */
@Controller
public class GoldSellerAction {
	@Autowired
	private ISysConfigService configService;
	@Autowired
	private IUserConfigService userConfigService;
	@Autowired
	private IPaymentService paymentService;
	@Autowired
	private IGoldRecordService goldRecordService;
	@Autowired
	private IGoldLogService goldLogService;
	@Autowired
	private IUserService userService;
	@Autowired
	private IPredepositLogService predepositLogService;
	@Autowired
	private PayTools payTools;

	@SecurityMapping(title = "金币兑换", value = "/seller/gold_record.htm*", rtype = "seller", rname = "金币管理", rcode = "gold_seller", rgroup = "其他设置")
	@RequestMapping("/seller/gold_record.htm")
	public ModelAndView gold_record(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mv = new JModelAndView(
				"user/default/usercenter/gold_record.html",
				configService.getSysConfig(),
				this.userConfigService.getUserConfig(), 0, request, response);
		if (!this.configService.getSysConfig().isGold()) {
			mv = new JModelAndView("error.html", configService.getSysConfig(),
					this.userConfigService.getUserConfig(), 1, request,
					response);
			mv.addObject("op_title", "商城未开启金币功能");
			mv.addObject("url", CommUtil.getURL(request) + "/seller/index.htm");
		} else {
			Map params = new HashMap();
			params.put("type", "admin");
			params.put("install", true);
			params.put("mark", "alipay_wap");
			params.put("mark2", "weixin");
			List<Payment> payments = this.paymentService
					.query("select obj from Payment obj where obj.type=:type and obj.mark!=:mark and obj.mark!=:mark2 and obj.install=:install",
							params, -1, -1);
			String gold_session = CommUtil.randomString(32);
			request.getSession(false)
					.setAttribute("gold_session", gold_session);
			mv.addObject("gold_session", gold_session);
			mv.addObject("payments", payments);
		}
		return mv;
	}

	@SecurityMapping(title = "金币兑换保存", value = "/buyer/gold_record_save.htm*", rtype = "seller", rname = "金币管理", rcode = "gold_seller", rgroup = "其他设置")
	@RequestMapping("/seller/gold_record_save.htm")
	public ModelAndView gold_record_save(HttpServletRequest request,
			HttpServletResponse response, String id, String gold_payment,
			String gold_exchange_info, String gold_session) {
		ModelAndView mv = new JModelAndView("line_pay.html",
				configService.getSysConfig(),
				this.userConfigService.getUserConfig(), 1, request, response);
		if (this.configService.getSysConfig().isGold()) {
			String gold_session1 = CommUtil.null2String(request.getSession(
					false).getAttribute("gold_session"));
			if (!gold_session1.equals("") && gold_session1.equals(gold_session)) {// 防止表单重复提交
				request.getSession(false).removeAttribute("gold_session");
				WebForm wf = new WebForm();
				GoldRecord obj = null;
				if (CommUtil.null2String(id).equals("")) {
					obj = wf.toPo(request, GoldRecord.class);
					obj.setAddTime(new Date());
					if (gold_payment.equals("outline")) {
						obj.setGold_pay_status(1);
					} else {
						obj.setGold_pay_status(0);
					}
					obj.setGold_sn("gold"
							+ CommUtil.formatTime("yyyyMMddHHmmss", new Date())
							+ SecurityUserHolder.getCurrentUser().getId());
					obj.setGold_user(SecurityUserHolder.getCurrentUser());
					obj.setGold_count(obj.getGold_money()
							* this.configService.getSysConfig()
									.getGoldMarketValue());
					this.goldRecordService.save(obj);
				} else {
					GoldRecord gr = this.goldRecordService.getObjById(CommUtil
							.null2Long(id));
					obj = (GoldRecord) wf.toPo(request, gr);
					this.goldRecordService.update(obj);
				}
				if (gold_payment.equals("outline")) {
					GoldLog log = new GoldLog();
					if (id != null && !id.equals("")) {
						log = obj.getLog();
					}
					log.setAddTime(new Date());
					log.setGl_payment(obj.getGold_payment());
					log.setGl_content("线下支付");
					log.setGl_money(obj.getGold_money());
					log.setGl_count(obj.getGold_count());
					log.setGl_type(0);
					log.setGl_user(obj.getGold_user());
					log.setGr(obj);
					this.goldLogService.save(log);
					mv = new JModelAndView("success.html",
							configService.getSysConfig(),
							this.userConfigService.getUserConfig(), 1, request,
							response);
					mv.addObject("op_title", "线下支付提交成功，等待审核");
					mv.addObject("url", CommUtil.getURL(request)
							+ "/seller/gold_record_list.htm");
				} else if (gold_payment.equals("balance")) {
					User user = this.userService.getObjById(SecurityUserHolder
							.getCurrentUser().getId());
					double balance = CommUtil.null2Double(user
							.getAvailableBalance());
					if (balance > obj.getGold_money()) {
						user.setGold(user.getGold() + obj.getGold_count());// 增加金币数量
						user.setAvailableBalance(BigDecimal.valueOf(CommUtil
								.subtract(user.getAvailableBalance(),
										obj.getGold_money())));// 减少用户预存款
						this.userService.update(user);
						// 兑换完成更新金币记录信息
						obj.setGold_pay_status(2);
						obj.setGold_status(1);
						this.goldRecordService.update(obj);
						// 记录预付款日志
						PredepositLog pre_log = new PredepositLog();
						pre_log.setAddTime(new Date());
						pre_log.setPd_log_user(user);
						pre_log.setPd_op_type("兑换金币");
						pre_log.setPd_log_amount(BigDecimal.valueOf(-obj
								.getGold_money()));
						pre_log.setPd_log_info("兑换金币物减少可用预存款");
						pre_log.setPd_type("可用预存款");
						this.predepositLogService.save(pre_log);
						// 记录金币操作日志
						GoldLog log = new GoldLog();
						log.setAddTime(new Date());
						log.setGl_payment(obj.getGold_payment());
						log.setGl_content("预存款支付");
						log.setGl_money(obj.getGold_money());
						log.setGl_count(obj.getGold_count());
						log.setGl_type(0);
						log.setGl_user(obj.getGold_user());
						log.setGr(obj);
						this.goldLogService.save(log);
						mv = new JModelAndView("success.html",
								configService.getSysConfig(),
								this.userConfigService.getUserConfig(), 1,
								request, response);
						mv.addObject("op_title", "金币兑换成功");
						mv.addObject("url", CommUtil.getURL(request)
								+ "/seller/gold_record_list.htm");
					} else {
						mv = new JModelAndView("error.html",
								configService.getSysConfig(),
								this.userConfigService.getUserConfig(), 1,
								request, response);
						mv.addObject("op_title", "预存款金额不足");
						mv.addObject("url", CommUtil.getURL(request)
								+ "/seller/gold_record.htm");
					}
				} else {
					mv.addObject("payType", gold_payment);
					mv.addObject("type", "gold");
					mv.addObject("url", CommUtil.getURL(request));
					mv.addObject("payTools", payTools);
					mv.addObject("gold_id", obj.getId());
					Map params = new HashMap();
					params.put("install", true);
					params.put("mark", obj.getGold_payment());
					params.put("type", "admin");
					List<Payment> payments = this.paymentService
							.query("select obj from Payment obj where obj.install=:install and obj.mark=:mark and obj.type=:type",
									params, -1, -1);
					mv.addObject("payment_id", payments.size() > 0 ? payments
							.get(0).getId() : new Payment());
				}
			} else {
				mv = new JModelAndView("error.html",
						configService.getSysConfig(),
						this.userConfigService.getUserConfig(), 1, request,
						response);
				mv.addObject("op_title", "您已经提交过该请求");
				mv.addObject("url", CommUtil.getURL(request)
						+ "/seller/gold_record_list.htm");
			}
		} else {
			mv = new JModelAndView("error.html", configService.getSysConfig(),
					this.userConfigService.getUserConfig(), 1, request,
					response);
			mv.addObject("op_title", "系统未开启金币");
			mv.addObject("url", CommUtil.getURL(request) + "/seller/index.htm");
		}
		return mv;
	}

	@SecurityMapping(title = "金币兑换", value = "/seller/gold_record_list.htm*", rtype = "seller", rname = "金币管理", rcode = "gold_seller", rgroup = "其他设置")
	@RequestMapping("/seller/gold_record_list.htm")
	public ModelAndView gold_record_list(HttpServletRequest request,
			HttpServletResponse response, String currentPage) {
		ModelAndView mv = new JModelAndView(
				"user/default/usercenter/gold_record_list.html",
				configService.getSysConfig(),
				this.userConfigService.getUserConfig(), 0, request, response);
		if (!this.configService.getSysConfig().isGold()) {
			mv = new JModelAndView("error.html", configService.getSysConfig(),
					this.userConfigService.getUserConfig(), 1, request,
					response);
			mv.addObject("op_title", "系统未开启金币");
			mv.addObject("url", CommUtil.getURL(request) + "/seller/index.htm");
		} else {
			GoldRecordQueryObject qo = new GoldRecordQueryObject(currentPage,
					mv, "addTime", "desc");
			qo.addQuery("obj.gold_user.id", new SysMap("user_id",
					SecurityUserHolder.getCurrentUser().getId()), "=");
			IPageList pList = this.goldRecordService.list(qo);
			CommUtil.saveIPageList2ModelAndView("", "", "", pList, mv);
		}
		return mv;
	}

	@SecurityMapping(title = "金币兑换支付", value = "/seller/gold_pay.htm*", rtype = "seller", rname = "金币管理", rcode = "gold_seller", rgroup = "其他设置")
	@RequestMapping("/seller/gold_pay.htm")
	public ModelAndView gold_pay(HttpServletRequest request,
			HttpServletResponse response, String id) {
		ModelAndView mv = new JModelAndView(
				"user/default/usercenter/gold_pay.html",
				configService.getSysConfig(),
				this.userConfigService.getUserConfig(), 0, request, response);
		if (this.configService.getSysConfig().isGold()) {
			GoldRecord obj = this.goldRecordService.getObjById(CommUtil
					.null2Long(id));
			if (obj.getGold_user().getId()
					.equals(SecurityUserHolder.getCurrentUser().getId())) {
				String gold_session = CommUtil.randomString(32);
				request.getSession(false).setAttribute("gold_session",
						gold_session);
				mv.addObject("gold_session", gold_session);
				mv.addObject("obj", obj);
			} else {
				mv = new JModelAndView("error.html",
						configService.getSysConfig(),
						this.userConfigService.getUserConfig(), 1, request,
						response);
				mv.addObject("op_title", "参数错误，您没有该兑换信息");
				mv.addObject("url", CommUtil.getURL(request)
						+ "/seller/gold_record_list.htm");
			}
		} else {
			mv = new JModelAndView("error.html", configService.getSysConfig(),
					this.userConfigService.getUserConfig(), 1, request,
					response);
			mv.addObject("op_title", "系统未开启金币");
			mv.addObject("url", CommUtil.getURL(request) + "/seller/index.htm");
		}
		return mv;
	}

	@SecurityMapping(title = "金币兑换详情", value = "/seller/gold_view.htm*", rtype = "seller", rname = "金币管理", rcode = "gold_seller", rgroup = "其他设置")
	@RequestMapping("/seller/gold_view.htm")
	public ModelAndView gold_view(HttpServletRequest request,
			HttpServletResponse response, String id) {
		ModelAndView mv = new JModelAndView(
				"user/default/usercenter/gold_view.html",
				configService.getSysConfig(),
				this.userConfigService.getUserConfig(), 0, request, response);
		if (this.configService.getSysConfig().isGold()) {
			GoldRecord obj = this.goldRecordService.getObjById(CommUtil
					.null2Long(id));
			if (obj.getGold_user().getId()
					.equals(SecurityUserHolder.getCurrentUser().getId())) {
				mv.addObject("obj", obj);
			} else {
				mv = new JModelAndView("error.html",
						configService.getSysConfig(),
						this.userConfigService.getUserConfig(), 1, request,
						response);
				mv.addObject("op_title", "参数错误，您没有该兑换信息");
				mv.addObject("url", CommUtil.getURL(request)
						+ "/seller/gold_record_list.htm");
			}
		} else {
			mv = new JModelAndView("error.html", configService.getSysConfig(),
					this.userConfigService.getUserConfig(), 1, request,
					response);
			mv.addObject("op_title", "系统未开启金币");
			mv.addObject("url", CommUtil.getURL(request) + "/seller/index.htm");
		}
		return mv;
	}

	@SecurityMapping(title = "兑换日志", value = "/seller/gold_log.htm*", rtype = "seller", rname = "金币管理", rcode = "gold_seller", rgroup = "其他设置")
	@RequestMapping("/seller/gold_log.htm")
	public ModelAndView gold_log(HttpServletRequest request,
			HttpServletResponse response, String currentPage) {
		ModelAndView mv = new JModelAndView(
				"user/default/usercenter/gold_log.html",
				configService.getSysConfig(),
				this.userConfigService.getUserConfig(), 0, request, response);
		if (!this.configService.getSysConfig().isGold()) {
			mv = new JModelAndView("error.html", configService.getSysConfig(),
					this.userConfigService.getUserConfig(), 1, request,
					response);
			mv.addObject("op_title", "系统未开启金币");
			mv.addObject("url", CommUtil.getURL(request) + "/seller/index.htm");
		} else {
			GoldLogQueryObject qo = new GoldLogQueryObject(currentPage, mv,
					"addTime", "desc");
			qo.addQuery("obj.gl_user.id", new SysMap("user_id",
					SecurityUserHolder.getCurrentUser().getId()), "=");
			IPageList pList = this.goldLogService.list(qo);
			CommUtil.saveIPageList2ModelAndView("", "", "", pList, mv);
			mv.addObject("user", this.userService.getObjById(SecurityUserHolder
					.getCurrentUser().getId()));
		}
		return mv;
	}
}