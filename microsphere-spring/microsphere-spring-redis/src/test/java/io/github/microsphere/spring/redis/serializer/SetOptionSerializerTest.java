package io.github.microsphere.spring.redis.serializer;

import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * {@link RedisStringCommands.SetOption} {@link EnumSerializer} 测试
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class SetOptionSerializerTest extends AbstractSerializerTest<Enum> {

    @Override
    protected RedisSerializer<Enum> getSerializer() {
        return new EnumSerializer(RedisStringCommands.SetOption.class);
    }

    @Override
    protected RedisStringCommands.SetOption getValue() {
        return RedisStringCommands.SetOption.SET_IF_ABSENT;
    }
}
