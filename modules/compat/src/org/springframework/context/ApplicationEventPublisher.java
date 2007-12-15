package org.springframework.context;

import java.util.Locale;

public interface ApplicationEventPublisher {
  public void publishEvent(ApplicationEvent event);
}
