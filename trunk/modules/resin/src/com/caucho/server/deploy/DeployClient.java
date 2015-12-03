/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.caucho.bam.BamError;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.RemoteListenerUnavailableException;
import com.caucho.bam.ServiceUnavailableException;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.manager.BamManager;
import com.caucho.bam.manager.SimpleBamManager;
import com.caucho.bam.proxy.BamProxyFactory;
import com.caucho.bam.query.QueryCallback;
import com.caucho.bam.query.QueryFutureCallback;
import com.caucho.config.ConfigException;
import com.caucho.env.git.GitCommitJar;
import com.caucho.env.git.GitCommitTree;
import com.caucho.env.git.GitObjectStream;
import com.caucho.env.repository.CommitBuilder;
import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositoryException;
import com.caucho.env.repository.RepositoryTagEntry;
import com.caucho.env.repository.RepositoryTagListener;
import com.caucho.hmtp.HmtpClient;
import com.caucho.server.admin.GitJarStreamSource;
import com.caucho.server.cluster.ServletService;
import com.caucho.util.CurrentTime;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.InputStreamSource;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.Vfs;

/**
 * Deploy Client API
 */
public class DeployClient implements Repository
{
  private static final L10N L = new L10N(DeployClient.class);
  
  public static final String USER_ATTRIBUTE = "user";
  public static final String MESSAGE_ATTRIBUTE = "message";
  public static final String VERSION_ATTRIBUTE = "version";
  
  private static final long DEPLOY_TIMEOUT = 10 * 60 * 1000L;

  private Broker _broker;
  private ActorSender _bamClient;
  private String _deployAddress;
  private final DeployActorProxy _deployProxy;
  
  private String _url;

  private SimpleBamManager _bamManager;

  public DeployClient()
  {
    this(null);
  }
  
  public DeployClient(String serverId)
  {
    ServletService server = ServletService.getCurrent();

    if (server == null)
      throw new IllegalStateException(L.l("DeployClient was not called in a Resin context. For external clients, use the DeployClient constructor with host,port arguments."));
    
    _bamClient = server.createAdminClient(getClass().getSimpleName());
    

    //_deployAddress = "deploy@" + serverId + ".resin.caucho";
    _deployAddress = DeployActor.ADDRESS;
    
    BamManager bamManager = server.getAdminBrokerManager();
    
    _deployProxy = bamManager.createProxy(DeployActorProxy.class,
                                          _deployAddress,
                                          _bamClient);
  }
  
  public DeployClient(String url, 
                      ActorSender client)
  {
    _bamClient = client;
    _bamManager = new SimpleBamManager(client.getBroker());
    
    _url = url;

    _deployAddress = DeployActor.ADDRESS;
    
    _deployProxy = _bamManager.createProxy(DeployActorProxy.class,
                                           _deployAddress, 
                                           _bamClient);

  }
  
  public DeployClient(String host, int port,
                      String userName, String password)
  {
    String url = "http://" + host + ":" + port + "/hmtp";
    
    _url = url;
    
    HmtpClient client = new HmtpClient(url);
    try {
      client.setVirtualHost("admin.resin");

      client.connect(userName, password);

      _bamClient = client;
      _bamManager = new SimpleBamManager(_bamClient.getBroker());
    
      _deployAddress = DeployActor.ADDRESS;
      
      /*
      BamManager bamManager = server.getAdminBrokerManager();
      return BamProxyFactory.createProxy(api, to, sender);
*/
      _deployProxy = _bamManager.createProxy(DeployActorProxy.class,
                                             _deployAddress, 
                                             _bamClient);
    } catch (RemoteConnectionFailedException e) {
      throw new RemoteConnectionFailedException(L.l("Connection to '{0}' failed for remote administration.\n  Ensure the local server has started, or include --server and --port parameters to connect to a remote server.\n  {1}",
                                                    url, e.getMessage()), e);
    } catch (RemoteListenerUnavailableException e) {
      throw new RemoteListenerUnavailableException(L.l("Connection to '{0}' failed for remote administration because RemoteAdminService (HMTP) is not enabled.\n  Ensure <resin:RemoteAdminService> is enabled in resin.xml.\n  {1}",
                                                       url, e.getMessage()), e);
    }
  }
  
