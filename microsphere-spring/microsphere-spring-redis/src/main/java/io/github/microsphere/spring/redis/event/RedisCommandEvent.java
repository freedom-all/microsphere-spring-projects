package io.github.microsphere.spring.redis.event;

import io.github.microsphere.spring.redis.context.RedisContext;
import io.github.microsphere.spring.redis.interceptor.RedisMethodContext;
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
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;


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

    public static final byte VERSION_DEFAULT = -1;

    public static final byte VERSION_1 = 1;

    /**
     * Serialization Version
     */
    public static final byte SERIALIZATION_VERSION = VERSION_1;


    private static final String REDIS_COMMANDS_PACKAGE_NAME = "org.springframework.data.redis.connection.";

    private static final ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

    /**
     * Command interface simple name, such as：
     * RedisStringCommands
     * RedisHashCommands
     */
    private final String interfaceName;

    /**
     * Command interface method name, for example, set method
     */
    private final String methodName;

    private transient final int parameterCount;

    /**
     * Method parameter type list, such as: [{@link java.lang.String},{@link java.lang.Integer}]
     */
    private final String[] parameterTypes;

    /**
     * List of method parameter objects
     */
    private final byte[][] parameters;

    /**
     * Event source Application name
     */
    private final String sourceApplication;

    private transient RedisContext redisContext;

    private transient @Nullable
    RedisMethodContext redisMethodContext;

    public RedisCommandEvent(String interfaceName, String methodName, String[] parameterTypes, byte[][] parameters, String sourceApplication) {
        super("default");
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameterCount = parameterTypes.length;
        this.parameters = parameters;
        this.sourceApplication = sourceApplication;
    }

    public RedisCommandEvent(RedisMethodContext redisMethodContext) {
        super(redisMethodContext);
        Method method = redisMethodContext.getMethod();
        this.interfaceName = resolveInterfaceName(method);
        this.methodName = method.getName();
        Parameter[] parameters = redisMethodContext.getParameters();
        this.parameterCount = parameters.length;
        this.parameterTypes = new String[parameterCount];
        this.parameters = new byte[parameterCount][];
        this.sourceApplication = redisMethodContext.getApplicationName();
        this.redisMethodContext = redisMethodContext;
        init(parameters, parameterCount);
    }

    private void init(Parameter[] parameters, int parameterCount) {
        for (int i = 0; i < parameterCount; i++) {
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
     * Source Bean name (non-serialized field, initialized by the consumer)
     *
     * @return Source Bean name
     */
    public String getSourceBeanName() {
        return getRedisMethodContext().getSourceBeanName();
    }

    public RedisContext getRedisContext() {
        RedisContext redisContext = this.redisContext;
        if (redisContext == null) {
            return redisMethodContext.getRedisContext();
        }
        return redisContext;
    }

    public void setRedisContext(RedisContext redisContext) {
        this.redisContext = redisContext;
    }

    public RedisMethodContext getRedisMethodContext() {
        return redisMethodContext;
    }

    public void setRedisMethodContext(RedisMethodContext redisMethodContext) {
        this.redisMethodContext = redisMethodContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RedisCommandEvent that = (RedisCommandEvent) o;

        if (!Objects.equals(interfaceName, that.interfaceName)) return false;
        if (!Objects.equals(methodName, that.methodName)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(parameterTypes, that.parameterTypes)) return false;
        if (!Arrays.deepEquals(parameters, that.parameters)) return false;
        return Objects.equals(sourceApplication, that.sourceApplication);
    }

    @Override
    public int hashCode() {
        int result = interfaceName != null ? interfaceName.hashCode() : 0;
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        result = 31 * result + Arrays.deepHashCode(parameters);
        result = 31 * result + (sourceApplication != null ? sourceApplication.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RedisCommandEvent.class.getSimpleName() + "[", "]").add("interfaceName='" + interfaceName + "'").add("methodName='" + methodName + "'").add("parameterCount=" + parameterCount).add("parameterTypes=" + Arrays.toString(parameterTypes)).add("parameters=" + Arrays.toString(parameters)).add("sourceApplication='" + sourceApplication + "'").toString();
    }
}
