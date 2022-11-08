package io.github.microsphere.spring.redis.metadata;

import org.springframework.data.redis.connection.RedisCommands;

import java.util.Objects;

/**
 * {@link RedisCommands Redis command} Method parameter meta information
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public final class ParameterMetadata {

    private final int parameterIndex;

    private final String parameterType;

    public ParameterMetadata(int parameterIndex, String parameterType) {
        this.parameterIndex = parameterIndex;
        this.parameterType = parameterType;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public String getParameterType() {
        return parameterType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterMetadata)) return false;
        ParameterMetadata that = (ParameterMetadata) o;
        return parameterIndex == that.parameterIndex && Objects.equals(parameterType, that.parameterType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterIndex, parameterType);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ParameterMetadata{");
        sb.append("parameterIndex=").append(parameterIndex);
        sb.append(", parameterType='").append(parameterType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
