package org.springframework.context;

public interface Lifecycle
{
  public boolean isRunning();

  public void start();

  public void stop();
}
