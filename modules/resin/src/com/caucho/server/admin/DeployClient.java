/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import com.caucho.bam.*;
import com.caucho.bam.hmtp.HmtpClient;
import com.caucho.git.*;
import com.caucho.server.cluster.HmuxBamClient;
import com.caucho.server.resin.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.io.*;
import java.util.HashMap;

/**
 * Deploy Client API
 */
public class DeployClient
{
  private static final L10N L = new L10N(DeployClient.class);

  private Broker _broker;
  private ActorClient _conn;
  private String _deployJid;
  
  private HmuxBamClient _client;

  public DeployClient()
  {
    Resin resin = Resin.getCurrent();

    _broker = resin.getManagement().getAdminBroker();
    _conn = _broker.getConnection("admin", null);

    _deployJid = "deploy@resin.caucho";
  }

  public DeployClient(String serverId)
  {
    Resin resin = Resin.getCurrent();

    _broker = resin.getManagement().getAdminBroker();
    _conn = _broker.getConnection("admin", null);

    _deployJid = "deploy@" + serverId + ".resin.caucho";
  }
  
  public DeployClient(String host, int port,
		      String userName, String password)
  {
    HmtpClient client = new HmtpClient("http://" + host + ":" + port + "/hmtp");
    client.setVirtualHost("admin.resin");

    client.connect(userName, password);

    _conn = client;
    
    _deployJid = "deploy@resin.caucho";
  }

  /**
   * Uploads the contents of a jar/zip file to a Resin server.  
   * The jar is unzipped and each component is uploaded separately.
   * For wars, this means that only the changed files need updates.
   *
   * @param jar path to the jar file
   * @param tag symbolic name of the jar file to add
   * @param user user name for the commit message
   * @param message the commit message
   * @param version the symbolic version for the jar
   * @param extraAttr any extra information for the commit
   */
  public String deployJarContents(Path jar,
				  String tag,
				  String user,
				  String message,
				  String version,
				  HashMap<String,String> extraAttr)
    throws IOException
  {
    GitCommitJar commit = new GitCommitJar(jar);

    try {
      return deployJar(commit, tag, user, message, version, extraAttr);
    } finally {
      commit.close();
    }
  }

  /**
   * Uploads a stream to a jar/zip file to a Resin server
   *
   * @param is stream containing a jar/zip
   * @param tag symbolic name of the jar file to add
   * @param user user name for the commit message
   * @param message the commit message
   * @param version the symbolic version for the jar
   * @param extraAttr any extra information for the commit
   */
  public String deployJarContents(InputStream is,
				  String tag,
				  String user,
				  String message,
				  String version,
				  HashMap<String,String> extraAttr)
    throws IOException
  {
    GitCommitJar commit = new GitCommitJar(is);

    try {
      return deployJar(commit, tag, user, message, version, extraAttr);
    } finally {
      commit.close();
    }
  }

  /**
   * Undeploys a tag
   *
   * @param tag symbolic name of the jar file to add
   * @param user user name for the commit message
   * @param message the commit message
   */
  public String undeploy(String tag,
			 String user,
			 String message,
			 HashMap<String,String> extraAttr)
    throws IOException
  {
    RemoveTagQuery query = new RemoveTagQuery(tag, user, message);

    return (String) querySet(query);
  }

  /**
   * Deploys a war, but does not start it
   *
   * @param host the virtual host
   * @param name the web-app name
   */
  public Boolean deployWar(String host, String name)
  {
    String tag = createTag("wars", host, name);

    return deploy(tag);
  }

  /**
   * Deploys an ear, but does not start it
   *
   * @param host the virtual host
   * @param name the ear name
   */
  public Boolean deployEar(String host, String name)
  {
    String tag = createTag("ears", host, name);

    return deploy(tag);
  }

  /**
   * Deploy controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public Boolean deploy(String tag)
  {
    ControllerDeployQuery query = new ControllerDeployQuery(tag);

    return (Boolean) querySet(query);
  }

  /**
   * Starts a controller
   *
   * @param type the controller type: war, ear, etc.
   * @param host the virtual host
   * @param name the web-app/ear name
   */
  public Boolean start(String type, String host, String name)
  {
    String tag = createTag(type, host, name);

    return start(tag);
  }
  
  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public Boolean start(String tag)
  {
    ControllerStartQuery query = new ControllerStartQuery(tag);

    return (Boolean) querySet(query);
  }

