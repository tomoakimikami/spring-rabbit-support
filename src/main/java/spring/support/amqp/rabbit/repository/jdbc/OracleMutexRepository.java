/**
 *
 */
package spring.support.amqp.rabbit.repository.jdbc;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import spring.support.amqp.rabbit.repository.MutexRepository;

/**
 * @author yoshidan
 */
@Repository
@ConditionalOnMissingBean(MutexRepository.class)
@ConditionalOnBean(JdbcTemplate.class)
public class OracleMutexRepository implements MutexRepository {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  /**
   * select for update no waitでロックを取得する。
   * エラーが発生した場合は既に処理済みのメッセージか処理中なので2重配信を意味する。
   *
   * レコードが無かった場合 -> MutexNotFoundException = producerがmutexを作成していないためエラー
   * リソースビジーが発生 -> CannotAcquireLockExceptions = 他のコンシューマが処理中のためエラー
   */
  @Override
  public void lock(String mutex) {
    String query = "select 1 from RABBITMQ_MUTEX where MUTEX = ? for update nowait";
    Optional.ofNullable(jdbcTemplate.queryForObject(query, Integer.class, mutex))
        .orElseThrow(() -> new MutexNotFoundException("Mutex not found in database. arg=" + mutex));
  }

  /**
   * Mutexを削除する。
   *
   * @param mutex mutex
   */
  @Override
  public void delete(String mutex) {
    jdbcTemplate.update("delete from RABBITMQ_MUTEX where MUTEX = ?", mutex);
  }

  @Override
  public String create() {
    long sequence =
        jdbcTemplate.queryForObject("select RABBITMQ_MUTEX_SEQ.nextval from dual", Long.class);
    String mutex = String.valueOf(System.currentTimeMillis()) + sequence;
    jdbcTemplate.update("insert into RABBITMQ_MUTEX values(?,SYSTIMESTAMP)", mutex);
    return mutex;
  }

  private static class MutexNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MutexNotFoundException(String message) {
      super(message);
    }

  }

}