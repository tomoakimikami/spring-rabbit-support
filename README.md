# spring-rabbit-support
2重配信制御とか

## 準備

下記DDLを実行しておきます。

```sql
DROP TABLE RABBITMQ_MUTEX;
DROP SEQUENCE RABBITMQ_MUTEX_SEQ;
CREATE TABLE RABBITMQ_MUTEX ( MUTEX NUMBER(18) ,CREATED_AT TIMESTAMP );
CREATE SEQUENCE RABBITMQ_MUTEX_SEQ START WITH 1 INCREMENT BY 1 MAXVALUE 99999 CYCLE;
```

## アプリケーションへ組み込み

### ライブラリ追加設定

* Mavenの場合

pom.xmlのdependenciesへ下記を追加します。

```xml
<dependency>
    <groupId>spring.support</groupId>
    <artifactId>spring-rabbit-support</artifactId>
    <version>1.0.0</version>
</dependency>
```

* Gradleの場合

build.gradleのdependenciesへ下記を追加します。

```groovy
compile('spring.supprt:spring-rabbit-support:1.0.0')
```

### Spring Boot設定

デフォルトだと、Spring Bootは、外部ライブラリ内に定義されたBeanを探してDI登録したりはしないので、
DI登録対象とするため、Applicationクラスなどに下記アノテーションを追加して、外部ライブラリ内のコンポーネントを登録させます。

```java
@SpringBootApplication(scanBasePackages = {
  "アプリケーションルートパッケージ", // アプリケーション側のパッケージルート
  "spring.support.amqp.rabbit"   // 二重配信制御コンポーネントのパッケージルート
})
```

### 送信処理側

送信処理を行う側では下記Beanを二重配信制御機能付きメッセージ送信コンポーネントとしてDI定義しておきます。

```java
@Autowired
private ExactlyOnceDeliveryProducer producer;
```

送信時は当該クラスの下記メソッドを呼び出すことで、メッセージと一対一に対応するMUTEX IDが発行され、DBへレコードが登録されます。
また、メッセージ自体のカスタムプロパティとしてもMUTEX IDは登録されます。(プロパティのヘッダキーはx-mutex-id)

```java
/**
 * DBトランザクション完了後にメッセージを送信する.
 *
 * @param exchange エクスチェンジ名
 * @param routingKey ルーティングキー名
 * @param payload メッセージ本体
 */
public String send(String exchange, String routingKey, Object payload) {
  ...
}
```

### 受信処理側

受信処理を行う側で下記メソッドを定義しておきます。
メッセージ・キューは、送信時に設定したexchange/routingKeyによりメッセージが配信された先のキュー名を指定します。
containerFactoryは、RabbitListenerContainerFactoryのbeanを指定します。
このbeanはSpring Bootにより自動登録されますが、独自にカスタマイズしたbeanを登録している場合は、そちらを指定します。

```java
/**
 * メッセージ受信.
 * HeadersおよびPayloadアノテーションは実装ではなくインタフェース側に必要
 *
 * @param headers ヘッダ
 * @param dto 受信DTO
 */
@Transactional
@ExactlyOnceDelivery
@RabbitListener(queues = "メッセージ・キュー", containerFactory = "containerFactory")
void receive(@Headers Map<String, String> headers, @Payload Sample data) {
  ...
}
```

上記はリスナーメソッドで、指定したキューにメッセージが配信されると、その内容を引数としてメソッドが呼び出されます。
ExactOnceDeliveryアノテーションにより、あるメッセージの受信に対しては、当該メソッドが一回だけ呼び出されることが保証されます。
