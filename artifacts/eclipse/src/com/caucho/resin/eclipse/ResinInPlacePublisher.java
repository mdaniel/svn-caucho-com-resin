package com.caucho.resin.eclipse;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.core.internal.GenericPublisher;
import org.eclipse.jst.server.generic.core.internal.GenericServer;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.wst.server.core.IModuleArtifact;

/**
 * This "publisher" doesn't actually publish anything, but instead makes sure
 * that the correct resin.xml file is available to the resin instance (it may 
 * need to be pulled out of a bundle .jar file into a temp dir) and initializes
 * the variables that point the resin to the workspace. 
 * 
 * @author Emil Ong
 *
 */
@SuppressWarnings("restriction")
public class ResinInPlacePublisher extends GenericPublisher
                                   implements ResinIdentifiers
{
  public static final String PUBLISHER_ID = 
    "org.eclipse.jst.server.generic.resin.resininplacepublisher";

  @Override
  public IStatus[] publish(IModuleArtifact[] resource, 
                           IProgressMonitor monitor)
  {
    if (getModule().length > 1)
      return null;

    IWebModule webModule = 
      (IWebModule) getModule()[0].loadAdapter(IWebModule.class, null);
    
    IContainer[] folders = webModule.getResourceFolders();

    if (folders.length != 1) {
      IStatus s = new Status(IStatus.ERROR, 
                             CorePlugin.PLUGIN_ID, 0, 
                             "Cannot find web content folder",
                             null);
      CorePlugin.getDefault().getLog().log(s);
      return new IStatus[] { s };
    }
    
    String webContentFolder = folders[0].getLocation().toString();
    String webappId = getModule()[0].getName();
    
    try {
      if (monitor.isCanceled())
        return null;

      ResinServer resinServer = (ResinServer) getServer();
      Map properties = resinServer.getServerInstanceProperties(); 
      
      String configLocation = 
        (String) properties.get(ResinServer.RESIN_CONF_PROJECT_LOCATION);
      
      VariableUtil.setVariable(RESIN_CONFIGURATION_FILE_NAME_ID, 
                               configLocation);
      VariableUtil.setVariable("webapp.dir", webContentFolder);
      VariableUtil.setVariable("webapp.id", webappId);
    } 
    catch (CoreException e) {
      IStatus s = new Status(IStatus.ERROR, 
                             CorePlugin.PLUGIN_ID, 0, 
                             "In place Resin publish failed",
                             e);
      CorePlugin.getDefault().getLog().log(s);
      return new IStatus[] { s };
    }
    
    return null;
  }

  @Override
  public IStatus[] unpublish(IProgressMonitor monitor)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
