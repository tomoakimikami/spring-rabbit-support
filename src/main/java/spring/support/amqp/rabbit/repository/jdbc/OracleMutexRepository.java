package spring.support.amqp.rabbit.repository.jdbc;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import spring.support.amqp.rabbit.repository.MutexRepository;

/**
 * MutexRepositoryのOracle用実装.
 * @author yoshidan
 */
@Repository
@ConditionalOnMissingBean(MutexRepository.class)
@ConditionalOnBean(JdbcTemplate.class)
public class OracleMutexRepository implements MutexRepository {
  /**
   * JDBCテンプレート.
   */
  @Autowired
  private JdbcTemplate jdbcTemplate;

  /**
   * {@inheritDoc}.
   */
  @Override
  public void lock(String mutex) {
    String query = "select 1 from RABBITMQ_MUTEX where MUTEX = ? for update nowait";
    Optional.ofNullable(jdbcTemplate.queryForObject(query, Integer.class, mutex))
        .orElseThrow(() -> new MutexNotFoundException("Mutex not found in database. arg=" + mutex));
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public void delete(String mutex) {
    jdbcTemplate.update("delete from RABBITMQ_MUTEX where MUTEX = ?", mutex);
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String create() {
    long sequence =
        jdbcTemplate.queryForObject("select RABBITMQ_MUTEX_SEQ.nextval from dual", Long.class);
    String mutex = String.valueOf(System.currentTimeMillis()) + sequence;
    jdbcTemplate.update("insert into RABBITMQ_MUTEX values(?,SYSTIMESTAMP)", mutex);
    return mutex;
  }

  /**
   * ミューテックス不在例外.
   * @author Tomoaki Mikami
   */
  private static class MutexNotFoundException extends RuntimeException {
    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * コンストラクタ.
     * @param message エラーメッセージ
     */
    public MutexNotFoundException(String message) {
      super(message);
    }
  }
}
