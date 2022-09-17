package io.github.microsphere.spring.webmvc.util;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Spring Web MVC 静态工具类
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@SuppressWarnings("unchecked")
public abstract class WebMvcUtils {

    public static final String HANDLER_METHOD_ARGUMENTS_ATTRIBUTE_NAME_PREFIX = "HM.ARGS:";

    public static final String HANDLER_METHOD_REQUEST_BODY_ARGUMENT_ATTRIBUTE_NAME_PREFIX = "HM.RB.ARG:";

    public static final String HANDLER_METHOD_RETURN_VALUE_ATTRIBUTE_NAME_PREFIX = "HM.RV:";

    public static final Set<Class<? extends HttpMessageConverter<?>>> supportedConverterTypes;

    static {
        Set<Class<? extends HttpMessageConverter<?>>> converterTypes = new HashSet<>(3);
        converterTypes.add(MappingJackson2HttpMessageConverter.class);
        converterTypes.add(StringHttpMessageConverter.class);
        supportedConverterTypes = Collections.unmodifiableSet(converterTypes);
    }

    /**
     * 获取当前 {@link HttpServletRequest} 对象
     * <p>
     * 默认情况，{@link HttpServletRequest} 是在 {@link RequestContextFilter} 初始化，从 Servlet HTTP 请求线程 {@link ThreadLocal} 中获取
     * 不过这个行为会被 io.github.microsphere.framework.commons.infrastructure.autoconfigure.CommonWebMvcAutoConfiguration#requestContextFilter() Bean 覆盖，
     * {@link HttpServletRequest} 是从 {@link InheritableThreadLocal} 中获取，能够在子线程中获取。
     *
     * @return 返回当前 {@link HttpServletRequest} 对象，获取不到返回<code>null</code>
     */
    public static HttpServletRequest getHttpServletRequest() throws IllegalStateException {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (requestAttributes instanceof ServletRequestAttributes) {
            request = ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return request;
    }


    public static HttpServletRequest getHttpServletRequest(WebRequest webRequest) {
        HttpServletRequest request = null;
        if (webRequest instanceof ServletWebRequest) {
            request = ((ServletWebRequest) webRequest).getRequest();
        }
        return request;
    }

    public static <T> T getAttribute(HttpServletRequest request, String attributeName) {
        return (T) request.getAttribute(attributeName);
    }

    public static void setAttribute(HttpServletRequest request, String attributeName, Object attributeValue) {
        if (attributeName == null || attributeValue == null) {
            return;
        }
        request.setAttribute(attributeName, attributeValue);
    }

    /**
     * 获取当前 Servlet Request 请求关联的 {@link WebApplicationContext}
     *
     * @return 当前 Servlet Request 请求关联的 {@link WebApplicationContext}
     * @throws IllegalStateException 如果在非 Web 场景下，将抛出异常
     */
    public static WebApplicationContext getWebApplicationContext() throws IllegalStateException {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            throw new IllegalStateException("请在 Servlet Web 应用中使用!");
        }
        ServletContext servletContext = request.getServletContext();
        return WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
    }

    /**
     * 设置 {@link HandlerMethod} 中的 {@link RequestBody @RequestBody} 方法参数存放到 {@link ServletRequest} 上下文
     *
     * @param method              Handler {@link Method}
     * @param requestBodyArgument {@link RequestBody @RequestBody} 方法参数
     */
    public static void setHandlerMethodRequestBodyArgument(Method method, Object requestBodyArgument) {
        setHandlerMethodRequestBodyArgument(getHttpServletRequest(), method, requestBodyArgument);
    }

    public static void setHandlerMethodReturnValue(HttpServletRequest request, Method method, Object returnValue) {
        String attributeName = getHandlerMethodReturnValueAttributeName(method);
        if (request != null && returnValue != null) {
            request.setAttribute(attributeName, returnValue);
        }
    }

    /**
     * 设置 {@link HandlerMethod} 中的 {@link RequestBody @RequestBody} 方法参数存放到 {@link ServletRequest} 上下文
     *
     * @param request             {@link ServletRequest}
     * @param method              Handler {@link Method}
     * @param requestBodyArgument {@link RequestBody @RequestBody} 方法参数
     */
    public static void setHandlerMethodRequestBodyArgument(ServletRequest request, Method method, Object requestBodyArgument) {
        String attributeName = getHandlerMethodRequestBodyArgumentAttributeName(method);
        if (request != null && requestBodyArgument != null) {
            request.setAttribute(attributeName, requestBodyArgument);
        }
    }

    /**
     * 从 {@link ServletRequest} 上下文获取 {@link HandlerMethod} 中的 {@link RequestBody @RequestBody} 方法参数
     *
     * @param request       {@link ServletRequest}
     * @param handlerMethod {@link HandlerMethod}
     * @param <T>           {@link RequestBody @RequestBody} 方法参数类型
     * @return {@link RequestBody @RequestBody} 方法参数如果存在的话，否则，<code>null</code>
     */
    public static <T> T getHandlerMethodRequestBodyArgument(ServletRequest request, HandlerMethod handlerMethod) {
        return getHandlerMethodRequestBodyArgument(request, handlerMethod.getMethod());
    }

