package io.github.microsphere.spring.webmvc.util;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import static org.springframework.web.context.ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM;
import static org.springframework.web.context.ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM;

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

    /**
     * The name of AbstractJsonpResponseBodyAdvice class which was present in Spring Framework since 4.1
     */
    public static final String ABSTRACT_JSONP_RESPONSE_BODY_ADVICE_CLASS_NAME =
            "org.springframework.web.servlet.mvc.findWebApplicationContextMethod.annotation.AbstractJsonpResponseBodyAdvice";

    /**
     * Indicates current version of Spring Framework is 4.1 or above
     */
    private final static boolean ABSTRACT_JSONP_RESPONSE_BODY_ADVICE_PRESENT =
            ClassUtils.isPresent(ABSTRACT_JSONP_RESPONSE_BODY_ADVICE_CLASS_NAME, WebMvcUtils.class.getClassLoader());

    /**
     * {@link RequestMappingHandlerMapping} Context name
     */
    private final static String REQUEST_MAPPING_HANDLER_MAPPING_CONTEXT_NAME = RequestMappingHandlerMapping.class.getName();

    /**
     * Any number of these characters are considered delimiters between
     * multiple values in a single init-param String value.
     *
     * @see ContextLoader#INIT_PARAM_DELIMITERS
     */
    private static final String INIT_PARAM_DELIMITERS = ",; \t\n";

    /**
     * RequestContextUtils#findWebApplicationContext(HttpServletRequest, ServletContext) method
     *
     * @since Spring 4.2.1
     */
    private static final Method findWebApplicationContextMethod = findMethod(RequestContextUtils.class,
            "findWebApplicationContext", HttpServletRequest.class, ServletContext.class);

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

    /**
     * Determine whether the Bean Type is present annotated by {@link ControllerAdvice}
     *
     * @param beanType Bean Type
     * @return If {@link ControllerAdvice} bean type is present , return <code>true</code> , or <code>false</code>.
     */
    public static boolean isControllerAdviceBeanType(Class<?> beanType) {
        return AnnotationUtils.findAnnotation(beanType, ControllerAdvice.class) != null;
    }

    /**
     * Get the {@link WebApplicationContext} from {@link HttpServletRequest}
     *
     * @param request        {@link HttpServletRequest}
     * @param servletContext {@link ServletContext}
     * @return {@link WebApplicationContext}
     * @throws IllegalStateException if no servlet-specific context has been found
     * @see RequestContextUtils#getWebApplicationContext(ServletRequest)
     * @see DispatcherServlet#WEB_APPLICATION_CONTEXT_ATTRIBUTE
     */
    public static WebApplicationContext getWebApplicationContext(HttpServletRequest request, ServletContext servletContext) {

        WebApplicationContext webApplicationContext = null;

        if (findWebApplicationContextMethod != null) {

            try {

                webApplicationContext = (WebApplicationContext)
                        invokeMethod(findWebApplicationContextMethod, null, request, servletContext);

            } catch (IllegalStateException e) {

            }

        }

        if (webApplicationContext == null) {

            webApplicationContext = RequestContextUtils.findWebApplicationContext(request, servletContext);

        }

        return webApplicationContext;

    }

    protected static String appendInitParameter(String existedParameterValue, String... parameterValues) {

        String[] existedParameterValues = StringUtils.hasLength(existedParameterValue) ?
                existedParameterValue.split(INIT_PARAM_DELIMITERS) :
                new String[0];

        List<String> parameterValuesList = new ArrayList<String>();

        if (!ObjectUtils.isEmpty(existedParameterValues)) {
            parameterValuesList.addAll(Arrays.asList(existedParameterValues));
        }

        parameterValuesList.addAll(Arrays.asList(parameterValues));

        String newParameterValue = StringUtils.arrayToDelimitedString(parameterValuesList.toArray(), ",");

        return newParameterValue;
    }

    /**
     * Append {@link ServletContext#setInitParameter(String, String) ServletContext Intialized Parameters}
     *
     * @param servletContext  {@link ServletContext}
     * @param parameterName   the name of init parameter
     * @param parameterValues the values of init parameters
     */
    public static void appendInitParameters(ServletContext servletContext, String parameterName, String... parameterValues) {

        Assert.notNull(servletContext);
        Assert.hasLength(parameterName);
        Assert.notNull(parameterValues);

        String existedParameterValue = servletContext.getInitParameter(parameterName);

        String newParameterValue = appendInitParameter(existedParameterValue, parameterValues);

        if (StringUtils.hasLength(newParameterValue)) {
            servletContext.setInitParameter(parameterName, newParameterValue);
        }

    }

    /**
     * Append  initialized parameter for {@link ApplicationContextInitializer Global Initializer Class}
     *
     * @param servletContext          {@link ServletContext}
     * @param contextInitializerClass the class of {@link ApplicationContextInitializer}
     * @see ContextLoader#GLOBAL_INITIALIZER_CLASSES_PARAM
     */
    public static void appendGlobalInitializerClassInitParameter(ServletContext servletContext,
                                                                 Class<? extends ApplicationContextInitializer> contextInitializerClass) {

        String contextInitializerClassName = contextInitializerClass.getName();

        appendInitParameters(servletContext, GLOBAL_INITIALIZER_CLASSES_PARAM, contextInitializerClassName);

    }

    /**
     * Append  initialized parameter for {@link ApplicationContextInitializer Context Initializer Class}
     *
     * @param servletContext          {@link ServletContext}
     * @param contextInitializerClass the class of {@link ApplicationContextInitializer}
     * @see ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
     */
    public static void appendContextInitializerClassInitParameter(ServletContext servletContext,
                                                                  Class<? extends ApplicationContextInitializer> contextInitializerClass) {

        String contextInitializerClassName = contextInitializerClass.getName();

        appendInitParameters(servletContext, CONTEXT_INITIALIZER_CLASSES_PARAM, contextInitializerClassName);

    }


    /**
     * Append initialized parameter for {@link ApplicationContextInitializer Context Initializer Class} into {@link
     * FrameworkServlet}
     *
     * @param servletContext          {@link ServletContext}
     * @param contextInitializerClass the class of {@link ApplicationContextInitializer}
     * @see FrameworkServlet#applyInitializers(ConfigurableApplicationContext)
     */
    public static void appendFrameworkServletContextInitializerClassInitParameter(
            ServletContext servletContext,
            Class<? extends ApplicationContextInitializer> contextInitializerClass) {

        Collection<? extends ServletRegistration> servletRegistrations =
                WebUtils.findServletRegistrations(servletContext, FrameworkServlet.class).values();

        for (ServletRegistration servletRegistration : servletRegistrations) {
            String contextInitializerClassName = servletRegistration.getInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM);
            String newContextInitializerClassName = appendInitParameter(contextInitializerClassName, contextInitializerClass.getName());
            servletRegistration.setInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM, newContextInitializerClassName);
        }

    }

    /**
     * Is page render request
     *
     * @param modelAndView {@link ModelAndView}
     * @return If current request is for page render , return <code>true</code> , or <code>false</code>.
     */
    public static boolean isPageRenderRequest(ModelAndView modelAndView) {

        if (modelAndView != null) {

            String viewName = modelAndView.getViewName();

            return StringUtils.hasText(viewName);

        }

        return false;

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
