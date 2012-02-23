/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.server.webapp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.Bean;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterRegistration;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.MultipartConfigElement;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SessionCookieConfig;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.SchemaBean;
import com.caucho.config.el.CandiElResolver;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.SingletonBindingHandle;
import com.caucho.config.j2ee.PersistenceContextRefConfig;
import com.caucho.config.types.EjbLocalRef;
import com.caucho.config.types.EjbRef;
import com.caucho.config.types.InitParam;
import com.caucho.config.types.PathBuilder;
import com.caucho.config.types.Period;
import com.caucho.config.types.ResourceRef;
import com.caucho.config.types.Validator;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.manager.EjbModule;
import com.caucho.env.deploy.DeployContainer;
import com.caucho.env.deploy.DeployGenerator;
import com.caucho.env.deploy.DeployMode;
import com.caucho.env.deploy.EnvironmentDeployInstance;
import com.caucho.env.deploy.RepositoryDependency;
import com.caucho.i18n.CharacterEncoding;
import com.caucho.java.WorkDir;
import com.caucho.jsp.JspServlet;
import com.caucho.jsp.cfg.JspConfig;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.jsp.cfg.JspTaglib;
import com.caucho.jsp.el.JspApplicationContextImpl;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.enhancer.AbstractScanClass;
import com.caucho.loader.enhancer.ScanClass;
import com.caucho.loader.enhancer.ScanListener;
import com.caucho.make.AlwaysModified;
import com.caucho.make.DependencyContainer;
import com.caucho.management.server.HostMXBean;
import com.caucho.naming.Jndi;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.rewrite.DispatchRule;
import com.caucho.rewrite.IfSecure;
import com.caucho.rewrite.Not;
import com.caucho.rewrite.RedirectSecure;
import com.caucho.rewrite.RewriteFilter;
import com.caucho.rewrite.WelcomeFile;
import com.caucho.security.AbstractSingleSignon;
import com.caucho.security.Authenticator;
import com.caucho.security.BasicLogin;
import com.caucho.security.ClusterSingleSignon;
import com.caucho.security.Login;
import com.caucho.security.MemorySingleSignon;
import com.caucho.security.RoleMapManager;
import com.caucho.security.SingleSignon;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.ExceptionFilterChain;
import com.caucho.server.dispatch.FilterChainBuilder;
import com.caucho.server.dispatch.FilterConfigImpl;
import com.caucho.server.dispatch.FilterManager;
import com.caucho.server.dispatch.FilterMapper;
import com.caucho.server.dispatch.FilterMapping;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationBuilder;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.dispatch.ServletConfigImpl;
import com.caucho.server.dispatch.ServletManager;
import com.caucho.server.dispatch.ServletMapper;
import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.dispatch.ServletRegexp;
import com.caucho.server.dispatch.SubInvocation;
import com.caucho.server.dispatch.UrlMap;
import com.caucho.server.dispatch.VersionInvocation;
import com.caucho.server.host.Host;
import com.caucho.server.httpcache.AbstractProxyCache;
import com.caucho.server.log.AbstractAccessLog;
import com.caucho.server.log.AccessLog;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.security.ConstraintManager;
import com.caucho.server.security.LoginConfig;
import com.caucho.server.security.PermitEmptyRolesConstraint;
import com.caucho.server.security.SecurityConstraint;
import com.caucho.server.security.TransportConstraint;
import com.caucho.server.security.WebResourceCollection;
import com.caucho.server.session.SessionManager;
import com.caucho.server.util.CauchoSystem;
import com.caucho.server.webbeans.SessionContextContainer;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Resin's webApp implementation.
 */
@Configurable
@SuppressWarnings("serial")
public class UnknownWebApp extends WebApp
{
  /**
   * Creates the webApp with its environment loader.
   */
  UnknownWebApp(UnknownWebAppController controller)
  {
    super(controller);
  }
}
