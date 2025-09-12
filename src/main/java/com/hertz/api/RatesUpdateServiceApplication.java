package com.hertz.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.hertz.rates.common.utils.logging.HertzLogger;

/**
 * Rates Update Service Application
 * 
 * Description: Defines RatesUpdateServiceApplication SpringBoot class for rates update service.
 */
@SpringBootApplication(exclude = {com.hertz.digital.msf.autoconfigure.OpenApiAutoConfiguration.class})
@EnableConfigurationProperties
public class RatesUpdateServiceApplication implements CommandLineRunner {

  public final static HertzLogger logger = new HertzLogger(RatesUpdateServiceApplication.class);

  static {
    System.getProperties().setProperty("log4j1.compatibility", "true");
    System.getProperties().setProperty("log4j.configuration", "config/devLog4jproperties.xml");
    System.getProperties().setProperty("java.security.egd", "file:///dev/urandom");
  }

  public static void main(String[] args) {
    SpringApplication.run(RatesUpdateServiceApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    logger.info("Spring Boot Rates Update Service Application Started....");
  }
}
