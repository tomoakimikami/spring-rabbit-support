package spring.support.amqp.rabbit.repository;

/**
 * ミューテックス情報エンティティ用リポジトリインタフェース.
 * 
 * @author yoshidan
 */
public interface MutexRepository {

  /**
   * select for update no waitでロックを取得する.
   * <p>
   * エラーが発生した場合は既に処理済みのメッセージか処理中なので2重配信を意味する。
   * </p>
   * <dl>
   * <dt>レコードが無かった場合</dt>
   * <dd>MutexNotFoundException = producerがmutexを作成していないためエラー</dd>
   * <dt>リソースビジーが発生</dt>
   * <dd>CannotAcquireLockExceptions = 他のコンシューマが処理中のためエラー</dd>
   * </dl>
   * @param mutex
   *          ミューテックスキー
   */
  void lock(String mutex);

  /**
   * 削除.
   * 
   * @param mutex
   *          ミューテックスキー
   */
  void delete(String mutex);

  /**
   * 採番、producerで実行する.
   * 
   * @return mutex
   */
  String create();
}
