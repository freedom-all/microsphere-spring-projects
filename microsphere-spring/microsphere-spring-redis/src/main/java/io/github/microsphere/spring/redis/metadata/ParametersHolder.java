package io.github.microsphere.spring.redis.metadata;

import io.github.microsphere.spring.redis.serializer.Serializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * {@link Parameter} Holder
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class ParametersHolder {

    private static final Logger logger = LoggerFactory.getLogger(ParametersHolder.class);

    private static final ThreadLocal<Map<Object, Parameter>> value = ThreadLocal.withInitial(() -> new IdentityHashMap<>(4));

    public static void init(List<ParameterMetadata> parameterMetadataList, Object[] parameterValues) {
        int length = parameterValues == null ? 0 : parameterValues.length;
        if (length < 1) {
            return;
        }
        try {
            Map<Object, Parameter> metadataMap = value.get();
            for (int i = 0; i < length; i++) {
                Object parameterValue = parameterValues[i];
                ParameterMetadata parameterMetadata = parameterMetadataList.get(i);
                getOrCreateParameter(metadataMap, parameterValue, parameterMetadata);
            }
        } catch (Throwable e) {
            logger.error("Redis failed to initialize Redis command method parameter {}!", parameterMetadataList, e);
        }
    }




    public static Parameter[] bulkGet(Object[] parameterValues) {
        Map<Object, Parameter> metadataMap = value.get();
        int length = parameterValues.length;
        Parameter[] parameters = new Parameter[length];
        for (int i = 0; i < length; i++) {
            Object parameterValue = parameterValues[i];
            Parameter parameter = metadataMap.get(parameterValue);
            // serialize parameter
            Serializers.serializeRawParameter(parameter);
            parameters[i] = parameter;
        }
        return parameters;
    }

    public static Parameter get(Object parameterValue) {
        Map<Object, Parameter> metadataMap = value.get();
        Parameter parameter = metadataMap.get(parameterValue);
        return parameter;
    }

    private static Parameter getOrCreateParameter(Map<Object, Parameter> metadataMap, Object parameterValue, ParameterMetadata parameterMetadata) {
        Parameter parameter = metadataMap.computeIfAbsent(parameterValue, p -> new Parameter(parameterValue, parameterMetadata));
        return parameter;
    }

    public static void clear() {
        value.remove();
    }
}
