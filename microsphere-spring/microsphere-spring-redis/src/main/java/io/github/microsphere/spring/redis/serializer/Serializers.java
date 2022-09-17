package io.github.microsphere.spring.redis.serializer;

import io.github.microsphere.spring.redis.event.RedisCommandEvent;
import io.github.microsphere.spring.redis.metadata.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * {@link RedisSerializer} 工具类，主要用于 Redis 命令方法参数类型序列化和反序列化
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public abstract class Serializers {

    private static final Logger logger = LoggerFactory.getLogger(Serializers.class);

    private static final ClassLoader classLoader = Serializers.class.getClassLoader();

    static final JdkSerializationRedisSerializer defaultSerializer = new JdkSerializationRedisSerializer();

    static final StringRedisSerializer stringSerializer = StringRedisSerializer.UTF_8;

    /**
     * 泛型参数化 {@link RedisSerializer}
     * Key 为类型的全名称，Value 为 {@link RedisSerializer} 实现
     */
    static final Map<String, RedisSerializer<?>> typedSerializers = new HashMap<>(32);

    static {
        initializeBuiltinSerializers();
        initializeParameterizedSerializers();
    }

    public static <T> RedisSerializer<T> getSerializer(Class<T> parameterizedType) {
        return parameterizedType == null ? null : (RedisSerializer<T>) getSerializer(parameterizedType.getName());
    }

    public static RedisSerializer<?> getSerializer(String typeName) {
        RedisSerializer<?> serializer = typedSerializers.get(typeName);

        if (serializer == null) {
            logger.debug("未找到类型 {} 的 RedisSerializer 实现类，将使用默认 RedisSerializer 实现类：{}", typeName, defaultSerializer.getClass().getName());
            serializer = defaultSerializer;
            typedSerializers.put(typeName, serializer);
        } else {
            logger.trace("找到类型 {} 的 RedisSerializer 实现类：{}", typeName, serializer.getClass().getName());
        }

        return serializer;
    }

    public static byte[] serializeRawParameter(Parameter parameter) {
        byte[] rawParameterValue = parameter.getRawValue();
        if (rawParameterValue == null) {
            Object parameterValue = parameter.getValue();
            RedisSerializer serializer = getSerializer(parameter.getParameterType());
            if (serializer != null) {
                rawParameterValue = serializer.serialize(parameterValue);
                parameter.setRawValue(rawParameterValue);
            }
        }
        return rawParameterValue;
    }

    public static byte[] serialize(RedisCommandEvent event) {
        return defaultSerializer.serialize(event);
    }

    public static byte[] defaultSerialize(Object object) {
        return defaultSerializer.serialize(object);
    }

    public static Object deserialize(byte[] bytes, String parameterType) {
        RedisSerializer<?> serializer = getSerializer(parameterType);
        if (serializer == null) {
            logger.error("无法将字节流反序列化为目标类型 {} 的对象", parameterType);
            return null;
        }
        Object object = serializer.deserialize(bytes);
        return object;
    }

    public static <T> T deserialize(byte[] bytes, Class<T> type) {
        Object object = defaultSerializer.deserialize(bytes);
        if (type.isInstance(object)) {
            return type.cast(object);
        } else {
            logger.error("无法将字节流反序列化为目标类型 {} 的对象", type.getName());
            return null;
        }
    }

    /**
     * 初始化内建类型 Serializers
     */
    private static void initializeBuiltinSerializers() {

        // 初始化简单类型 Serializers
        initializeSimpleSerializers();

        // 初始化数组类型 Serializers
        initializeArrayTypeSerializers();

        // 初始化集合类型 Serializers
        initializeCollectionTypeSerializers();

        // 初始化枚举类型 Serializers
        initializeEnumerationSerializers();

        // 初始化 Spring Data Redis 类型 Serializers
        initializeSpringDataRedisSerializers();
    }

    /**
     * 初始化简单类型 Serializers
     */
    private static void initializeSimpleSerializers() {

        // boolean 或 Boolean 类型
        initializeSerializer(boolean.class, BooleanSerializer.INSTANCE);
        initializeSerializer(Boolean.class, BooleanSerializer.INSTANCE);

        // int 或 Integer 类型
        initializeSerializer(int.class, IntegerSerializer.INSTANCE);
        initializeSerializer(Integer.class, IntegerSerializer.INSTANCE);

        // long 或 Long 类型
        initializeSerializer(long.class, LongSerializer.INSTANCE);
        initializeSerializer(Long.class, LongSerializer.INSTANCE);

        // double 或 Double 类型
        initializeSerializer(double.class, DoubleSerializer.INSTANCE);
        initializeSerializer(Double.class, DoubleSerializer.INSTANCE);

        // String 类型
        initializeSerializer(String.class, stringSerializer);
    }

    /**
     * 初始化集合类型 Serializers
     */
    private static void initializeCollectionTypeSerializers() {

        // Iterable 类型
        initializeSerializer(Iterable.class, defaultSerializer);

        // Iterator 类型
        initializeSerializer(Iterator.class, defaultSerializer);

        // Collection 类型
        initializeSerializer(Collection.class, defaultSerializer);

        // List 类型
        initializeSerializer(List.class, defaultSerializer);

        // Set 类型
        initializeSerializer(Set.class, defaultSerializer);

        // Map 类型
        initializeSerializer(Map.class, defaultSerializer);

        // Queue 类型
        initializeSerializer(Queue.class, defaultSerializer);
    }

    /**
     * 初始化数组类型 Serializers
     */
    private static void initializeArrayTypeSerializers() {

        // byte[] 类型
        //initializeSerializer(byte[].class, ByteArraySerializer.INSTANCE);
        initializeSerializer(byte[].class, defaultSerializer);

        // int[] 类型
        initializeSerializer(int[].class, defaultSerializer);

        // byte[][] 类型
        initializeSerializer(byte[][].class, defaultSerializer);
    }

    /**
     * 初始化枚举类型 Serializers
     */
    private static void initializeEnumerationSerializers() {
        initializeSerializer(TimeUnit.class, new EnumSerializer(TimeUnit.class));
    }

    /**
     * 初始化 Spring Data Redis 类型 Serializers
     */
    private static void initializeSpringDataRedisSerializers() {

        // org.springframework.data.redis.core.types.Expiration 类型
        initializeSerializer(Expiration.class, ExpirationSerializer.INSTANCE);

        // org.springframework.data.redis.connection.SortParameters 类型
        initializeSerializer(SortParameters.class, SortParametersSerializer.INSTANCE);

        // org.springframework.data.redis.connection.RedisListCommands.Position 类型
        initializeSerializer(RedisListCommands.Position.class, new EnumSerializer(RedisListCommands.Position.class));

        // org.springframework.data.redis.connection.RedisStringCommands.SetOption 类型
        initializeSerializer(RedisStringCommands.SetOption.class, new EnumSerializer(RedisStringCommands.SetOption.class));

        // org.springframework.data.redis.connection.RedisZSetCommands.Range 类型
        initializeSerializer(RedisZSetCommands.Range.class, RangeSerializer.INSTANCE);

        // org.springframework.data.redis.connection.RedisZSetCommands.Aggregate
        initializeSerializer(RedisZSetCommands.Aggregate.class, new EnumSerializer(RedisZSetCommands.Aggregate.class));

        // org.springframework.data.redis.connection.RedisZSetCommands.Weights 类型
        initializeSerializer(RedisZSetCommands.Weights.class, WeightsSerializer.INSTANCE);

        // org.springframework.data.redis.connection.ReturnType 类型
        initializeSerializer(ReturnType.class, new EnumSerializer(ReturnType.class));

        // org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation 类型
        initializeSerializer(RedisGeoCommands.GeoLocation.class, GeoLocationSerializer.INSTANCE);

        // org.springframework.data.geo.Point 类型
        initializeSerializer(Point.class, PointSerializer.INSTANCE);
    }

    /**
     * 初始化泛型参数 Serializers
     */
    private static void initializeParameterizedSerializers() {
        List<RedisSerializer> serializers = loadSerializers();
        initializeParameterizedSerializers(serializers);
    }

    private static void initializeParameterizedSerializers(List<RedisSerializer> serializers) {
        for (RedisSerializer serializer : serializers) {
            initializeParameterizedSerializer(serializer);
        }
    }

    private static void initializeParameterizedSerializer(RedisSerializer serializer) {
        Class<?> parameterizedType = ResolvableType.forType(serializer.getClass())
                .as(RedisSerializer.class)
                .getGeneric(0)
                .resolve();

        if (parameterizedType != null) {
            initializeSerializer(parameterizedType, serializer);
        } else {
            logger.warn("RedisSerializer 实现类：{} 未能找到参数类型", serializer.getClass());
        }
    }

    private static List<RedisSerializer> loadSerializers() {
        return SpringFactoriesLoader.loadFactories(RedisSerializer.class, classLoader);
    }

    static void initializeSerializer(Class<?> type, RedisSerializer serializer) {
        String typeName = type.getName();
        logger.debug("初始化类型[{}]的 RedisSerializer 实现类：{} , ", typeName, serializer.getClass());
        typedSerializers.put(typeName, serializer);
    }

}
