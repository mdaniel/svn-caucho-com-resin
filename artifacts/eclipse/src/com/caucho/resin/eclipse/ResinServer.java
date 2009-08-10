package com.caucho.resin.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.core.internal.GenericServer;
import org.eclipse.jst.server.generic.core.internal.GenericServerRuntime;
import org.eclipse.jst.server.generic.servertype.definition.Property;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;

@SuppressWarnings("restriction")
public class ResinServer extends GenericServer {
  static final String RESIN_CONF_TYPE = "resin.conf.type";
  static final String RESIN_CONF_BUNDLE = "resin.conf.bundle";
  static final String RESIN_CONF_RESIN_HOME = "resin.conf.resin.home";
  static final String RESIN_CONF_USER = "resin.conf.user";
  
  static final String RESIN_CONF_USER_LOCATION 
    = "resin.conf.user.location";
  static final String RESIN_CONF_BUNDLE_LOCATION 
    = "resin.conf.bundle.location";
  
  static final String RESIN_CONF_PROJECT_LOCATION 
  = "resin.conf.project.location";

  @Override
  public void saveConfiguration(IProgressMonitor monitor) 
    throws CoreException
  {
    super.saveConfiguration(monitor);
    
    String resinConfProjectLocation = 
      (String) getServerInstanceProperties().get(RESIN_CONF_PROJECT_LOCATION);
    
    if (resinConfProjectLocation != null)
      return;

    IServer server = getServer();
    IRuntime runtime = server.getRuntime();
    GenericServerRuntime genericRuntime = 
      (GenericServerRuntime) runtime.loadAdapter(GenericServerRuntime.class, 
                                                 null);
    ServerRuntime typeDef = genericRuntime.getServerTypeDefinition();

    String confType = 
      (String) getServerInstanceProperties().get(RESIN_CONF_TYPE);
    
    IFile configIFile = null;
    
    if (RESIN_CONF_BUNDLE.equals(confType)) {
      String filename = 
        getPropertyDefault(typeDef, RESIN_CONF_BUNDLE_LOCATION);
      File configFile = PublisherUtil.locateBundleFile(typeDef, filename);
      FileInputStream fileContents = null;
      
      try {
        fileContents = new FileInputStream(configFile);
      
        IFolder configFolder = server.getServerConfiguration();
        configIFile = configFolder.getFile(configFile.getName());
        configIFile.create(fileContents, true, monitor);
      }
      catch (IOException e) {
        PublisherUtil.throwCoreException("error copying file from bundle: "
                                         + filename,
                                         e);
      }
      finally {
        try {
          if (fileContents != null)
            fileContents.close();
        }
        catch (IOException e) {
          PublisherUtil.throwCoreException("error closing file from bundle: "
                                           + filename, 
                                           e);
        }
      }
    }
    else if (RESIN_CONF_RESIN_HOME.equals(confType)) {
      String resinHome = 
        (String) getServerInstanceProperties().get(ResinIdentifiers.RESIN_HOME_PROPERTY);
      IPath resinConfPath = new Path(resinHome).append("conf");
      
      IPath resinConfFilePath = resinConfPath.append("resin.xml");
      File resinConfFile = resinConfFilePath.toFile();
      
      if (! resinConfFile.exists()) {
        resinConfFilePath = resinConfPath.append("resin.conf");
        resinConfFile = resinConfFilePath.toFile();
        
        if (! resinConfFile.exists())
          PublisherUtil.throwCoreException("Cannot file Resin configuration in Resin home directory");
      }
    
      IFolder configFolder = server.getServerConfiguration();
      configIFile = configFolder.getFile(resinConfFile.getName());
      configIFile.createLink(resinConfFilePath, IResource.NONE, monitor);
    }
    else if (RESIN_CONF_USER.equals(confType)) {
      String userConf = 
        (String) getServerInstanceProperties().get(RESIN_CONF_USER_LOCATION);
      IPath userConfPath = new Path(userConf);
      File userConfFile = userConfPath.toFile(); 

      IFolder configFolder = server.getServerConfiguration();
      configIFile = configFolder.getFile(userConfFile.getName());
      configIFile.createLink(userConfPath, IResource.NONE, monitor);
    }
    else {
      PublisherUtil.throwCoreException("Internal configuration error");
    }
    
    getServerInstanceProperties().put(RESIN_CONF_PROJECT_LOCATION,
                                      configIFile.getLocation().toOSString());
  }
  
  /**
   * This is a hack to let us store internal data in the serverdef file 
   * that's not exposed to the user.
   * @param runtime The server definition, created from the serverdef file
   * @param key     The internal data key to fetch
   * @return        The default value of the internal property
   */
  private String getPropertyDefault(ServerRuntime runtime, String key)
  {
    List properties = runtime.getProperty();
    Iterator iterator = properties.iterator();
    
    while (iterator.hasNext()) {
      Property property = (Property) iterator.next();
      
      if (key.equals(property.getId()))
        return property.getDefault();
    }
    
    return null;
  }
}
