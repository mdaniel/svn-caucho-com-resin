/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.doc;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.MergePath;
import com.caucho.relaxng.CompactVerifierFactoryImpl;
import com.caucho.relaxng.SchemaImpl;
import com.caucho.relaxng.program.Item;
import com.caucho.relaxng.program.ElementItem;
import com.caucho.relaxng.program.AttributeItem;
import com.caucho.relaxng.program.NameItem;
import com.caucho.relaxng.program.NameClassItem;
import com.caucho.util.L10N;

import java.util.*;
import java.util.logging.Logger;

/**
 * One-time script for generating documentation stubs based on rnc information.
 *
 * Any directive that contains other directives is top-level.
 */
public class DirectivesGenerator
  implements Runnable
{
  public enum Specialize { TOPLEVEL, MERGE, IGNOREDUPLICATE };

  private static final L10N L = new L10N(DirectivesGenerator.class);
  private static final Logger log = Logger.getLogger(DirectivesGenerator.class.getName());

  private Path _destination = Vfs.lookup("resin/doc/directives");

  private RootDirective _rootDirective;
  private LinkedList<Directive> _stack = new LinkedList<Directive>();

  private int _recursionDepth = 0;
  private IdentityHashMap<Item, Boolean> _processed = new IdentityHashMap<Item, Boolean>();

  private HashMap<String, String> _topLevel = new HashMap<String, String>();
  private HashMap<String, String> _ignoreDuplicates = new HashMap<String, String>();
  private HashMap<String, String> _merge = new HashMap<String, String>();

  private TreeMap<String, Set<Directive>> _reverseIndex = new TreeMap<String, Set<Directive>>();
  private int _totalDirectiveCount;


  public DirectivesGenerator()
  {
    specialize("access-log", "*", Specialize.TOPLEVEL);
    specialize("annotation", "*", Specialize.IGNOREDUPLICATE);
    specialize("archive-directory", "*", Specialize.IGNOREDUPLICATE);
    specialize("arg", "*", Specialize.IGNOREDUPLICATE);
    specialize("args", "*", Specialize.IGNOREDUPLICATE);
    specialize("authenticator", "*", Specialize.TOPLEVEL);
    specialize("auto-compile", "*", Specialize.IGNOREDUPLICATE);
    specialize("case-insensitive", "*", Specialize.TOPLEVEL);
    specialize("character-encoding", "*", Specialize.TOPLEVEL);
    specialize("class-loader", "*", Specialize.TOPLEVEL);
    specialize("class", "*", Specialize.IGNOREDUPLICATE);
    specialize("cluster-definition", "*", Specialize.TOPLEVEL, Specialize.MERGE);
    specialize("compiler", "*", Specialize.IGNOREDUPLICATE);
    specialize("connection-factory", "*", Specialize.TOPLEVEL);
    specialize("connection-max", "*", Specialize.IGNOREDUPLICATE);
    specialize("connector", "*", Specialize.TOPLEVEL);
    specialize("database", "*", Specialize.TOPLEVEL);
    specialize("data-source", "*", Specialize.IGNOREDUPLICATE);
    specialize("dependency-check-interval", "*", Specialize.TOPLEVEL);
    specialize("dependency", "*", Specialize.TOPLEVEL);
    specialize("description", "*", Specialize.TOPLEVEL);
    specialize("display-name", "*", Specialize.TOPLEVEL);
    specialize("document-directory", "*", Specialize.IGNOREDUPLICATE);
    specialize("ear-default", "*", Specialize.MERGE, Specialize.TOPLEVEL);
    specialize("ear-deploy", "*", Specialize.TOPLEVEL);
    specialize("ejb-jar", "*", Specialize.TOPLEVEL);
    specialize("ejb-ref", "*", Specialize.TOPLEVEL);
    specialize("ejb-server", "*", Specialize.TOPLEVEL);
    specialize("enable", "*", Specialize.IGNOREDUPLICATE);
    specialize("encoding", "*", Specialize.IGNOREDUPLICATE);
    specialize("env-entry", "*", Specialize.TOPLEVEL);
    specialize("error-page", "*", Specialize.TOPLEVEL);
    specialize("expand-cleanup-fileset", "*", Specialize.TOPLEVEL, Specialize.MERGE);
    specialize("expand-directory", "*", Specialize.TOPLEVEL);
    specialize("expand-path", "*", Specialize.TOPLEVEL);
    specialize("expand-prefix", "*", Specialize.TOPLEVEL);
    specialize("expand-suffix", "*", Specialize.TOPLEVEL);
    specialize("fileset", "*", Specialize.TOPLEVEL);
    specialize("grant", "*", Specialize.TOPLEVEL);
    specialize("host-default", "*", Specialize.MERGE, Specialize.TOPLEVEL);
    specialize("host-deploy", "*", Specialize.TOPLEVEL);
    specialize("host", "server", Specialize.TOPLEVEL);
    specialize("icon", "*", Specialize.TOPLEVEL);
    specialize("id", "*", Specialize.IGNOREDUPLICATE);
    specialize("init-param", "*", Specialize.TOPLEVEL);
    specialize("init", "*", Specialize.TOPLEVEL);
    specialize("javac", "*", Specialize.TOPLEVEL);
    specialize("jndi-link", "*", Specialize.TOPLEVEL);
    specialize("jndi-name", "*", Specialize.TOPLEVEL);
    specialize("jsse-ssl", "*", Specialize.TOPLEVEL);
    specialize("large-icon", "*", Specialize.TOPLEVEL);
    specialize("log", "*", Specialize.TOPLEVEL);
    specialize("mbean-name", "*", Specialize.TOPLEVEL);
    specialize("mbean-listener", "*", Specialize.TOPLEVEL);
    specialize("name", "*", Specialize.IGNOREDUPLICATE);
    specialize("openssl", "*", Specialize.TOPLEVEL);
    specialize("path", "*", Specialize.IGNOREDUPLICATE);
    specialize("port", "cluster-definition, server", Specialize.TOPLEVEL);
    specialize("read-only", "*", Specialize.IGNOREDUPLICATE);
    specialize("resource-env-ref", "*", Specialize.TOPLEVEL);
    specialize("resource-ref", "*", Specialize.TOPLEVEL);
    specialize("rcore:choose", "*", Specialize.TOPLEVEL);
    specialize("rcore:env", "*", Specialize.TOPLEVEL);
    specialize("rcore:if", "*", Specialize.TOPLEVEL);
    specialize("rcore:import", "*", Specialize.TOPLEVEL);
    specialize("rcore:include", "*", Specialize.TOPLEVEL);
    specialize("rcore:log", "*", Specialize.TOPLEVEL);
    specialize("rcore:message", "*", Specialize.TOPLEVEL);
    specialize("rcore:otherwise", "*", Specialize.TOPLEVEL);
    specialize("rcore:set", "*", Specialize.TOPLEVEL);
    specialize("rcore:type", "*", Specialize.TOPLEVEL);
    specialize("redeploy-mode", "*", Specialize.TOPLEVEL);
    specialize("redeploy-timeout", "*", Specialize.TOPLEVEL);
    specialize("redeploy-check-interval", "*", Specialize.TOPLEVEL);
    specialize("reference", "*", Specialize.TOPLEVEL);
    specialize("regexp", "*", Specialize.TOPLEVEL);
    specialize("resource", "*", Specialize.TOPLEVEL);
    specialize("rewrite-dispatch", "*", Specialize.TOPLEVEL);
    specialize("role-link", "*", Specialize.IGNOREDUPLICATE);
    specialize("role-name", "*", Specialize.IGNOREDUPLICATE);
    specialize("root-directory", "*", Specialize.IGNOREDUPLICATE);
    specialize("server", "resin", Specialize.TOPLEVEL);
    specialize("server-id", "resin", Specialize.TOPLEVEL);
    specialize("small-icon", "*", Specialize.TOPLEVEL);
    specialize("srun", "cluster-definition, cluster", Specialize.TOPLEVEL);
    specialize("startup-mode", "*", Specialize.TOPLEVEL);
    specialize("system-property", "*", Specialize.TOPLEVEL);
    specialize("web-app-default", "*", Specialize.MERGE, Specialize.TOPLEVEL);
    specialize("web-app-deploy", "*", Specialize.TOPLEVEL);
    specialize("web-app", "host", Specialize.TOPLEVEL);
    specialize("work-dir", "*", Specialize.IGNOREDUPLICATE);
    specialize("xml:lang", "*", Specialize.TOPLEVEL);
    specialize("xml:schemaLocation", "*", Specialize.IGNOREDUPLICATE);

/**

specialize("backup", "*", Specialize.IGNOREDUPLICATE); // Directive[port/backup]
specialize("backup", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/backup]
specialize("cache-size", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/cache-size]
specialize("cache-size", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-server/cache-size]
specialize("cache-timeout", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/cache-timeout]
specialize("cache-timeout", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-server/cache-timeout]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/message-destination/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/cmp-field/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/query/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/query/query-method/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/activation-config/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/message-destination-ref/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/relationships/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-ref/deployment-extension]
specialize("deployment-extension", "*", Specialize.IGNOREDUPLICATE); // Directive[resource-env-ref/deployment-extension]
specialize("ejb-class", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/ejb-class]
specialize("ejb-class", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/ejb-class]
specialize("ejb-class", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/ejb-class]
specialize("ejb-local-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/ejb-bean/ejb-local-ref]
specialize("ejb-local-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/ejb-local-ref]
specialize("ejb-local-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/ejb-local-ref]
specialize("ejb-local-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/ejb-local-ref]
specialize("ejb-local-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app-deploy/web-app/ejb-local-ref]
specialize("factory", "*", Specialize.IGNOREDUPLICATE); // Directive[jndi-link/factory]
specialize("factory", "*", Specialize.IGNOREDUPLICATE); // Directive[reference/factory]
specialize("format", "*", Specialize.IGNOREDUPLICATE); // Directive[access-log/format]
specialize("format", "*", Specialize.IGNOREDUPLICATE); // Directive[log/format]
specialize("group", "*", Specialize.IGNOREDUPLICATE); // Directive[port/group]
specialize("group", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/group]
specialize("home", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/home]
specialize("home", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-ref/home]
specialize("host", "*", Specialize.IGNOREDUPLICATE); // Directive[host]
specialize("host", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/host]
specialize("host", "*", Specialize.IGNOREDUPLICATE); // Directive[server/http/host]
specialize("host-name", "*", Specialize.IGNOREDUPLICATE); // Directive[host-deploy/host-name]
specialize("host-name", "*", Specialize.IGNOREDUPLICATE); // Directive[host/host-name]
specialize("index", "*", Specialize.IGNOREDUPLICATE); // Directive[port/index]
specialize("index", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/index]
specialize("jsp12:description", "*", Specialize.IGNOREDUPLICATE); // Directive[jsp12:taglib/jsp12:description]
specialize("jsp12:description", "*", Specialize.IGNOREDUPLICATE); // Directive[jsp12:taglib/jsp12:validator/jsp12:description]
specialize("jsp12:description", "*", Specialize.IGNOREDUPLICATE); // Directive[jsp12:taglib/jsp12:validator/jsp12:init-param/jsp12:description]
specialize("keepalive-max", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/keepalive-max]
specialize("keepalive-max", "*", Specialize.IGNOREDUPLICATE); // Directive[server/http/keepalive-max]
specialize("keepalive-max", "*", Specialize.IGNOREDUPLICATE); // Directive[server/keepalive-max]
specialize("lazy-init", "*", Specialize.IGNOREDUPLICATE); // Directive[ear-deploy/lazy-init]
specialize("lazy-init", "*", Specialize.IGNOREDUPLICATE); // Directive[host-deploy/lazy-init]
specialize("lazy-init", "*", Specialize.IGNOREDUPLICATE); // Directive[host/lazy-init]
specialize("lazy-init", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app-deploy/lazy-init]
specialize("listener", "*", Specialize.IGNOREDUPLICATE); // Directive[listener]
specialize("listener", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app-deploy/web-app/listener]
specialize("local", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/local]
specialize("local", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/local]
specialize("local-transaction-optimization", "*", Specialize.IGNOREDUPLICATE); // Directive[connection-factory/local-transaction-optimization]
specialize("local-transaction-optimization", "*", Specialize.IGNOREDUPLICATE); // Directive[host/local-transaction-optimization]
specialize("local-transaction-optimization", "*", Specialize.IGNOREDUPLICATE); // Directive[resin/local-transaction-optimization]
specialize("local-transaction-optimization", "*", Specialize.IGNOREDUPLICATE); // Directive[server/local-transaction-optimization]
specialize("max-idle-time", "*", Specialize.IGNOREDUPLICATE); // Directive[database/max-idle-time]
specialize("max-idle-time", "*", Specialize.IGNOREDUPLICATE); // Directive[server/persistent-store/max-idle-time]
specialize("mbean-interface", "*", Specialize.IGNOREDUPLICATE); // Directive[host/mbean-interface]
specialize("mbean-interface", "*", Specialize.IGNOREDUPLICATE); // Directive[resin/mbean-interface]
specialize("mbean-interface", "*", Specialize.IGNOREDUPLICATE); // Directive[server/mbean-interface]
specialize("message-destination", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/message-destination]
specialize("message-destination", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/message-destination]
specialize("message-destination", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app-deploy/web-app/message-destination]
specialize("message-destination-link", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/message-destination-ref/message-destination-link]
specialize("message-destination-link", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/message-destination-link]
specialize("message-destination-link", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/message-destination-ref/message-destination-link]
specialize("message-destination-name", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/message-destination/message-destination-name]
specialize("message-destination-name", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/message-destination/message-destination-name]
specialize("message-destination-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/message-destination-ref]
specialize("message-destination-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/ejb-bean/message-destination-ref]
specialize("message-destination-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/message-destination-ref]
specialize("message-destination-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/message-destination-ref]
specialize("message-destination-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/message-destination-ref]
specialize("message-destination-ref-name", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/message-destination-ref/message-destination-ref-name]
specialize("message-destination-ref-name", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/message-destination-ref/message-destination-ref-name]
specialize("message-destination-type", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/message-destination-ref/message-destination-type]
specialize("message-destination-type", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/message-destination-type]
specialize("message-destination-type", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/message-destination-ref/message-destination-type]
specialize("message-destination-usage", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/message-destination-ref/message-destination-usage]
specialize("message-destination-usage", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/message-destination-ref/message-destination-usage]
specialize("method", "*", Specialize.IGNOREDUPLICATE); // Directive[class-loader/enhancer/method]
specialize("method", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/container-transaction/method]
specialize("method", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/exclude-list/method]
specialize("method", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/method-permission/method]
specialize("method", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/method]
specialize("method-params", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/method-permission/method/method-params]
specialize("method-params", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/query/query-method/method-params]
specialize("password", "*", Specialize.IGNOREDUPLICATE); // Directive[database/password]
specialize("password", "*", Specialize.IGNOREDUPLICATE); // Directive[jsse-ssl/password]
specialize("password", "*", Specialize.IGNOREDUPLICATE); // Directive[openssl/password]
specialize("ping", "*", Specialize.IGNOREDUPLICATE); // Directive[database/ping]
specialize("ping", "*", Specialize.IGNOREDUPLICATE); // Directive[server/ping]
specialize("port", "*", Specialize.IGNOREDUPLICATE); // Directive[port]
specialize("port", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port]
specialize("port", "*", Specialize.IGNOREDUPLICATE); // Directive[server/http/port]
specialize("protocol", "*", Specialize.IGNOREDUPLICATE); // Directive[openssl/protocol]
specialize("protocol", "*", Specialize.IGNOREDUPLICATE); // Directive[port/protocol]
specialize("protocol", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/protocol]
specialize("read-timeout", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/read-timeout]
specialize("read-timeout", "*", Specialize.IGNOREDUPLICATE); // Directive[server/http/read-timeout]
specialize("remote", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/remote]
specialize("remote", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/remote]
specialize("remote", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-ref/remote]
specialize("require-file", "*", Specialize.IGNOREDUPLICATE); // Directive[ear-deploy/require-file]
specialize("require-file", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app-deploy/require-file]
specialize("resin-isolation", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/method/resin-isolation]
specialize("resin-isolation", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-server/resin-isolation]
specialize("security-identity", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/ejb-bean/security-identity]
specialize("security-identity", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/security-identity]
specialize("security-identity", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/security-identity]
specialize("security-identity", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/security-identity]
specialize("security-role", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/security-role]
specialize("security-role", "*", Specialize.IGNOREDUPLICATE); // Directive[host/application/security-role]
specialize("security-role", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app-deploy/web-app/security-role]
specialize("security-role-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/ejb-bean/security-role-ref]
specialize("security-role-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/security-role-ref]
specialize("security-role-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/security-role-ref]
specialize("security-role-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/security-role-ref]
specialize("server-id", "*", Specialize.IGNOREDUPLICATE); // Directive[port/server-id]
specialize("server-id", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/server-id]
specialize("server-id", "*", Specialize.IGNOREDUPLICATE); // Directive[server/http/server-id]
specialize("service-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/service-ref]
specialize("service-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/ejb-bean/service-ref]
specialize("service-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/service-ref]
specialize("service-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/service-ref]
specialize("service-ref", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/service-ref]
specialize("shareable", "*", Specialize.IGNOREDUPLICATE); // Directive[connection-factory/shareable]
specialize("shareable", "*", Specialize.IGNOREDUPLICATE); // Directive[host/shareable]
specialize("shareable", "*", Specialize.IGNOREDUPLICATE); // Directive[resin/shareable]
specialize("shareable", "*", Specialize.IGNOREDUPLICATE); // Directive[server/shareable]
specialize("socket-listen-backlog", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/socket-listen-backlog]
specialize("socket-listen-backlog", "*", Specialize.IGNOREDUPLICATE); // Directive[server/http/socket-listen-backlog]
specialize("static", "*", Specialize.IGNOREDUPLICATE); // Directive[class-loader/enhancer/class/static]
specialize("static", "*", Specialize.IGNOREDUPLICATE); // Directive[class-loader/enhancer/method/static]
specialize("stderr-log", "*", Specialize.IGNOREDUPLICATE); // Directive[host/stderr-log]
specialize("stderr-log", "*", Specialize.IGNOREDUPLICATE); // Directive[resin/stderr-log]
specialize("stderr-log", "*", Specialize.IGNOREDUPLICATE); // Directive[server/stderr-log]
specialize("stdout-log", "*", Specialize.IGNOREDUPLICATE); // Directive[host/stdout-log]
specialize("stdout-log", "*", Specialize.IGNOREDUPLICATE); // Directive[resin/stdout-log]
specialize("stdout-log", "*", Specialize.IGNOREDUPLICATE); // Directive[server/stdout-log]
specialize("tag", "*", Specialize.IGNOREDUPLICATE); // Directive[tag]
specialize("tag", "*", Specialize.IGNOREDUPLICATE); // Directive[taglib/tag]
specialize("taglib", "*", Specialize.IGNOREDUPLICATE); // Directive[taglib]
specialize("taglib", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app-deploy/web-app/taglib]
specialize("target", "*", Specialize.IGNOREDUPLICATE); // Directive[rewrite-dispatch/forward/target]
specialize("target", "*", Specialize.IGNOREDUPLICATE); // Directive[rewrite-dispatch/moved-permanently/target]
specialize("target", "*", Specialize.IGNOREDUPLICATE); // Directive[rewrite-dispatch/redirect/target]
specialize("tcp-no-delay", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/tcp-no-delay]
specialize("tcp-no-delay", "*", Specialize.IGNOREDUPLICATE); // Directive[server/http/tcp-no-delay]
specialize("temp-dir", "*", Specialize.IGNOREDUPLICATE); // Directive[host/temp-dir]
specialize("temp-dir", "*", Specialize.IGNOREDUPLICATE); // Directive[resin/temp-dir]
specialize("temp-dir", "*", Specialize.IGNOREDUPLICATE); // Directive[server/temp-dir]
specialize("trans-attribute", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/assembly-descriptor/container-transaction/trans-attribute]
specialize("trans-attribute", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/method/trans-attribute]
specialize("transaction-timeout", "*", Specialize.IGNOREDUPLICATE); // Directive[database/transaction-timeout]
specialize("transaction-timeout", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-server/transaction-timeout]
specialize("transaction-type", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/message-driven/transaction-type]
specialize("transaction-type", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/session/transaction-type]
specialize("transaction-type", "*", Specialize.IGNOREDUPLICATE); // Directive[persistence/persistence-unit/transaction-type]
specialize("type", "*", Specialize.IGNOREDUPLICATE); // Directive[authenticator/type]
specialize("type", "*", Specialize.IGNOREDUPLICATE); // Directive[connection-factory/type]
specialize("type", "*", Specialize.IGNOREDUPLICATE); // Directive[connector/message-listener/type]
specialize("type", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-server/bean/type]
specialize("type", "*", Specialize.IGNOREDUPLICATE); // Directive[resource/type]
specialize("type", "*", Specialize.IGNOREDUPLICATE); // Directive[server/persistent-store/type]
specialize("uri", "*", Specialize.IGNOREDUPLICATE); // Directive[taglib/uri]
specialize("uri", "*", Specialize.IGNOREDUPLICATE); // Directive[uri]
specialize("url-prefix", "*", Specialize.IGNOREDUPLICATE); // Directive[ear-deploy/url-prefix]
specialize("url-prefix", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app-deploy/url-prefix]
specialize("value", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/enterprise-beans/entity/cmp-field/sql-column/value]
specialize("value", "*", Specialize.IGNOREDUPLICATE); // Directive[persistence/persistence-unit/properties/property/value]
specialize("value", "*", Specialize.IGNOREDUPLICATE); // Directive[rcore:set/value]
specialize("var", "*", Specialize.IGNOREDUPLICATE); // Directive[host/var]
specialize("var", "*", Specialize.IGNOREDUPLICATE); // Directive[resin/var]
specialize("var", "*", Specialize.IGNOREDUPLICATE); // Directive[server/var]
specialize("verify-client", "*", Specialize.IGNOREDUPLICATE); // Directive[jsse-ssl/verify-client]
specialize("verify-client", "*", Specialize.IGNOREDUPLICATE); // Directive[openssl/verify-client]
specialize("version", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/version]
specialize("version", "*", Specialize.IGNOREDUPLICATE); // Directive[connector/version]
specialize("version", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/version]
specialize("version", "*", Specialize.IGNOREDUPLICATE); // Directive[persistence/version]
specialize("web-app", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app]
specialize("web-app", "*", Specialize.IGNOREDUPLICATE); // Directive[web-app-deploy/web-app]
specialize("write-timeout", "*", Specialize.IGNOREDUPLICATE); // Directive[server/cluster/port/write-timeout]
specialize("write-timeout", "*", Specialize.IGNOREDUPLICATE); // Directive[server/http/write-timeout]
specialize("xsi:schemaLocation", "*", Specialize.IGNOREDUPLICATE); // Directive[application-client/xsi:schemaLocation]
specialize("xsi:schemaLocation", "*", Specialize.IGNOREDUPLICATE); // Directive[connector/xsi:schemaLocation]
specialize("xsi:schemaLocation", "*", Specialize.IGNOREDUPLICATE); // Directive[ejb-jar/xsi:schemaLocation]
specialize("xsi:schemaLocation", "*", Specialize.IGNOREDUPLICATE); // Directive[persistence/xsi:schemaLocation]
*/


    _rootDirective = new RootDirective();
    _stack.addLast(_rootDirective);
  }

  private void specialize(String name, String parent, Specialize ... specializers)
  {
    for (Specialize specialize : specializers) {

      switch (specialize) {
        case TOPLEVEL:
          if (_topLevel.containsKey(name))
            throw new IllegalArgumentException(name);

          _topLevel.put(name, parent);

          break;

      case MERGE:
        if (_merge.containsKey(name))
          throw new IllegalArgumentException(name);

        _merge.put(name, parent);

        break;

      case IGNOREDUPLICATE:
        if (_ignoreDuplicates.containsKey(name))
          throw new IllegalArgumentException(name);

        _ignoreDuplicates.put(name, parent);

        break;

      default:
        throw new UnsupportedOperationException(String.valueOf(specialize));
      }
    }
  }

  public boolean isTopLevel(String name, Directive parent)
  {
    return isMatch(_topLevel, name, parent.getName());
  }

  public boolean isMerge(String name, Directive parent)
  {
    return isMatch(_merge, name, parent.getName());
  }

  public boolean isIgnoreDuplicate(String name, Directive parent)
  {
    return isMatch(_ignoreDuplicates, name, parent.getName());
  }

  private boolean isMatch(Map<String, String> map, String name, String parentName)
  {
    if (! map.containsKey(name))
      return false;

    String compare = map.get(name);

    if (compare == null)
      return false;

    if (compare.equals("*"))
      return true;

    if (name.length() == 0)
      return true;

    String[] split = compare.split("\\s*,\\s*");

    for (String parentNameCompare : split)
      if (parentName.endsWith(parentNameCompare))
        return true;

    return false;
  }

  public void setDestination(Path destination)
  {
    _destination = destination;
  }

  public void init()
  {
  }

  public void run()
  {
    try {
      runImpl();
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void runImpl()
    throws Exception
  {
    MergePath schemaPath = new MergePath();
    schemaPath.addClassPath();

    CompactVerifierFactoryImpl factory;
    factory = new CompactVerifierFactoryImpl();

    String[] schemaNames = new String[] {
      "com/caucho/server/resin/resin.rnc",
      "com/caucho/server/resin/server.rnc",
      "com/caucho/server/host/host.rnc",
      "com/caucho/server/webapp/resin-web-xml.rnc",
      "com/caucho/ejb/cfg/resin-ejb.rnc",
      "com/caucho/amber/cfg/persistence-30.rnc",
      "com/caucho/jca/jca.rnc",
      "com/caucho/jsp/cfg/jsp-tld.rnc",
      "com/caucho/server/e_app/app-client-14.rnc",
    };

    for (String schemaName : schemaNames) {
      Path path = schemaPath.lookup(schemaName);
      SchemaImpl schema = (SchemaImpl) factory.compileSchema(path);
      process(schema.getStartItem());
    }

    index(_rootDirective.getChildren());

    System.out.println("totalDirectiveCount: " + _totalDirectiveCount);

    System.out.println();
    System.out.println("DUPLICATE NAMES");
    System.out.println("===============");
    System.out.println();

    int duplicateCount = 0;

    for (Map.Entry<String, Set<Directive>> entry : _reverseIndex.entrySet()) {
      String name = entry.getKey();
      Set<Directive> directives = entry.getValue();

      if (directives.size() == 1)
        continue;

      for (Directive directive : directives) {

        if (directive instanceof DirectiveLink)
          continue;

        if (isIgnoreDuplicate(directive.getName(), directive.getParent()))
          continue;

        duplicateCount++;
        
        System.out.println("specialize(\"" + name + "\", \"*\", Specialize.IGNOREDUPLICATE); // " + directive);
      }
    }

    if (duplicateCount > 0) {
      System.out.println();
      System.out.println("" + duplicateCount + " duplicate(s) found, refusing to create files");
    }

    print(_rootDirective, duplicateCount == 0);
  }

  private void index(Collection<Directive> directives)
  {
    for (Directive directive : directives) {
      _totalDirectiveCount++;

      String name = directive.getName();

      Set<Directive> directivesForName = _reverseIndex.get(name);

      if (directivesForName == null) {
        directivesForName = new TreeSet<Directive>();
        _reverseIndex.put(name, directivesForName);
      }

      directivesForName.add(directive);

      index(directive.getChildren());
    }
  }

  private void print(Directive directive, boolean isCreateFiles)
  {
    Path path = _destination.lookup(directive.getPath());

    if (path.exists())
      System.out.println("skipping " + path);
    else if (!isCreateFiles) {
      System.out.println("SKIPPING  " + path);
    }
    else {
      System.out.println("WRITING  " + path);

      createFile(directive, path);
    }

    for (Directive child : directive.getChildren())
      print(child, isCreateFiles);
  }

  private void createFile(Directive directive, Path path)
  {
    // XXX:
  }

  private void process(Item item)
  {
    logFiner(item.toString());

    NameClassItem nameClassItem;

    if (item instanceof ElementItem) {
      nameClassItem = ((ElementItem) item).getNameClassItem();
    }
    else if (item instanceof AttributeItem) {
      nameClassItem = ((AttributeItem) item).getNameClassItem();
    }
    else
      nameClassItem = null;

    NameItem nameItem = null;

    if (nameClassItem instanceof NameItem)
      nameItem = (NameItem) nameClassItem;

    Directive directive = null;

    if (nameItem != null) {
      String name = nameItem.getQName().getName();

      if (name.equals("resource-default"))
        return;

      Directive parent = _stack.getLast();

      if (! name.equals(parent.getName())) {

        /**
        if (! parent.isTopLevel()) {
          Directive grandparent = parent.getParent();

          Directive newParent = _rootDirective.getChild(name);

          if (newParent == null) {
            newParent = new Directive(_rootDirective, parent);
            _rootDirective.addDirective(newParent);
          }

          grandparent.addDirectiveLink(newParent);

          parent = newParent;
          _stack.removeLast();
          _stack.addLast(parent);
        }
         */

        if (isTopLevel(name, parent)) {
          directive = _rootDirective.getChild(name);

          if (directive == null)
            directive = _rootDirective.addDirective(name);
        }
        else
          directive = parent.addDirective(name);
      }
    }

    if (_processed.containsKey(item))
      return;

    _processed.put(item, Boolean.TRUE);

    if (directive != null) {
      if (isMerge(directive.getName(), directive.getParent())) {
        directive.setMerge(true);
        return;
      }

      _stack.addLast(directive);
    }

    Iterator<Item> iterator = item.getItemsIterator();

    while (iterator.hasNext()) {
      if (++_recursionDepth > 200)
        throw new StackOverflowError("return, recursion > 200");

      process(iterator.next());

      _recursionDepth--;
    }

    if (directive != null)
      _stack.removeLast();
  }

  private void logFine(String msg)
  {
    logSystemOut(msg);
  }

  private void logFiner(String msg)
  {
    // logSystemOut(msg);
  }


  private void logSystemOut(String msg)
  {
    System.out.println(msg);
  }

  public static void main(String[] args)
  {
    DirectivesGenerator directivesGenerator = new DirectivesGenerator();

    int lastArgIndex = args.length - 1;

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      if (arg.equals("-d")
          || arg.startsWith("--d"))
      {
        if (i == lastArgIndex)
          throw new IllegalArgumentException(L.l("{0} requires an argument", arg));

        directivesGenerator.setDestination(Vfs.lookup(args[++i]));
      }
      else
        throw new IllegalArgumentException(L.l("unknown argument {0}", arg));
    }

    directivesGenerator.init();
    directivesGenerator.run();
  }

  private class Directive
    implements Comparable<Directive>
  {
    private Directive _parent;
    private String _name;
    private String _path;
    private int _depth;

    private boolean _isMerge;

    private LinkedHashMap<String, Directive> _childMap = new LinkedHashMap<String, Directive>();

    private Directive(Directive parent, String name)
    {
      init(parent, name);
    }

    public Directive(Directive parent, Directive copy)
    {
      init(parent, copy.getName());

      for (Directive directive : copy.getChildren())
        addDirective(directive);
    }

    private void init(Directive parent, String name)
    {
      _parent = parent;
      _name = name;

      if (_name == null)
        throw new IllegalArgumentException(L.l("`{0}' is required", "name"));

      if (parent != null)
      {
        String path = parent._path;

        if (path.length() > 0)
          _path = path + "/" + _name;
        else
          _path = _name;

        _depth = _parent._depth + 1;
      }
      else {
        _path = "";
        _depth = 0;
      }
    }

    public String getPath()
    {
      return _path;
    }

    public Directive getParent()
    {
      return _parent;
    }

    public int getDepth()
    {
      return _depth;
    }

    public Directive addDirective(String name)
    {
      Directive directive =  new Directive(this, name);

      return addDirective(directive);
    }

    protected Directive addDirective(Directive directive)
    {
     _childMap.put(directive.getName(), directive);

      return directive;
    }

    public Directive addDirectiveLink(Directive targetDirective)
    {
      Directive directive =  new DirectiveLink(this, targetDirective);

      return addDirective(directive);
    }

    public String getName()
    {
      return _name;
    }

    public Directive getChild(String name)
    {
      return _childMap.get(name);
    }

    public Collection<Directive> getChildren()
    {
      return new TreeSet<Directive>(_childMap.values());
    }

    public String toString()
    {
      StringBuilder sb =  new StringBuilder();

      sb.append("Directive[")
        .append(_path);

      sb.append("]");

      return sb.toString();
    }

    public boolean equals(Object o)
    {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final Directive directive = (Directive) o;

      return !(_path != null
               ? !_path.equals(directive._path)
               : directive._path != null);

    }

    public int hashCode()
    {
      return (_path != null ? _path.hashCode() : 0);
    }

    public int compareTo(Directive o)
    {
      return _path.compareTo(o._path);
    }

    public boolean isTopLevel()
    {
      return (_parent == null) || (_parent == _rootDirective);
    }

    public void setMerge(boolean isMerge)
    {
      _isMerge = isMerge;
    }

    public boolean isMerge()
    {
      return _isMerge;
    }
  }

  private class RootDirective
    extends Directive
  {
    public RootDirective()
    {
      super(null, "");
      init();
    }
  }

  private class DirectiveLink
    extends Directive
  {
    private final Directive _targetDirective;

    public DirectiveLink(Directive parent, Directive targetDirective)
    {
      super(parent, targetDirective.getName());

      _targetDirective = targetDirective;
    }

    public Directive addDirective(String name)
    {
      return _targetDirective.addDirective(name);
    }

    protected Directive addDirective(Directive directive)
    {
      return _targetDirective.addDirective(directive);
    }

    public Directive addDirectiveLink(Directive targetDirective)
    {
      return _targetDirective.addDirectiveLink(targetDirective);
    }

    public String toString()
    {
      return super.toString() + " --> " + _targetDirective;
    }
  }
}
