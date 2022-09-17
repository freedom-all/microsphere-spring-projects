package io.github.microsphere.spring.redis.event;

import io.github.microsphere.spring.redis.metadata.Parameter;
import io.github.microsphere.spring.redis.serializer.Serializers;
import org.springframework.context.ApplicationEvent;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import static io.github.microsphere.spring.redis.config.RedisConfiguration.REDIS_TEMPLATE_SOURCE;
import static io.github.microsphere.spring.redis.config.RedisConfiguration.STRING_REDIS_TEMPLATE_SOURCE;

/**
 * {@link RedisCommands Redis 命令} 事件
 * 当前支持的命令：
 * <ul>
 *     <li>RedisStringCommands</li>
 *     <li>RedisHashCommands</li>
 * </ul>
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see RedisCommands
 * @see RedisKeyCommands
 * @see RedisStringCommands
 * @see RedisListCommands
 * @see RedisSetCommands
 * @see RedisZSetCommands
 * @see RedisHashCommands
 * @see RedisTxCommands
 * @see RedisPubSubCommands
 * @see RedisConnectionCommands
 * @see RedisServerCommands
 * @see RedisScriptingCommands
 * @see RedisGeoCommands
 * @see RedisHyperLogLogCommands
 * @since 1.0.0
 */
public class RedisCommandEvent extends ApplicationEvent {

    private static final long serialVersionUID = -1L;

    private static final String REDIS_COMMANDS_PACKAGE_NAME = "org.springframework.data.redis.connection.";

    private static final ClassLoader classLoader = ClassUtils.getDefaultClassLoader();


    /**
     * 命令接口简单名称，比如：
     * RedisStringCommands
     * RedisHashCommands
     */
    private String interfaceName;

    /**
     * 命令接口方法名称，比如：set 方法
     */
    private String methodName;

    /**
     * 方法参数类型列表，比如：[java.lang.String,java.lang.String]
     */
    private String[] parameterTypes;

    /**
     * 方法参数对象列表
     */
    private byte[][] parameters;

    /**
     * 事件来源
     */
    private byte sourceFrom;

    /**
     * 事件来源应用名
     */
    private String sourceApplication;

    /**
     * 业务域（非序列化字段，由消费端初始化）
     */
    private transient String domain;

    /**
     * RedisTemplate Bean 名称（非序列化字段，由消费端初始化）
     */
    private transient String beanName;

    /**
     * key
     */
    private transient String key;

    public RedisCommandEvent() {
        super("this");
    }

    public RedisCommandEvent(Method method, Parameter[] parameters, byte sourceFrom, String sourceApplication) {
        super(method);
        this.interfaceName = resolveInterfaceName(method);
        this.methodName = method.getName();
        this.sourceFrom = sourceFrom;
        this.sourceApplication = sourceApplication;
        init(parameters);
    }

    private void init(Parameter[] parameters) {
        int length = parameters.length;
        this.parameterTypes = new String[length];
        this.parameters = new byte[length][];
        for (int i = 0; i < length; i++) {
            Parameter parameter = parameters[i];
            this.parameterTypes[i] = parameter.getParameterType();
            this.parameters[i] = parameter.getRawValue();
        }
    }

    private String resolveInterfaceName(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        String className = declaringClass.getName();
        if (className.startsWith(REDIS_COMMANDS_PACKAGE_NAME)) {
            return className.substring(REDIS_COMMANDS_PACKAGE_NAME.length());
        }
        return className;
    }

