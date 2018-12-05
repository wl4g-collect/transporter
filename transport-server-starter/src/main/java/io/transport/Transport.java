package io.transport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LoggingSystem;

import io.transport.common.internal.logback.LogbackLoggingSystem;

/**
 * Transporter engineering startup class.<br/>
 * <br/>
 * Springboot default loading basic configuration application.yml reference
 * source ConfigFileApplicationListener.<br/>
 * <b><font color=red>java -cp
 * ${APP_HOME}/libs/transport.jar:${APP_HOME}/conf</font></b><br/>
 * In which the spring boot default will only load the path defined by the
 * `ConfigFileApplicationListener.DEFAULT_SEARCH_LOCATIONS="classpath:/,
 * classpath:/config/, file:./, file:./config/".`, That is to load
 * application.yml, application.yaml, application.properties files instead of
 * loading custom configuration files, refer to
 * `org.springframework.boot.context.config.ConfigFileApplicationListener.Loader.getSearchLocations()`.
 * <br/>
 * <a href=
 * "https://blog.csdn.net/chengkui1990/article/details/79866499">See</a> <br/>
 * `@EnableConfigurationProperties` Not configured, but display configuration is
 * just in case.
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0
 * @date 2017年10月27日
 * @since
 */
@EnableConfigurationProperties
@SpringBootApplication(scanBasePackages = { "io.transport", "com.wl4g" })
public class Transport {

	static {
		registerConfiguration();
	}

	static void registerConfiguration() {
		/*
		 * 1.1 Customizing the entrance of the extended `logback`. Reference:
		 * org.springframework.boot.logging.LoggingSystem.get()
		 */
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LogbackLoggingSystem.class.getName());
	}

	public static void main(String[] args) {
		// 1.1 Build and start the container.
		SpringApplication.run(Transport.class, args);
	}

}
