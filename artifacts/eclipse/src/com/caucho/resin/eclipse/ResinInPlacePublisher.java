package com.caucho.resin.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.core.internal.GenericPublisher;
import org.eclipse.jst.server.generic.internal.core.util.FileUtil;
import org.eclipse.jst.server.generic.servertype.definition.Publisher;
import org.eclipse.jst.server.generic.servertype.definition.PublisherData;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.osgi.framework.Bundle;

/**
 * This "publisher" doesn't actually publish anything, but instead makes sure
 * that the correct resin.xml file is available to the resin instance (it may 
 * need to be pulled out of a bundle .jar file into a temp dir) and initializes
 * the variables that point the resin to the workspace. 
 * 
 * @author Emil Ong
 *
 */
public class ResinInPlacePublisher extends GenericPublisher {
  public static final String PUBLISHER_ID = 
    "org.eclipse.jst.server.generic.resin.resininplacepublisher";
  
  public static final String RESIN_CONFIGURATION_FILE_NAME_ID =
    "resin.configuration.file";
  
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

      ServerRuntime typeDef = getServerRuntime().getServerTypeDefinition();
      String filename = 
        PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                       RESIN_CONFIGURATION_FILE_NAME_ID);
      File configFile = PublisherUtil.locateBundleFile(typeDef, filename);
      
      VariableUtil.setVariable(RESIN_CONFIGURATION_FILE_NAME_ID, 
                               configFile.toString());
      VariableUtil.setVariable("wtp.webapp.dir", webContentFolder);
      VariableUtil.setVariable("wtp.webapp.id", webappId);
    } catch (CoreException e) {
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
