package example;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Named;

@ApplicationScoped
@Named("myService")  
public class MyServiceBean implements MyService {
  private String _message = "initial message";
  
  public void setMessage(String message)
  {
    _message = message;
  }
  
  public String getMessage()
  {
    return _message;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _message + "]";
  }
}