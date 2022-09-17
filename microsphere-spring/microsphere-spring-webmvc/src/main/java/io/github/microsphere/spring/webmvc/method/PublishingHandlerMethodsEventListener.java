package io.github.microsphere.spring.webmvc.method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 发布 {@link HandlerMethod} 事件监听器
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see HandlerMethodsInitializedEvent
 * @see RequestMappingInfoHandlerMethodsReadyEvent
 * @since 1.0.0
 */
public class PublishingHandlerMethodsEventListener implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(PublishingHandlerMethodsEventListener.class);

    private ApplicationContext context;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        if (!Objects.equals(context, applicationContext)) {
            return;
        }
        publishHandlerMethodsEvent(applicationContext);
    }

    private void publishHandlerMethodsEvent(ApplicationContext applicationContext) {
        Map<String, AbstractHandlerMethodMapping> handlerMappingsMap =
                BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, AbstractHandlerMethodMapping.class);
        Set<HandlerMethod> handlerMethods = new HashSet<>();

        Map<RequestMappingInfo, HandlerMethod> requestMappingInfoHandlerMethods = new HashMap<>();

        for (AbstractHandlerMethodMapping handlerMapping : handlerMappingsMap.values()) {
            handlerMethods.addAll(handlerMapping.getHandlerMethods().values());
            if (handlerMapping instanceof RequestMappingInfoHandlerMapping) {
                RequestMappingInfoHandlerMapping requestMappingInfoHandlerMapping = (RequestMappingInfoHandlerMapping) handlerMapping;
                requestMappingInfoHandlerMethods.putAll(requestMappingInfoHandlerMapping.getHandlerMethods());
            }
        }

        applicationContext.publishEvent(new HandlerMethodsInitializedEvent(applicationContext, handlerMethods));
        applicationContext.publishEvent(new RequestMappingInfoHandlerMethodsReadyEvent(applicationContext, requestMappingInfoHandlerMethods));
        logger.info("当前应用上下文[id : '{}'] 已发送 HandlerMethod 准备事件");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
