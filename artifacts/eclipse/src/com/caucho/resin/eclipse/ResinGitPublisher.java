package com.caucho.resin.eclipse;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.core.internal.publishers.AntPublisher;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.internal.ServerPlugin;

import com.caucho.server.admin.DeployClient;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

@SuppressWarnings("restriction")
public class ResinGitPublisher extends AntPublisher {
  public static final String PUBLISHER_ID = 
    "org.eclipse.jst.server.generic.resin.resingitpublisher";
  
  public static final String RESIN_CONFIGURATION_FILE_NAME_ID =
    "resin.configuration.file";
  
  public static final String RESIN_SERVER_ADDRESS_ID =
    "resin.server.address";
  
  public static final String RESIN_VIRTUAL_HOST_ID =
    "resin.virtual.host";
  
  public static final String RESIN_HMUX_PORT_ID =
    "resin.hmux.port";

  public static final String RESIN_DEPLOY_USERNAME_ID =
    "resin.deploy.username";
  
  public static final String RESIN_DEPLOY_PASSWORD_ID =
    "resin.deploy.password";

  private DeployClient _client = null;
  
  @Override
  public IStatus[] publish(IModuleArtifact[] resource, IProgressMonitor monitor)
  {
    if (getModule().length > 1)
      return null;
    
    // Have ant create the .war file
    IStatus[] result = super.publish(resource, monitor);
    
    // null means success... NOT Status.OK
    if (result != null)
      return result;
    
    ServerRuntime typeDef = getServerRuntime().getServerTypeDefinition();
    String host = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                 RESIN_VIRTUAL_HOST_ID);
    String user = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                 RESIN_DEPLOY_USERNAME_ID);
    String pass = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                 RESIN_DEPLOY_PASSWORD_ID);
    
    Path war = getWarPath();
    String tag = "wars/" + host + "/" + getModuleName(); 
    
    try {
      getDeployClient().deployJarContents(war, tag, user, "", null, null);
      getDeployClient().start(tag);
    }
    catch (IOException e) {
      IStatus s = new Status(IStatus.ERROR, 
                             CorePlugin.PLUGIN_ID, 
                             0, 
                             "Could not deploy war file to server", 
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
  
  private DeployClient getDeployClient()
  {
    if (_client == null) {
      ServerRuntime typeDef = getServerRuntime().getServerTypeDefinition();
      String server = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                     RESIN_SERVER_ADDRESS_ID);

      String portString = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                         RESIN_HMUX_PORT_ID);

      int port = Integer.valueOf(portString);

      _client = new DeployClient(server, port);
    }

    return _client;
  }
  
  private Path getWarPath()
  {
    String serverId = getServer().getServer().getId();
    IPath projectWorkingDir = 
      ServerPlugin.getInstance().getTempDirectory(serverId);

    String moduleName = getModuleName();
    
    String prefix = 
      projectWorkingDir.append(moduleName).toPortableString();
          
    return Vfs.lookup(prefix + ".war");
  }
  
  private String getModuleName() 
  {
    IModule module = getModule()[0];
    String moduleName = module.getName();
    
    if ("jst.web".equals(module.getModuleType().getId())) {
      IWebModule webModule = 
        (IWebModule) module.loadAdapter(IWebModule.class, null);
      
      if (webModule != null) {
        String contextRoot = webModule.getURI(module);
        moduleName = contextRoot.substring(0, contextRoot.lastIndexOf('.'));
      }
    }
    
    return moduleName;
  }
}