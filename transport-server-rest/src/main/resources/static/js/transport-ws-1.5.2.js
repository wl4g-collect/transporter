/**
 * Transport v1.5.1 | (c) 2017, 2022 Transporter Foundation, Inc.
 * Copyright 2017-2032 Wangl, Inc.
 * Licensed under MIT (https://github.com/wang4ever/transporter/blob/master/LICENSE)
 */
(function(window, document) {
	//
	// 1.0 全局配置
	//
	var setting = {
		/* 集群地址列表*/
		wsClusterUris: new Array(),
		/* 集群节点负载连接记录列表*/
		wsClusterConnectRecords: new Array(),
		/* 集群负载均衡筛选可用节点间隔时间(ms), (某节点间隔多久前连接过才被再次使用)*/
		wsClusterNodeChooseInterval: 10 * 60 * 1000,
		/* 集群负载均衡节点连续重连次数 */
		wsClusterNodeRetry: 3,
		/* 当前连接的节点 */
		currtNode: null,
		/* Transporter平台应用唯一ID*/
		appId: "",
		/* 设备连接唯一Id*/
		deviceId: "",
		/* 设备连接所属组Id*/
		groupId: "",
		/* 是否debug模式*/
		isDebug: false,
		/* Socket连接对象*/
		socket: null,
		/* On message回调*/
		onMessage: function(window) {},
		/* 发送结果回调*/
		onResult: function(window, window) {},
		/* 心跳监视器Timer*/
		watchDogTimer: null,
		/* 发送超时时间(ms)*/
		senderTimeout: 3000,
		/* (上一次)最近收到消息时间戳(ms)*/
		lastReceivedTime: new Date().getTime(),
		/* 心跳监视频率(ms)*/
		keepAliveWatchDogDelay: (5 * 1000),
		/* 心跳发送频率(默认:20~70s)*/
		keepAliveDelay: function(){
			var delay = parseInt((Math.random()*50+20))*1000;
			if(setting.isDebug) console.log("Gen random `keepAliveDelay` is "+ delay);
			return delay;
		},
		/* 心跳超时间隔(keepAliveTimeout应大于keepAliveDelay)*/
		keepAliveTimeout: (150 * 1000),
		/* 重试连接延时等待随机间隔(默认:20~40s), 采用随机防止雪崩*/
		retryConnectDelay: function(){
			var delay = parseInt((Math.random()*20+20))*1000;
			if(setting.isDebug) console.log("Gen random `retryConnectDelay` is "+ delay);
			return delay;
		}
	};

	/**
	 * 1.1 心跳监视器(WatchDog)
	 */
	var keepAliveWatchDog = function() {
		if(setting.watchDogTimer == null) {
			if(setting.isDebug) console.log("The 'WatchDog' monitor starting...");

			setting.watchDogTimer = window.setInterval(function() {
				// 若最后一次接收数据的时间距离now大于keepAliveTimeout, 则认为连接中断
				var diff = new Date().getTime() - setting.lastReceivedTime;
				if(diff > setting.keepAliveTimeout) {
					if(setting.isDebug) console.log("Heart timeout reconnecting... (lastReceivedTime=" + setting.lastReceivedTime + ")");
					// Stop watchDog timer.
					stopWatchDogTimer();

					// Retry connect.
					window.setTimeout(function(){
						reconnect(setting);
					}, setting.retryConnectDelay());
				} else
					if(setting.isDebug) console.log("The connect(heartbeat) is normal.");

			}, setting.keepAliveWatchDogDelay);
		}
	};

	/**
	 * 1.2 心跳发送器
	 */
	var heartbeater = function(running) {
		if(running) {
			if(setting.isDebug) console.log("A heart beater is being sent...");
			if(isOpen()) {
				// 链路检测数据包
				var activeReq = { head: { actionId: 3 } };
				setting.socket.send(JSON.stringify(activeReq));
			} else {
				if(setting.isDebug) console.warn("Heart beater failure connection disconnect.");
			}
		}
		if(setting.isDebug) console.log("The heart beater is sent to complete.");
		window.setTimeout(function(){ heartbeater(true); }, setting.keepAliveDelay());
	};

	//
	// 1.3 连接核心函数
	//
	var reconnect = function(options) {
		if (!options) options = {};

		// Overwrite and define settings with options if they exist.
		for(var k in options){
			var v = options[k];
			if(v != null && v.length != 0)
				setting[k] = v;
		}
		// Check required parameters.
		if(StringUtils.isAnyEmpty(setting.wsClusterUris, setting.appId, setting.deviceId, setting.groupId)){
			throw Error("Connection failure, required parameter 'wsClusterUris/appId/deviceId/groupId' is null.");
		}

		// Initial WebSocket
		try {
			// Get LB calculator current node.
			setting.currtNode = HashRoutingLBEngine.determineCurrentLookupNode();
			if(setting.isDebug) console.log("Connect to '" + setting.currtNode.wsUri + "'...");

			if(StringUtils.isEmpty(setting.currtNode)) {
				console.error("Load balancer gets the current server node is null.");
				return;
			}
			if(isOpen()) {
				if(setting.isDebug) console.log("Using an established connection.");
			} else { // 非OPEN状态才新建连接
				if(window.WebSocket)
					setting.socket = new WebSocket(encodeURI(setting.currtNode.wsUri));
				else if(window.MozWebSocket) 
					setting.socket = new MozWebSocket(encodeURI(setting.currtNode.wsUri));
            }
		} catch(err) {
			console.error("Connect fail." + err);
			HashRoutingLBEngine.onConnectFailed(setting.currtNode);

			// Retry connect.
			window.setTimeout(function(){
				reconnect(setting);
			}, setting.retryConnectDelay());
		}

		// WebSocket connect.
		setting.socket.onopen = function(event) {
			if(setting.isDebug) console.log("Connect successfully.");
			// 1.1 Start heart timer.
			heartbeater(false);

			// 1.2 Start watch dog timer.
			keepAliveWatchDog();

			// 1.3 Login connect.
			login();
		};

		// on error.
		setting.socket.onerror = function(event) {
			if(setting.isDebug) console.log("Connect error.");
			// if(setting.isDebug) console.log(event);

			// Save connect error record.
			HashRoutingLBEngine.onConnectFailed(setting.currtNode);

			// Retry connect.
			window.setTimeout(function(){
				reconnect(setting);
			}, setting.retryConnectDelay());
		};

		// on close.
		setting.socket.onclose = function(event) {
			if(setting.isDebug) console.log("Connect closed.");
			// if(setting.isDebug) console.log(event);
			// 重置接收数据包时间戳(让下一个心跳监视周期重连)
			setting.lastReceivedTime = 0;
		};

		// Receive messages.
		setting.socket.onmessage = function(event) {
			if(setting.isDebug) console.log("On message: '" + event.data + "'");
			// Update the last received message time(For heart beat check).
			setting.lastReceivedTime = new Date().getTime();
			// Processing.
			if(typeof(event.data) == "string"){
				var json = JSON.parse(event.data);
				if(json != null && json.head != null){
					// Message routing (action)
					switch (json.head.actionId) {
					case 2: // CONNECT_RESP
						if(setting.isDebug) { 
							console.log("Login connection successfully.");
						}
						// If the server is deployed in ROUTING mode, login success will
						// return cluster node list information.
						//
						if(json["hostAndPorts"] != undefined){
							for(var i=0; i<json.hostAndPorts.length; i++)
								setting.wsClusterUris.push(json.hostAndPorts[i]);
						}
						break;
					case 4: // ACTIVE_RESP
						if(setting.isDebug) console.log("It's a heart beat.");
						break;
					case 5: // TRANSPORT
						setting.onMessage(json.payload); // 服务器推送来的消息
						break;
					case 6: // TRANSPORTACK_RESP
						// Ignore ack messages.
						//
						break;
					case 8: // RESULT_RESP
						// Check authentication error.
						if(json.code == -401){
							console.warn("Failed authentication.");
							// Stop watchDog timer.
							stopWatchDogTimer();
							return;
						}
						setting.onResult(json.code, json.message);
						break;
					default:
						console.error("System exception, unknown type message." + json.head);
					}
				} else
					console.error("System exception, illegal format information." + event.data);
			} else
				console.error("System exception, type messages that are not supported." + event.data);
		};
	};

	/**
	 * 1.4 请求登录认证
	 */
	var login = function() {
		if(isOpen()) {
			// wrapper connect message.
			var connectReq = {
				head: { actionId: 1 },
				appId: setting.appId,
				deviceInfo: { groupId: setting.groupId, deviceId: setting.deviceId, deviceType: "Browser"}
			};
			setting.socket.send(JSON.stringify(connectReq));
		} else {
			console.warn("Login failure, invalid connection.");
			// Close connect.
			connectClose();
			// Reconnect.
			window.setTimeout(function(){
				reconnect(setting);
			}, setting.retryConnectDelay());
		}
	};

	/**
	 * 1.5 Close connect.
	 */
	var connectClose = function() {
		try {
			if(isOpen()) {
				setting.socket.close();
			} else {
				console.warn("Login failure, invalid connection.");
			}
		} catch(err) {
			console.error("Close connect fail." + err);
		}
	};

	/**
	 * 1.6 Check connect is open.
	 */
	var isOpen = function() {
		return (setting.socket != null && setting.socket.readyState == WebSocket.OPEN);
	};

	/**
	 * 1.7 Stop watchDog timer.
	 */
	var stopWatchDogTimer = function() {
		if(setting.watchDogTimer != null) {
			window.clearInterval(setting.watchDogTimer);
			setting.watchDogTimer = null;
		}
	};

	//
	// 2.0 提供对外调用接口
	//
	window.Transporter = {
		init: function(options){ // 2.1 绑定初始化连接方法.
			reconnect(options);
		},
		send: function(params){
			params = params || {};
			var retryCount = 0;
			var senderTimer = window.setInterval(function(){
				++retryCount;
				if(isOpen()) {
					// 2.2 发起传输请求
					var transportReq = {
						head: { actionId: 5 },
						fromDeviceId: setting.deviceId,
						toDeviceId: params.toDeviceId,
						toGroupId: params.toGroupId,
						payload: params.payload,
					};
					setting.socket.send(JSON.stringify(transportReq));

					// 2.3 Clear interval.
					window.clearInterval(senderTimer);

				} else if (retryCount >= (setting.senderTimeout/100)) { // 发送超时
					// 2.5 Clear interval.
					window.clearInterval(senderTimer);

					// 2.6 Invoke err callback.
					setting.onResult(-1, "Retry sending timeout.");
				}
			}, 100);
		}
	};

	//
	// 3.1 字符串工具
	//
	var StringUtils = {
		isEmpty: function(param){
			if(!StringUtils.isArray(param))
				return (param == null || param == undefined || param == '' || param.length == 0);
			else
				return param.length == 0;
		},
		isAnyEmpty: function(params){
			for(var i = 0; i < arguments.length; i++){
				if(StringUtils.isEmpty(arguments[i]))
					return true;
			}
			return false;
		},
		isArray : function (arr) {
		    return Object.prototype.toString.call(arr) === '[object Array]';
		}
	};

	//
	// 3.2 CRC16工具
	//
	var CRC16Utils = {
		_auchCRCHi : [
		    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
		    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
		    0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
		    0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
		    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
		    0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41,
		    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
		    0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
		    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
		    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40,
		    0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
		    0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
		    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
		    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40,
		    0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
		    0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
		    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
		    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
		    0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
		    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
		    0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
		    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40,
		    0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
		    0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
		    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
		    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40
		],
		_auchCRCLo : [
		    0x00, 0xC0, 0xC1, 0x01, 0xC3, 0x03, 0x02, 0xC2, 0xC6, 0x06,
		    0x07, 0xC7, 0x05, 0xC5, 0xC4, 0x04, 0xCC, 0x0C, 0x0D, 0xCD,
		    0x0F, 0xCF, 0xCE, 0x0E, 0x0A, 0xCA, 0xCB, 0x0B, 0xC9, 0x09,
		    0x08, 0xC8, 0xD8, 0x18, 0x19, 0xD9, 0x1B, 0xDB, 0xDA, 0x1A,
		    0x1E, 0xDE, 0xDF, 0x1F, 0xDD, 0x1D, 0x1C, 0xDC, 0x14, 0xD4,
		    0xD5, 0x15, 0xD7, 0x17, 0x16, 0xD6, 0xD2, 0x12, 0x13, 0xD3,
		    0x11, 0xD1, 0xD0, 0x10, 0xF0, 0x30, 0x31, 0xF1, 0x33, 0xF3,
		    0xF2, 0x32, 0x36, 0xF6, 0xF7, 0x37, 0xF5, 0x35, 0x34, 0xF4,
		    0x3C, 0xFC, 0xFD, 0x3D, 0xFF, 0x3F, 0x3E, 0xFE, 0xFA, 0x3A,
		    0x3B, 0xFB, 0x39, 0xF9, 0xF8, 0x38, 0x28, 0xE8, 0xE9, 0x29,
		    0xEB, 0x2B, 0x2A, 0xEA, 0xEE, 0x2E, 0x2F, 0xEF, 0x2D, 0xED,
		    0xEC, 0x2C, 0xE4, 0x24, 0x25, 0xE5, 0x27, 0xE7, 0xE6, 0x26,
		    0x22, 0xE2, 0xE3, 0x23, 0xE1, 0x21, 0x20, 0xE0, 0xA0, 0x60,
		    0x61, 0xA1, 0x63, 0xA3, 0xA2, 0x62, 0x66, 0xA6, 0xA7, 0x67,
		    0xA5, 0x65, 0x64, 0xA4, 0x6C, 0xAC, 0xAD, 0x6D, 0xAF, 0x6F,
		    0x6E, 0xAE, 0xAA, 0x6A, 0x6B, 0xAB, 0x69, 0xA9, 0xA8, 0x68,
		    0x78, 0xB8, 0xB9, 0x79, 0xBB, 0x7B, 0x7A, 0xBA, 0xBE, 0x7E,
		    0x7F, 0xBF, 0x7D, 0xBD, 0xBC, 0x7C, 0xB4, 0x74, 0x75, 0xB5,
		    0x77, 0xB7, 0xB6, 0x76, 0x72, 0xB2, 0xB3, 0x73, 0xB1, 0x71,
		    0x70, 0xB0, 0x50, 0x90, 0x91, 0x51, 0x93, 0x53, 0x52, 0x92,
		    0x96, 0x56, 0x57, 0x97, 0x55, 0x95, 0x94, 0x54, 0x9C, 0x5C,
		    0x5D, 0x9D, 0x5F, 0x9F, 0x9E, 0x5E, 0x5A, 0x9A, 0x9B, 0x5B,
		    0x99, 0x59, 0x58, 0x98, 0x88, 0x48, 0x49, 0x89, 0x4B, 0x8B,
		    0x8A, 0x4A, 0x4E, 0x8E, 0x8F, 0x4F, 0x8D, 0x4D, 0x4C, 0x8C,
		    0x44, 0x84, 0x85, 0x45, 0x87, 0x47, 0x46, 0x86, 0x82, 0x42,
		    0x43, 0x83, 0x41, 0x81, 0x80, 0x40
		],
		CRC16 : function (buffer) {
			var ucCRCHi = (0xffff & 0xff00) >> 8;
			var ucCRCLo = 0xffff & 0x00ff;
			var iIndex;
			for (var i = 0; i < buffer.length; ++i) {
				iIndex = (ucCRCLo ^ buffer[i]) & 0x00ff;
				ucCRCLo = ucCRCHi ^ CRC16Utils._auchCRCHi[iIndex];
				ucCRCHi = CRC16Utils._auchCRCLo[iIndex];
			}
			return ((ucCRCHi & 0x00ff) << 8) | (ucCRCLo & 0x00ff) & 0xffff;
		},
		crc16Modbus : function (str) {
		    return CRC16Utils.CRC16(StringUtils.isArray(str) ? str : CRC16Utils.strToByte(str));
		},
		strToByte : function (str) {
		    var tmp = str.split(''), arr = [];
		    for (var i = 0, c = tmp.length; i < c; i++) {
		        var j = encodeURI(tmp[i]);
		        if (j.length == 1) {
		            arr.push(j.charCodeAt());
		        } else {
		            var b = j.split('%');
		            for (var m = 1; m < b.length; m++) {
		                arr.push(parseInt('0x' + b[m]));
		            }
		        }
		    }
		    return arr;
		}
	};

	//
	// 3.3 负载均衡Hash工具
	//
	var HashRoutingLBEngine = {
		determineCurrentLookupNode : function(){
			var preselectedNodes = new Array(); // 预选用的节点
			// 3.3.1 获取所有节点
			var wsUris = setting.wsClusterUris;
			for(var i=0; i < wsUris.length; i++){
				if(HashRoutingLBEngine.isRetryNode(wsUris[i])){
					preselectedNodes.push({wsUri:wsUris[i]});
				}
			}
			var crc16 = CRC16Utils.crc16Modbus(setting.deviceId);
			var size = preselectedNodes.length;
			if (size == 0) {
				// 清空连接记录，强制重置节点列表
				setting.wsClusterConnectRecords = new Array();
				throw Error("All cluster nodes of the server failed.");
			}
			var nodeIndex = crc16 % size & (size - 1);
			return preselectedNodes[nodeIndex];
		},
		onConnectFailed : function(nodeRecord){
			if(!StringUtils.isEmpty(nodeRecord)){
				nodeRecord.timestamp = new Date().getTime();
				nodeRecord.status = false;
				setting.wsClusterConnectRecords.push(nodeRecord);
			}
		},
		isRetryNode : function(wsUri){
			var now = new Date().getTime();
			var failCount = 0;
			var records = setting.wsClusterConnectRecords;
			for(var i=0; i < records.length; i++){
				var record = records[i];
				if(record.wsUri == wsUri){
					if(Math.abs(now-record.timestamp) < setting.wsClusterNodeChooseInterval
						&& !record.status){
						++failCount;
					}
				}
			}
			return failCount < setting.wsClusterNodeRetry;
		}
	};
})(window, document);