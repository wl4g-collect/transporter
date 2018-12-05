package io.transport.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.transport.common.bean.Message;
import io.transport.common.bean.ChannelMetricsInfo;
import io.transport.core.MonitorService;

@RestController
@RequestMapping("/admin/api/")
public class AdminController extends BasicController {

	@Autowired
	private MonitorService monitorService;
	@Value("${spring.application.name:}")
	private String nodeName;

	@RequestMapping(value = "metrics", produces = "application/json;charset=UTF-8")
	public Message metrics() {
		ChannelMetricsInfo info = this.monitorService.metricsInfo();
		if (logger.isInfoEnabled())
			logger.info("获取到监控信息: {}", info);

		return info;
	}

}
