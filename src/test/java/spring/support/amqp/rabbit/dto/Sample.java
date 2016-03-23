package spring.support.amqp.rabbit.dto;

import java.util.Date;

import lombok.Data;

@Data
public class Sample {

  private String name;

  private long age;

  private Date now;
}