  public String getUrl()
  {
    if (_url != null)
      return _url;
    else if (_bamClient != null)
      return _bamClient.getAddress();
    else
      return null;
  }

  /**
   * Uploads the contents of a jar/zip file to a Resin server.  
   * The jar is unzipped and each component is uploaded separately.
   * For wars, this means that only the changed files need updates.
   *
   * @param tag symbolic name of the jar file to add
   * @param jar path to the jar file
   * @param attributes commit attributes including user, message, and version
   */
  @Override
  public String commitArchive(CommitBuilder commit,
                              Path jar)
  {
    return commitArchive(commit, jar, DEPLOY_TIMEOUT);
  }
  
  public String commitArchive(CommitBuilder commit,
                              Path jar,
                              long timeout)
  {
    commit.validate();
    
    GitCommitJar gitCommit = null;

    try {
      gitCommit = new GitCommitJar(jar);
      
      String tag = commit.getId();
      
      return deployJar(tag, gitCommit, commit.getAttributes(), timeout);
    }
    catch (IOException e) {
      throw new RepositoryException(e);
    }
    finally {
      if (gitCommit != null)
        gitCommit.close();
    }
  }

  /**
   * Uploads the contents of a jar/zip file to a Resin server.  
   * The jar is unzipped and each component is uploaded separately.
   * For wars, this means that only the changed files need updates.
   *
   * @param tag symbolic name of the jar file to add
   * @param jar path to the jar file
   * @param attributes commit attributes including user, message, and version
   */
  @Override
  public String commitArchive(CommitBuilder commit,
                              InputStream is)
  {
    commit.validate();
    
    GitCommitJar gitCommit = null;

    try {
      gitCommit = new GitCommitJar(is);
      
      String tag = commit.getId();
      
      long timeout = DEPLOY_TIMEOUT;
      
      return deployJar(tag, gitCommit, commit.getAttributes(), timeout);
    }
    catch (IOException e) {
      throw new RepositoryException(e);
    }
    finally {
      if (gitCommit != null)
        gitCommit.close();
    }
  }

  /**
   * Uploads the contents of a jar/zip file to a Resin server.  
   * The jar is unzipped and each component is uploaded separately.
   * For wars, this means that only the changed files need updates.
   *
   * @param tag symbolic name of the jar file to add
   * @param jar path to the jar file
   * @param attributes commit attributes including user, message, and version
   */
  @Override
  public String commitPath(CommitBuilder commit,
                           Path path)
  {
    return commitPath(commit, path, DEPLOY_TIMEOUT);
  }
  
  public String commitPath(CommitBuilder commit,
                           Path path,
                           long timeout)
  {
    commit.validate();
    
    GitCommitJar gitCommit = null;
    
    if (! path.exists()) {
      throw new ConfigException(L.l("'{0}' is not an existing path for deploy commit.",
                                    path));
    }

    try {
      gitCommit = GitCommitJar.createDirectory(path);
      
      String tag = commit.getId();
      
      return deployJar(tag, gitCommit, commit.getAttributes(), timeout);
    }
    catch (IOException e) {
      throw new RepositoryException(e);
    }
    finally {
      if (gitCommit != null)
        gitCommit.close();
    }
  }

  /**
   * Copies a tag
   *
   * @param tag the new tag to create
   * @param sourceTag the source tag from which to copy
   * @param attributes commit attributes including user and message
   */
  public Boolean copyTag(CommitBuilder target,
                         CommitBuilder source)
  {
    target.validate();
    source.validate();
    
    String targetId = target.getId();
    String sourceId = source.getId();

    return _deployProxy.copyTag(targetId, sourceId, target.getAttributes());
  }

  /**
   * deletes a tag from the repository
   *
   * @param tag the tag to remove
   * @param attributes commit attributes including user and message
   */
  @Override
  public boolean removeTag(CommitBuilder commit)
  {
    commit.validate();
    
    String tag = commit.getId();

    return _deployProxy.removeTag(tag, commit.getAttributes());
  }
  
