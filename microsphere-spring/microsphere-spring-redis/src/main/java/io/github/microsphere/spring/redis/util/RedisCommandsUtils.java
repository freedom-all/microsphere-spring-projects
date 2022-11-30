package io.github.microsphere.spring.redis.util;

import io.github.microsphere.spring.redis.event.RedisCommandEvent;
import io.github.microsphere.spring.redis.metadata.ParameterMetadata;
import io.github.microsphere.spring.redis.serializer.Serializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisConnection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static java.util.Collections.unmodifiableList;

/**
 * {@link RedisCommands Redis Command} Utilities Class
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public abstract class RedisCommandsUtils {

    private static final Logger logger = LoggerFactory.getLogger(RedisCommandsUtils.class);

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public static final String REDIS_GEO_COMMANDS = "RedisGeoCommands";

    public static final String REDIS_HASH_COMMANDS = "RedisHashCommands";

    public static final String REDIS_HYPER_LOG_LOG_COMMANDS = "RedisHyperLogLogCommands";

    public static final String REDIS_KEY_COMMANDS = "RedisKeyCommands";

    public static final String REDIS_LIST_COMMANDS = "RedisListCommands";

    public static final String REDIS_SET_COMMANDS = "RedisSetCommands";

    public static final String REDIS_SCRIPTING_COMMANDS = "RedisScriptingCommands";

    public static final String REDIS_SERVER_COMMANDS = "RedisServerCommands";

    public static final String REDIS_STREAM_COMMANDS = "RedisStreamCommands";

    public static final String REDIS_STRING_COMMANDS = "RedisStringCommands";

    public static final String REDIS_ZSET_COMMANDS = "RedisZSetCommands";

    public static final String REDIS_TX_COMMANDS = "RedisTxCommands";

    public static final String REDIS_PUB_SUB_COMMANDS = "RedisPubSubCommands";

    public static final String REDIS_CONNECTION_COMMANDS = "RedisConnectionCommands";

    public static Object getRedisCommands(RedisConnection redisConnection, String interfaceName) {
        switch (interfaceName) {
            case REDIS_STRING_COMMANDS:
                return redisConnection.stringCommands();
            case REDIS_HASH_COMMANDS:
                return redisConnection.hashCommands();
            case REDIS_LIST_COMMANDS:
                return redisConnection.listCommands();
            case REDIS_SET_COMMANDS:
                return redisConnection.setCommands();
            case REDIS_ZSET_COMMANDS:
                return redisConnection.zSetCommands();
            case REDIS_KEY_COMMANDS:
                return redisConnection.keyCommands();
            case REDIS_SCRIPTING_COMMANDS:
                return redisConnection.scriptingCommands();

            case REDIS_GEO_COMMANDS:
                return redisConnection.geoCommands();
            case REDIS_SERVER_COMMANDS:
                return redisConnection.serverCommands();
            case REDIS_STREAM_COMMANDS:
                // TODO The Redis Spring Data version needs to be upgraded
                // return redisConnection.streamCommands();
            default:
                throw new UnsupportedOperationException(interfaceName);
        }

    }

    public static String buildCommandMethodId(RedisCommandEvent event) {
        return buildCommandMethodId(event.getInterfaceName(), event.getMethodName(), event.getParameterTypes());
    }

    public static String buildCommandMethodId(String interfaceName, String methodName, Class<?>... parameterTypes) {
        int length = parameterTypes.length;
        String[] parameterTypeNames = new String[length];
        for (int i = 0; i < length; i++) {
            parameterTypeNames[i] = parameterTypes[i].getName();
        }
        return buildCommandMethodId(interfaceName, methodName, parameterTypeNames);
    }

    public static String buildCommandMethodId(String interfaceName, String methodName, String... parameterTypes) {
        StringBuilder infoBuilder = new StringBuilder(interfaceName);
        infoBuilder.append(".").append(methodName);
        StringJoiner paramTypesInfo = new StringJoiner(",", "(", ")");
        for (String parameterType : parameterTypes) {
            paramTypesInfo.add(parameterType);
        }
        infoBuilder.append(paramTypesInfo);
        return infoBuilder.toString();
    }

    public static List<ParameterMetadata> buildParameterMetadata(Method method, Class<?>[] parameterTypes) {
        int parameterCount = parameterTypes.length;
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        List<ParameterMetadata> parameterMetadataList = new ArrayList<>(parameterCount);
        for (int i = 0; i < parameterCount; i++) {
            String parameterType = parameterTypes[i].getName();
            String parameterName = parameterNames[i];
            ParameterMetadata parameterMetadata = new ParameterMetadata(i, parameterType, parameterName);
            parameterMetadataList.add(parameterMetadata);
            // Preload the RedisSerializer implementation for the Method parameter type
            Serializers.getSerializer(parameterType);
        }
        return unmodifiableList(parameterMetadataList);
    }

    public static List<ParameterMetadata> buildParameterMetadataList(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        return buildParameterMetadata(method, parameterTypes);
    }

}
