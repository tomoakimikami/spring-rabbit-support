/**
 * 
 */
package spring.support.amqp.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import spring.support.amqp.rabbit.repository.MutexRepository;
import spring.support.amqp.rabbit.repository.jdbc.OracleMutexRepository;

/**
 * @author yoshidan
 *
 */
@Configuration
public class RabbitSupportConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "exactlyOnceDeliveryProducer")
  @ConditionalOnBean(RabbitTemplate.class)
  public ExactlyOnceDeliveryProducer exactlyOnceDeliveryProducer() {
    return new ExactlyOnceDeliveryProducer();
  }

  @Bean
  @ConditionalOnMissingBean(name = "exactlyOnceDeliveryAdvice")
  public ExactlyOnceDeliveryAdvice exactlyOnceDeliveryAdvice() {
    return new ExactlyOnceDeliveryAdvice();
  }

  @Bean
  @ConditionalOnMissingBean(name = "mutexRepository")
  public MutexRepository mutexRepository() {
    return new OracleMutexRepository();
  }
}
