package example.session.local;

import javax.ejb.*;
import java.rmi.*;

public interface Counter extends EJBObject {
  int hit() throws RemoteException;
}