  /**
   * Stops a controller
   *
   * @param type the controller type: war, ear, etc.
   * @param host the virtual host
   * @param name the web-app/ear name
   */
  public Boolean stop(String type, String host, String name)
  {
    String tag = createTag(type, host, name);

    return stop(tag);
  }

  /**
   * Stops a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public Boolean stop(String tag)
  {
    ControllerStopQuery query = new ControllerStopQuery(tag);

    return (Boolean) querySet(query);
  }

  /**
   * Undeploys a controller
   *
   * @param type the controller type: war, ear, etc.
   * @param host the virtual host
   * @param name the web-app/ear name
   */
  public Boolean undeploy(String type, String host, String name)
  {
    String tag = createTag(type, host, name);

    return undeploy(tag);
  }

  /**
   * Undeploy a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public Boolean undeploy(String tag)
  {
    ControllerUndeployQuery query = new ControllerUndeployQuery(tag);

    return (Boolean) querySet(query);
  }

  //
  // low-level routines
  //

  private String deployJar(GitCommitJar commit,
			   String tag,
			   String user,
			   String message,
			   String version,
			   HashMap<String,String> extraAttr)
    throws IOException
  {
    String []files = getCommitList(commit.getCommitList());

    for (String sha1 : files) {
      GitJarStreamSource gitSource = new GitJarStreamSource(sha1, commit);
      StreamSource source = new StreamSource(gitSource);

      DeploySendQuery query = new DeploySendQuery(sha1, source);

      querySet(query);
    }

    String result = commit(tag, commit.getDigest(), user, message, version, extraAttr);

    deploy(tag);

    return result;
  }

  public void sendFile(String sha1,
		       long length,
		       InputStream is)
    throws IOException
  {
    InputStream blobIs = GitCommitTree.writeBlob(is, length);

    sendRawFile(sha1, blobIs);
  }

  public void sendRawFile(String sha1,
			  InputStream is)
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
    DeployAddFileQuery query
      = new DeployAddFileQuery(tag, name, sha1);

    return (String) querySet(query);
  }

  /**
   * Public for QA, but not normally exposed.
   */
  private String commit(String tag,
		       String sha1,
		       String user,
		       String message,
		       String version,
		       HashMap<String,String> attr)
  {
    // server/2o66
    DeployCommitQuery query
      = new DeployCommitQuery(tag, sha1, user, message, version, attr);

    return (String) querySet(query);
  }
  
  public StatusQuery status(String tag)
  {
    StatusQuery query = new StatusQuery(tag);

    return (StatusQuery) queryGet(query);
  }

  public HostQuery []listHosts()
  {
    ListHostsQuery query = new ListHostsQuery();

    return (HostQuery []) queryGet(query);
  }

  public WebAppQuery []listWebApps(String host)
  {
    return (WebAppQuery []) queryGet(new ListWebAppsQuery(host));
  }

  public TagQuery []listTags(String host)
  {
    return (TagQuery []) queryGet(new ListTagsQuery(host));
  }

  private Serializable queryGet(Serializable query)
  {
    if (_conn != null)
      return _conn.queryGet(_deployJid, query);
    else {
      return (Serializable) _client.queryGet(_deployJid, query);
    }
  }

  private Serializable querySet(Serializable query)
  {
    if (_conn != null)
      return _conn.querySet(_deployJid, query);
    else {
      return (Serializable) _client.querySet(_deployJid, query);
    }
  }

  private String createTag(String type, String host, String name)
  {
    while (name.startsWith("/"))
      name = name.substring(1);
    
    return type + "/" + host + "/" + name;
  }

  @Override
  public String toString()
  {
    if (_broker != null)
      return getClass().getSimpleName() + "[" + _deployJid + "]";
    else
      return getClass().getSimpleName() + "[" + _client + "]";
  }
}