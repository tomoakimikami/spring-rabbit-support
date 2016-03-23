/**
 * 
 */
package spring.support.amqp.rabbit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Optional;

import javax.transaction.TransactionRolledbackException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import spring.support.amqp.rabbit.repository.MutexRepository;

/**
 * 1回配信を保証する。
 * 
 * TransactionInterceptorの内側で動かして、エラー発生時にはロックごとロールバックする。
 * 
 * @author yoshidan
 *
 */
@Aspect
@Component("exactlyOnceDeliveryAdvice")
@ConditionalOnMissingBean(name = "exactlyOnceDeliveryAdvice")
public class ExactlyOnceDeliveryAdvice {

  @Autowired
  private MutexRepository repository;

  @Pointcut("within(@ExactlyOnceDeliveryAdvice.ExactlyOnceDelivery *) || "
      + "@annotation(ExactlyOnceDeliveryAdvice.ExactlyOnceDelivery)")
  public void exactlyOnceDelivery() {}

  @Pointcut("within(@org.springframework.amqp.rabbit.annotation.RabbitListener *) || "
      + "@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
  public void rabbitListener() {}

  @Around("exactlyOnceDelivery() && rabbitListener() && args(java.util.Map,..)")
  public Object invoke(ProceedingJoinPoint joinPoint) throws Throwable {

    // NoTransactionException
    TransactionStatus status = TransactionAspectSupport.currentTransactionStatus();

    @SuppressWarnings("unchecked")
    Map<String, String> args = (Map<String, String>) joinPoint.getArgs()[0];

    Optional<String> mutexOptional = Optional.of(args.get(ExactlyOnceDeliveryProducer.MUTEX));
    String mutex = mutexOptional.orElseThrow(
        () -> new IllegalArgumentException(ExactlyOnceDeliveryProducer.MUTEX + " is required"));

    // メッセージロック
    repository.lock(mutex);

    Object retValue = joinPoint.proceed();

    // 成功時はロックを削除
    // ロールバックフラグが設定されているか本処理で例外が発生した場合は、ロックだけ解放されリトライ用にレコードは残す。
    if (status.isRollbackOnly()) {
      // setRollbackOnlyだけだとSimpleMessageListenerContainerでconsumer.rollbackOnExceptionIfNecessary(ex);が実行されず
      // DLQに入らないため必ずエラーにする。
      // 本来はspring-rabbit側で対応する必要があると思われる。
      throw new TransactionRolledbackException("transactionStatus is rollback only");
    } else {
      repository.delete(mutex);
    }

    return retValue;

  }

  @Target(value = {ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ExactlyOnceDelivery {
  }
}
