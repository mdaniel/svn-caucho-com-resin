/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus;

import com.caucho.config.ConfigException;
import com.caucho.java.WorkDir;
import com.caucho.license.*;
import com.caucho.loader.*;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.expr.ExprFactoryPro;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.lib.session.*;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.ProModuleContext;
import com.caucho.quercus.page.*;
import com.caucho.quercus.program.UndefinedFunction;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.util.HashMap;
import java.util.logging.*;
import javax.servlet.http.*;

/**
 * Facade for the PHP language.
 */
public class ProQuercus extends QuercusContext implements ProfileQuercus
{
  private static final L10N L = new L10N(ProQuercus.class);
  private static final Logger log
    = Logger.getLogger(ProQuercus.class.getName());

  private String _contextId;

  private ClassLoader _compileClassLoader;

  private HashMap<String,Integer> _profileNameToIndexMap
    = new HashMap<String,Integer>();

  private HashMap<Integer,String> _profileIndexToNameMap
    = new HashMap<Integer,String>();
  
  private AbstractFunction []_profileFunctionMap
    = new AbstractFunction[256];

  private boolean _isProfile;
  private double _profileProbability;

  public ProQuercus()
  {
    EnvironmentClassLoader env = Environment.getEnvironmentClassLoader();
    
    if (env != null)
      _contextId = env.getId();

    if (_contextId == null)
      _contextId = "default";

    _profileNameToIndexMap.put("__undefined__", 0);
    _profileIndexToNameMap.put(0, "__undefined__");

    _profileNameToIndexMap.put("__top__", 1);
    _profileIndexToNameMap.put(1, "__top__");
  }
  
  public static ProQuercus create()
  {
    try {
      Class cl = Class.forName("com.caucho.license.LicenseCheckImpl");
      LicenseCheck license = (LicenseCheck) cl.newInstance();

      license.requirePersonal(1);
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      throw new ConfigException(L.l("Compiled Quercus a valid license.\nSee http://www.caucho.com for information on licensing."));
    }

    return new ProQuercus();
  }
  
  @Override
  public String getVersion()
  {
    return "Pro " + QuercusVersion.getVersionNumber();
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

  /**
   * Set profiling mode
   */
  @Override
  public void setProfileProbability(double probability)
  {
    _profileProbability = probability;
    
    _isProfile = probability > 0;
  }

  public int getProfileIndex(String name)
  {
    synchronized (_profileNameToIndexMap) {
      Integer index = _profileNameToIndexMap.get(name);

      if (index != null)
	return index;

      index = _profileNameToIndexMap.size();
      _profileNameToIndexMap.put(name, index);
      _profileIndexToNameMap.put(index, name);

      return index;
    }
  }

  public String getProfileName(int index)
  {
    synchronized (_profileNameToIndexMap) {
      return _profileIndexToNameMap.get(index);
    }
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
      return new ProEnv(this, page, out, request, response);
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
    return new ProPageManager(this);
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
    if (_compileClassLoader == null)
      _compileClassLoader = SimpleLoader.create(WorkDir.getLocalWorkDir());
    
    /*
    if (_compileClassLoader == null) {
      _compileClassLoader
        = SimpleLoader.create(getPwd().lookup("WEB-INF/work"));
    }
    */
                                                
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
  protected void extendFunctionMap(String name, int id)
  {
    super.extendFunctionMap(name, id);
    
    synchronized (_functionNameMap) {
      if (_profileFunctionMap.length <= id) {
        AbstractFunction []functionMap = new AbstractFunction[id + 256];
        System.arraycopy(_profileFunctionMap, 0,
                         functionMap, 0, _profileFunctionMap.length);
        _profileFunctionMap = functionMap;
      }
      
      int globalId = -1;
      
      int ns = name.lastIndexOf('\\');
      
      if (ns > 0) {
        globalId = getFunctionId(name.substring(ns + 1));
      }

      _profileFunctionMap[id] = new UndefinedFunction(id, name, globalId);
    }
  }

  public int setFunction(String name, AbstractFunction fun)
  {
    int id = super.setFunction(name, fun);

    int index = getProfileIndex(name);
    ProfileFunction profFun = new ProfileFunction(fun, index);
    
    synchronized (_functionNameMap) {
      _profileFunctionMap[id] = profFun;
    }

    return id;
  }

  /**
   * Returns the profiling functions
   */
  public AbstractFunction []getProfileFunctionMap()
  {
    return _profileFunctionMap;
  }
  
  @Override
  public ExprFactory createExprFactory()
  {
    return new ExprFactoryPro();
  }
}

