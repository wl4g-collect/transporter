package io.transport.rest.controller;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.alibaba.fastjson.JSONObject;

@RestControllerAdvice
public class BasicController {
	final protected Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Wrapper outgoing information
	 * 
	 * @param t
	 * @return
	 */
	@ExceptionHandler(value = Throwable.class)
	private Object wrapException(Throwable t) {
		JSONObject ret = new JSONObject();
		if (t instanceof IllegalAccessException)
			ret.put("code", "401");
		else
			ret.put("code", "500");
		ret.put("data", ExceptionUtils.getRootCauseMessage(t));

		logger.error("Processing error: {}", t);
		return ret;
	}

}
