package io.transport.rest.config;

// import java.util.List;
//
//// import javax.annotation.Resource;
//
// import org.springframework.context.annotation.Configuration;
// import org.springframework.http.converter.HttpMessageConverter;
// import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
// import
// org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
//
// import com.alibaba.fastjson.serializer.SerializerFeature;
// import com.alibaba.fastjson.support.config.FastJsonConfig;
// import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
//
//// import io.transport.rest.interceptor.AuthInterceptor;
//
/// **
// * WEB related registration Configurator
// *
// * @author Wangl.sir <983708408@qq.com>
// * @version v1.0
// * @date 2018年5月14日
// * @since
// */
// @Configuration
// public class WebMvcConfigurer extends WebMvcConfigurerAdapter {
//
// // @Resource
// // private AuthInterceptor authInterceptor;
//
// @Override
// public void addInterceptors(InterceptorRegistry registry) {
// // 1.1 Authentication interceptor.
// // registry.addInterceptor(authInterceptor).addPathPatterns("/**");
// super.addInterceptors(registry);
// }
//
// // 增加fastjson配置会导致springboot-admin-client上报信息时json格式异常（即: json报文的
// status字段值会使用 $ref.xxx模式），导致springboot-admin-server-ui展示Health信息部分出不来的问题.
// @Override
// public void configureMessageConverters(List<HttpMessageConverter<?>>
// converters) {
// super.configureMessageConverters(converters);
// /**
// * 1.需要先定义一个convert转换消息的对象；<br/>
// * 2.添加FastJson的配置信息，比如是否要格式化返回的JSON数据；<br/>
// * 3.在convert中添加配置信息；<br/>
// * 4.将convert添加到converters中；<br/>
// */
// FastJsonHttpMessageConverter fastConverter = new
// FastJsonHttpMessageConverter();
// FastJsonConfig fastJsonConfig = new FastJsonConfig();
// // fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat);
// fastJsonConfig.setSerializerFeatures(new SerializerFeature[] {
// SerializerFeature.NotWriteDefaultValue,
// SerializerFeature.SkipTransientField, SerializerFeature.WriteMapNullValue });
// fastConverter.setFastJsonConfig(fastJsonConfig);
// converters.add(fastConverter);
// }
//
// }
