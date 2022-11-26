package io.github.microsphere.spring.redis.event;

import io.github.microsphere.spring.redis.context.RedisContext;
import io.github.microsphere.spring.redis.metadata.Parameter;
import io.github.microsphere.spring.redis.serializer.Serializers;
import org.springframework.context.ApplicationEvent;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.data.redis.connection.RedisHyperLogLogCommands;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.connection.RedisPubSubCommands;
import org.springframework.data.redis.connection.RedisScriptingCommands;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisSetCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.RedisTxCommands;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;


/**
 * {@link RedisCommands Redis command} event
 * The supported commands：
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
     * Command interface simple name, such as：
     * RedisStringCommands
     * RedisHashCommands
     */
    private String interfaceName;

    /**
     * Command interface method name, for example, set method
     */
    private String methodName;

    /**
     * Method parameter type list, such as: [Java. Lang. String, Java. Lang. String]
     */
    private String[] parameterTypes;

    /**
     * List of method parameter objects
     */
    private byte[][] parameters;

    /**
     * Event source Application name
     */
    private String sourceApplication;

    /**
     * Source Bean name (non-serialized field, initialized by the consumer)
     */
    private transient String sourceBeanName;

    private transient RedisContext redisContext;

    public RedisCommandEvent() {
        this("this");
    }

    public RedisCommandEvent(Object source) {
        super(source);
    }

    public RedisCommandEvent(Method method, Parameter[] parameters, String sourceApplication) {
        this(method);
        this.interfaceName = resolveInterfaceName(method);
        this.methodName = method.getName();
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

    public byte[] getParameter(int parameterIndex) {
        return parameters[parameterIndex];
    }

    /**
     * Gets the parameter type (String) of the specified index.
     *
     * @param parameterIndex Parameter array index
     * @return Parameter type (String)
     */
    public String getParameterType(int parameterIndex) {
        return parameterTypes[parameterIndex];
    }

    /**
     * Gets the parameter type of the specified index
     *
     * @param parameterIndex Parameter array index
     * @return The parameter type
     */
    public Class<?> getParameterClass(int parameterIndex) {
        String parameterType = getParameterType(parameterIndex);
        return ClassUtils.resolveClassName(parameterType, classLoader);
    }

    /**
     * Gets all parameter types
     *
     * @return All parameter Types
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
     * Gets a list of method parameters (object type, not byte[])
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
     * @return Event source Application name
     */
    public String getSourceApplication() {
        return sourceApplication;
    }

    /**
     * @return Source Bean name
     */
    public String getSourceBeanName() {
        return sourceBeanName;
    }

    /**
     * @param sourceBeanName Source Bean name
     */
    public void setSourceBeanName(String sourceBeanName) {
        this.sourceBeanName = sourceBeanName;
    }

    /**
     * @param redisContext {@link RedisContext}
     */
    public void setRedisContext(RedisContext redisContext) {
        this.redisContext = redisContext;
    }

    public boolean isSourceFromRedisTemplate() {
        return redisContext != null && redisContext.getRedisTemplateBeanNames().contains(sourceBeanName);
    }

    public boolean isSourceFromRedisConnectionFactory() {
        return redisContext != null && redisContext.getRedisConnectionFactoryBeanNames().contains(sourceBeanName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RedisCommandEvent)) return false;
        RedisCommandEvent that = (RedisCommandEvent) o;
        return Objects.equals(interfaceName, that.interfaceName) &&
                Objects.equals(methodName, that.methodName) &&
                Arrays.deepEquals(parameterTypes, that.parameterTypes) &&
                Arrays.deepEquals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(interfaceName, methodName);
        result = 31 * result + Arrays.deepHashCode(parameterTypes);
        result = 31 * result + Arrays.deepHashCode(parameters);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RedisCommandEvent{");
        sb.append("interfaceName='").append(getInterfaceName()).append('\'');
        sb.append(", methodName='").append(getMethodName()).append('\'');
        sb.append(", parameterTypes=").append(Arrays.toString(getParameterTypes()));
        sb.append(", parameterCount=").append(getParameterCount());
        sb.append(", rawParameters=").append(Arrays.toString(getParameters()));
        sb.append(", objectParameters=").append(Arrays.toString(getObjectParameters()));
        sb.append(", sourceApplication='").append(getSourceApplication()).append('\'');
        sb.append(", sourceBeanName='").append(getSourceBeanName()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