    /**
     * 从 {@link ServletRequest} 上下文获取 {@link HandlerMethod} 中的 {@link RequestBody @RequestBody} 方法参数
     *
     * @param request {@link ServletRequest}
     * @param method  Handler {@link Method}
     * @param <T>     {@link RequestBody @RequestBody} 方法参数类型
     * @return {@link RequestBody @RequestBody} 方法参数如果存在的话，否则，<code>null</code>
     */
    public static <T> T getHandlerMethodRequestBodyArgument(ServletRequest request, Method method) {
        String attributeName = getHandlerMethodRequestBodyArgumentAttributeName(method);
        return request == null ? null : (T) request.getAttribute(attributeName);
    }

    public static Object[] getHandlerMethodArguments(WebRequest webRequest, MethodParameter parameter) {
        Method method = parameter.getMethod();
        HttpServletRequest request = getHttpServletRequest(webRequest);
        final Object[] arguments;
        if (request != null) {
            arguments = WebMvcUtils.getHandlerMethodArguments(request, method);
        } else {
            arguments = new Object[method.getParameterCount()];
        }
        return arguments;
    }

    /**
     * 获取 {@link HandlerMethod} 方法参数
     *
     * @param request       {@link ServletRequest}
     * @param handlerMethod {@link HandlerMethod}
     * @return non-null，如果返回数组中的元素均为 null，说明方法没有参数或未经过 {@link HandlerMethodArgumentResolverWrapper} 处理
     */
    public static Object[] getHandlerMethodArguments(ServletRequest request, HandlerMethod handlerMethod) {
        return getHandlerMethodArguments(request, handlerMethod.getMethod());
    }

    /**
     * 获取 {@link HandlerMethod} 方法参数
     *
     * @param request {@link ServletRequest}
     * @param method  {@link Method}
     * @return non-null，如果返回数组中的元素均为 null，说明方法没有参数或未经过 {@link HandlerMethodArgumentResolverWrapper} 处理
     */
    public static Object[] getHandlerMethodArguments(ServletRequest request, Method method) {
        String attributeName = getHandlerMethodArgumentsAttributeName(method);
        Object[] arguments = (Object[]) request.getAttribute(attributeName);
        if (arguments == null) {
            arguments = new Object[method.getParameterCount()];
            request.setAttribute(attributeName, arguments);
        }
        return arguments;
    }

    /**
     * 获取 {@link HandlerMethod} 方法参数
     *
     * @param method {@link Method}
     * @return non-null，如果返回数组中的元素均为 null，说明方法没有参数或未经过 {@link HandlerMethodArgumentResolverWrapper} 处理
     */
    public static Object[] getHandlerMethodArguments(Method method) {
        return getHandlerMethodArguments(getHttpServletRequest(), method);
    }

    /**
     * 获取 {@link HandlerMethod} 方法返回值
     *
     * @param request       {@link ServletRequest}
     * @param handlerMethod {@link HandlerMethod}
     * @param <T>           方法返回值类型
     * @return {@link HandlerMethod} 方法返回值
     */
    public static <T> T getHandlerMethodReturnValue(ServletRequest request, HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        return getHandlerMethodReturnValue(request, method);
    }

    /**
     * 获取 {@link HandlerMethod} 方法返回值
     *
     * @param request {@link ServletRequest}
     * @param method  {@link Method}
     * @param <T>     方法返回值类型
     * @return {@link HandlerMethod} 方法返回值
     */
    public static <T> T getHandlerMethodReturnValue(ServletRequest request, Method method) {
        String attributeName = getHandlerMethodReturnValueAttributeName(method);
        return (T) request.getAttribute(attributeName);
    }

    /**
     * 获取 {@link HandlerMethod} 方法返回值
     *
     * @param method {@link Method}
     * @param <T>    方法返回值类型
     * @return {@link HandlerMethod} 方法返回值
     */
    public static <T> T getHandlerMethodReturnValue(Method method) {
        HttpServletRequest request = getHttpServletRequest();
        return getHandlerMethodReturnValue(request, method);
    }

    private static String getHandlerMethodRequestBodyArgumentAttributeName(Method method) {
        return HANDLER_METHOD_REQUEST_BODY_ARGUMENT_ATTRIBUTE_NAME_PREFIX + getMethodInfo(method);
    }

    private static String getHandlerMethodReturnValueAttributeName(Method method) {
        return HANDLER_METHOD_RETURN_VALUE_ATTRIBUTE_NAME_PREFIX + getMethodInfo(method);
    }

    private static String getHandlerMethodArgumentsAttributeName(Method method) {
        return HANDLER_METHOD_ARGUMENTS_ATTRIBUTE_NAME_PREFIX + getMethodInfo(method);
    }

    private static String getMethodInfo(Method method) {
        return String.valueOf(method);
    }
}
