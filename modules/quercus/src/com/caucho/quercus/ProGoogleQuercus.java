/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus;

import com.caucho.java.WorkDir;
import com.caucho.loader.*;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.expr.ExprFactoryPro;
import com.caucho.quercus.lib.session.*;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.ProModuleContext;
import com.caucho.quercus.page.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.util.logging.*;
import javax.servlet.http.*;

/**
 * Facade for the PHP language.
 */
public class ProGoogleQuercus extends GoogleQuercus
{
  private static final L10N L = new L10N(ProGoogleQuercus.class);
  private static final Logger log
    = Logger.getLogger(ProGoogleQuercus.class.getName());

  private String _contextId;

  private ClassLoader _compileClassLoader;

  public ProGoogleQuercus()
  {
    super();
    
    EnvironmentClassLoader env = Environment.getEnvironmentClassLoader();
    
    if (env != null)
      _contextId = env.getId();

    if (_contextId == null)
      _contextId = "default";
  }
  
  /**
   * Returns true if this is the Professional version.
   */
  @Override
  public boolean isPro()
  {
    return true;
  }

  /**
   * Enables profiling mode
   */
  @Override
  public boolean isProfile()
  {
    return false;
    
    //return _isProfile;
  }
  
  @Override
  public String getVersion()
  {
    return "Pro " + QuercusVersion.getVersionNumber();
  }

  public Env createEnv(QuercusPage page,
                       WriteStream out,
                       HttpServletRequest request,
                       HttpServletResponse response)
  {
    //if (_profileProbability > 0
	//    && RandomUtil.nextDouble() <= _profileProbability)
    //  return new ProfileEnv(this, page, out, request, response);
    //else
    return new ProGoogleEnv(this, page, out, request, response);
  }
  
  /**
   * Creates the professional module context.
   */
  @Override
  protected ModuleContext createModuleContext(ModuleContext parent,
                                              ClassLoader loader)
  {
    return new ProModuleContext(parent, loader);
  }

  @Override
  protected PageManager createPageManager()
  {
    return new ProGooglePageManager(this);
  }

  @Override
  protected QuercusSessionManager createSessionManager()
  {
    //return new ProSessionManager(_contextId);
    
    return super.createSessionManager();
  }

  /**
   * Returns the compile classloader
   */
  public ClassLoader getCompileClassLoader()
  {
    if (_compileClassLoader == null) {
      _compileClassLoader = SimpleLoader.create(WorkDir.getLocalWorkDir());
    }
    
    return _compileClassLoader;
  }

  /**
   * Sets the compile classloader
   */
  @Override
  public void setCompileClassLoader(ClassLoader loader)
  {
    _compileClassLoader = loader;
  }

  @Override
  public ExprFactory createExprFactory()
  {
    return new ExprFactoryPro();
  }
}

