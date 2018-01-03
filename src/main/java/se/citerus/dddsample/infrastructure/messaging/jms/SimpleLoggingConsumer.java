package se.citerus.dddsample.infrastructure.messaging.jms;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLoggingConsumer implements MessageListener {

  private final Logger logger = LoggerFactory.getLogger(SimpleLoggingConsumer.class);

  @Override
  public void onMessage(Message message) {
    logger.debug("Received JMS message: {}", message);
  }

}
