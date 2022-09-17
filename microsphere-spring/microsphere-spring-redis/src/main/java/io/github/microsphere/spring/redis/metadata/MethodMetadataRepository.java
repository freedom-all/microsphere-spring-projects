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
 * Redis Sync 方法元数据仓库
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class MethodMetadataRepository {

    private static final Logger logger = LoggerFactory.getLogger(MethodMetadataRepository.class);

    /**
     * 接口类名与 {@link Class} 对象缓存（减少类加载性能消耗）
     */
    static final Map<String, Class<?>> redisCommandInterfacesCache = new HashMap<>();

    /**
     * 命令接口类名与 {@link RedisConnection} 命令对象函数（如：{@link RedisConnection#keyCommands()}）绑定
     */
    static final Map<String, Function<RedisConnection, Object>> redisCommandBindings = new HashMap<>();

    static final Map<Method, List<ParameterMetadata>> replicatedCommandMethodsMetadata = new HashMap<>();

    /**
     * 方法简单签名与 {@link Method} 对象缓存（减少反射性能消耗）
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
            logger.warn("Redis Sync 事件发布者与消费者 API 存在差异，请及时升级消费端 redis-sync artifacts！");
            logger.debug("Redis Sync 复制命令方法将通过 Java 反射来查找（接口: {}，方法名:{}，参数列表：{}）...", interfaceNme, methodName, Arrays.toString(parameterTypes));
            Class<?> interfaceClass = getRedisCommandsInterfaceClass(interfaceNme);
            if (interfaceClass == null) {
                logger.warn("当前 Redis Sync 消费端无法找到 Redis 命令接口：{}，请确认 spring-data artifacts API 是否兼容，复制命令方法查找结束！", interfaceNme);
                return null;
            }
            method = ReflectionUtils.findMethod(interfaceClass, methodName, event.getParameterClasses());
            if (method == null) {
                logger.warn("当前 Redis Sync 消费端Redis 命令接口（类名：{}）中的方法（{}），复制命令方法查找结束！",
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
     * 获取指定类名的 {@link RedisCommands} 命令接口 {@link Class}
     *
     * @param interfaceName {@link RedisCommands} 命令接口类名
     * @return 如果没有找到的话，返回 <code>null</code>
     */
    public static Class<?> getRedisCommandsInterfaceClass(String interfaceName) {
        return redisCommandInterfacesCache.get(interfaceName);
    }

    public static Function<RedisConnection, Object> getRedisCommandBindingFunction(String interfaceName) {
        return redisCommandBindings.getOrDefault(interfaceName, redisConnection -> redisConnection);
    }

    /**
     * 初始化 {@link RedisCommands} 命令接口的名称与 {@link Class} 对象缓存
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
                logger.debug("Redis 命令接口 {} 绑定 RedisConnection 命令对象方法 {}", interfaceName, method);
            }, method -> interfaceClass.equals(method.getReturnType()) && method.getParameterCount() < 1);
        }
    }

    /**
     * 初始化 {@link RedisCommands} 命令复制命令方法，包括：
     * <ul>
     *     <li>{@link #initRedisKeyCommandsReplicatedCommandMethods() RedisKeyCommands 复制命令方法}</li>
     *     <li>{@link #initRedisStringCommandsReplicatedCommandMethods() RedisStringCommands 复制命令方法}</li>
     *     <li>{@link #initRedisListCommandsReplicatedCommandMethods() RedisListCommands 复制命令方法}</li>
     *     <li>{@link #initRedisSetCommandsReplicatedCommandMethods() RedisSetCommands 复制命令方法}</li>
     *     <li>{@link #initRedisZSetCommandsReplicatedCommandMethods() RedisZSetCommands 复制命令方法}</li>
     *     <li>{@link #initRedisHashCommandsReplicatedCommandMethods() RedisHashCommands 复制命令方法}</li>
     *     <li>{@link #initRedisScriptingCommandsReplicatedCommandMethods() RedisScriptingCommands 复制命令方法}</li>
     *     <li>{@link #initRedisGeoCommandsReplicatedCommandMethods() RedisGeoCommands 复制命令方法}</li>
     *     <li>{@link #initRedisHyperLogLogCommandsReplicatedCommandMethods() RedisHyperLogLogCommands 复制命令方法}</li>
     * </ol>
     * <p>
     * 不支持：
     * <ul>
     *     <li>{@link RedisTxCommands}</li>
     *     <li>{@link RedisPubSubCommands}</li>
     *     <li>{@link RedisConnectionCommands}</li>
     *     <li>{@link RedisServerCommands}</li>
     * </ul>
     */
    private static void initReplicatedCommandMethods() {

        // TODO: 支持配置化

        // 初始化 {@link RedisKeyCommands} 复制命令方法
        initRedisKeyCommandsReplicatedCommandMethods();

        // 初始化 {@link RedisStringCommands} 复制命令方法
        initRedisStringCommandsReplicatedCommandMethods();

        // 初始化 {@link RedisListCommands} 复制命令方法
        initRedisListCommandsReplicatedCommandMethods();

        // 初始化 {@link RedisSetCommands} 复制命令方法
        initRedisSetCommandsReplicatedCommandMethods();

        // 初始化 {@link RedisZSetCommands} 复制命令方法
        initRedisZSetCommandsReplicatedCommandMethods();

        // 初始化 {@link RedisHashCommands} 复制命令方法
        initRedisHashCommandsReplicatedCommandMethods();

        // 初始化 {@link RedisScriptingCommands} 复制命令方法
        initRedisScriptingCommandsReplicatedCommandMethods();

        // 初始化 {@link RedisGeoCommands} 复制命令方法
        initRedisGeoCommandsReplicatedCommandMethods();

        // 初始化 {@link RedisHyperLogLogCommands}
        initRedisHyperLogLogCommandsReplicatedCommandMethods();

    }

    /**
     * 初始化 {@link RedisKeyCommands} 复制命令方法
     */
    private static void initRedisKeyCommandsReplicatedCommandMethods() {

        /**
         * del(byte[]...) 方法
         * @see <a href="https://redis.io/commands/del">Redis Documentation: DEL</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "del", byte[][].class);

        /**
         * unlink(byte[]...) 方法
         * @see <a href="https://redis.io/commands/unlink">Redis Documentation: UNLINK</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "unlink", byte[][].class);

        /**
         * touch(byte[]...) 方法
         * @see <a href="https://redis.io/commands/touch">Redis Documentation: TOUCH</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "touch", byte[][].class);

        /**
         * rename(byte[], byte[]) 方法
         * @see <a href="https://redis.io/commands/rename">Redis Documentation: RENAME</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "rename", byte[].class, byte[].class);

        /**
         * renameNX(byte[], byte[]) 方法
         * @see <a href="https://redis.io/commands/renamenx">Redis Documentation: RENAMENX</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "renameNX", byte[].class, byte[].class);

        /**
         * expire(byte[], long) 方法
         * @see <a href="https://redis.io/commands/expire">Redis Documentation: EXPIRE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "expire", byte[].class, long.class);

        /**
         * pExpire(byte[], long) 方法
         * @see <a href="https://redis.io/commands/pexpire">Redis Documentation: PEXPIRE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "pExpire", byte[].class, long.class);

        /**
         * expireAt(byte[], long) 方法
         * @see <a href="https://redis.io/commands/expireat">Redis Documentation: EXPIREAT</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "expireAt", byte[].class, long.class);

        /**
         * pExpireAt(byte[], long) 方法
         * @see <a href="https://redis.io/commands/pexpireat">Redis Documentation: PEXPIREAT</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "pExpireAt", byte[].class, long.class);

        /**
         * persist(byte[]) 方法
         * @see <a href="https://redis.io/commands/persist">Redis Documentation: PERSIST</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "persist", byte[].class);

        /**
         * move(byte[],int) 方法
         * @see <a href="https://redis.io/commands/move">Redis Documentation: MOVE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "move", byte[].class, int.class);

        /**
         * ttl(byte[], TimeUnit) 方法
         * @see <a href="https://redis.io/commands/ttl">Redis Documentation: TTL</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "ttl", byte[].class, TimeUnit.class);

        /**
         * pTtl(byte[], TimeUnit) 方法
         * @see <a href="https://redis.io/commands/pttl">Redis Documentation: PTTL</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "pTtl", byte[].class, TimeUnit.class);

        /**
         * sort(byte[], SortParameters) 方法
         * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "sort", byte[].class, SortParameters.class);

        /**
         * sort(byte[], SortParameters,byte[]) 方法
         * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "sort", byte[].class, SortParameters.class, byte[].class);

        /**
         * restore(byte[], long, byte[]) 方法
         * @see <a href="https://redis.io/commands/restore">Redis Documentation: RESTORE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "restore", byte[].class, long.class, byte[].class);

        /**
         * restore(byte[], long, byte[], boolean) 方法
         * @see <a href="https://redis.io/commands/restore">Redis Documentation: RESTORE</a>
         */
        initReplicatedCommandMethod(RedisKeyCommands.class, "restore", byte[].class, long.class, byte[].class, boolean.class);
    }

    /**
     * 初始化 {@link RedisStringCommands} 复制命令方法
     */
    private static void initRedisStringCommandsReplicatedCommandMethods() {

        /**
         * set(byte[],byte[]) 方法
         * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "set", byte[].class, byte[].class);

        /**
         * set(byte[], byte[], Expiration, SetOption) 方法
         * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "set", byte[].class, byte[].class, Expiration.class, RedisStringCommands.SetOption.class);

        /**
         * setNX(byte[],byte[]) 方法
         * @see <a href="https://redis.io/commands/setnx">Redis Documentation: SETNX</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "setNX", byte[].class, byte[].class);

        /**
         * setEx(byte[], long, byte[]) 方法
         * @see <a href="https://redis.io/commands/setex">Redis Documentation: SETEX</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "setEx", byte[].class, long.class, byte[].class);

        /**
         * pSetEx(byte[], long, byte[]) 方法
         * @see <a href="https://redis.io/commands/psetex">Redis Documentation: PSETEX</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "pSetEx", byte[].class, long.class, byte[].class);

        /**
         * mSet(Map<byte[], byte[]>) 方法
         * @see <a href="https://redis.io/commands/mset">Redis Documentation: MSET</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "mSet", Map.class);

        /**
         * mSetNX(Map<byte[], byte[]>) 方法
         * @see <a href="https://redis.io/commands/msetnx">Redis Documentation: MSETNX</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "mSetNX", Map.class);

        /**
         * incr(byte[]) 方法
         * @see <a href="https://redis.io/commands/incr">Redis Documentation: INCR</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "incr", byte[].class);

        /**
         * incrBy(byte[], long) 方法
         * @see <a href="https://redis.io/commands/incrby">Redis Documentation: INCRBY</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "incrBy", byte[].class, long.class);

        /**
         * incrBy(byte[], double) 方法
         * @see <a href="https://redis.io/commands/incrbyfloat">Redis Documentation: INCRBYFLOAT</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "incrBy", byte[].class, double.class);

        /**
         * decr(byte[]) 方法
         * @see <a href="https://redis.io/commands/decr">Redis Documentation: DECR</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "decr", byte[].class);

        /**
         * decrBy(byte[], long) 方法
         * @see <a href="https://redis.io/commands/decrby">Redis Documentation: DECRBY</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "decrBy", byte[].class, long.class);

        /**
         * append(byte[], byte[]) 方法
         * @see <a href="https://redis.io/commands/append">Redis Documentation: APPEND</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "append", byte[].class, byte[].class);

        /**
         * setRange(byte[], byte[], long) 方法
         * @see <a href="https://redis.io/commands/setrange">Redis Documentation: SETRANGE</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "setRange", byte[].class, byte[].class, long.class);

        /**
         * setBit(byte[], long, boolean) 方法
         * @see <a href="https://redis.io/commands/setbit">Redis Documentation: SETBIT</a>
         */
        initReplicatedCommandMethod(RedisStringCommands.class, "setBit", byte[].class, long.class, boolean.class);
    }


    /**
     * 初始化 {@link RedisListCommands} 复制命令方法
     */
    private static void initRedisListCommandsReplicatedCommandMethods() {

        /**
         * rPush(byte[] ,byte[]...) 方法
         * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "rPush", byte[].class, byte[][].class);

        /**
         * lPush(byte[] ,byte[]...) 方法
         * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lPush", byte[].class, byte[][].class);

        /**
         * rPushX(byte[] ,byte[]) 方法
         * @see <a href="https://redis.io/commands/rpushx">Redis Documentation: RPUSHX</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "rPushX", byte[].class, byte[].class);

        /**
         * lPushX(byte[] ,byte[]) 方法
         * @see <a href="https://redis.io/commands/lpushx">Redis Documentation: LPUSHX</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lPushX", byte[].class, byte[].class);

        /**
         * lTrim(byte[], long, long) 方法
         * @see <a href="https://redis.io/commands/ltrim">Redis Documentation: LTRIM</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lTrim", byte[].class, long.class, long.class);

        /**
         * lInsert(byte[], Position, byte[], byte[]) 方法
         * @see <a href="https://redis.io/commands/linsert">Redis Documentation: LINSERT</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lInsert", byte[].class, RedisListCommands.Position.class, byte[].class, byte[].class);

        /**
         * lSet(byte[], long, byte[]) 方法
         * @see <a href="https://redis.io/commands/lset">Redis Documentation: LSET</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lSet", byte[].class, long.class, byte[].class);

        /**
         * lRem(byte[], long, byte[]) 方法
         * @see <a href="https://redis.io/commands/lrem">Redis Documentation: LREM</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lRem", byte[].class, long.class, byte[].class);

        /**
         * lPop(byte[]) 方法
         * @see <a href="https://redis.io/commands/lpop">Redis Documentation: LPOP</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "lPop", byte[].class);

        /**
         * rPop(byte[]) 方法
         * @see <a href="https://redis.io/commands/rpop">Redis Documentation: RPOP</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "rPop", byte[].class);

        /**
         * bLPop(int, byte[]...) 方法
         * @see <a href="https://redis.io/commands/blpop">Redis Documentation: BLPOP</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "bLPop", int.class, byte[][].class);

        /**
         * bRPop(int, byte[]...) 方法
         * @see <a href="https://redis.io/commands/brpop">Redis Documentation: BRPOP</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "bRPop", int.class, byte[][].class);

        /**
         * rPopLPush(byte[], byte[]) 方法
         * @see <a href="https://redis.io/commands/rpoplpush">Redis Documentation: RPOPLPUSH</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "rPopLPush", byte[].class, byte[].class);

        /**
         * bRPopLPush(int, byte[], byte[]) 方法
         * @see <a href="https://redis.io/commands/brpoplpush">Redis Documentation: BRPOPLPUSH</a>
         */
        initReplicatedCommandMethod(RedisListCommands.class, "bRPopLPush", int.class, byte[].class, byte[].class);
    }

    /**
     * 初始化 {@link RedisSetCommands} 复制命令方法
     */
    private static void initRedisSetCommandsReplicatedCommandMethods() {

        /**
         * sAdd(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/sadd">Redis Documentation: SADD</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sAdd", byte[].class, byte[][].class);

        /**
         * sRem(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/srem">Redis Documentation: SREM</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sRem", byte[].class, byte[][].class);

        /**
         * sPop(byte[]) 方法
         * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sPop", byte[].class);

        /**
         * sPop(byte[], long) 方法
         * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sPop", byte[].class, long.class);

        /**
         * sMove(byte[], byte[], byte[]) 方法
         * @see <a href="https://redis.io/commands/smove">Redis Documentation: SMOVE</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sMove", byte[].class, byte[].class, byte[].class);

        /**
         * sInterStore(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/sinterstore">Redis Documentation: SINTERSTORE</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sInterStore", byte[].class, byte[][].class);

        /**
         * sUnionStore(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/sunionstore">Redis Documentation: SUNIONSTORE</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sUnionStore", byte[].class, byte[][].class);

        /**
         * sDiffStore(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/sdiffstore">Redis Documentation: SDIFFSTORE</a>
         */
        initReplicatedCommandMethod(RedisSetCommands.class, "sDiffStore", byte[].class, byte[][].class);
    }

    /**
     * 初始化 {@link RedisZSetCommands} 复制命令方法
     */
    private static void initRedisZSetCommandsReplicatedCommandMethods() {

        /**
         * zAdd(byte[], double, byte[]) 方法
         * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zAdd", byte[].class, double.class, byte[].class);

        /**
         * zAdd(byte[], Set<Tuple>) 方法
         * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
         * TODO 处理 {@link Tuple}
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zAdd", byte[].class, Set.class);

        /**
         * zRem(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zRem", byte[].class, byte[][].class);

        /**
         * zIncrBy(byte[], double, byte[]) 方法
         * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zIncrBy", byte[].class, double.class, byte[].class);

        /**
         * zRemRange(byte[], long, long) 方法
         * @see <a href="https://redis.io/commands/zremrangebyrank">Redis Documentation: ZREMRANGEBYRANK</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zRemRange", byte[].class, long.class, long.class);

        /**
         * zRemRangeByScore(byte[], Range) 方法
         * @see <a href="https://redis.io/commands/zremrangebyscore">Redis Documentation: ZREMRANGEBYSCORE</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zRemRangeByScore", byte[].class, RedisZSetCommands.Range.class);

        /**
         * zRemRangeByScore(byte[], double, double) 方法
         * @see <a href="https://redis.io/commands/zremrangebyscore">Redis Documentation: ZREMRANGEBYSCORE</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zRemRangeByScore", byte[].class, double.class, double.class);

        /**
         * zUnionStore(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/zunionstore">Redis Documentation: ZUNIONSTORE</a>
         */
        initReplicatedCommandMethod(RedisZSetCommands.class, "zUnionStore", byte[].class, byte[][].class);

        /**
         * zUnionStore(byte[], Aggregate, int[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/zunionstore">Redis Documentation: ZUNIONSTORE</a>
         * TODO {@link Aggregate}
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
     * 初始化 {@link RedisHashCommands} 复制命令方法
     */
    private static void initRedisHashCommandsReplicatedCommandMethods() {

        /**
         * hSet(byte[], byte[], byte[]) 方法
         * @see <a href="https://redis.io/commands/hset">Redis Documentation: HSET</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hSet", byte[].class, byte[].class, byte[].class);

        /**
         * hSetNX(byte[], byte[], byte[]) 方法
         * @see <a href="https://redis.io/commands/hsetnx">Redis Documentation: HSETNX</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hSetNX", byte[].class, byte[].class, byte[].class);

        /**
         * hMSet(byte[], Map<byte[], byte[]>) 方法
         * @see <a href="https://redis.io/commands/hmset">Redis Documentation: HMSET</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hMSet", byte[].class, Map.class);

        /**
         * hIncrBy(byte[], byte[], long) 方法
         * @see <a href="https://redis.io/commands/hmset">Redis Documentation: HMSET</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hIncrBy", byte[].class, byte[].class, long.class);

        /**
         * hIncrBy(byte[], byte[], double) 方法
         * @see <a href="https://redis.io/commands/hincrbyfloat">Redis Documentation: HINCRBYFLOAT</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hIncrBy", byte[].class, byte[].class, double.class);

        /**
         * hDel(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/hdel">Redis Documentation: HDEL</a>
         */
        initReplicatedCommandMethod(RedisHashCommands.class, "hDel", byte[].class, byte[][].class);

    }

    /**
     * 初始化 {@link RedisScriptingCommands} 复制命令方法
     */
    private static void initRedisScriptingCommandsReplicatedCommandMethods() {

        /**
         * scriptLoad(byte[]) 方法
         * @see <a href="https://redis.io/commands/script-load">Redis Documentation: SCRIPT LOAD</a>
         */
        initReplicatedCommandMethod(RedisScriptingCommands.class, "scriptLoad", byte[].class);

        /**
         * eval(byte[], ReturnType, int, byte[]...) 方法
         * @see <a href="https://redis.io/commands/eval">Redis Documentation: EVAL</a>
         */
        initReplicatedCommandMethod(RedisScriptingCommands.class, "eval", byte[].class, ReturnType.class, int.class, byte[][].class);

        /**
         * evalSha(String, ReturnType, int numKeys, byte[]...) 方法
         * @see <a href="https://redis.io/commands/evalsha">Redis Documentation: EVALSHA</a>
         */
        initReplicatedCommandMethod(RedisScriptingCommands.class, "evalSha", String.class, ReturnType.class, int.class, byte[][].class);

        /**
         * evalSha(byte[], ReturnType, int, byte[]...) 方法
         * @see <a href="https://redis.io/commands/evalsha">Redis Documentation: EVALSHA</a>
         */
        initReplicatedCommandMethod(RedisScriptingCommands.class, "evalSha", byte[].class, ReturnType.class, int.class, byte[][].class);

    }

    /**
     * 初始化 {@link RedisGeoCommands} 复制命令方法
     */
    private static void initRedisGeoCommandsReplicatedCommandMethods() {

        /**
         * geoAdd(byte[], Point, byte[]) 方法
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, Point.class, byte[].class);

        /**
         * geoAdd(byte[], GeoLocation<byte[]>) 方法
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, RedisGeoCommands.GeoLocation.class);

        /**
         * geoAdd(byte[], Map<byte[], Point>) 方法
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, Map.class);

        /**
         * geoAdd(byte[], Iterable<RedisGeoCommands.GeoLocation<byte[]>>) 方法
         * @see <a href="https://redis.io/commands/geoadd">Redis Documentation: GEOADD</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoAdd", byte[].class, Iterable.class);

        /**
         * geoRemove(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
         */
        initReplicatedCommandMethod(RedisGeoCommands.class, "geoRemove", byte[].class, byte[][].class);
    }

    /**
     * 初始化 {@link RedisHyperLogLogCommands}
     */
    private static void initRedisHyperLogLogCommandsReplicatedCommandMethods() {

        /**
         * pfAdd(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/pfadd">Redis Documentation: PFADD</a>
         */
        initReplicatedCommandMethod(RedisHyperLogLogCommands.class, "pfAdd", byte[].class, byte[][].class);

        /**
         * pfMerge(byte[], byte[]...) 方法
         * @see <a href="https://redis.io/commands/pfmerge">Redis Documentation: PFMERGE</a>
         */
        initReplicatedCommandMethod(RedisHyperLogLogCommands.class, "pfMerge", byte[].class, byte[][].class);
    }

    private static void initReplicatedCommandMethod(Class<?> declaredClass, String methodName, Class<?>... parameterTypes) {
        try {
            logger.debug("初始化复制命令方法[声明类: {} , 方法名: {}, 参数类型: {}]...", declaredClass.getName(), methodName, Arrays.toString(parameterTypes));
            Method method = declaredClass.getMethod(methodName, parameterTypes);
            // 减少 Method 运行时检查
            method.setAccessible(true);
            initReplicatedCommandMethodMethod(method, parameterTypes);
            initReplicatedCommandMethodCache(declaredClass, method, parameterTypes);
        } catch (Throwable e) {
            logger.error("无法初始化复制命令方法[声明类: {} , 方法名: {}, 参数类型: {}]，原因：{}", declaredClass.getName(), methodName, Arrays.toString(parameterTypes), e.getMessage());
            if (RedisConfiguration.FAIL_FAST_ENABLED) {
                logger.error("Fail-Fast 模式已激活，异常即将抛出，可通过 JVM 启动参数 -D{}=false 关闭 Fail-Fast 模式", RedisConfiguration.FAIL_FAST_ENABLED_PROPERTY_NAME);
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static void initReplicatedCommandMethodMethod(Method method, Class<?>[] parameterTypes) {
        if (replicatedCommandMethodsMetadata.containsKey(method)) {
            throw new IllegalArgumentException("重复初始化复制命令方法: " + method);
        }
        List<ParameterMetadata> parameterMetadataList = buildParameterMetadata(parameterTypes);
        replicatedCommandMethodsMetadata.put(method, parameterMetadataList);
        logger.debug("初始化复制命令方法元信息成功，方法: {}, 参数元信息: {}", method.getName(), parameterMetadataList);
    }

    private static void initReplicatedCommandMethodCache(Class<?> declaredClass, Method method, Class<?>[] parameterTypes) {
        String id = buildCommandMethodId(declaredClass.getName(), method.getName(), parameterTypes);
        if (replicatedCommandMethodsCache.putIfAbsent(id, method) == null) {
            logger.debug("缓存复制命令方法[id : {} , 方法: {}]", id, method);
        } else {
            logger.warn("复制命令方法[id : {} , 方法: {}]已缓存", id, method);
        }
    }

    private static List<ParameterMetadata> buildParameterMetadata(Class<?>[] parameterTypes) {
        int parameterCount = parameterTypes.length;
        List<ParameterMetadata> parameterMetadataList = new ArrayList<>(parameterCount);
        for (int i = 0; i < parameterCount; i++) {
            String parameterType = parameterTypes[i].getName();
            ParameterMetadata parameterMetadata = new ParameterMetadata(i, parameterType);
            parameterMetadataList.add(parameterMetadata);

            // 为方法参数类型提前加载 RedisSerializer 实现
            Serializers.getSerializer(parameterType);
        }
        return unmodifiableList(parameterMetadataList);
    }
}
