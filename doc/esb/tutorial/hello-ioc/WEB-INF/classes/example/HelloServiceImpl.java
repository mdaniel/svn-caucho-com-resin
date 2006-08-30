package example;

public class HelloServiceImpl implements HelloService {
  private String _hello;

  /**
   * Injection setter for the _hello field.
   */
  public void setHello(String hello)
  {
    _hello = hello;
  }

  /**
   * Returns "hello, world".
   */
  public String hello()
  {
    return _hello;
  }
}
