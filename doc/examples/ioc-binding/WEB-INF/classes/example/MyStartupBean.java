package example;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.enterprise.inject.Current;

@Startup
public class MyStartupBean {
  private @Current StartupResourceBean _startupResource;
  private @Current MyService _service;

  @PostConstruct
  public void init()
  {
    _startupResource.setData(this + ": initial value");
    _service.setMessage(this + ": initial value");
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName();
  }
}