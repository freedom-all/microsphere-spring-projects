package io.github.microsphere.spring.test.jdbc.embedded;

import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
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
@Import(EmbeddedDataBaseBeanDefinitionRegistrar.class)
@Repeatable(EnableEmbeddedDatabases.class)
public @interface EnableEmbeddedDatabase {

    /**
     * @return 嵌入式数据库类型，默认 SQLite
     */
    EmbeddedDatabaseType type() default EmbeddedDatabaseType.SQLITE;

    /**
     * @return {@link DataSource} Bean 名称
     */
    String dataSource();

    /**
     * @return 是否为 Primary Bean
     */
    boolean primary() default false;

    /**
     * @return JDBC Properties 配置，Key-value 字符串形式
     */
    String[] properties() default {};
}
