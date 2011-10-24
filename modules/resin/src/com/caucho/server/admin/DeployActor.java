/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.management.ObjectName;

import com.caucho.bam.BamError;
import com.caucho.bam.Query;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.deploy.CopyTagQuery;
import com.caucho.cloud.deploy.RemoveTagQuery;
import com.caucho.cloud.deploy.SetTagQuery;
import com.caucho.config.ConfigException;
import com.caucho.config.Service;
import com.caucho.env.deploy.DeployControllerService;
import com.caucho.env.deploy.DeployException;
import com.caucho.env.deploy.DeployTagItem;
import com.caucho.env.repository.RepositorySystem;
import com.caucho.env.repository.RepositorySpi;
import com.caucho.env.repository.RepositoryTagEntry;
import com.caucho.jmx.Jmx;
import com.caucho.management.server.DeployControllerMXBean;
import com.caucho.management.server.EAppMXBean;
import com.caucho.management.server.EarDeployMXBean;
import com.caucho.management.server.WebAppDeployMXBean;
import com.caucho.management.server.WebAppMXBean;
import com.caucho.server.cluster.Server;
import com.caucho.server.host.HostController;
import com.caucho.server.webapp.WebAppContainer;
import com.caucho.server.webapp.WebAppController;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

public class DeployActor extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(DeployActor.class.getName());

  private static final L10N L = new L10N(DeployActor.class);

  private Server _server;

  private RepositorySpi _repository;

  private AtomicBoolean _isInit = new AtomicBoolean();

  public DeployActor()
  {
    super("deploy@resin.caucho", BamSystem.getCurrentBroker());
  }
  
  /*
  @Override
  public ManagedBroker getBroker()
  {
    return _server.getAdminBroker();
  }
  */

  @PostConstruct
  public void init()
  {
    if (_isInit.getAndSet(true))
      return;

    _server = Server.getCurrent();

    if (_server == null)
      throw new ConfigException(L.l("resin:DeployService requires an active Server.\n  {0}",
                                    Thread.currentThread().getContextClassLoader()));

    _repository = RepositorySystem.getCurrentRepositorySpi();

    setBroker(getBroker());
    MultiworkerMailbox mailbox
      = new MultiworkerMailbox(getActor().getAddress(), 
                               getActor(), getBroker(), 2);
    
    getBroker().addMailbox(mailbox);
  }

  @Query
  public boolean commitList(long id, String to, String from,
                            DeployCommitListQuery commitList)
  {
    ArrayList<String> uncommittedList = new ArrayList<String>();

    if (commitList.getCommitList() != null) {
      for (String commit : commitList.getCommitList()) {
        if (! _repository.exists(commit))
          uncommittedList.add(commit);
      }
    }

    DeployCommitListQuery resultList
      = new DeployCommitListQuery(uncommittedList);

    getBroker().queryResult(id, from, to, resultList);

    return true;
  }

  @Query
  public void tagCopy(long id,
                      String to,
                      String from,
                      CopyTagQuery query)
  {
    String tag = query.getTag();
    String sourceTag = query.getSourceTag();

    RepositoryTagEntry entry = _repository.getTagMap().get(sourceTag);

    if (entry == null) {
      log.fine(this + " copyError dst='" + query.getTag() + "' src='" + query.getSourceTag() + "'");

      throw new DeployException(L.l("deploy-copy: '{0}' is an unknown source tag.",
                                    query.getSourceTag()));
      /*
      getBroker().queryError(id, from, to, query,
                             new BamError(BamError.TYPE_CANCEL,
                                          BamError.ITEM_NOT_FOUND,
                             "unknown tag"));
      return;
      */
    }

    log.fine(this + " copy dst='" + query.getTag() + "' src='" + query.getSourceTag() + "'");
    
    String server = "default";
    
    TreeMap<String,String> metaDataMap = new TreeMap<String,String>();
    
    if (query.getAttributes() != null)
      metaDataMap.putAll(query.getAttributes());
    
    if (server != null)
      metaDataMap.put("server", server);

    boolean result = _repository.putTag(tag,
                                        entry.getRoot(),
                                        metaDataMap);

    getBroker().queryResult(id, from, to, result);
  }

  @Query
  public void tagState(long id,
                       String to,
                       String from,
                       TagStateQuery query)
  {
    // XXX: just ping the tag?
    // updateDeploy();
    
    String tag = query.getTag();
    
    DeployControllerService deploy = DeployControllerService.getCurrent();
    DeployTagItem item = null;
    
    if (deploy != null) {
      deploy.update(tag);
      item = deploy.getTagItem(tag);
    }
    
    if (item != null) {
      TagStateQuery result = new TagStateQuery(tag, item.getState(), 
                                               item.getDeployException());
      
      getBroker().queryResult(id, from, to, result);
    }
    else
      getBroker().queryResult(id, from, to, null);
  }

  @Query
  public Boolean removeTag(RemoveTagQuery query)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " query " + query);
    
    String server = "default";
    
    HashMap<String,String> commitMetaData = new HashMap<String,String>();
    
    if (query.getAttributes() != null)
      commitMetaData.putAll(query.getAttributes());
    
    commitMetaData.put("server", server);

    return _repository.removeTag(query.getTag(), commitMetaData);
  }

  @Query
  public boolean sendFileQuery(long id, String to, String from,
                               DeploySendQuery query)
  {
    String sha1 = query.getSha1();

    if (log.isLoggable(Level.FINER))
      log.finer(this + " sendFileQuery sha1=" + sha1);

    InputStream is = null;
    try {
      is = query.getInputStream();

      _repository.writeRawGitFile(sha1, is);

      getBroker().queryResult(id, from, to, true);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      getBroker().queryResult(id, from, to, false);
    } finally {
      IoUtil.close(is);
    }

    return true;
  }

  @Query
  public boolean setTagQuery(long id, String to, String from, SetTagQuery query)
  {
    String tagName = query.getTag();
    String contentHash = query.getContentHash();
    
    if (contentHash == null)
      throw new NullPointerException();

    String server = "default";
    
    TreeMap<String,String> commitMetaData = new TreeMap<String,String>();
    
    if (query.getAttributes() != null)
      commitMetaData.putAll(query.getAttributes());
    
    commitMetaData.put("server", server);
    
    boolean result = _repository.putTag(tagName, 
                                        contentHash,
                                        commitMetaData);

    getBroker().queryResult(id, from, to, String.valueOf(result));

    return true;
  }

  @Query
  public boolean queryTags(long id,
                           String to,
                           String from,
                           QueryTagsQuery tagsQuery)
  {
    ArrayList<TagResult> tags = new ArrayList<TagResult>();

    Pattern pattern = Pattern.compile(tagsQuery.getPattern());

    for (Map.Entry<String, RepositoryTagEntry> entry :
         _repository.getTagMap().entrySet()) {
      String tag = entry.getKey();

      if (pattern.matcher(tag).matches())
        tags.add(new TagResult(tag, entry.getValue().getRoot()));
    }

    getBroker()
      .queryResult(id, from, to, tags.toArray(new TagResult[tags.size()]));

    return true;
  }

  /**
   * @deprecated
   */
  @Query
  public boolean controllerDeploy(long id,
                                  String to,
                                  String from,
                                  ControllerDeployQuery query)
  {
    String status = deploy(query.getTag());

    log.fine(this + " deploy '" + query.getTag() + "' -> " + status);

    getBroker().queryResult(id, from, to, true);

    return true;
  }

  private String deploy(String gitPath)
  {
    return start(gitPath);
  }

  /**
   * @deprecated
   */
  @Query
  public boolean controllerStart(long id,
                                 String to,
                                 String from,
                                 ControllerStartQuery query)
  {
    String status = start(query.getTag());

    log.fine(this + " start '" + query.getTag() + "' -> " + status);

    getBroker().queryResult(id, from, to, true);

    return true;
  }

  private String start(String tag)
  {
    DeployControllerService service = DeployControllerService.getCurrent();
    
    DeployTagItem controller = service.getTagItem(tag);

    if (controller == null)
      return L.l("'{0}' is an unknown controller", controller);

    try {
      controller.toStart();

      return controller.getState();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }
  }

  /**
   * @deprecated
   */
  @Query
  public boolean controllerStop(long id,
                                String to,
                                String from,
                                ControllerStopQuery query)
  {
    String status = stop(query.getTag());

    log.fine(this + " stop '" + query.getTag() + "' -> " + status);

    getBroker().queryResult(id, from, to, true);

    return true;
  }

  private String stop(String tag)
  {
    DeployControllerService service = DeployControllerService.getCurrent();

    DeployTagItem controller = service.getTagItem(tag);

    if (controller == null)
      return L.l("'{0}' is an unknown controller", controller);

    try {
      controller.toStop();

      return controller.getState();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }
  }

  @Query
  public boolean controllerRestart(long id,
                                   String to,
                                   String from,
                                   ControllerRestartQuery query)
  {
    String status = restart(query.getTag());

    log.fine(this + " restart '" + query.getTag() + "' -> " + status);

    getBroker().queryResult(id, from, to, true);

    return true;
  }

  private String restart(String tag)
  {
    DeployControllerService service = DeployControllerService.getCurrent();

    DeployTagItem controller = service.getTagItem(tag);

    if (controller == null)
      return L.l("'{0}' is an unknown controller", controller);

    try {
      controller.toRestart();

      return controller.getState();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }
  }

  /**
   * @deprecated
   */
  @Query
  public boolean controllerUndeploy(long id,
                                    String to,
                                    String from,
                                    ControllerUndeployQuery query)
  {
    String status = undeploy(query.getTag());

    log.fine(this + " undeploy '" + query.getTag() + "' -> " + status);

    getBroker().queryResult(id, from, to, true);

    return true;
  }

  /**
   * @deprecated
   */
  private String undeploy(String tag)
  {
    DeployControllerMXBean controller = findController(tag);

    if (controller == null)
      return L.l("'{0}' is an unknown controller", controller);

    try {
      Path root = Vfs.lookup(controller.getRootDirectory());

      root.removeAll();

      controller.stop();

      if (controller.destroy())
        return "undeployed";
      else
        return L.l("'{0}' failed to undeploy application '{1}'",
                   controller,
                   tag);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }
  }

  /**
   * @deprecated
   */
  @Query
  public boolean controllerUndeploy(long id,
                                    String to,
                                    String from,
                                    UndeployQuery query)
  {
    if (true)
      throw new UnsupportedOperationException();
    
    String status = null;//undeploy(query.getTag(), query.getUser(), query.getMessage());

    log.fine(this + " undeploy '" + query.getTag() + "' -> " + status);

    getBroker().queryResult(id, from, to, true);

    return true;
  }

  private String undeploy(String tag, Map<String,String> commitMetaData)
  {
    DeployControllerMXBean controller = findController(tag);

    if (controller == null)
      return L.l("'{0}' is an unknown controller", controller);

    try {
      Path root = Vfs.lookup(controller.getRootDirectory());

      root.removeAll();

      controller.stop();

      if (controller.destroy()) {
        String server = "default";
        
        HashMap<String,String> metaDataCopy = new HashMap<String,String>();
        
        if (commitMetaData != null)
          metaDataCopy.putAll(commitMetaData);
        
        metaDataCopy.put("server", server);
        
        _repository.removeTag(tag, metaDataCopy);

        return "undeployed";
      }
      else
        return L.l("'{0}' failed to remove application '{1}'",
                   controller,
                   tag);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }
  }

  /**
   * @deprecated
   */
  @Query
  public boolean sendAddFileQuery(long id, String to, String from,
                                  DeployAddFileQuery query)
  {
    String tag = query.getTag();
    String name = query.getName();
    String contentHash = query.getHex();

    try {
      DeployControllerMXBean deploy = findController(tag);

      if (deploy == null) {
        if (log.isLoggable(Level.FINE))
          log.fine(this + " sendAddFileQuery '" + tag + "' is an unknown DeployController");

        getBroker().queryResult(id, from, to, "no-deploy: " + tag);

        return true;
      }

      Path root = Vfs.lookup(deploy.getRootDirectory());
      root = root.createRoot();

      Path path = root.lookup(name);

      if (! path.getParent().exists())
        path.getParent().mkdirs();

      _repository.expandToPath(contentHash, path);

      getBroker().queryResult(id, from, to, "ok");

      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      getBroker().queryResult(id, from, to, "fail");

      return true;
    }
  }

  /**
   * @deprecated
   **/
  @Query
  public boolean listWebApps(long id,
                             String to,
                             String from,
                             ListWebAppsQuery listQuery)
  {
    ArrayList<WebAppQuery> apps = new ArrayList<WebAppQuery>();

    String stage = _server.getStage();

    for (HostController host : _server.getHostControllers()) {
      if (listQuery.getHost().equals(host.getName())) {
        WebAppContainer webAppContainer
          = host.getDeployInstance().getWebAppContainer();
        
        for (WebAppController webApp : webAppContainer.getWebAppList()) {
          WebAppQuery q = new WebAppQuery();
          String name = webApp.getId();

          if (name.startsWith("/"))
            name = name.substring(1);

          q.setTag("wars/" + stage + "/" + host.getName() + "/" + name);

          q.setHost(host.getName());
          q.setUrl(webApp.getURL());

          apps.add(q);
        }
      }
    }

    getBroker()
      .queryResult(id, from, to, apps.toArray(new WebAppQuery[apps.size()]));

    return true;
  }

  /**
   * @deprecated
   **/
  @Query
  public boolean listTags(long id,
                          String to,
                          String from,
                          ListTagsQuery listQuery)
  {
    ArrayList<TagQuery> tags = new ArrayList<TagQuery>();

    for (String tag : _repository.getTagMap().keySet()) {
      if (tag.startsWith("wars/default") || tag.startsWith("ears/default")) {
        int p = "wars/default/".length();
        int q = tag.indexOf('/', p + 1);

        if (q < 0)
          continue;

        String host = tag.substring(p, q);
        // String name = tag.substring(q + 1);

        tags.add(new TagQuery(host, tag));
      }
    }

    getBroker()
      .queryResult(id, from, to, tags.toArray(new TagQuery[tags.size()]));

    return true;
  }

  /**
   * @deprecated
   **/
  @Query
  public boolean listHosts(long id,
                           String to,
                           String from,
                           ListHostsQuery query)
  {
    List<HostQuery> hosts = new ArrayList<HostQuery>();

    for (HostController controller : _server.getHostControllers()) {
      if ("admin.resin".equals(controller.getName()))
         continue;

      HostQuery q = new HostQuery();
      q.setName(controller.getName());

      hosts.add(q);
    }

    getBroker()
      .queryResult(id, from, to, hosts.toArray(new HostQuery[hosts.size()]));

    return true;
  }

  /**
   * @deprecated
   **/
  @Query
  public boolean status(long id,
                        String to,
                        String from,
                        StatusQuery query)
  {
    String tag = query.getTag();

    String errorMessage = statusMessage(tag);
    String state = null;

    StatusQuery result = new StatusQuery(tag, state, errorMessage);

    getBroker().queryResult(id, from, to, result);

    return true;
  }

  private String statusMessage(String tag)
  {
    int p = tag.indexOf('/');
    int q = tag.indexOf('/', p + 1);
    int r = tag.lastIndexOf('/');

    if (p < 0 || q < 0 || r < 0)
      return L.l("'{0}' is an unknown type", tag);

    String type = tag.substring(0, p);
    // String stage = tag.substring(p + 1, q);
    String host = tag.substring(q + 1, r);
    String name = tag.substring(r + 1);

    // String state = null;
    String errorMessage = tag + " is an unknown resource";

    try {
      if (type.equals("ears")) {
        String pattern
          = "resin:type=EApp,Host=" + host + ",name=" + name;

        EAppMXBean ear = (EAppMXBean) Jmx.findGlobal(pattern);

        if (ear != null) {
          ear.update();
          // state = ear.getState();
          errorMessage = ear.getErrorMessage();

          return errorMessage;
        }
        else
          return L.l("'{0}' is an unknown ear", tag);
      }
      else if (type.equals("wars")) {
        String pattern
          = "resin:type=WebApp,Host=" + host + ",name=/" + name;

        WebAppMXBean war = (WebAppMXBean) Jmx.findGlobal(pattern);

        if (war != null) {
          war.update();
          // state = war.getState();
          errorMessage = war.getErrorMessage();

          return errorMessage;
        }
        else
          return L.l("'{0}' is an unknown war", tag);
      }
      else
        return L.l("'{0}' is an unknown tag", tag);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return errorMessage;
  }

  private DeployControllerMXBean findController(String tag)
  {
    int p = tag.indexOf('/');
    int q = tag.indexOf('/', p + 1);
    int r = tag.lastIndexOf('/');

    if (p < 0 || q < 0 || r < 0)
      return null;

    String type = tag.substring(0, p);
    // String stage = tag.substring(p + 1, q);

    String host;
    String name;

    if (q < r) {
      host = tag.substring(q + 1, r);
      name = tag.substring(r + 1);
    }
    else {
      host = tag.substring(q + 1);
      name = "";
    }

    try {
      if (type.equals("ears")) {
        String pattern
          = "resin:type=EApp,Host=" + host + ",name=" + name;

        EAppMXBean ear = (EAppMXBean) Jmx.findGlobal(pattern);

        return ear;
      }
      else if (type.equals("wars")) {
        String pattern
          = "resin:type=WebApp,Host=" + host + ",name=/" + name;

        WebAppMXBean war = (WebAppMXBean) Jmx.findGlobal(pattern);

        return war;
      }
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    return null;
  }
}
