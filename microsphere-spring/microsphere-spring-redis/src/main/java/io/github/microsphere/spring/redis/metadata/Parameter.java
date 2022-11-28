package io.github.microsphere.spring.redis.metadata;

import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.lang.Nullable;

/**
 * {@link RedisCommands Redis 命令} Method parameters encapsulate the object
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class Parameter {

    private final Object value;

    private final ParameterMetadata metadata;

    private @Nullable byte[] rawValue;

    public Parameter(Object value, ParameterMetadata metadata) {
        this.value = value;
        this.metadata = metadata;
    }

    public Object getValue() {
        return value;
    }

    public ParameterMetadata getMetadata() {
        return metadata;
    }

    public @Nullable byte[] getRawValue() {
        return rawValue;
    }

    public void setRawValue(@Nullable byte[] rawValue) {
        this.rawValue = rawValue;
    }

    public int getParameterIndex() {
        return metadata.getParameterIndex();
    }

    public String getParameterType() {
        return metadata.getParameterType();
    }
}
