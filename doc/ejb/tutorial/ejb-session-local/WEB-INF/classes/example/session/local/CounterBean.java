package example.session.local;

import javax.ejb.*;

import com.caucho.ejb.*;

public class CounterBean extends AbstractSessionBean {
  int count;

  // no initialization needed
  public void ejbCreate()
  {
  }

  public int hit()
  {
    return ++count;
  }
}
