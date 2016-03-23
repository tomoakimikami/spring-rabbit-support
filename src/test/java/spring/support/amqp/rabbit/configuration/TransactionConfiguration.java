package spring.support.amqp.rabbit.configuration;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @generated
 */
@Configuration
public class TransactionConfiguration {

  @Primary
  @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
  @Bean
  @Autowired
  public DataSource dataSource(DataSourceProperties properties) {
    DriverManagerDataSource dataSource = new DriverManagerDataSource(properties.getUrl(),
        properties.getUsername(), properties.getPassword()) {
      @Override
      public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        conn.setAutoCommit(false);

        return conn;
      }
    };
    dataSource.setDriverClassName(properties.getDriverClassName());
    return dataSource;
  }


  @Bean
  @Autowired
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  @Autowired
  public DataSourceTransactionManager transactionManager(DataSource dataSource) {
    DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
    transactionManager.setDataSource(dataSource);
    return transactionManager;
  }

}
