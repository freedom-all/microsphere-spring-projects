package io.github.microsphere.spring.webmvc.config;

import io.github.microsphere.spring.webmvc.interceptor.DelegatingMethodHandlerInterceptor;
import io.github.microsphere.spring.webmvc.interceptor.MethodHandlerInterceptor;
import io.github.microsphere.spring.webmvc.method.PublishingHandlerMethodsEventListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * 通用 {@link WebMvcConfigurer Spring WebMVC 配置}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@Import(value = {
        PublishingHandlerMethodsEventListener.class,
        DelegatingMethodHandlerInterceptor.class
})
public class CommonWebMvcConfigurer implements WebMvcConfigurer {

    @Autowired
    private ObjectProvider<MethodHandlerInterceptor> methodHandlerInterceptors;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        methodHandlerInterceptors.forEach(interceptor -> {
            // 非代理 MethodHandlerInterceptor 实例均被注册
            if (!interceptor.isDelegate()) {
                registry.addInterceptor(interceptor);
            }
        });
    }

}
