package com.developer.test.config;

import com.developer.test.model.Task;
import com.developer.test.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@ConditionalOnProperty(name = "app.datastore.use-file-storage", havingValue = "false", matchIfMissing = true)
public class HibernateConfig {
    @Bean(destroyMethod = "close")
    public SessionFactory sessionFactory(
            @Value("${app.mysql.url}") String mysqlUrl,
            @Value("${app.mysql.username}") String mysqlUsername,
            @Value("${app.mysql.password:}") String mysqlPassword,
            @Value("${app.mysql.driver-class-name:com.mysql.cj.jdbc.Driver}") String driverClassName,
            @Value("${app.hibernate.dialect:org.hibernate.dialect.MySQL8Dialect}") String dialect) {
        Properties properties = new Properties();
        properties.put(AvailableSettings.DRIVER, driverClassName);
        properties.put(AvailableSettings.URL, mysqlUrl);
        properties.put(AvailableSettings.USER, mysqlUsername);
        properties.put(AvailableSettings.PASS, mysqlPassword);
        properties.put(AvailableSettings.DIALECT, dialect);
        properties.put(AvailableSettings.HBM2DDL_AUTO, "update");
        properties.put(AvailableSettings.SHOW_SQL, "false");
        properties.put(AvailableSettings.FORMAT_SQL, "false");
        properties.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");

        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
        registryBuilder.applySettings(properties);

        return new MetadataSources(registryBuilder.build())
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Task.class)
                .buildMetadata()
                .buildSessionFactory();
    }
}
