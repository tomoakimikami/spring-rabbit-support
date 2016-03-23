/**
 * 
 */
package spring.support.amqp.rabbit.configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * @author yoshidan
 */
@Configuration
public class RabbitConfiguration {

  @Bean
  public Exchange defaultExchange() {
    return new DirectExchange("default.exchange");
  }

  @Bean
  public Exchange errorExchange() {
    return new DirectExchange("error.exchange");
  }

  @Bean
  public Queue defaultQueue() {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("x-dead-letter-exchange", "error.exchange");
    arguments.put("x-dead-letter-routing-key", "error.routing-key");
    return new Queue("default.queue", true, false, false, arguments);
  }

  @Bean
  public Queue errorQueue() {
    return new Queue("error.queue");
  }

  @Bean
  @Autowired
  public Binding defaultQueueBinding(@Qualifier("defaultQueue") Queue queue,
      @Qualifier("defaultExchange") Exchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with("routing-key").noargs();
  }

  @Bean
  @Autowired
  public Binding retryQueueBinding(@Qualifier("errorQueue") Queue queue,
      @Qualifier("errorExchange") Exchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with("error.routing-key").noargs();
  }

  @Bean
  @Autowired
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    // json
    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    return rabbitTemplate;
  }

  @Bean
  @Autowired
  public SimpleRabbitListenerContainerFactory requeueRejectContainerFactory(ConnectionFactory cf,
      DataSourceTransactionManager dataSource) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(cf);
    // エラー時にDLQ
    factory.setDefaultRequeueRejected(false);
    // json
    factory.setMessageConverter(new Jackson2JsonMessageConverter());
    factory.setConcurrentConsumers(10);
    // これやるとチャネルのack帰すまで同じTx 、ログがめちゃ出るしあまり大差ないかも factory.setTransactionManager(dataSource);
    factory.setConsecutiveActiveTrigger(1);
    return factory;
  }
}
