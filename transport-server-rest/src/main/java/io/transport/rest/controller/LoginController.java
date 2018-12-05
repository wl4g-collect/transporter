package io.transport.rest.controller;

import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.transport.common.bean.LoginMessage;
import io.transport.common.bean.Message;
import io.transport.common.cache.JedisService;
import io.transport.core.config.Configuration;
import io.transport.rest.config.AccessProcessor;

@RestController
@RequestMapping(LoginController.ADMIN_URI)
public class LoginController extends BasicController {
	final public static String ADMIN_URI = "/admin/";
	final public static String LOGIN_URI = ADMIN_URI + "login";
	final public static int AUTH_TIMEOUT_SEC = 30 * 60 * 60; // second

	@Resource
	private Configuration conf;
	@Resource
	private AccessProcessor processor;
	@Resource
	private JedisService jedisService;

	@RequestMapping(value = LoginController.LOGIN_URI, produces = "application/json;charset=UTF-8")
	public Message login(String username, String password) {
		LoginMessage resp = new LoginMessage();
		// 1.1 Check UserName & password.
		String s = username + ":" + password;
		if (this.processor.getUserList().contains(s)) {
			String token = UUID.randomUUID().toString().replaceAll("-", "");
			resp.getAuthInfo().setToken(token);
			// Save token to cache.
			this.jedisService.set(conf.getCtlRestAuthPkey() + token, username, AUTH_TIMEOUT_SEC);
		} else {
			resp.setCode("-401");
			resp.setMsg("Authentication failed, username or password incorrect.");
			logger.warn("Authentication failed, username={}, password={}", username, password);
		}
		return resp;
	}

}
