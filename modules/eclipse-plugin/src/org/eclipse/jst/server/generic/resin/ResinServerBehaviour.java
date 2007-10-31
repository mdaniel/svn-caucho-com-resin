package org.eclipse.jst.server.generic.resin;

import org.eclipse.jst.server.generic.core.internal.GenericServerBehaviour;

public class ResinServerBehaviour
  extends GenericServerBehaviour
{

  /* (non-Javadoc)
* @see org.eclipse.wst.server.core.model.ServerBehaviourDelegate#stop(boolean)
*/
  public void stop(boolean force)
  {
    super.stop(true);
  }
}
