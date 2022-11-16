package io.github.microsphere.spring.redis.metadata;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.redis.event.RedisCommandEvent;
import io.github.microsphere.spring.redis.serializer.Serializers;
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
import static java.util.Collections.unmodifiableList;

/**
 * Redis Method Metadata Repository
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class MethodMetadataRepository {

    private static final Logger logger = LoggerFactory.getLogger(MethodMetadataRepository.class);

    /**
     * Interface Class name and {@link Class} object cache (reduces class loading performance cost)
     */
    static final Map<String, Class<?>> redisCommandInterfacesCache = new HashMap<>();

    /**
     * Command interface class name and {@link RedisConnection} command object function
     * (such as: {@link RedisConnection#keyCommands ()}) binding
     */
    static final Map<String, Function<RedisConnection, Object>> redisCommandBindings = new HashMap<>();

    static final Map<Method, List<ParameterMetadata>> replicatedCommandMethodsMetadata = new HashMap<>();

    /**
     * Method Simple signature with {@link Method} object caching (reduces reflection cost)
     */
    static final Map<String, Method> replicatedCommandMethodsCache = new HashMap<>();

    static {
        initRedisCommandsInterfaces();
        initReplicatedCommandMethods();
    }

    public static boolean isReplicatedCommandMethod(Method method) {
        return replicatedCommandMethodsMetadata.containsKey(method);
    }

    public static List<ParameterMetadata> getParameterMetadataList(Method method) {
        return replicatedCommandMethodsMetadata.get(method);
    }

    public static Method findReplicatedCommandMethod(RedisCommandEvent event) {
        String interfaceNme = event.getInterfaceName();
        String methodName = event.getMethodName();
        String[] parameterTypes = event.getParameterTypes();
        Method method = getReplicatedCommandMethod(interfaceNme, methodName, parameterTypes);
        if (method == null) {
            logger.warn("Redis event publishers and consumers have different apis. Please update consumer microsphere-spring-redis artifacts in time!");
            logger.debug("Redis copy command methods will use Java reflection to find (interface :{}, method name :{}, parameter list :{})...", interfaceNme, methodName, Arrays.toString(parameterTypes));
            Class<?> interfaceClass = getRedisCommandsInterfaceClass(interfaceNme);
            if (interfaceClass == null) {
                logger.warn("The current Redis consumer cannot find Redis command interface: {}. Please confirm whether the spring-data artifacts API is compatible.", interfaceNme);
                return null;
            }
            method = ReflectionUtils.findMethod(interfaceClass, methodName, event.getParameterClasses());
            if (method == null) {
                logger.warn("Current Redis consumer Redis command interface (class name: {}) in the method ({}), copy command method search end!",
                        interfaceNme, buildCommandMethodId(interfaceNme, methodName, parameterTypes));
                return null;
            }
        }
        return method;
    }

    public static Method getReplicatedCommandMethod(String interfaceName, String methodName, String... parameterTypes) {
        String id = buildCommandMethodId(interfaceName, methodName, parameterTypes);
        return replicatedCommandMethodsCache.get(id);
    }

    public static Set<Method> getReplicatedCommandMethods() {
        return replicatedCommandMethodsMetadata.keySet();
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
     * Initializes {@link RedisCommands} command copy command methods, including:
     * <ul>
     *     <li>{@link #initRedisKeyCommandsReplicatedCommandMethods() RedisKeyCommands Copy command Method}</li>
     *     <li>{@link #initRedisStringCommandsReplicatedCommandMethods() RedisStringCommands Replicated command Method}</li>
     *     <li>{@link #initRedisListCommandsReplicatedCommandMethods() RedisListCommands Replicated command Method}</li>
     *     <li>{@link #initRedisSetCommandsReplicatedCommandMethods() RedisSetCommands Replicated command Method}</li>
     *     <li>{@link #initRedisZSetCommandsReplicatedCommandMethods() RedisZSetCommands Replicated command Method}</li>
     *     <li>{@link #initRedisHashCommandsReplicatedCommandMethods() RedisHashCommands Replicated command Method}</li>
     *     <li>{@link #initRedisScriptingCommandsReplicatedCommandMethods() RedisScriptingCommands Replicated command Method}</li>
     *     <li>{@link #initRedisGeoCommandsReplicatedCommandMethods() RedisGeoCommands Replicated command Method}</li>
     *     <li>{@link #initRedisHyperLogLogCommandsReplicatedCommandMethods() RedisHyperLogLogCommands Replicated command Method}</li>
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
    private static void initReplicatedCommandMethods() {

        // TODO: Support for Configuration

        // Initialize {@link RedisKeyCommands} Replicated command Method
        initRedisKeyCommandsReplicatedCommandMethods();

        // Initialize {@link RedisStringCommands} Replicated command Method
        initRedisStringCommandsReplicatedCommandMethods();

        // Initialize {@link RedisListCommands} Replicated command Method
        initRedisListCommandsReplicatedCommandMethods();

        // Initialize {@link RedisSetCommands} Replicated command Method
        initRedisSetCommandsReplicatedCommandMethods();

        // Initialize {@link RedisZSetCommands} Replicated command Method
        initRedisZSetCommandsReplicatedCommandMethods();

        // Initialize {@link RedisHashCommands} Replicated command Method
        initRedisHashCommandsReplicatedCommandMethods();

        // Initialize {@link RedisScriptingCommands} Replicated command Method
        initRedisScriptingCommandsReplicatedCommandMethods();

        // Initialize {@link RedisGeoCommands} Replicated command Method
        initRedisGeoCommandsReplicatedCommandMethods();

        // Initialize {@link RedisHyperLogLogCommands}
        initRedisHyperLogLogCommandsReplicatedCommandMethods();

    }

    /**
     * Initialize {@link RedisKeyCommands} Replicated command Method
     */
    private static void initRedisKeyCommandsReplicatedCommandMethods() {

        /**
         * del(byte[]...) Method 
         * @see <a href="https://redis.io/commands/del">Redis Documentation: DEL</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "del", byte[][].class);

        /**
         * unlink(byte[]...) Method 
         * @see <a href="https://redis.io/commands/unlink">Redis Documentation: UNLINK</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "unlink", byte[][].class);

        /**
         * touch(byte[]...) Method 
         * @see <a href="https://redis.io/commands/touch">Redis Documentation: TOUCH</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "touch", byte[][].class);

        /**
         * rename(byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/rename">Redis Documentation: RENAME</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "rename", byte[].class, byte[].class);

        /**
         * renameNX(byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/renamenx">Redis Documentation: RENAMENX</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "renameNX", byte[].class, byte[].class);

        /**
         * expire(byte[], long) Method 
         * @see <a href="https://redis.io/commands/expire">Redis Documentation: EXPIRE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "expire", byte[].class, long.class);

        /**
         * pExpire(byte[], long) Method 
         * @see <a href="https://redis.io/commands/pexpire">Redis Documentation: PEXPIRE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "pExpire", byte[].class, long.class);

        /**
         * expireAt(byte[], long) Method 
         * @see <a href="https://redis.io/commands/expireat">Redis Documentation: EXPIREAT</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "expireAt", byte[].class, long.class);

        /**
         * pExpireAt(byte[], long) Method 
         * @see <a href="https://redis.io/commands/pexpireat">Redis Documentation: PEXPIREAT</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "pExpireAt", byte[].class, long.class);

        /**
         * persist(byte[]) Method 
         * @see <a href="https://redis.io/commands/persist">Redis Documentation: PERSIST</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "persist", byte[].class);

        /**
         * move(byte[],int) Method 
         * @see <a href="https://redis.io/commands/move">Redis Documentation: MOVE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "move", byte[].class, int.class);

        /**
         * ttl(byte[], TimeUnit) Method 
         * @see <a href="https://redis.io/commands/ttl">Redis Documentation: TTL</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "ttl", byte[].class, TimeUnit.class);

        /**
         * pTtl(byte[], TimeUnit) Method 
         * @see <a href="https://redis.io/commands/pttl">Redis Documentation: PTTL</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "pTtl", byte[].class, TimeUnit.class);

        /**
         * sort(byte[], SortParameters) Method 
         * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "sort", byte[].class, SortParameters.class);

        /**
         * sort(byte[], SortParameters,byte[]) Method 
         * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "sort", byte[].class, SortParameters.class, byte[].class);

        /**
         * restore(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/restore">Redis Documentation: RESTORE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "restore", byte[].class, long.class, byte[].class);

        /**
         * restore(byte[], long, byte[], boolean) Method 
         * @see <a href="https://redis.io/commands/restore">Redis Documentation: RESTORE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "restore", byte[].class, long.class, byte[].class, boolean.class);
    }

    /**
     * Initialize {@link RedisStringCommands} Replicated command Method
     */
    private static void initRedisStringCommandsReplicatedCommandMethods() {

        /**
         * set(byte[],byte[]) Method 
         * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "set", byte[].class, byte[].class);

        /**
         * set(byte[], byte[], Expiration, SetOption) Method 
         * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "set", byte[].class, byte[].class, Expiration.class, RedisStringCommands.SetOption.class);

        /**
         * setNX(byte[],byte[]) Method 
         * @see <a href="https://redis.io/commands/setnx">Redis Documentation: SETNX</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "setNX", byte[].class, byte[].class);

        /**
         * setEx(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/setex">Redis Documentation: SETEX</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "setEx", byte[].class, long.class, byte[].class);

        /**
         * pSetEx(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/psetex">Redis Documentation: PSETEX</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "pSetEx", byte[].class, long.class, byte[].class);

        /**
         * mSet(Map<byte[], byte[]>) Method 
         * @see <a href="https://redis.io/commands/mset">Redis Documentation: MSET</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "mSet", Map.class);

        /**
         * mSetNX(Map<byte[], byte[]>) Method 
         * @see <a href="https://redis.io/commands/msetnx">Redis Documentation: MSETNX</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "mSetNX", Map.class);

        /**
         * incr(byte[]) Method 
         * @see <a href="https://redis.io/commands/incr">Redis Documentation: INCR</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "incr", byte[].class);

        /**
         * incrBy(byte[], long) Method 
         * @see <a href="https://redis.io/commands/incrby">Redis Documentation: INCRBY</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "incrBy", byte[].class, long.class);

        /**
         * incrBy(byte[], double) Method 
         * @see <a href="https://redis.io/commands/incrbyfloat">Redis Documentation: INCRBYFLOAT</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "incrBy", byte[].class, double.class);

        /**
         * decr(byte[]) Method 
         * @see <a href="https://redis.io/commands/decr">Redis Documentation: DECR</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "decr", byte[].class);

        /**
         * decrBy(byte[], long) Method 
         * @see <a href="https://redis.io/commands/decrby">Redis Documentation: DECRBY</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "decrBy", byte[].class, long.class);

        /**
         * append(byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/append">Redis Documentation: APPEND</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "append", byte[].class, byte[].class);

        /**
         * setRange(byte[], byte[], long) Method 
         * @see <a href="https://redis.io/commands/setrange">Redis Documentation: SETRANGE</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "setRange", byte[].class, byte[].class, long.class);

        /**
         * setBit(byte[], long, boolean) Method 
         * @see <a href="https://redis.io/commands/setbit">Redis Documentation: SETBIT</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "setBit", byte[].class, long.class, boolean.class);
    }


    /**
     * Initialize {@link RedisListCommands} Replicated command Method
     */
    private static void initRedisListCommandsReplicatedCommandMethods() {

        /**
         * rPush(byte[] ,byte[]...) Method 
         * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "rPush", byte[].class, byte[][].class);

        /**
         * lPush(byte[] ,byte[]...) Method 
         * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lPush", byte[].class, byte[][].class);

        /**
         * rPushX(byte[] ,byte[]) Method 
         * @see <a href="https://redis.io/commands/rpushx">Redis Documentation: RPUSHX</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "rPushX", byte[].class, byte[].class);

        /**
         * lPushX(byte[] ,byte[]) Method 
         * @see <a href="https://redis.io/commands/lpushx">Redis Documentation: LPUSHX</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lPushX", byte[].class, byte[].class);

        /**
         * lTrim(byte[], long, long) Method 
         * @see <a href="https://redis.io/commands/ltrim">Redis Documentation: LTRIM</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lTrim", byte[].class, long.class, long.class);

        /**
         * lInsert(byte[], Position, byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/linsert">Redis Documentation: LINSERT</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lInsert", byte[].class, RedisListCommands.Position.class, byte[].class, byte[].class);

        /**
         * lSet(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/lset">Redis Documentation: LSET</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lSet", byte[].class, long.class, byte[].class);

        /**
         * lRem(byte[], long, byte[]) Method 
         * @see <a href="https://redis.io/commands/lrem">Redis Documentation: LREM</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lRem", byte[].class, long.class, byte[].class);

        /**
         * lPop(byte[]) Method 
         * @see <a href="https://redis.io/commands/lpop">Redis Documentation: LPOP</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lPop", byte[].class);

        /**
         * rPop(byte[]) Method 
         * @see <a href="https://redis.io/commands/rpop">Redis Documentation: RPOP</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "rPop", byte[].class);

        /**
         * bLPop(int, byte[]...) Method 
         * @see <a href="https://redis.io/commands/blpop">Redis Documentation: BLPOP</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "bLPop", int.class, byte[][].class);

        /**
         * bRPop(int, byte[]...) Method 
         * @see <a href="https://redis.io/commands/brpop">Redis Documentation: BRPOP</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "bRPop", int.class, byte[][].class);

        /**
         * rPopLPush(byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/rpoplpush">Redis Documentation: RPOPLPUSH</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "rPopLPush", byte[].class, byte[].class);

        /**
         * bRPopLPush(int, byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/brpoplpush">Redis Documentation: BRPOPLPUSH</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "bRPopLPush", int.class, byte[].class, byte[].class);
    }

    /**
     * Initialize {@link RedisSetCommands} Replicated command Method
     */
    private static void initRedisSetCommandsReplicatedCommandMethods() {

        /**
         * sAdd(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/sadd">Redis Documentation: SADD</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sAdd", byte[].class, byte[][].class);

        /**
         * sRem(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/srem">Redis Documentation: SREM</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sRem", byte[].class, byte[][].class);

        /**
         * sPop(byte[]) Method 
         * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sPop", byte[].class);

        /**
         * sPop(byte[], long) Method 
         * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sPop", byte[].class, long.class);

        /**
         * sMove(byte[], byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/smove">Redis Documentation: SMOVE</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sMove", byte[].class, byte[].class, byte[].class);

        /**
         * sInterStore(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/sinterstore">Redis Documentation: SINTERSTORE</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sInterStore", byte[].class, byte[][].class);

        /**
         * sUnionStore(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/sunionstore">Redis Documentation: SUNIONSTORE</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sUnionStore", byte[].class, byte[][].class);

        /**
         * sDiffStore(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/sdiffstore">Redis Documentation: SDIFFSTORE</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sDiffStore", byte[].class, byte[][].class);
    }

    /**
     * Initialize {@link RedisZSetCommands} Replicated command Method
     */
    private static void initRedisZSetCommandsReplicatedCommandMethods() {

        /**
         * zAdd(byte[], double, byte[]) Method 
         * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zAdd", byte[].class, double.class, byte[].class);

        /**
         * zAdd(byte[], Set<Tuple>) Method 
         * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
         * TODO Support {@link Tuple}
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zAdd", byte[].class, Set.class);

        /**
         * zRem(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zRem", byte[].class, byte[][].class);

        /**
         * zIncrBy(byte[], double, byte[]) Method 
         * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zIncrBy", byte[].class, double.class, byte[].class);

        /**
         * zRemRange(byte[], long, long) Method 
         * @see <a href="https://redis.io/commands/zremrangebyrank">Redis Documentation: ZREMRANGEBYRANK</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zRemRange", byte[].class, long.class, long.class);

        /**
         * zRemRangeByScore(byte[], Range) Method 
         * @see <a href="https://redis.io/commands/zremrangebyscore">Redis Documentation: ZREMRANGEBYSCORE</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zRemRangeByScore", byte[].class, RedisZSetCommands.Range.class);

        /**
         * zRemRangeByScore(byte[], double, double) Method 
         * @see <a href="https://redis.io/commands/zremrangebyscore">Redis Documentation: ZREMRANGEBYSCORE</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zRemRangeByScore", byte[].class, double.class, double.class);

        /**
         * zUnionStore(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/zunionstore">Redis Documentation: ZUNIONSTORE</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zUnionStore", byte[].class, byte[][].class);

        /**
         * zUnionStore(byte[], Aggregate, int[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/zunionstore">Redis Documentation: ZUNIONSTORE</a>
         * TODO Support {@link Aggregate}
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zUnionStore", byte[].class, RedisZSetCommands.Aggregate.class, int[].class, byte[][].class);

        /**
         * zInterStore(byte[], byte[]...)*
         * @see <a href="https://redis.io/commands/zinterstore">Redis Documentation: ZINTERSTORE</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zInterStore", byte[].class, byte[][].class);

        /**
         * zInterStore(byte[], Aggregate, int[] weights, byte[]...)
         * @see <a href="https://redis.io/commands/zinterstore">Redis Documentation: ZINTERSTORE</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zInterStore", byte[].class, RedisZSetCommands.Aggregate.class, int[].class, byte[][].class);

        /**
         * zInterStore(byte[], Aggregate, RedisZSetCommands.Weights, byte[]... sets)
         * @see <a href="https://redis.io/commands/zinterstore">Redis Documentation: ZINTERSTORE</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zInterStore", byte[].class, RedisZSetCommands.Aggregate.class, RedisZSetCommands.Weights.class, byte[][].class);

    }

    /**
     * Initialize {@link RedisHashCommands} Replicated command Method
     */
    private static void initRedisHashCommandsReplicatedCommandMethods() {

        /**
         * hSet(byte[], byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/hset">Redis Documentation: HSET</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hSet", byte[].class, byte[].class, byte[].class);

        /**
         * hSetNX(byte[], byte[], byte[]) Method 
         * @see <a href="https://redis.io/commands/hsetnx">Redis Documentation: HSETNX</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hSetNX", byte[].class, byte[].class, byte[].class);

        /**
         * hMSet(byte[], Map<byte[], byte[]>) Method 
         * @see <a href="https://redis.io/commands/hmset">Redis Documentation: HMSET</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hMSet", byte[].class, Map.class);

        /**
         * hIncrBy(byte[], byte[], long) Method 
         * @see <a href="https://redis.io/commands/hmset">Redis Documentation: HMSET</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hIncrBy", byte[].class, byte[].class, long.class);

        /**
         * hIncrBy(byte[], byte[], double) Method 
         * @see <a href="https://redis.io/commands/hincrbyfloat">Redis Documentation: HINCRBYFLOAT</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hIncrBy", byte[].class, byte[].class, double.class);

        /**
         * hDel(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/hdel">Redis Documentation: HDEL</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hDel", byte[].class, byte[][].class);

    }

    /**
     * Initialize {@link RedisScriptingCommands} Replicated command Method
     */
    private static void initRedisScriptingCommandsReplicatedCommandMethods() {

        /**
         * scriptLoad(byte[]) Method 
         * @see <a href="https://redis.io/commands/script-load">Redis Documentation: SCRIPT LOAD</a>
         */
        initReplicatedCommandMethod(RedisScriptingCommands.class, "scriptLoad", byte[].class);

        /**
         * eval(byte[], ReturnType, int, byte[]...) Method 
         * @see <a href="https://redis.io/commands/eval">Redis Documentation: EVAL</a>
         */
        initReplicatedCommandMethod(RedisScriptingCommands.class, "eval", byte[].class, ReturnType.class, int.class, byte[][].class);

        /**
         * evalSha(String, ReturnType, int numKeys, byte[]...) Method 
         * @see <a href="https://redis.io/commands/evalsha">Redis Documentation: EVALSHA</a>
         */
        initReplicatedCommandMethod(RedisScriptingCommands.class, "evalSha", String.class, ReturnType.class, int.class, byte[][].class);

        /**
         * evalSha(byte[], ReturnType, int, byte[]...) Method 
         * @see <a href="https://redis.io/commands/evalsha">Redis Documentation: EVALSHA</a>
         */
        initReplicatedCommandMethod(RedisScriptingCommands.class, "evalSha", byte[].class, ReturnType.class, int.class, byte[][].class);

    }

    /**
     * Initialize {@link RedisGeoCommands} Replicated command Method
     */
    private static void initRedisGeoCommandsReplicatedCommandMethods() {

        /**
         * geoAdd(byte[], Point, byte[]) Method 
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, Point.class, byte[].class);

        /**
         * geoAdd(byte[], GeoLocation<byte[]>) Method 
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, RedisGeoCommands.GeoLocation.class);

        /**
         * geoAdd(byte[], Map<byte[], Point>) Method 
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, Map.class);

        /**
         * geoAdd(byte[], Iterable<RedisGeoCommands.GeoLocation<byte[]>>) Method 
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, Iterable.class);

        /**
         * geoRemove(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoRemove", byte[].class, byte[][].class);
    }

    /**
     * Initialize {@link RedisHyperLogLogCommands}
     */
    private static void initRedisHyperLogLogCommandsReplicatedCommandMethods() {

        /**
         * pfAdd(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/pfadd">Redis Documentation: PFADD</a>
         */
        initReplicatedCommandMethod(RedisHyperLogLogCommands.class, "pfAdd", byte[].class, byte[][].class);

        /**
         * pfMerge(byte[], byte[]...) Method 
         * @see <a href="https://redis.io/commands/pfmerge">Redis Documentation: PFMERGE</a>
         */
        initReplicatedCommandMethod(RedisHyperLogLogCommands.class, "pfMerge", byte[].class, byte[][].class);
    }

    private static void initReplicatedCommandMethod(Class<?> declaredClass, String methodName, Class<?>... parameterTypes) {
        try {
            logger.debug("Initializes the Replicated command Method[Declared Class: {} , Method: {}, Parameter types: {}]...", declaredClass.getName(), methodName, Arrays.toString(parameterTypes));
            Method method = declaredClass.getMethod(methodName, parameterTypes);
            // Reduced Method runtime checks
            method.setAccessible(true);
            initReplicatedCommandMethodMethod(method, parameterTypes);
            initReplicatedCommandMethodCache(declaredClass, method, parameterTypes);
        } catch (Throwable e) {
            logger.error("Unable to initialize Replicated command Method[Declared Class: {}, Method: {}, Parameter types: {}], Reason: {}", declaredClass.getName(), methodName, Arrays.toString(parameterTypes), e.getMessage());
            if (RedisConfiguration.FAIL_FAST_ENABLED) {
                logger.error("Fail-Fast mode is activated and an exception is about to be thrown. You can disable Fail-Fast mode with the JVM startup parameter -D{}=false", RedisConfiguration.FAIL_FAST_ENABLED_PROPERTY_NAME);
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static void initReplicatedCommandMethodMethod(Method method, Class<?>[] parameterTypes) {
        if (replicatedCommandMethodsMetadata.containsKey(method)) {
            throw new IllegalArgumentException("Repeat the initialization Replicated command Method: " + method);
        }
        List<ParameterMetadata> parameterMetadataList = buildParameterMetadata(parameterTypes);
        replicatedCommandMethodsMetadata.put(method, parameterMetadataList);
        logger.debug("Initializing Replicated command Method metadata information successfully, Method: {}, parameter Replicated Command Method metadata information: {}", method.getName(), parameterMetadataList);
    }

    private static void initReplicatedCommandMethodCache(Class<?> declaredClass, Method method, Class<?>[] parameterTypes) {
        String id = buildCommandMethodId(declaredClass.getName(), method.getName(), parameterTypes);
        if (replicatedCommandMethodsCache.putIfAbsent(id, method) == null) {
            logger.debug("Cache Replicated command Method[id: {}, Method: {}]", id, method);
        } else {
            logger.warn("Replicated command Method[id: {}, Method: {}] is cached", id, method);
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
