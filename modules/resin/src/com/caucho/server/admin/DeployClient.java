/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.caucho.bam.ActorClient;
import com.caucho.bam.Broker;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.ServiceUnavailableException;
import com.caucho.env.git.GitCommit;
import com.caucho.env.git.GitCommitJar;
import com.caucho.env.git.GitCommitTree;
import com.caucho.env.git.GitTree;
import com.caucho.env.git.GitType;
import com.caucho.env.repository.AbstractRepository;
import com.caucho.env.repository.RepositoryException;
import com.caucho.hmtp.HmtpClient;
import com.caucho.server.cluster.Server;
import com.caucho.util.L10N;
import com.caucho.vfs.InputStreamSource;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;

/**
 * Deploy Client API
 */
public class DeployClient extends AbstractRepository
{
  private static final L10N L = new L10N(DeployClient.class);
  public static final String USER_ATTRIBUTE = "user";
  public static final String MESSAGE_ATTRIBUTE = "message";
  public static final String VERSION_ATTRIBUTE = "version";

  private Broker _broker;
  private ActorClient _bamClient;
  private String _deployJid;
  
  private String _url;
  
  public DeployClient()
  {
    Server server = Server.getCurrent();

    if (server == null)
      throw new IllegalStateException(L.l("DeployClient was not called in a Resin context. For external clients, use the DeployClient constructor with host,port arguments."));
    
    _bamClient = server.createAdminClient(getClass().getSimpleName());

    _deployJid = "deploy@resin.caucho";
  }