    public String getInterfaceName() {
        if (interfaceName.contains(".")) {
            return interfaceName;
        }
        return REDIS_COMMANDS_PACKAGE_NAME + interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public byte[][] getParameters() {
        return parameters;
    }

    /**
     * 事件是否源于 {@link RedisTemplate}
     *
     * @return 如果是，返回 <code>true</code>，否则，返回 <code>false</code>
     */
    public boolean isSourceFromRedisTemplate() {
        return this.sourceFrom == REDIS_TEMPLATE_SOURCE;
    }

    /**
     * 事件是否源于 {@link StringRedisTemplate}
     *
     * @return 如果是，返回 <code>true</code>，否则，返回 <code>false</code>
     */
    public boolean isSourceFromStringRedisTemplate() {
        return this.sourceFrom == STRING_REDIS_TEMPLATE_SOURCE;
    }

    /**
     * 获取指定索引得参数类型（String 类型表示）
     *
     * @param parameterIndex 参数数组下标
     * @return 参数类型（String 类型表示）
     */
    public String getParameterType(int parameterIndex) {
        return parameterTypes[parameterIndex];
    }

    /**
     * 获取指定索引得参数类型
     *
     * @param parameterIndex 参数数组下标
     * @return 参数类型
     */
    public Class<?> getParameterClass(int parameterIndex) {
        String parameterType = getParameterType(parameterIndex);
        return ClassUtils.resolveClassName(parameterType, classLoader);
    }

    /**
     * 获取所有参数类型
     *
     * @return 所有参数类型
     */
    public Class<?>[] getParameterClasses() {
        int parameterCount = getParameterCount();
        Class<?>[] parameterClasses = new Class[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            parameterClasses[i] = getParameterClass(i);
        }
        return parameterClasses;
    }

    /**
     * 获取方法参数列表（对象类型，非 byte[]）
     *
     * @return non-null
     */
    public Object[] getObjectParameters() {
        int length = getParameterCount();
        Object[] objectParameters = new Object[length];
        for (int i = 0; i < length; i++) {
            Object objectParameter = getObjectParameter(i);
            objectParameters[i] = objectParameter;
        }
        return objectParameters;
    }

    public Object getObjectParameter(int parameterIndex) {
        byte[] parameter = parameters[parameterIndex];
        String parameterType = getParameterType(parameterIndex);
        Object objectParameter = Serializers.deserialize(parameter, parameterType);
        return objectParameter;
    }

    public int getParameterCount() {
        return parameterTypes.length;
    }

    /**
     * @return 事件来源
     */
    public byte getSourceFrom() {
        return sourceFrom;
    }

    /**
     * @return 事件来源应用名
     */
    public String getSourceApplication() {
        return sourceApplication;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return 业务域（非序列化字段，由消费端初始化）
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return RedisTemplate Bean 名称
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * @param beanName RedisTemplate Bean 名称
     */
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RedisCommandEvent)) return false;
        RedisCommandEvent that = (RedisCommandEvent) o;
        return sourceFrom == that.sourceFrom &&
                Objects.equals(interfaceName, that.interfaceName) &&
                Objects.equals(methodName, that.methodName) &&
                Arrays.deepEquals(parameterTypes, that.parameterTypes) &&
                Arrays.deepEquals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(interfaceName, methodName, sourceFrom);
        result = 31 * result + Arrays.deepHashCode(parameterTypes);
        result = 31 * result + Arrays.deepHashCode(parameters);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RedisCommandEvent{");
        sb.append("interfaceName='").append(getInterfaceName()).append('\'');
        sb.append(", methodName='").append(getMethodName()).append('\'');
        sb.append(", key='").append(getKey()).append('\'');
        sb.append(", parameterTypes=").append(Arrays.toString(getParameterTypes()));
        sb.append(", parameterCount=").append(getParameterCount());
        sb.append(", rawParameters=").append(Arrays.toString(getParameters()));
        sb.append(", objectParameters=").append(Arrays.toString(getObjectParameters()));
        sb.append(", sourceFrom=").append(getSourceFrom());
        sb.append(", sourceApplication='").append(getSourceApplication()).append('\'');
        sb.append(", redisTemplate beanName='").append(getBeanName()).append('\'');
        sb.append(", domain=").append(getDomain());
        sb.append('}');
        return sb.toString();
    }
}
