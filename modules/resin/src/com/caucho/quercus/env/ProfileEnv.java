/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import com.caucho.java.LineMap;
import com.caucho.java.ScriptStackTrace;
import com.caucho.java.WorkDir;
import com.caucho.loader.SimpleLoader;
import com.caucho.quercus.Location;
import com.caucho.quercus.ProQuercus;
import com.caucho.quercus.ProResinQuercus;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.profile.ProfileReport;
import com.caucho.quercus.profile.ProfileStore;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Profiling
 */
public class ProfileEnv extends Env {
  private static final L10N L = new L10N(ProfileEnv.class);
  private static final Logger log
    = Logger.getLogger(ProfileEnv.class.getName());

  private int _hashSize = 1 << 16;
  private int _hashMask = _hashSize - 1;
  
  private int _stackDepth = 8192;
  
  private ProfileEntry []_hashMap = new ProfileEntry[_hashSize];
  
  private ProfileEntry []_stack = new ProfileEntry[_stackDepth];
  private int _top;

  public ProfileEnv(ProResinQuercus quercus,
		            QuercusPage page,
		            WriteStream out,
		            HttpServletRequest request,
		            HttpServletResponse response)
  {
    super(quercus, page, out, request, response);
  }

  public ProfileEnv(ProQuercus quercus)
  {
    super(quercus);
  }

  /**
   * Returns the pro quercue
   */
  public ProResinQuercus getProQuercus()
  {
    return (ProResinQuercus) getQuercus();
  }

  /**
   * Initialization - return the profile functions
   */
  @Override
  protected AbstractFunction []getDefaultFunctionMap()
  {
    return getProQuercus().getProfileFunctionMap();
  }

  /**
   * Evaluates the top-level code in profiling mode
   *
   * @return the result
   */
  @Override
  public Value executePageTop(QuercusPage page)
  {
    _stack[0] = new ProfileEntry(0, 0, null);
    _top = 0;

    pushProfile(1);
      
    long startTime = System.nanoTime();
    
    try {
      QuercusPage profilePage = page.getProfilePage();

      if (profilePage != null)
	return profilePage.execute(this);
      else
	return page.execute(this);
    } finally {
      popProfile(System.nanoTime() - startTime);
      
      generateProfileReport();
    }
  }

  /**
   * Evaluates the top-level code in profiling mode
   *
   * @return the result
   */
  @Override
  public Value executePage(QuercusPage page)
  {
    QuercusPage profilePage = page.getProfilePage();

    if (profilePage != null)
      return profilePage.execute(this);
    else
      return page.execute(this);
  }

  /**
   * Initialize the page, loading any functions and classes
   */
  @Override
  protected QuercusPage pageInit(QuercusPage page)
  {
    QuercusPage profilePage = page.getProfilePage();

    if (profilePage != null)
      page = profilePage;
    else if (page.getCompiledPage() != null)
      page = page.getCompiledPage();
    
    page.init(this);

    page.importDefinitions(this);

    return page;
  }

  //
  // profiling
  //

  @Override
  public void pushProfile(int id)
  {
    ProfileEntry []stack = _stack;
    int top = _top;
    
    ProfileEntry parent = stack[top];
    int parentId = parent.getId();

    int hash = (id * 65521 + parentId) & _hashMask;

    ProfileEntry entry;

    for (entry = _hashMap[hash]; entry != null; entry = entry.getHashNext()) {
      if (entry.getId() == id && entry.getParentId() == parentId)
	break;
    }

    if (entry == null) {
      entry = new ProfileEntry(id, parentId, _hashMap[hash]);
      _hashMap[hash] = entry;
    }

    top += 1;
    stack[top] = entry;

    _top = top;
  }
  
  @Override
  public void popProfile(long nanos)
  {
    ProfileEntry []stack = _stack;
    int top = _top;
    
    ProfileEntry entry = _stack[top--];

    _top = top;

    entry.add(nanos);
  }

  protected void generateProfileReport()
  {
    ProfileReport report = createProfileReport();

    ProfileStore.addReport(report);

    /*
    try {
      WriteStream out = Vfs.openWrite("stdout:");

      report.printHotSpotReport(out);
      report.printHierarchyReport(out);

      out.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    */
  }

  protected ProfileReport createProfileReport()
  {
    long time = CurrentTime.getCurrentTime();

    long profileId = ProfileStore.generateId();
    ProfileReport report
      = new ProfileReport(profileId, getSelfPath().getPath(), time);

    for (ProfileEntry entry : _hashMap) {
      for (; entry != null; entry = entry.getHashNext()) {
        int id = entry.getId();
        int parentId = entry.getParentId();

        if (id == 0)
          continue;

        String name = getProQuercus().getProfileName(id);
        String parentName = getProQuercus().getProfileName(parentId);

        report.addItem(name, parentName,
                       entry.getCount(), entry.getNanos() / 1000);
      }
    }

    return report;
  }
}

