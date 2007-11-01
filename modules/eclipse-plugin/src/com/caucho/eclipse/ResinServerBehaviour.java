package com.caucho.eclipse;

import org.eclipse.jst.server.generic.core.internal.GenericServerBehaviour;

public class ResinServerBehaviour
  extends GenericServerBehaviour
{

  public void stop(boolean force)
  {
    // change the default behaviour and always force the stop,
    // which causes eclipse to just terminate the process and
    // not run the <stop> defined in the serverdef file
    super.stop(true);
  }
}