  /**
   * Returns the state for a tag.
   */
  public String getTagState(String tag)
  {
    DeployTagStateQuery query = _deployProxy.getTagState(tag);
    
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
    DeployTagStateQuery query = _deployProxy.getTagState(tag); // (TagStateQuery) query(query);
    
    if (query != null)
      return query.getThrowable();
    else
      return null;
  }

  //
  // low-level routines
  //

  private String deployJar(String tag,
                           GitCommitJar commit,
                           Map<String,String> attributes,
                           long timeout)
    throws IOException
  {
    String []files = getCommitList(commit.getCommitList());

    SendQueryCallback cb = new SendQueryCallback(files, commit);
    
    for (int i = 0; ! cb.isEmpty() && i < 16; i++) {
      cb.sendNext();
    }
    
    cb.waitForDone(timeout);
    
    putTag(tag, commit.getDigest(), attributes);
    
    return commit.getDigest();
  }

  public void sendFile(String sha1, long length, InputStream is)
    throws IOException
  {
    InputStream blobIs = GitCommitTree.writeBlob(is, length);

    writeRawGitFile(sha1, blobIs);
  }

  public void writeRawGitFile(String sha1, InputStream is)
    throws IOException
  {
    InputStreamSource iss = new InputStreamSource(is);
    
    StreamSource source = new StreamSource(iss);

    QueryFutureCallback future = new QueryFutureCallback();
    _deployProxy.sendFile(sha1, source, future);
  }

  public String []getCommitList(String []commitList)
  {
    try {
      return _deployProxy.getCommitList(commitList);
    } catch (ServiceUnavailableException e) {
      throw new ServiceUnavailableException(L.l("Deploy service is not available.\n  Ensure <resin:AdminServices> or a <resin:DeployService> is enabled in resin.xml.\n  {0}",
                                                e.getMessage()),
                                              e);
                                                
    }
  }

  public boolean getFile(String tagName, String fileName, OutputStream os)
    throws IOException
  {
    StreamSource fileSource = _deployProxy.getFile(tagName, fileName);
    
    if (fileSource != null) {
      ReadStream is = null;
      GitObjectStream gitIs = new GitObjectStream(fileSource.getInputStream());
      
      try {
        is = Vfs.openRead(gitIs);
        
        is.writeToStream(os);
      } finally {
        gitIs.close();
        
        IoUtil.close(is);
      }
      
      return true;
    }
    else {
      return false;
    }
  }

  public String []listFiles(String tagName, String fileName)
    throws IOException
  {
    return _deployProxy.listFiles(tagName, fileName);
  }

  public String calculateFileDigest(InputStream is, long length)
    throws IOException
  {
    return GitCommitTree.calculateBlobDigest(is, length);
  }

  private boolean putTag(String tag,
                         String contentHash,
                         Map<String,String> attributes)
  {
    if (tag == null)
      throw new NullPointerException();
    if (contentHash == null)
      throw new NullPointerException();
    
    HashMap<String,String> attributeCopy;

    if (attributes != null)
      attributeCopy = new HashMap<String,String>(attributes);
    else
      attributeCopy = new HashMap<String,String>();
    
    return _deployProxy.putTag(tag, contentHash, attributeCopy);
  }
  
  public DeployTagResult []queryTags(String pattern)
  {
    return _deployProxy.queryTags(pattern);
  }

  /**
   * Undeploy a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param commit the encoded controller name
   */
  public boolean undeploy(CommitBuilder commit)
  {
    return removeTag(commit);
  }

  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   *
   */
  public DeployControllerState restart(String tag)
  {
    return _deployProxy.restart(tag);
  }

  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   *
   */
  public DeployControllerState restartCluster(String tag)
  {
    return _deployProxy.restartCluster(tag);
  }

  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public DeployControllerState start(String tag)
  {
    return _deployProxy.start(tag);
  }

