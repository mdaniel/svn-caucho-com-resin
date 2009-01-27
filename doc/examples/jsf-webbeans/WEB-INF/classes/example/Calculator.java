package example;

import javax.annotation.Named;
import javax.context.RequestScoped;

@RequestScoped
@Named("calc")  
public class Calculator {
  private int _a;
  private int _b;

  public void setA(int a)
  {
    _a = a;
  }

  public int getA()
  {
    return _a;
  }

  public void setB(int b)
  {
    _b = b;
  }

  public int getB()
  {
    return _b;
  }

  public int getSum()
  {
    return _a + _b;
  }
}
