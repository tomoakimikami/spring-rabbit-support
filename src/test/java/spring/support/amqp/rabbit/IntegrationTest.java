/**
 * 
 */
package spring.support.amqp.rabbit;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import spring.support.amqp.rabbit.repository.MutexRepository;


@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest("server.port=55558")
@RunWith(SpringJUnit4ClassRunner.class)
public class IntegrationTest {

  @Autowired
  private JdbcTemplate jdbc;

  @Autowired
  private MutexRepository repository;

  @Test
  public void txProduce() throws Exception {
    RestTemplate template = new RestTemplate();

    String mutex = template.postForObject("http://localhost:55558/amqp/tx", null, String.class);
    Thread.sleep(1000);
    boolean executed =
        template.getForObject("http://localhost:55558/amqp/mutex?mutex=" + mutex, Boolean.class);
    Assert.assertTrue(executed);

    List<String> value =
        jdbc.queryForList("select 1 from rabbitmq_mutex where mutex=?", String.class, mutex);
    Assert.assertTrue(value.isEmpty());

  }

  @Test
  public void DLQ() throws Exception {
    RestTemplate template = new RestTemplate();

    String mutex = template.postForObject("http://localhost:55558/amqp/txFail", null, String.class);
    Thread.sleep(1000);
    boolean executed =
        template.getForObject("http://localhost:55558/amqp/mutex?mutex=" + mutex, Boolean.class);
    Assert.assertFalse(executed);

    List<String> value =
        jdbc.queryForList("select 1 from rabbitmq_mutex where mutex=?", String.class, mutex);
    Assert.assertEquals(1, value.size());

  }

  @Test
  public void nonTxProduce() throws Exception {
    RestTemplate template = new RestTemplate();

    String mutex = template.postForObject("http://localhost:55558/amqp/nontx", null, String.class);
    Thread.sleep(1000);
    boolean executed =
        template.getForObject("http://localhost:55558/amqp/mutex?mutex=" + mutex, Boolean.class);
    Assert.assertTrue(executed);

    List<String> value =
        jdbc.queryForList("select 1 from rabbitmq_mutex where mutex=?", String.class, mutex);
    Assert.assertTrue(value.isEmpty());

  }

  /**
   * 2重配信防止
   */
  @Transactional
  @Rollback
  @Test
  public void doubleConsumeProtect() throws Exception {
    RestTemplate template = new RestTemplate();
    String mutex =
        template.postForObject("http://localhost:55558/amqp/mutexOnly", null, String.class);
    // ロックを取得しておく
    repository.lock(mutex);

    // 送信
    template.postForObject("http://localhost:55558/amqp/txMutexUse?mutex=" + mutex, null,
        void.class);

    // ロックエラー
    Thread.sleep(2000);
    boolean executed =
        template.getForObject("http://localhost:55558/amqp/mutex?mutex=" + mutex, Boolean.class);
    Assert.assertFalse(executed);
  }
}
