package io.github.microsphere.spring.test.jdbc.embedded;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 激活嵌入式数据库
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(EmbeddedDataBaseBeanDefinitionsRegistrar.class)
public @interface EnableEmbeddedDatabases {

    /**
     * @return 多个 {@link EnableEmbeddedDatabase} 配置
     */
    EnableEmbeddedDatabase[] value();
}