  public DeployClient(String serverId)
  {
    Server server = Server.getCurrent();

    if (server == null)
      throw new IllegalStateException(L.l("DeployClient was not called in a Resin context. For external clients, use the DeployClient constructor with host,port arguments."));
    
    _bamClient = server.createAdminClient(getClass().getSimpleName());

    _deployJid = "deploy@" + serverId + ".resin.caucho";
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
    
      _deployJid = "deploy@resin.caucho";
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
  public String putTagArchive(String tag,
                              Path jar,
                              String commitMessage,
                              Map<String,String> attributes)
  {
    GitCommitJar commit = null;

    try {
      commit = new GitCommitJar(jar);
      
      return deployJar(tag, commit, commitMessage, attributes);
    }
    catch (IOException e) {
      throw new RepositoryException(e);
    }
    finally {
      if (commit != null)
        commit.close();
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
  public String deployJarContents(String tag,
                                  Path jar,
                                  HashMap<String,String> attributes)
    throws IOException
  {
    GitCommitJar commit = new GitCommitJar(jar);

    String message = "deploy jar";
    
    try {
      return deployJar(tag, commit, message, attributes);
    } 
    finally {
      commit.close();
    }
  }

  /**
   * Uploads a stream to a jar/zip file to a Resin server
   *
   * @param tag symbolic name of the jar file to add
   * @param is stream containing a jar/zip
   * @param attributes commit attributes including user, message, and version
   */
  public String deployJarContents(String tag,
                                  InputStream is,
                                  HashMap<String,String> attributes)
    throws IOException
  {
    GitCommitJar commit = new GitCommitJar(is);
    
    String message = "init message";

    try {
      return deployJar(tag, commit, message, attributes);
    } 
    finally {
      commit.close();
    }
  }

  /**
   * Copies a tag
   *
   * @param tag the new tag to create
   * @param sourceTag the source tag from which to copy
   * @param attributes commit attributes including user and message
   */
  public Boolean copyTag(String tag,
                         String sourceTag,
                         HashMap<String,String> attributes)
  {
    String user = null;
    String message = null;
    String version = null;

    if (attributes != null) {
      user = attributes.get(USER_ATTRIBUTE);
      message = attributes.get(MESSAGE_ATTRIBUTE);
      version = attributes.get(VERSION_ATTRIBUTE);
    }

    CopyTagQuery query = 
      new CopyTagQuery(tag, sourceTag, user, message, version);

    return (Boolean) querySet(query);
  }

  /**
   * deletes a tag from the repository
   *
   * @param tag the tag to remove
   * @param attributes commit attributes including user and message
   */
  public Boolean removeTag(String tag, 
                           HashMap<String,String> attributes)
  {
    String user = attributes.get(USER_ATTRIBUTE);
    String message = attributes.get(MESSAGE_ATTRIBUTE);

    RemoveTagQuery query = new RemoveTagQuery(tag, user, message);

    return (Boolean) querySet(query);
  }
  
  /**
   * Returns the state for a tag.
   */
  public String getTagState(String tag)
  {
    TagStateQuery query = new TagStateQuery(tag);
    
    query = (TagStateQuery) queryGet(query);
    
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
    
    query = (TagStateQuery) queryGet(query);
    
    if (query != null)
      return query.getThrowable();
    else
      return null;
  }

  //
  // low-level routines
  //

  protected String deployJar(String tag,
                             GitCommitJar commit,
                             String commitMessage,
                             Map<String,String> attributes)
    throws IOException
  {
    String []files = getCommitList(commit.getCommitList());

    for (String sha1 : files) {
      GitJarStreamSource gitSource = new GitJarStreamSource(sha1, commit);
      StreamSource source = new StreamSource(gitSource);

      DeploySendQuery query = new DeploySendQuery(sha1, source);

      querySet(query);
    }
    
    putTag(tag, commit.getDigest(), commitMessage, attributes);
    
    return commit.getDigest();
  }

  public void sendFile(String sha1, long length, InputStream is)
    throws IOException
  {
    InputStream blobIs = GitCommitTree.writeBlob(is, length);

    writeRawGitFile(sha1, blobIs);
  }

  @Override
  public void writeRawGitFile(String sha1, InputStream is)
    throws IOException
  {
    InputStreamSource iss = new InputStreamSource(is);
    
    StreamSource source = new StreamSource(iss);

    DeploySendQuery query = new DeploySendQuery(sha1, source);

    querySet(query);
  }

  public String []getCommitList(String []commitList)
  {
    DeployCommitListQuery query = new DeployCommitListQuery(commitList);
    
    DeployCommitListQuery result = (DeployCommitListQuery) queryGet(query);

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

    return (String) querySet(query);
  }

  @Override
  public boolean putTag(String tag,
                        String contentHash,
                        String commitMessage,
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
    

    String user = attributeCopy.remove(USER_ATTRIBUTE);
    String version = attributeCopy.remove(VERSION_ATTRIBUTE);

    // server/2o66
    SetTagQuery query
      = new SetTagQuery(tag, contentHash, user, commitMessage, version, 
                        attributeCopy);

    querySet(query);
    
    return true;
  }
  
  protected String setTag(String tag,
                          String sha1,
                          HashMap<String,String> attributes)
  {
    HashMap<String,String> attributeCopy;

    if (attributes != null)
      attributeCopy = new HashMap<String,String>(attributes);
    else
      attributeCopy = new HashMap<String,String>();

    String user = attributeCopy.remove(USER_ATTRIBUTE);
    String message = attributeCopy.remove(MESSAGE_ATTRIBUTE);
    String version = attributeCopy.remove(VERSION_ATTRIBUTE);

    // server/2o66
    SetTagQuery query
      = new SetTagQuery(tag, sha1, user, message, version, attributeCopy);

    return (String) querySet(query);
  }
  
  public TagResult []queryTags(String pattern)
  {
    QueryTagsQuery query = new QueryTagsQuery(pattern);

    return (TagResult []) queryGet(query);
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

    return (Boolean) querySet(query);
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

    return (Boolean) querySet(query);
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

    return (Boolean) querySet(query);
  }

  /**
   * Undeploy a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   *
   * @deprecated
   */
  public Boolean undeploy(String tag)
  {
    ControllerUndeployQuery query = new ControllerUndeployQuery(tag);

    return (Boolean) querySet(query);
  }

  /**
   * Undeploys a tag
   *
   * @param tag symbolic name of the jar file to add
   * @param user user name for the commit message
   * @param message the commit message
   *
   * @deprecated
   */
  public Boolean undeploy(String tag,
                          String user,
                          String message,
                          HashMap<String,String> extraAttr)
  {
    UndeployQuery query = new UndeployQuery(tag, user, message);

    return (Boolean) querySet(query);
  }

  /**
   * @deprecated
   **/
  public StatusQuery status(String tag)
  {
    StatusQuery query = new StatusQuery(tag);

    return (StatusQuery) queryGet(query);
  }

  /**
   * @deprecated
   **/
  public HostQuery []listHosts()
  {
    ListHostsQuery query = new ListHostsQuery();

    return (HostQuery []) queryGet(query);
  }

  /**
   * @deprecated
   **/
  public WebAppQuery []listWebApps(String host)
  {
    return (WebAppQuery []) queryGet(new ListWebAppsQuery(host));
  }

  /**
   * @deprecated
   **/
  public TagQuery []listTags(String host)
  {
    return (TagQuery []) queryGet(new ListTagsQuery(host));
  }

  protected Serializable queryGet(Serializable query)
  {
    try {
      return (Serializable) _bamClient.queryGet(_deployJid, query);
    } catch (ServiceUnavailableException e) {
      throw new ServiceUnavailableException("Deploy service is not available, possibly because the resin.xml is missing a <resin:DeployService> tag\n  " + e.getMessage(),
                                            e);
    }
  }
  
  public void close()
  {
    _bamClient.close();
  }

  protected Serializable querySet(Serializable query)
  {
    return (Serializable) _bamClient.querySet(_deployJid, query);
  }

  @Override
  public String toString()
  {
    if (_broker != null)
      return getClass().getSimpleName() + "[" + _deployJid + "]";
    else
      return getClass().getSimpleName() + "[" + _bamClient + "]";
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#addCommit(com.caucho.env.git.GitCommit)
   */
  @Override
  public String addCommit(GitCommit commit) throws IOException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#exists(java.lang.String)
   */
  @Override
  public boolean exists(String sha1)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#expandToPath(java.lang.String, com.caucho.vfs.Path)
   */
  @Override
  public void expandToPath(String contentHash, Path path)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#getRepositoryRootHash()
   */
  @Override
  public String getRepositoryRootHash()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#getType(java.lang.String)
   */
  @Override
  public GitType getType(String sha1)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#openBlob(java.lang.String)
   */
  @Override
  public InputStream openBlob(String sha1) throws IOException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#openRawGitFile(java.lang.String)
   */
  @Override
  public InputStream openRawGitFile(String contentHash) throws IOException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#readCommit(java.lang.String)
   */
  @Override
  public GitCommit readCommit(String commitHash) throws IOException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#readTree(java.lang.String)
   */
  @Override
  public GitTree readTree(String treeHash) throws IOException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#removeTag(java.lang.String, java.lang.String, java.util.Map)
   */
  @Override
  public boolean removeTag(String tagName, String commitMessage,
                           Map<String, String> commitMetaData)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#setRepositoryRootHash(java.lang.String)
   */
  @Override
  public void setRepositoryRootHash(String repositoryCommitHash)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.AbstractRepository#writeBlobToStream(java.lang.String, java.io.OutputStream)
   */
  @Override
  public void writeBlobToStream(String blobHash, OutputStream os)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.env.repository.Repository#addBlob(java.io.InputStream)
   */
  @Override
  public String addBlob(InputStream is) throws IOException
  {
    // TODO Auto-generated method stub
    return null;
  }
}

