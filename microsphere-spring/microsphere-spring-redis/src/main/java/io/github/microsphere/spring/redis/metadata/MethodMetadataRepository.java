package io.github.microsphere.spring.redis.metadata;

import io.github.microsphere.spring.redis.event.RedisCommandEvent;
import io.github.microsphere.spring.redis.serializer.Serializers;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisConnection;
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
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.SortParameters;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.github.microsphere.spring.redis.util.RedisCommandsUtils.buildCommandMethodId;
import static io.github.microsphere.spring.redis.util.RedisConstants.FAIL_FAST_ENABLED;
import static io.github.microsphere.spring.redis.util.RedisConstants.FAIL_FAST_ENABLED_PROPERTY_NAME;
import static java.util.Collections.unmodifiableList;

/**
 * Redis Method Metadata Repository
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class MethodMetadataRepository {

    private static final Logger logger = LoggerFactory.getLogger(MethodMetadataRepository.class);

    private static volatile boolean initialized = false;

    /**
     * Interface Class name and {@link Class} object cache (reduces class loading performance cost)
     */
    static final Map<String, Class<?>> redisCommandInterfacesCache = new HashMap<>();

    /**
     * Command interface class name and {@link RedisConnection} command object function
     * (such as: {@link RedisConnection#keyCommands ()}) binding
     */
    static final Map<String, Function<RedisConnection, Object>> redisCommandBindings = new HashMap<>();

    static final Map<Method, List<ParameterMetadata>> interceptedCommandMethodsMetadata = new HashMap<>();

    /**
     * Method Simple signature with {@link Method} object caching (reduces reflection cost)
     */
    static final Map<String, Method> replicatedCommandMethodsCache = new HashMap<>();

    static {
        init();
    }

    /**
     * Initialize Method Metadata
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initRedisMethodsAccessible();
        initRedisCommandsInterfaces();
        initInterceptedCommandMethods();
        initialized = true;
    }

    public static boolean isInterceptedCommandMethod(Method method) {
        return interceptedCommandMethodsMetadata.containsKey(method);
    }

    public static List<ParameterMetadata> getParameterMetadataList(Method method) {
        return interceptedCommandMethodsMetadata.get(method);
    }

    public static Method findInterceptedCommandMethod(RedisCommandEvent event) {
        String interfaceNme = event.getInterfaceName();
        String methodName = event.getMethodName();
        String[] parameterTypes = event.getParameterTypes();
        Method method = getInterceptedCommandMethod(interfaceNme, methodName, parameterTypes);
        if (method == null) {
            logger.warn("Redis event publishers and consumers have different apis. Please update consumer microsphere-spring-redis artifacts in time!");
            logger.debug("Redis command methods will use Java reflection to find (interface :{}, method name :{}, parameter list :{})...", interfaceNme, methodName, Arrays.toString(parameterTypes));
            Class<?> interfaceClass = getRedisCommandsInterfaceClass(interfaceNme);
            if (interfaceClass == null) {
                logger.warn("The current Redis consumer cannot find Redis command interface: {}. Please confirm whether the spring-data artifacts API is compatible.", interfaceNme);
                return null;
            }
            method = ReflectionUtils.findMethod(interfaceClass, methodName, event.getParameterClasses());
            if (method == null) {
                logger.warn("Current Redis consumer Redis command interface (class name: {}) in the method ({}), command method search end!", interfaceNme, buildCommandMethodId(interfaceNme, methodName, parameterTypes));
                return null;
            }
        }
        return method;
    }

    public static Method getInterceptedCommandMethod(String interfaceName, String methodName, String... parameterTypes) {
        String id = buildCommandMethodId(interfaceName, methodName, parameterTypes);
        return replicatedCommandMethodsCache.get(id);
    }

    public static Set<Method> getInterceptedCommandMethods() {
        return interceptedCommandMethodsMetadata.keySet();
    }

    /**
     * Gets the {@link RedisCommands} command interface for the specified Class name {@link Class}
     *
     * @param interfaceName {@link RedisCommands} Command interface class name
     * @return If not found, return <code>null<code>
     */
    public static Class<?> getRedisCommandsInterfaceClass(String interfaceName) {
        return redisCommandInterfacesCache.get(interfaceName);
    }

    public static Function<RedisConnection, Object> getRedisCommandBindingFunction(String interfaceName) {
        return redisCommandBindings.getOrDefault(interfaceName, redisConnection -> redisConnection);
    }

    private static void initRedisMethodsAccessible() {
        initRedisMethodsAccessible(RedisConnection.class);
        List<Class<?>> allInterfaces = ClassUtils.getAllInterfaces(RedisConnection.class);
        for (Class<?> interfaceClass : allInterfaces) {
            initRedisMethodsAccessible(interfaceClass);
        }
    }

    private static void initRedisMethodsAccessible(Class<?> interfaceClass) {
        for (Method method : interfaceClass.getMethods()) {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
        }
    }

    /**
     * Initializes the name of the {@link RedisCommands} command interface with the {@link Class} object cache
     */
    private static void initRedisCommandsInterfaces() {
        initRedisCommandsInterfacesCache();
        initRedisCommandBindings();
    }

    private static void initRedisCommandsInterfacesCache() {
        Class<?>[] interfaces = RedisCommands.class.getInterfaces();
        for (Class<?> interfaceClass : interfaces) {
            redisCommandInterfacesCache.put(interfaceClass.getName(), interfaceClass);
        }
    }

    private static void initRedisCommandBindings() {
        Class<?> redisConnectionClass = RedisConnection.class;
        for (Map.Entry<String, Class<?>> entry : redisCommandInterfacesCache.entrySet()) {
            String interfaceName = entry.getKey();
            Class<?> interfaceClass = entry.getValue();
            ReflectionUtils.doWithMethods(redisConnectionClass, method -> {
                redisCommandBindings.put(interfaceName, redisConnection -> ReflectionUtils.invokeMethod(method, redisConnection));
                logger.debug("Redis command interface {} Bind RedisConnection command object method {}", interfaceName, method);
            }, method -> interfaceClass.equals(method.getReturnType()) && method.getParameterCount() < 1);
        }
    }

    /**
     * Initializes {@link RedisCommands} command methods, including:
     * <ul>
     *     <li>{@link #initRedisKeyCommandsInterceptedCommandMethods() RedisKeyCommands intercepted command Method}</li>
     *     <li>{@link #initRedisStringCommandsInterceptedCommandMethods() RedisStringCommands intercepted command Method}</li>
     *     <li>{@link #initRedisListCommandsInterceptedCommandMethods() RedisListCommands intercepted command Method}</li>
     *     <li>{@link #initRedisSetCommandsInterceptedCommandMethods() RedisSetCommands intercepted command Method}</li>
     *     <li>{@link #initRedisZSetCommandsInterceptedCommandMethods() RedisZSetCommands intercepted command Method}</li>
     *     <li>{@link #initRedisHashCommandsInterceptedCommandMethods() RedisHashCommands intercepted command Method}</li>
     *     <li>{@link #initRedisScriptingCommandsInterceptedCommandMethods() RedisScriptingCommands intercepted command Method}</li>
     *     <li>{@link #initRedisGeoCommandsInterceptedCommandMethods() RedisGeoCommands intercepted command Method}</li>
     *     <li>{@link #initRedisHyperLogLogCommandsInterceptedCommandMethods() RedisHyperLogLogCommands intercepted command Method}</li>
     * </ol>
     * <p>
     * Not Support：
     * <ul>
     *     <li>{@link RedisTxCommands}</li>
     *     <li>{@link RedisPubSubCommands}</li>
     *     <li>{@link RedisConnectionCommands}</li>
     *     <li>{@link RedisServerCommands}</li>
     * </ul>
     */
    private static void initInterceptedCommandMethods() {

        // TODO: Support for Configuration

        // Initialize {@link RedisKeyCommands} intercepted command Method
        initRedisKeyCommandsInterceptedCommandMethods();

        // Initialize {@link RedisStringCommands} intercepted command Method
        initRedisStringCommandsInterceptedCommandMethods();

        // Initialize {@link RedisListCommands} intercepted command Method
        initRedisListCommandsInterceptedCommandMethods();

        // Initialize {@link RedisSetCommands} intercepted command Method
        initRedisSetCommandsInterceptedCommandMethods();

        // Initialize {@link RedisZSetCommands} intercepted command Method
        initRedisZSetCommandsInterceptedCommandMethods();

        // Initialize {@link RedisHashCommands} intercepted command Method
        initRedisHashCommandsInterceptedCommandMethods();

        // Initialize {@link RedisScriptingCommands} intercepted command Method
        initRedisScriptingCommandsInterceptedCommandMethods();

        // Initialize {@link RedisGeoCommands} intercepted command Method
        initRedisGeoCommandsInterceptedCommandMethods();

        // Initialize {@link RedisHyperLogLogCommands}
        initRedisHyperLogLogCommandsInterceptedCommandMethods();

    }

    /**
     * Initialize {@link RedisKeyCommands} intercepted command Method
     */
    private static void initRedisKeyCommandsInterceptedCommandMethods() {

        /**
         * del(byte[]...) Method 
         * @see <a href="https://redis.io/commands/del">Redis Documentation: DEL</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "del", byte[][].class);

        /**
         * unlink(byte[]...) Method 
         * @see <a href="https://redis.io/commands/unlink">Redis Documentation: UNLINK</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "unlink", byte[][].class);

        /**
         * touch(byte[]...) Method 
         * @see <a href="https://redis.io/commands/touch">Redis Documentation: TOUCH</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "touch", byte[][].class);

        /**
         * rename(byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/rename">Redis Documentation: RENAME</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "rename", byte[].class, byte[].class);

        /**
         * renameNX(byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/renamenx">Redis Documentation: RENAMENX</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "renameNX", byte[].class, byte[].class);

        /**
         * expire(byte[], long) Method 
         * @see <a href="https://redis.io/commands/expire">Redis Documentation: EXPIRE</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "expire", byte[].class, long.class);

        /**
         * pExpire(byte[], long) Method 
         * @see <a href="https://redis.io/commands/pexpire">Redis Documentation: PEXPIRE</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "pExpire", byte[].class, long.class);

        /**
         * expireAt(byte[], long) Method 
         * @see <a href="https://redis.io/commands/expireat">Redis Documentation: EXPIREAT</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "expireAt", byte[].class, long.class);

        /**
         * pExpireAt(byte[], long) Method 
         * @see <a href="https://redis.io/commands/pexpireat">Redis Documentation: PEXPIREAT</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "pExpireAt", byte[].class, long.class);

        /**
         * persist(byte[]) Method 
         * @see <a href="https://redis.io/commands/persist">Redis Documentation: PERSIST</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "persist", byte[].class);

        /**
         * move(byte[],int) Method 
         * @see <a href="https://redis.io/commands/move">Redis Documentation: MOVE</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "move", byte[].class, int.class);

        /**
         * ttl(byte[], TimeUnit) Method 
         * @see <a href="https://redis.io/commands/ttl">Redis Documentation: TTL</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "ttl", byte[].class, TimeUnit.class);

        /**
         * pTtl(byte[], TimeUnit) Method 
         * @see <a href="https://redis.io/commands/pttl">Redis Documentation: PTTL</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "pTtl", byte[].class, TimeUnit.class);

        /**
         * sort(byte[], SortParameters) Method 
         * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "sort", byte[].class, SortParameters.class);

        /**
         * sort(byte[], SortParameters,byte[]) Method 
         * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "sort", byte[].class, SortParameters.class, byte[].class);

        /**
         * restore(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/restore">Redis Documentation: RESTORE</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "restore", byte[].class, long.class, byte[].class);

        /**
         * restore(byte[], long, byte[], boolean) Method 
         * @see <a href="https://redis.io/commands/restore">Redis Documentation: RESTORE</a>
         */
        initInterceptedCommandMethod(RedisKeyCommands.class, "restore", byte[].class, long.class, byte[].class, boolean.class);
    }

    /**
     * Initialize {@link RedisStringCommands} intercepted command Method
     */
    private static void initRedisStringCommandsInterceptedCommandMethods() {

        /**
         * set(byte[],byte[]) Method 
         * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "set", byte[].class, byte[].class);

        /**
         * set(byte[], byte[], Expiration, SetOption) Method 
         * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "set", byte[].class, byte[].class, Expiration.class, RedisStringCommands.SetOption.class);

        /**
         * setNX(byte[],byte[]) Method 
         * @see <a href="https://redis.io/commands/setnx">Redis Documentation: SETNX</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "setNX", byte[].class, byte[].class);

        /**
         * setEx(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/setex">Redis Documentation: SETEX</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "setEx", byte[].class, long.class, byte[].class);

        /**
         * pSetEx(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/psetex">Redis Documentation: PSETEX</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "pSetEx", byte[].class, long.class, byte[].class);

        /**
         * mSet(Map<byte[], byte[]>) Method 
         * @see <a href="https://redis.io/commands/mset">Redis Documentation: MSET</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "mSet", Map.class);

        /**
         * mSetNX(Map<byte[], byte[]>) Method 
         * @see <a href="https://redis.io/commands/msetnx">Redis Documentation: MSETNX</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "mSetNX", Map.class);

        /**
         * incr(byte[]) Method 
         * @see <a href="https://redis.io/commands/incr">Redis Documentation: INCR</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "incr", byte[].class);

        /**
         * incrBy(byte[], long) Method 
         * @see <a href="https://redis.io/commands/incrby">Redis Documentation: INCRBY</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "incrBy", byte[].class, long.class);

        /**
         * incrBy(byte[], double) Method 
         * @see <a href="https://redis.io/commands/incrbyfloat">Redis Documentation: INCRBYFLOAT</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "incrBy", byte[].class, double.class);

        /**
         * decr(byte[]) Method 
         * @see <a href="https://redis.io/commands/decr">Redis Documentation: DECR</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "decr", byte[].class);

        /**
         * decrBy(byte[], long) Method 
         * @see <a href="https://redis.io/commands/decrby">Redis Documentation: DECRBY</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "decrBy", byte[].class, long.class);

        /**
         * append(byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/append">Redis Documentation: APPEND</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "append", byte[].class, byte[].class);

        /**
         * setRange(byte[], byte[], long) Method 
         * @see <a href="https://redis.io/commands/setrange">Redis Documentation: SETRANGE</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "setRange", byte[].class, byte[].class, long.class);

        /**
         * setBit(byte[], long, boolean) Method 
         * @see <a href="https://redis.io/commands/setbit">Redis Documentation: SETBIT</a>
         */
        initInterceptedCommandMethod(RedisStringCommands.class, "setBit", byte[].class, long.class, boolean.class);
    }


    /**
     * Initialize {@link RedisListCommands} intercepted command Method
     */
    private static void initRedisListCommandsInterceptedCommandMethods() {

        /**
         * rPush(byte[] ,byte[]...) Method 
         * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "rPush", byte[].class, byte[][].class);

        /**
         * lPush(byte[] ,byte[]...) Method 
         * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "lPush", byte[].class, byte[][].class);

        /**
         * rPushX(byte[] ,byte[]) Method 
         * @see <a href="https://redis.io/commands/rpushx">Redis Documentation: RPUSHX</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "rPushX", byte[].class, byte[].class);

        /**
         * lPushX(byte[] ,byte[]) Method 
         * @see <a href="https://redis.io/commands/lpushx">Redis Documentation: LPUSHX</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "lPushX", byte[].class, byte[].class);

        /**
         * lTrim(byte[], long, long) Method 
         * @see <a href="https://redis.io/commands/ltrim">Redis Documentation: LTRIM</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "lTrim", byte[].class, long.class, long.class);

        /**
         * lInsert(byte[], Position, byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/linsert">Redis Documentation: LINSERT</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "lInsert", byte[].class, RedisListCommands.Position.class, byte[].class, byte[].class);

        /**
         * lSet(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/lset">Redis Documentation: LSET</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "lSet", byte[].class, long.class, byte[].class);

        /**
         * lRem(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/lrem">Redis Documentation: LREM</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "lRem", byte[].class, long.class, byte[].class);

        /**
         * lPop(byte[]) Method 
         * @see <a href="https://redis.io/commands/lpop">Redis Documentation: LPOP</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "lPop", byte[].class);

        /**
         * rPop(byte[]) Method 
         * @see <a href="https://redis.io/commands/rpop">Redis Documentation: RPOP</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "rPop", byte[].class);

        /**
         * bLPop(int, byte[]...) Method 
         * @see <a href="https://redis.io/commands/blpop">Redis Documentation: BLPOP</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "bLPop", int.class, byte[][].class);

        /**
         * bRPop(int, byte[]...) Method 
         * @see <a href="https://redis.io/commands/brpop">Redis Documentation: BRPOP</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "bRPop", int.class, byte[][].class);

        /**
         * rPopLPush(byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/rpoplpush">Redis Documentation: RPOPLPUSH</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "rPopLPush", byte[].class, byte[].class);

        /**
         * bRPopLPush(int, byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/brpoplpush">Redis Documentation: BRPOPLPUSH</a>
         */
        initInterceptedCommandMethod(RedisListCommands.class, "bRPopLPush", int.class, byte[].class, byte[].class);
    }

    /**
     * Initialize {@link RedisSetCommands} intercepted command Method
     */
    private static void initRedisSetCommandsInterceptedCommandMethods() {

        /**
         * sAdd(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/sadd">Redis Documentation: SADD</a>
         */
        initInterceptedCommandMethod(RedisSetCommands.class, "sAdd", byte[].class, byte[][].class);

        /**
         * sRem(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/srem">Redis Documentation: SREM</a>
         */
        initInterceptedCommandMethod(RedisSetCommands.class, "sRem", byte[].class, byte[][].class);

        /**
         * sPop(byte[]) Method 
         * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
         */
        initInterceptedCommandMethod(RedisSetCommands.class, "sPop", byte[].class);

        /**
         * sPop(byte[], long) Method 
         * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
         */
        initInterceptedCommandMethod(RedisSetCommands.class, "sPop", byte[].class, long.class);

        /**
         * sMove(byte[], byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/smove">Redis Documentation: SMOVE</a>
         */
        initInterceptedCommandMethod(RedisSetCommands.class, "sMove", byte[].class, byte[].class, byte[].class);

        /**
         * sInterStore(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/sinterstore">Redis Documentation: SINTERSTORE</a>
         */
        initInterceptedCommandMethod(RedisSetCommands.class, "sInterStore", byte[].class, byte[][].class);

        /**
         * sUnionStore(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/sunionstore">Redis Documentation: SUNIONSTORE</a>
         */
        initInterceptedCommandMethod(RedisSetCommands.class, "sUnionStore", byte[].class, byte[][].class);

        /**
         * sDiffStore(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/sdiffstore">Redis Documentation: SDIFFSTORE</a>
         */
        initInterceptedCommandMethod(RedisSetCommands.class, "sDiffStore", byte[].class, byte[][].class);
    }

    /**
     * Initialize {@link RedisZSetCommands} intercepted command Method
     */
    private static void initRedisZSetCommandsInterceptedCommandMethods() {

        /**
         * zAdd(byte[], double, byte[]) Method 
         * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zAdd", byte[].class, double.class, byte[].class);

        /**
         * zAdd(byte[], Set<Tuple>) Method 
         * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
         * TODO Support {@link Tuple}
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zAdd", byte[].class, Set.class);

        /**
         * zRem(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zRem", byte[].class, byte[][].class);

        /**
         * zIncrBy(byte[], double, byte[]) Method 
         * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zIncrBy", byte[].class, double.class, byte[].class);

        /**
         * zRemRange(byte[], long, long) Method 
         * @see <a href="https://redis.io/commands/zremrangebyrank">Redis Documentation: ZREMRANGEBYRANK</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zRemRange", byte[].class, long.class, long.class);

        /**
         * zRemRangeByScore(byte[], Range) Method 
         * @see <a href="https://redis.io/commands/zremrangebyscore">Redis Documentation: ZREMRANGEBYSCORE</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zRemRangeByScore", byte[].class, RedisZSetCommands.Range.class);

        /**
         * zRemRangeByScore(byte[], double, double) Method 
         * @see <a href="https://redis.io/commands/zremrangebyscore">Redis Documentation: ZREMRANGEBYSCORE</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zRemRangeByScore", byte[].class, double.class, double.class);

        /**
         * zUnionStore(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/zunionstore">Redis Documentation: ZUNIONSTORE</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zUnionStore", byte[].class, byte[][].class);

        /**
         * zUnionStore(byte[], Aggregate, int[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/zunionstore">Redis Documentation: ZUNIONSTORE</a>
         * TODO Support {@link Aggregate}
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zUnionStore", byte[].class, RedisZSetCommands.Aggregate.class, int[].class, byte[][].class);

        /**
         * zInterStore(byte[], byte[]...)*
         * @see <a href="https://redis.io/commands/zinterstore">Redis Documentation: ZINTERSTORE</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zInterStore", byte[].class, byte[][].class);

        /**
         * zInterStore(byte[], Aggregate, int[] weights, byte[]...)
         * @see <a href="https://redis.io/commands/zinterstore">Redis Documentation: ZINTERSTORE</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zInterStore", byte[].class, RedisZSetCommands.Aggregate.class, int[].class, byte[][].class);

        /**
         * zInterStore(byte[], Aggregate, RedisZSetCommands.Weights, byte[]... sets)
         * @see <a href="https://redis.io/commands/zinterstore">Redis Documentation: ZINTERSTORE</a>
         */
        initInterceptedCommandMethod(RedisZSetCommands.class, "zInterStore", byte[].class, RedisZSetCommands.Aggregate.class, RedisZSetCommands.Weights.class, byte[][].class);

    }

    /**
     * Initialize {@link RedisHashCommands} intercepted command Method
     */
    private static void initRedisHashCommandsInterceptedCommandMethods() {

        /**
         * hSet(byte[], byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/hset">Redis Documentation: HSET</a>
         */
        initInterceptedCommandMethod(RedisHashCommands.class, "hSet", byte[].class, byte[].class, byte[].class);

        /**
         * hSetNX(byte[], byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/hsetnx">Redis Documentation: HSETNX</a>
         */
        initInterceptedCommandMethod(RedisHashCommands.class, "hSetNX", byte[].class, byte[].class, byte[].class);

        /**
         * hMSet(byte[], Map<byte[], byte[]>) Method 
         * @see <a href="https://redis.io/commands/hmset">Redis Documentation: HMSET</a>
         */
        initInterceptedCommandMethod(RedisHashCommands.class, "hMSet", byte[].class, Map.class);

        /**
         * hIncrBy(byte[], byte[], long) Method 
         * @see <a href="https://redis.io/commands/hmset">Redis Documentation: HMSET</a>
         */
        initInterceptedCommandMethod(RedisHashCommands.class, "hIncrBy", byte[].class, byte[].class, long.class);

        /**
         * hIncrBy(byte[], byte[], double) Method 
         * @see <a href="https://redis.io/commands/hincrbyfloat">Redis Documentation: HINCRBYFLOAT</a>
         */
        initInterceptedCommandMethod(RedisHashCommands.class, "hIncrBy", byte[].class, byte[].class, double.class);

        /**
         * hDel(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/hdel">Redis Documentation: HDEL</a>
         */
        initInterceptedCommandMethod(RedisHashCommands.class, "hDel", byte[].class, byte[][].class);

    }

    /**
     * Initialize {@link RedisScriptingCommands} intercepted command Method
     */
    private static void initRedisScriptingCommandsInterceptedCommandMethods() {

        /**
         * scriptLoad(byte[]) Method 
         * @see <a href="https://redis.io/commands/script-load">Redis Documentation: SCRIPT LOAD</a>
         */
        initInterceptedCommandMethod(RedisScriptingCommands.class, "scriptLoad", byte[].class);

        /**
         * eval(byte[], ReturnType, int, byte[]...) Method 
         * @see <a href="https://redis.io/commands/eval">Redis Documentation: EVAL</a>
         */
        initInterceptedCommandMethod(RedisScriptingCommands.class, "eval", byte[].class, ReturnType.class, int.class, byte[][].class);

        /**
         * evalSha(String, ReturnType, int numKeys, byte[]...) Method 
         * @see <a href="https://redis.io/commands/evalsha">Redis Documentation: EVALSHA</a>
         */
        initInterceptedCommandMethod(RedisScriptingCommands.class, "evalSha", String.class, ReturnType.class, int.class, byte[][].class);

        /**
         * evalSha(byte[], ReturnType, int, byte[]...) Method 
         * @see <a href="https://redis.io/commands/evalsha">Redis Documentation: EVALSHA</a>
         */
        initInterceptedCommandMethod(RedisScriptingCommands.class, "evalSha", byte[].class, ReturnType.class, int.class, byte[][].class);

    }

    /**
     * Initialize {@link RedisGeoCommands} intercepted command Method
     */
    private static void initRedisGeoCommandsInterceptedCommandMethods() {

        /**
         * geoAdd(byte[], Point, byte[]) Method 
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initInterceptedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, Point.class, byte[].class);

        /**
         * geoAdd(byte[], GeoLocation<byte[]>) Method 
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initInterceptedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, RedisGeoCommands.GeoLocation.class);

        /**
         * geoAdd(byte[], Map<byte[], Point>) Method 
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initInterceptedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, Map.class);

        /**
         * geoAdd(byte[], Iterable<RedisGeoCommands.GeoLocation<byte[]>>) Method 
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initInterceptedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, Iterable.class);

        /**
         * geoRemove(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
         */
        initInterceptedCommandMethod(RedisGeoCommands.class, "geoRemove", byte[].class, byte[][].class);
    }

    /**
     * Initialize {@link RedisHyperLogLogCommands}
     */
    private static void initRedisHyperLogLogCommandsInterceptedCommandMethods() {

        /**
         * pfAdd(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/pfadd">Redis Documentation: PFADD</a>
         */
        initInterceptedCommandMethod(RedisHyperLogLogCommands.class, "pfAdd", byte[].class, byte[][].class);

        /**
         * pfMerge(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/pfmerge">Redis Documentation: PFMERGE</a>
         */
        initInterceptedCommandMethod(RedisHyperLogLogCommands.class, "pfMerge", byte[].class, byte[][].class);
    }

    private static void initInterceptedCommandMethod(Class<?> declaredClass, String methodName, Class<?>... parameterTypes) {
        try {
            logger.debug("Initializes the intercepted command Method[Declared Class: {} , Method: {}, Parameter types: {}]...", declaredClass.getName(), methodName, Arrays.toString(parameterTypes));
            Method method = declaredClass.getMethod(methodName, parameterTypes);
            // Reduced Method runtime checks
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            initInterceptedCommandMethodMethod(method, parameterTypes);
            initInterceptedCommandMethodCache(declaredClass, method, parameterTypes);
        } catch (Throwable e) {
            logger.error("Unable to initialize intercepted command Method[Declared Class: {}, Method: {}, Parameter types: {}], Reason: {}", declaredClass.getName(), methodName, Arrays.toString(parameterTypes), e.getMessage());
            if (FAIL_FAST_ENABLED) {
                logger.error("Fail-Fast mode is activated and an exception is about to be thrown. You can disable Fail-Fast mode with the JVM startup parameter -D{}=false", FAIL_FAST_ENABLED_PROPERTY_NAME);
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static void initInterceptedCommandMethodMethod(Method method, Class<?>[] parameterTypes) {
        if (interceptedCommandMethodsMetadata.containsKey(method)) {
            throw new IllegalArgumentException("Repeat the initialization intercepted command Method: " + method);
        }
        List<ParameterMetadata> parameterMetadataList = buildParameterMetadata(parameterTypes);
        interceptedCommandMethodsMetadata.put(method, parameterMetadataList);
        logger.debug("Initializing intercepted command Method metadata information successfully, Method: {}, parameter Intercepted Command Method metadata information: {}", method.getName(), parameterMetadataList);
    }

    private static void initInterceptedCommandMethodCache(Class<?> declaredClass, Method method, Class<?>[] parameterTypes) {
        String id = buildCommandMethodId(declaredClass.getName(), method.getName(), parameterTypes);
        if (replicatedCommandMethodsCache.putIfAbsent(id, method) == null) {
            logger.debug("Cache intercepted command Method[id: {}, Method: {}]", id, method);
        } else {
            logger.warn("Intercepted command Method[id: {}, Method: {}] is cached", id, method);
        }
    }

    private static List<ParameterMetadata> buildParameterMetadata(Class<?>[] parameterTypes) {
        int parameterCount = parameterTypes.length;
        List<ParameterMetadata> parameterMetadataList = new ArrayList<>(parameterCount);
        for (int i = 0; i < parameterCount; i++) {
            String parameterType = parameterTypes[i].getName();
            ParameterMetadata parameterMetadata = new ParameterMetadata(i, parameterType);
            parameterMetadataList.add(parameterMetadata);
            // Preload the RedisSerializer implementation for the Method parameter type
            Serializers.getSerializer(parameterType);
        }
        return unmodifiableList(parameterMetadataList);
    }
}
