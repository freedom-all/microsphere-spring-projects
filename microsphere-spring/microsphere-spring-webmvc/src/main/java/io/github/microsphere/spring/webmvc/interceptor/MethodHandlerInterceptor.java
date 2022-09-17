package io.github.microsphere.spring.webmvc.interceptor;

import io.github.microsphere.spring.webmvc.util.WebMvcUtils;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HandlerMethod} {@link HandlerInterceptor} 抽象实现
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public abstract class MethodHandlerInterceptor implements HandlerInterceptor {

    /**
     * 当前 {@link HandlerInterceptor} 是否为代理对象
     */
    private final boolean delegate;

    public MethodHandlerInterceptor() {
        this(Boolean.FALSE);
    }

    public MethodHandlerInterceptor(boolean delegated) {
        this.delegate = delegated;
    }

    @Override
    public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            if (supports(request, response, handlerMethod)) {
                return preHandle(request, response, handlerMethod);
            }
        }
        return true;
    }

    protected abstract boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                         HandlerMethod handlerMethod) throws Exception;

    @Override
    public final void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                                 @Nullable ModelAndView modelAndView) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            if (supports(request, response, handlerMethod)) {
                postHandle(request, response, handlerMethod, modelAndView);
            }
        }
    }

    protected abstract void postHandle(HttpServletRequest request, HttpServletResponse response,
                                       HandlerMethod handlerMethod, ModelAndView modelAndView) throws Exception;

    @Override
    public final void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                      @Nullable Exception ex) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            if (supports(request, response, handlerMethod)) {
                afterCompletion(request, response, handlerMethod, ex);
            }
        }
    }

    protected abstract void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                            HandlerMethod handlerMethod, Exception ex) throws Exception;

    protected boolean supports(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        return true;
    }

    /**
     * 当前 {@link HandlerInterceptor} 是否为代理示例
     *
     * @return 如果是，<code>true</code>，否则为 <code>false</code>
     */
    public boolean isDelegate() {
        return delegate;
    }

    /**
     * 当前处理方法是否为代理 {@link HandlerMethod}
     *
     * @param handlerMethod {@link HandlerMethod}
     * @return <code>true</code> 如果代理 {@link HandlerInterceptor}，默认 <code>false</code>
     */
    public boolean isDelegate(HandlerMethod handlerMethod) {
        return false;
    }

    /**
     * 获取 {@link HandlerMethod} 方法参数
     *
     * @param request       {@link ServletRequest}
     * @param handlerMethod {@link HandlerMethod}
     * @return non-null
     */
    protected Object[] getHandlerMethodArguments(ServletRequest request, HandlerMethod handlerMethod) {
        return WebMvcUtils.getHandlerMethodArguments(request, handlerMethod);
    }

    /**
     * 获取 {@link HandlerMethod} 方法返回值
     *
     * @param request       {@link ServletRequest}
     * @param handlerMethod {@link HandlerMethod}
     * @param <T>           方法返回值类型
     * @return {@link HandlerMethod} 方法返回值
     */
    protected <T> T getHandlerMethodReturnValue(ServletRequest request, HandlerMethod handlerMethod) {
        return WebMvcUtils.getHandlerMethodReturnValue(request, handlerMethod);
    }

    /**
     * 从 {@link ServletRequest} 上下文获取 {@link HandlerMethod} 中的 {@link RequestBody @RequestBody} 方法参数
     *
     * @param request       {@link ServletRequest}
     * @param handlerMethod {@link HandlerMethod}
     * @param <T>           {@link RequestBody @RequestBody} 方法参数类型
     * @return {@link RequestBody @RequestBody} 方法参数如果存在的话，否则，<code>null</code>
     */
    protected <T> T getHandlerMethodRequestBodyArgument(ServletRequest request, HandlerMethod handlerMethod) {
        return WebMvcUtils.getHandlerMethodRequestBodyArgument(request, handlerMethod);
    }
}
