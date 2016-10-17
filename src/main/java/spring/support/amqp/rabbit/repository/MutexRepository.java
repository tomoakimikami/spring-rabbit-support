/**
 *
 */
package spring.support.amqp.rabbit.repository;

/**
 * @author yoshidan
 *
 */
public interface MutexRepository {

  /**
   * ロック取得
   *
   * @param mutex
   */
  void lock(String mutex);

  /**
   * 削除
   *
   * @param mutex
   */
  void delete(String mutex);

  /**
   * 採番、producerで実行する。
   *
   * @return mutex
   */
  String create();
}