  /**
   * Stops a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public DeployControllerState stop(String tag)
  {
    return _deployProxy.stop(tag);
  }

  public void close()
  {
    _bamClient.close();
  }

  @Override
  public String toString()
  {
    if (_broker != null)
      return getClass().getSimpleName() + "[" + _deployAddress + "]";
    else
      return getClass().getSimpleName() + "[" + _bamClient + "]";
  }

  public static final void fillInVersion(CommitBuilder commit,
                                         String version)
  {
    String []parts = version.split("\\.");
    if (parts.length < 2)
      throw new ConfigException(L.l(
        "erroneous version '{0}'. Version expected in format %d.%d[.%d[.%s]]",
        version));

    int major = Integer.parseInt(parts[0]);
    int minor = Integer.parseInt(parts[1]);
    int micro = 0;

    if (parts.length > 2)
      micro = Integer.parseInt(parts[2]);

    String qualifier = null;

    if (parts.length == 4)
      qualifier = parts[3];

    commit.version(major, minor, micro, qualifier);
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.Repository#addNotificationListener(java.lang.String, com.caucho.env.repository.RepositoryTagListener)
   */
  @Override
  public void addListener(String tagName, RepositoryTagListener listener)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.Repository#getTagContentHash(java.lang.String)
   */
  @Override
  public String getTagContentHash(String tag)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.Repository#removeNotificationListener(java.lang.String, com.caucho.env.repository.RepositoryTagListener)
   */
  @Override
  public void removeListener(String tagName, RepositoryTagListener listener)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.Repository#getTagMap()
   */
  @Override
  public Map<String, RepositoryTagEntry> getTagMap()
  {
    // TODO Auto-generated method stub
    return null;
  }

  class SendQueryCallback implements QueryCallback {
    private ArrayList<String> _list;
    
    private BamError _error;
    private GitCommitJar _commit;
    
    private AtomicInteger _inProgressCount = new AtomicInteger();
    
    private AtomicLong _lastUpdate = new AtomicLong();
    
    private volatile boolean _isDone;
    
    SendQueryCallback(String []hashList,
                      GitCommitJar commit)
    {
      _list = new ArrayList<String>();
      
      for (String hash : hashList) {
        _list.add(hash);
      }
      
      _commit = commit;
    }

    @Override
    public void onQueryError(String to, String from, Serializable payload,
                             BamError error)
    {
      if (_error == null) {
        _error = error;
      }
      
      onDone();
    }

    @Override
    public void onQueryResult(String to, String from, Serializable payload)
    {
      _lastUpdate.set(CurrentTime.getCurrentTimeActual());
      
      sendNext();
      
      _inProgressCount.decrementAndGet();
      
      if (_inProgressCount.get() == 0) {
        onDone();
      }
    }
    
    boolean isEmpty()
    {
      return _list.isEmpty();
    }
    
    boolean isDone()
    {
      return _isDone || _inProgressCount.get() == 0 && _list.isEmpty();
    }
    
    void sendNext()
    {
      String sha1 = null;
      
      synchronized (_list) {
        if (_list.size() > 0) {
          _inProgressCount.incrementAndGet();
          sha1 = _list.remove(0);
        }
      }
      
      if (sha1 == null) {
        if (_inProgressCount.get() == 0)
          onDone();
        return;
      }
      
      boolean isValid = false;
      
      try {
        GitJarStreamSource gitSource = new GitJarStreamSource(sha1, _commit);
        StreamSource source = new StreamSource(gitSource);

        // DeploySendQuery query = new DeploySendQuery(sha1, source);

        _deployProxy.sendFile(sha1, source, this);
        // _bamClient.query(_deployAddress, query, this);
        
        isValid = true;
      } finally {
        if (! isValid)
          onDone();
      }
    }
    
    void onDone()
    {
      synchronized (this) {
        _isDone = true;
        notifyAll();
      }
    }
    
    boolean waitForDone(long timeout)
    {
      _lastUpdate.set(CurrentTime.getCurrentTimeActual());

      synchronized (this) {
        long delta;
        
        while (! isDone() && (delta = getTimeDelta(timeout)) > 0) {
          try {
            Thread.interrupted();
            wait(delta);
          } catch (Exception e) {
          }
        }
      }
      
      if (_error != null)
        throw _error.createException();
      
      return _isDone;
    }
    
    /**
     * The timeout is calculated based on the last valid response received
     * to cover large deployments where each sub-file is a slow upload.
     */
    private long getTimeDelta(long timeout)
    {
      return _lastUpdate.get() + timeout - CurrentTime.getCurrentTimeActual();
    }
  }
}

