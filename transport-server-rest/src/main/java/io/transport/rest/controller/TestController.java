package io.transport.rest.controller;

import java.util.Arrays;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;

import io.transport.common.bean.PushMessageBean;
import io.transport.common.bean.PushMessageBean.RowKey;
//import io.transport.common.executor.SafeThreadPoolExecutors;
import io.transport.persistent.PersistentService;

@RestController("TransportTestController")
@RequestMapping("/test")
public class TestController {

	@Resource
	private PersistentService persistentService;

	@RequestMapping("test")
	public String test() {
		// SafeThreadPoolExecutors.hbasePersisPool.getKeepAliveTime();
		return "ok";
	}

	// @RequestMapping("put")
	public String put(String payload) {
		PushMessageBean msg = new PushMessageBean();
		msg.setMsgId(12345678);
		msg.setPayload(payload);
		msg.setRecTime(System.currentTimeMillis());

		RowKey rowKey = new RowKey();
		rowKey.setFromDeviceId("a12345678");
		rowKey.setToDeviceId("b12345678");
		rowKey.setToGroupId("c12345678");
		rowKey.setSentTime(System.currentTimeMillis());
		msg.setRowKey(rowKey);

		this.persistentService.batchSavePushMessage(Arrays.asList(msg));
		return "ok";
	}

	// @RequestMapping("getRowByMsgId")
	public String getRowByMsgId(String msgId) {

		return this.persistentService.findRowkey(Integer.parseInt(msgId));
	}

}
