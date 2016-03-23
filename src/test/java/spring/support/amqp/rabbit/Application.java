package spring.support.amqp.rabbit;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import spring.support.amqp.rabbit.ExactlyOnceDeliveryAdvice.ExactlyOnceDelivery;
import spring.support.amqp.rabbit.dto.Sample;
import spring.support.amqp.rabbit.repository.MutexRepository;

/**
 * @generated
 */
@RestController
@RequestMapping("/amqp")
@Slf4j
@SpringBootApplication
public class Application {

  @Autowired
  private ExactlyOnceDeliveryProducer producer;

  @Autowired
  private MutexRepository repository;

  @Autowired
  private RabbitTemplate template;

  private Set<String> mutexExecuted = new HashSet<>();

  @ExactlyOnceDelivery
  @Transactional
  @RabbitListener(queues = "default.queue", containerFactory = "requeueRejectContainerFactory")
  public void consumeSucces(@Headers Map<String, String> headers, Sample data) {
    log.info("message consumed " + data);
    if (data.getAge() == 100) {
      throw new IllegalArgumentException();
    }
    mutexExecuted.add(headers.get("x-message-mutex"));
  }

  @RequestMapping(value = "/mutex", method = RequestMethod.GET)
  public boolean mutex(@RequestParam String mutex) {
    return mutexExecuted.contains(mutex);
  }

  @Transactional
  @RequestMapping(value = "/tx", method = RequestMethod.POST)
  public String produce() {
    return producer.send("default.exchange", "routing-key", new Sample());
  }

  @Transactional
  @RequestMapping(value = "/txFail", method = RequestMethod.POST)
  public String produceFail() {
    Sample data = new Sample();
    data.setAge(100L);
    return producer.send("default.exchange", "routing-key", data);
  }

  @RequestMapping(value = "/nontx", method = RequestMethod.POST)
  public String produceImmediate() {
    return producer.send("default.exchange", "routing-key", new Sample());
  }


  @RequestMapping(value = "/mutexOnly", method = RequestMethod.POST)
  public String mutexOnly() {
    return repository.create();
  }

  @RequestMapping(value = "/txMutexUse", method = RequestMethod.POST)
  public void mutexOnly(@RequestParam String mutex) {
    MessageProperties properties = new MessageProperties();
    properties.getHeaders().put(ExactlyOnceDeliveryProducer.MUTEX, mutex);
    Message message = template.getMessageConverter().toMessage(new Sample(), properties);
    template.send("default.exchange", "routing-key", message);
  }

}
