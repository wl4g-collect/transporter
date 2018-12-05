package io.transport.rest.interceptor;

//import javax.annotation.Resource;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
////import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//import org.springframework.web.servlet.ModelAndView;
//
//import io.transport.common.cache.JedisService;
//import io.transport.core.config.Configuration;
//import io.transport.rest.config.AccessProcessor;
//import io.transport.rest.controller.LoginController;
//
///**
// * Authentication interceptor
// * 
// * @author Wangl.sir <983708408@qq.com>
// * @version v1.0
// * @date 2018年5月14日
// * @since
// */
// @Component
//public class AuthInterceptor implements HandlerInterceptor {
//	final private static Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
//	final private static String[] REMOTE_ADDRS_K = { "x-forwarded-for", "Proxy-Client-IP", "WL-Proxy-Client-IP" };
//
//	@Resource
//	private AccessProcessor processor;
//	@Resource
//	private Configuration conf;
//	@Resource
//	private JedisService jedisService;
//
//	@Override
//	public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object arg2, Exception e)
//			throws Exception {
//		// Ignore process.
//		//
//	}
//
//	@Override
//	public void postHandle(HttpServletRequest req, HttpServletResponse resp, Object arg2, ModelAndView model)
//			throws Exception {
//		// Ignore process.
//		//
//	}
//
//	@Override
//	public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object arg2) throws Exception {
//		String uri = String.valueOf(req.getRequestURI()).trim();
//		uri = StringUtils.endsWith(uri, "/") ? uri.substring(0, uri.length() - 1) : uri;
//		String addr = String.valueOf(this.getRemoteAddress(req)).trim();
//		String token = String.valueOf(req.getParameter("token")).trim();
//
//		// All for black-and-white list certification.
//		if (this.processor.isPermittedRequest(addr))
//			return true;
//
//		// No authentication is required for the login interface.
//		if (StringUtils.equals(uri, LoginController.LOGIN_URI))
//			return true;
//
//		/*
//		 * Check token, Corresponding to
//		 * io.transport.rest.controller.LoginController.login()
//		 */
//		if (!this.jedisService.exists(conf.getCtlRestAuthPkey() + token))
//			throw new IllegalAccessException("Illegal access or token expiration. from token: " + token);
//		if (logger.isInfoEnabled())
//			logger.info("Access from remote address：{}", addr);
//
//		// throw new IllegalAccessException(
//		// "Illegal access, authentication failed or non white list. from
//		// address: " + addr);
//		return true;
//	}
//
//	/**
//	 * Get a remote address
//	 * 
//	 * @param req
//	 * @return
//	 */
//	private String getRemoteAddress(HttpServletRequest req) {
//		String remoteAddr = req.getRemoteAddr();
//		if (StringUtils.isEmpty(remoteAddr)) {
//			for (String k : REMOTE_ADDRS_K) {
//				remoteAddr = req.getHeader(k);
//				if (!StringUtils.isEmpty(remoteAddr))
//					break;
//			}
//		}
//		return remoteAddr;
//	}
//
//}
