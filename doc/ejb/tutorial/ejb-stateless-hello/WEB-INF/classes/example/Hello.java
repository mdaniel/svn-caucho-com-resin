package example;

import java.rmi.*;
import javax.ejb.*;

public interface Hello extends EJBObject {
  public String hello() throws RemoteException;
}
