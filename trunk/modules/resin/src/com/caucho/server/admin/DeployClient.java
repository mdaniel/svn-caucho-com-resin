/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.server.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.caucho.bam.BamError;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.ServiceUnavailableException;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.query.QueryCallback;
import com.caucho.cloud.deploy.CopyTagQuery;
import com.caucho.cloud.deploy.RemoveTagQuery;
import com.caucho.cloud.deploy.SetTagQuery;
import com.caucho.env.git.GitCommitJar;
import com.caucho.env.git.GitCommitTree;
import com.caucho.env.repository.CommitBuilder;
import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositoryException;
import com.caucho.env.repository.RepositoryTagEntry;
import com.caucho.env.repository.RepositoryTagListener;
import com.caucho.hmtp.HmtpClient;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.InputStreamSource;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;

/**
 * Deploy Client API
 */
public class DeployClient implements Repository
{
  private static final L10N L = new L10N(DeployClient.class);
  
  public static final String USER_ATTRIBUTE = "user";
  public static final String MESSAGE_ATTRIBUTE = "message";
  public static final String VERSION_ATTRIBUTE = "version";
  
  private static final long DEPLOY_TIMEOUT = 600 * 1000L;

  private Broker _broker;
  private ActorSender _bamClient;
  private String _deployAddress;
  
  private String _url;
  
  public DeployClient()
  {
    Server server = Server.getCurrent();

    if (server == null)
      throw new IllegalStateException(L.l("DeployClient was not called in a Resin context. For external clients, use the DeployClient constructor with host,port arguments."));
    
    _bamClient = server.createAdminClient(getClass().getSimpleName());

    _deployAddress = "deploy@resin.caucho";
  }

