package example;

/**
 * The Greeting service returns a simple string.
 */
public class GreetingImpl implements GreetingAPI {
  private String _greeting = "Hello, world";

  /**
   * Configures the greeting.
   */
  public void setGreeting(String greeting)
  {
    _greeting = greeting;
  }
  
  /**
   * Returns the greeting string.
   */
  public String greeting()
  {
    return _greeting;
  }
}
