package io.github.microsphere.spring.redis.serializer;

import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * {@link RedisListCommands.Position} {@link EnumSerializer} 测试
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class PositionSerializerTest extends AbstractSerializerTest<Enum> {

    @Override
    protected RedisSerializer<Enum> getSerializer() {
        return new EnumSerializer(RedisListCommands.Position.class);
    }

    @Override
    protected RedisListCommands.Position getValue() {
        return RedisListCommands.Position.AFTER;
    }
}
