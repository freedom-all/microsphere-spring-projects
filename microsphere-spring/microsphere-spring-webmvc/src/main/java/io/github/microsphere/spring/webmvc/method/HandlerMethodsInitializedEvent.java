package io.github.microsphere.spring.webmvc.method;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.web.method.HandlerMethod;

import java.util.Collections;
import java.util.Set;

/**
 * 所有{@link HandlerMethod} 已初始化事件
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see RequestMappingInfoHandlerMethodsReadyEvent
 * @since 1.0.0
 */
public class HandlerMethodsInitializedEvent extends ApplicationEvent {

    private final Set<HandlerMethod> handlerMethods;

    /**
     * Create a new HandlerMethodsInitializedEvent.
     *
     * @param applicationContext {@link ApplicationContext}
     */
    public HandlerMethodsInitializedEvent(ApplicationContext applicationContext, Set<HandlerMethod> handlerMethods) {
        super(applicationContext);
        this.handlerMethods = Collections.unmodifiableSet(handlerMethods);
    }

    public ApplicationContext getApplicationContext() {
        return (ApplicationContext) getSource();
    }

    /**
     * @return 不可变集合
     */
    public Set<HandlerMethod> getHandlerMethods() {
        return handlerMethods;
    }
}
