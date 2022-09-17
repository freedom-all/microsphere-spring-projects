package io.github.microsphere.spring.test.kafka.embedded;

import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;

/**
 *
 * @see <a href="https://docs.spring.io/spring-kafka/docs/current/reference/html/#using-the-same-brokers-for-multiple-test-classes">EmbeddedKafkaHolder</a>
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public final class EmbeddedKafkaHolder {

    private static EmbeddedKafkaRule embeddedKafka = new EmbeddedKafkaRule(1, false);

    private static boolean started;

    public static EmbeddedKafkaRule getEmbeddedKafka() {
        if (!started) {
            try {
                embeddedKafka.before();
            }
            catch (Exception e) {
                throw new KafkaException(e);
            }
            started = true;
        }
        return embeddedKafka;
    }

    private EmbeddedKafkaHolder() {
        super();
    }

}