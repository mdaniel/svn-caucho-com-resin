/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.deploy;

import io.baratine.core.ServiceExceptionNotFound;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amp.AmpSystem;
import com.caucho.amp.remote.ClientAmpLocal;
import com.caucho.baratine.client.ServiceManagerClient;
import com.caucho.util.L10N;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.StreamSourceInputStream;

/**
 * Deploy Client API
 */
public class DeployClient implements AutoCloseable
{
  private static final Logger log
    = Logger.getLogger(DeployClient.class.getName());
  
  private static final L10N L = new L10N(DeployClient.class);
  
  public static final String USER_ATTRIBUTE = "user";
  public static final String MESSAGE_ATTRIBUTE = "message";
  public static final String VERSION_ATTRIBUTE = "version";
  
  //private static final long DEPLOY_TIMEOUT = 10 * 60 * 1000L;
  private static final long DEPLOY_TIMEOUT = 60 * 1000L;

  private String _deployAddress;
  private final DeployService _deployService;
  
  private String _url;

  private ServiceManagerClient _rampClient;

  /*
  public DeployClient()
  {
    this(null);
  }
  */
  
  public DeployClient()
  {
    this(new ClientAmpLocal(AmpSystem.getCurrentManager()));
  }
  
  public DeployClient(ServiceManagerClient hampClient)
  {
    _rampClient = hampClient;
    
    Objects.requireNonNull(hampClient);
    
    // _url = url;

    _deployAddress = DeployServiceImpl.ADDRESS;
    
    String address;
    
    if (_rampClient instanceof ClientAmpLocal) {
      address = _deployAddress;
    }
    else {
      address = "remote://" + _deployAddress;
    }
    
    _deployService = _rampClient.lookup(address)
                                .as(DeployService.class);
  }
  
  /*
  public DeployClient(String url,
                      String userName, String password)
  {
    _url = url;
    
    HampResinClient client = new HampResinClient(url, userName, password);
    try {
      client.setVirtualHost("admin.resin");

      // client.connect(userName, password);
      client.connect();

      _rampClient = client;
      // _bamManager = new SimpleBamManager(_bamClient.getBroker());
    
      _deployAddress = "remote://" + DeployActor.ADDRESS;
      
      // BamManager bamManager = server.getAdminBrokerManager();
      // return BamProxyFactory.createProxy(api, to, sender);
      
      _deployService = _rampClient.lookup(_deployAddress)
                                  .as(DeployActorProxy.class);
      
    } catch (ServiceNotFoundException | ServiceConnectException e) {
      throw e.rethrow(L.l("Connection to '{0}' failed for remote administration.\n  Ensure the local server has started, or include --server and --port parameters to connect to a remote server.\n  {1}",
                          url, e.getMessage()));
    } catch (Exception e) {
      throw e;
    }
  }
  */
  
  public String getUrl()
  {
    if (_url != null)
      return _url;
    /*
    else if (_bamClient != null)
      return _bamClient.getAddress();
      */
    else if (_rampClient != null)
      return _rampClient.getUrl();
    else
      return null;
  }
  
  /**
   * Returns the state for a tag.
   */
  public String getTagState(String tag)
  {
    DeployTagStateQuery query = _deployService.getTagState(tag);
    
    if (query != null)
      return query.getState();
    else
      return null;
  }
  
  /**
   * Returns the state for a tag.
   */
  public Throwable getTagException(String tag)
  {
    DeployTagStateQuery query = _deployService.getTagState(tag); // (TagStateQuery) query(query);
    
    if (query != null)
      return query.getThrowable();
    else
      return null;
  }

  public void writeRawGitFile(String sha1, InputStream is)
    throws IOException
  {
    StreamSourceInputStream iss = new StreamSourceInputStream(is);
    
    StreamSource source = new StreamSource(iss);

    // QueryFutureCallback future = new QueryFutureCallback();
    _deployService.sendFile(sha1, source, null);
  }

  public String []getCommitList(String []commitList)
  {
    try {
      return _deployService.getCommitList(commitList);
    } catch (ServiceExceptionNotFound e) {
      throw e.rethrow(L.l("Deploy service is not available.\n  Ensure <resin:AdminServices> or a <resin:DeployService> is enabled in resin.xml.\n  {0}",
                          e.getMessage()));
    }
  }

  public String []listFiles(String tagName, String fileName)
    throws IOException
  {
    return _deployService.listFiles(tagName, fileName);
  }

  private boolean putTag(String tag,
                         String contentHash,
                         Map<String,String> attributes)
  {
    Objects.requireNonNull(tag);
    Objects.requireNonNull(contentHash);
    
    HashMap<String,String> attributeCopy;

    if (attributes != null)
      attributeCopy = new HashMap<String,String>(attributes);
    else
      attributeCopy = new HashMap<String,String>();
    
    return _deployService.putTag(tag, contentHash, attributeCopy);
  }
  
  public DeployTagResult []queryTags(String pattern)
  {
    return _deployService.queryTags(pattern);
  }

  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   *
   */
  public DeployControllerState restart(String tag)
  {
    return _deployService.restart(tag);
  }

  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   *
   */
  public DeployControllerState restartCluster(String tag)
  {
    return _deployService.restartCluster(tag);
  }

  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public DeployControllerState start(String tag)
  {
    return _deployService.start(tag);
  }

  /**
   * Stops a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public DeployControllerState stop(String tag)
  {
    return _deployService.stop(tag);
  }

  public void close()
  {
    ServiceManagerClient rampClient = _rampClient;
    
    if (rampClient != null) {
      try {
        rampClient.close();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _rampClient + "]";
  }
}