  public DeployClient(String serverId)
  {
    Server server = Server.getCurrent();

    if (server == null)
      throw new IllegalStateException(L.l("DeployClient was not called in a Resin context. For external clients, use the DeployClient constructor with host,port arguments."));
    
    _bamClient = server.createAdminClient(getClass().getSimpleName());

    _deployAddress = "deploy@" + serverId + ".resin.caucho";
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
    
      _deployAddress = "deploy@resin.caucho";
    } catch (RemoteConnectionFailedException e) {
      throw new RemoteConnectionFailedException(L.l("Connection to '{0}' failed for remote deploy. Check the server and make sure <resin:RemoteAdminService> is enabled in the resin.xml.\n  {1}",
                                                    url, e.getMessage()),
                                                e);
    }
  }
  
  public String getUrl()
  {
    return _url;
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
    commit.validate();
    
    GitCommitJar gitCommit = null;

    try {
      gitCommit = new GitCommitJar(jar);
      
      String tag = commit.getId();
      
      return deployJar(tag, gitCommit, commit.getAttributes());
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
      
      return deployJar(tag, gitCommit, commit.getAttributes());
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
    commit.validate();
    
    GitCommitJar gitCommit = null;

    try {
      gitCommit = new GitCommitJar(path);
      
      String tag = commit.getId();
      
      return deployJar(tag, gitCommit, commit.getAttributes());
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

    CopyTagQuery query
      = new CopyTagQuery(targetId, sourceId, target.getAttributes());

    return (Boolean) query(query);
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

    RemoveTagQuery query = new RemoveTagQuery(tag, commit.getAttributes());

    return (Boolean) query(query);
  }
  
  /**
   * Returns the state for a tag.
   */
  public String getTagState(String tag)
  {
    TagStateQuery query = new TagStateQuery(tag);
    
    query = (TagStateQuery) query(query);
    
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
    TagStateQuery query = new TagStateQuery(tag);
    
    query = (TagStateQuery) query(query);
    
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
                           Map<String,String> attributes)
    throws IOException
  {
    String []files = getCommitList(commit.getCommitList());

    SendQueryCallback cb = new SendQueryCallback(files, commit);
    
    for (int i = 0; ! cb.isEmpty() && i < 5; i++) {
      cb.sendNext();
    }
    
    cb.waitForDone(DEPLOY_TIMEOUT);
    
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

    DeploySendQuery query = new DeploySendQuery(sha1, source);

    query(query);
  }

  public String []getCommitList(String []commitList)
  {
    DeployCommitListQuery query = new DeployCommitListQuery(commitList);
    
    DeployCommitListQuery result = (DeployCommitListQuery) query(query);

    return result.getCommitList();
  }

  public String calculateFileDigest(InputStream is, long length)
    throws IOException
  {
    return GitCommitTree.calculateBlobDigest(is, length);
  }

  public String addDeployFile(String tag, String name, String sha1)
  {
    DeployAddFileQuery query = new DeployAddFileQuery(tag, name, sha1);

    return (String) query(query);
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
    
    // server/2o66
    SetTagQuery query
      = new SetTagQuery(tag, contentHash, attributeCopy);

    query(query);
    
    return true;
  }
  
  public TagResult []queryTags(String pattern)
  {
    QueryTagsQuery query = new QueryTagsQuery(pattern);

    return (TagResult []) query(query);
  }

  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   *
   */
  public Boolean restart(String tag)
  {
    ControllerRestartQuery query = new ControllerRestartQuery(tag);

    return (Boolean) query(query);
  }

  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   *
   * @deprecated
   */
  public Boolean start(String tag)
  {
    ControllerStartQuery query = new ControllerStartQuery(tag);

    return (Boolean) query(query);
  }

  /**
   * Stops a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   *
   * @deprecated
   */
  public Boolean stop(String tag)
  {
    ControllerStopQuery query = new ControllerStopQuery(tag);

    return (Boolean) query(query);
  }

  /**
   * Deploy controller based on a deployment tag: wars/default/foo.com/my-war
   *
   * @param tag the encoded controller name
   *
   * @deprecated
   */
  public Boolean deploy(String tag)
  {
    ControllerDeployQuery query = new ControllerDeployQuery(tag);

    return (Boolean) query(query);
  }

  /**
   * Undeploy a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public boolean undeploy(CommitBuilder commit)
  {
    return removeTag(commit);
  }

  /**
   * @deprecated
   **/
  public StatusQuery status(String tag)
  {
    StatusQuery query = new StatusQuery(tag);

    return (StatusQuery) query(query);
  }

  /**
   * @deprecated
   **/
  public HostQuery []listHosts()
  {
    ListHostsQuery query = new ListHostsQuery();

    return (HostQuery []) query(query);
  }

  /**
   * @deprecated
   **/
  public WebAppQuery []listWebApps(String host)
  {
    return (WebAppQuery []) query(new ListWebAppsQuery(host));
  }

  /**
   * @deprecated
   **/
  public TagQuery []listTags(String host)
  {
    return (TagQuery []) query(new ListTagsQuery(host));
  }

  protected Serializable query(Serializable query)
  {
    try {
      return (Serializable) _bamClient.query(_deployAddress, query);
    } catch (ServiceUnavailableException e) {
      throw new ServiceUnavailableException("Deploy service is not available, possibly because the resin.xml is missing a <resin:DeployService> tag\n  " + e.getMessage(),
                                            e);
    }
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
   * @see com.caucho.env.repository.Repository#getTagMap()
   */
  @Override
  public Map<String, RepositoryTagEntry> getTagMap()
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
  
  class SendQueryCallback implements QueryCallback {
    private ArrayList<String> _list;
    
    private BamError _error;
    private GitCommitJar _commit;
    
    private AtomicInteger _inProgressCount = new AtomicInteger();
    
    private volatile boolean _isDone;
    
    SendQueryCallback(String []hashList,
                      GitCommitJar commit)
    {
      _list = new ArrayList<String>();
      for (String hash : hashList)
        _list.add(hash);
      
      _commit = commit;
    }

    @Override
    public void onQueryError(String to, String from, Serializable payload,
                             BamError error)
    {
      if (_error == null)
        _error = error;
      
      onDone();
    }

    @Override
    public void onQueryResult(String to, String from, Serializable payload)
    {
      sendNext();
      
      _inProgressCount.decrementAndGet();
      
      if (_inProgressCount.get() == 0)
        onDone();
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

        DeploySendQuery query = new DeploySendQuery(sha1, source);

        _bamClient.query(_deployAddress, query, this);
        
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
      long expires = Alarm.getCurrentTimeActual() + timeout;

      synchronized (this) {
        long delta;
        
        while (! isDone()
               && (delta = (expires - Alarm.getCurrentTimeActual())) > 0) {
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
  }
}

