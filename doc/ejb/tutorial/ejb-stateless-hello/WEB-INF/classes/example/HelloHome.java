package example;

import java.rmi.*;
import javax.ejb.*;

public interface HelloHome extends EJBHome {
  public Hello create() throws RemoteException, CreateException;
}
