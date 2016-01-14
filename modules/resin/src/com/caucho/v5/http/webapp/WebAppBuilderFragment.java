/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.JarPath;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * Builder to manage the webapp fragments.
 */
public class WebAppBuilderFragment
{
  private static final L10N L = new L10N(WebAppBuilderFragment.class);
  private static final Logger log
    = Logger.getLogger(WebAppBuilderFragment.class.getName());
  
  private final WebAppBuilder _builder;
  
  private Ordering _absoluteOrdering;
  private List<WebAppFragmentConfig> _webFragments;
  private boolean _isApplyingWebFragments = false;
  
  /**
   * Builder Creates the webApp with its environment loader.
   */
  public WebAppBuilderFragment(WebAppBuilder builder)
  {
    Objects.requireNonNull(builder);
    
    _builder = builder;
  }
  
  private WebAppBuilder getBuilder()
  {
    return _builder;
  }
  
  public void addBuilderProgram(ConfigProgram program)
  {
    WebAppBuilder builder = getBuilder();

    boolean isFragment = builder.isFragment();
    
    try {
      builder.setFragment(true);
      
      program.configure(builder);
    } finally {
      builder.setFragment(isFragment);
    }
  }

  public void initWebFragments()
  {
    loadWebFragments();

    List<WebAppFragmentConfig> fragments = sortWebFragments();
    
    _webFragments = fragments;

    _isApplyingWebFragments = true;

    for (WebAppConfig fragment : fragments) {
      addBuilderProgram(fragment.getBuilderProgram()); // .configure(this);
    }

    _isApplyingWebFragments = false;
  }

  public boolean isApplyingWebFragments()
  {
    return _isApplyingWebFragments;
  }

  public Ordering createAbsoluteOrdering()
  {
    if (_absoluteOrdering == null)
      _absoluteOrdering = new Ordering();

    return _absoluteOrdering;
  }

  private void loadWebFragments()
  {
    if (_webFragments == null) {
      _webFragments = new ArrayList<WebAppFragmentConfig>();
    }

    try {
      Enumeration<URL> fragments
        = getBuilder().getClassLoader().getResources("META-INF/web-fragment.xml");

      /* XXX:
      ConfigXml config = new ConfigXml();
      config.setEL(getBuilder().isServletAllowEL());

      if (log.isLoggable(Level.FINER) && fragments.hasMoreElements())
        log.finer(L.l("loading web-fragments ({0})", this));

      while (fragments.hasMoreElements()) {
        URL url = fragments.nextElement();

        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER,
            L.l("Loading web-fragment '{0}:{1}'.", this, url));
        }

        WebAppFragmentConfig fragmentConfig = new WebAppFragmentConfig();
        
        config.configure(fragmentConfig, Vfs.lookup(url.toString()));
        
        fragmentConfig.setRootPath(getRoot(Vfs.lookup(url.toString()),
                                           "META-INF/web-fragment.xml"));

        _webFragments.add(fragmentConfig);
      }
      */
    } catch (IOException e) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE,
          L.l("'{0}' error '{1}' loading fragments.", this, e), e);
    }
  }
  
  private PathImpl getRoot(PathImpl path, String name)
  {
    if (path instanceof JarPath) {
      return ((JarPath) path).getContainer();
    }
    else {
      String url = path.getURL();
      
      return VfsOld.lookup(url.substring(0, url.length() - name.length()));
    }
  }

  private List<WebAppFragmentConfig> sortWebFragments() 
  {
    Map<String, WebAppFragmentConfig> namedFragments = new HashMap<>();

    List<WebAppFragmentConfig> anonFragments = new ArrayList<>();

    for (WebAppFragmentConfig fragment : _webFragments) {
      if (fragment.getName() != null)
        namedFragments.put(fragment.getName(), fragment);
      else
        anonFragments.add(fragment);
    }

    if (_absoluteOrdering != null) {
      return getWebFragments(null,
                             _absoluteOrdering,
                             namedFragments,
                             anonFragments);
    } 
    else {
      List<WebAppFragmentConfig> source = new ArrayList<>();
      List<WebAppFragmentConfig> result = new ArrayList<>();
      
      Set<String> names = getOrderingNames(_webFragments);
      
      source.addAll(_webFragments);
      
      sortFragments(names, source, result);
      
      return result;
    }
  }
  
  private Set<String> getOrderingNames(List<WebAppFragmentConfig> fragments)
  {
    Set<String> names = new HashSet<String>();
    
    for (WebAppFragmentConfig fragment : fragments) {
      Ordering ordering = fragment.getOrdering();
      
      if (ordering != null) {
        addOrderingNames(names, fragment.getName(), ordering.getBefore());
        addOrderingNames(names, fragment.getName(), ordering.getAfter());
      }
    }
    
    return names;
  }
  
  private void addOrderingNames(Set<String> names,
                                String selfName,
                                Ordering ordering)
  {
    if (ordering == null)
      return;
    
    for (Object order : ordering.getOrder()) {
      if (order instanceof String) {
        names.add((String) order);
      }
      else if (ordering.isOthers(order)) {
        if (selfName != null) {
          names.add(selfName);
        }
      }
    }
  }
  
  
  private void sortFragments(Set<String> names,
                             List<WebAppFragmentConfig> sourceList,
                             List<WebAppFragmentConfig> resultList)
  {
    while (sourceList.size() > 0) {
      int sourceSize = sourceList.size();
      
      for (int i = 0; i < sourceList.size(); i++) {
        WebAppFragmentConfig source = sourceList.get(i);

        if (isFragmentInsertable(source, names, sourceList, resultList)) {
          resultList.add(source);
          sourceList.remove(source);
        }
      }
      
      if (sourceList.size() == sourceSize) {
        throw new ConfigException(L.l("web-fragments at '{0}' appear to have circular dependency. Consider using <absolute-ordering> in web.xml.\n  {1}", 
                                      this,
                                      sourceList));
        
      }
    }
  }
  
  private boolean isFragmentInsertable(WebAppFragmentConfig source,
                                       Set<String> names,
                                       List<WebAppFragmentConfig> sourceList,
                                       List<WebAppFragmentConfig> resultList)
  {
    if (isBeforeOrderingPresent(source, names, sourceList)) {
      return false;
    }
    
    if (! isBeforeOrderingValid(source, names, sourceList, resultList)) {
      return false;
    }
    
    if (! isAfterOrderingValid(source, names, sourceList, resultList)) {
      return false;
    }

    return true;
  }
  
  /*
  private boolean isAllOrderingPresent(List<WebAppFragmentConfig> list,
                                       Ordering ordering)
  {
    if (ordering == null)
      return true;
    
    for (Object key : ordering.getOrder()) {
      if (key instanceof String) {
        String keyName = (String) key;
        
        if (! isFragmentInList(list, keyName)) {
          return false;
        }
      }
    }

    return true;
  }
  */
  
  private boolean isBeforeOrderingValid(WebAppFragmentConfig source,
                                        Set<String> names,
                                        List<WebAppFragmentConfig> sourceList,
                                        List<WebAppFragmentConfig> resultList)
  {
    for (WebAppFragmentConfig fragment : resultList) {
      Ordering ordering = fragment.getOrdering();
      
      if (ordering != null) {
        Ordering before = ordering.getBefore();
        
        if (! isBeforeOrderingValid(before, source, names, 
                                    sourceList, resultList)) {
          return false;
        }
      }
    }
    
    return true;
  }
  
  private boolean isBeforeOrderingPresent(WebAppFragmentConfig source,
                                          Set<String> names,
                                          List<WebAppFragmentConfig> sourceList)
  {
    for (WebAppFragmentConfig fragment : sourceList) {
      if (fragment == source)
        continue;
      
      Ordering ordering = fragment.getOrdering();
      
      if (ordering != null) {
        Ordering before = ordering.getBefore();
        
        if (isBeforeOrderingPresent(before, source, names, sourceList)) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  private boolean isBeforeOrderingValid(Ordering before,
                                        WebAppFragmentConfig source,
                                        Set<String> names,
                                        List<WebAppFragmentConfig> sourceList,
                                        List<WebAppFragmentConfig> resultList)
  {
    if (before == null)
      return true;
    
    String name = source.getName();
    
    boolean isOthers = false;
    
    for (Object order : before.getOrder()) {
      if (name != null && name.equals(order)) {
        return ! isOthers;
      }
      
      if (before.isOthers(order)) {
        if (isOther(name, names)) {
          return true;
        }
        
        isOthers = isAnyOther(names, sourceList);
      }
      else if (order instanceof String) {
        String key = (String) order;
        
        if (! isFragmentInList(resultList, key))
          return false;
      }
    }
    
    return true;
  }
  
  private boolean isAfterOrderingValid(WebAppFragmentConfig source,
                                       Set<String> names,
                                       List<WebAppFragmentConfig> sourceList,
                                       List<WebAppFragmentConfig> resultList)
  {
    Ordering ordering = source.getOrdering();
    
    if (ordering == null)
      return true;
    
    Ordering after = ordering.getAfter();
    
    if (after == null)
      return true;
    
    for (Object order : after.getOrder()) {
      if (after.isOthers(order)) {
        if (isAnyOther(names, sourceList))
          return false;
      }
      else if (order instanceof String) {
        String key = (String) order;
        
        if (! isFragmentInList(resultList, key))
          return false;
      }
    }
    
    return true;
  }
  
  private boolean isFragmentInList(List<WebAppFragmentConfig> list,
                                   String keyName)
  {
    for (WebAppFragmentConfig config : list) {
      if (keyName.equals(config.getName()))
        return true;
    }
    
    return false;
  }
  
  private boolean isOther(String name, Set<String> names)
  {
    return name == null || ! names.contains(name);
  }
  
  private boolean isBeforeOrderingPresent(Ordering before,
                                          WebAppFragmentConfig source,
                                          Set<String> names,
                                          List<WebAppFragmentConfig> sourceList)
  {
    if (before == null)
      return false;
    
    String name = source.getName();
    
    for (Object subOrder : before.getOrder()) {
      if (name != null && name.equals(subOrder))
        return true;
      
      if (before.isOthers(subOrder)) {
        if (isOther(name, names))
          return true;
      }
    }
    
    return false;
  }
  
  /*
  private boolean isOrderingPresent2(Ordering order,
                                    String name,
                                    Set<String> names,
                                    List<WebAppFragmentConfig> sourceList)
  {
    for (Object subOrder : order.getOrder()) {
      if (name != null && name.equals(subOrder))
        return true;
      
      if (order.isOthers(subOrder)) {
        if (isOther(name, names))
          return true;
        
        // If an others fragment is still in the source list, add it.
        if (isAnyOther(names, sourceList)) {
          return false;
        }
      }
    }
    
    return false;
  }
  */
  
  private boolean isAnyOther(Set<String> names,
                             List<WebAppFragmentConfig> sourceList)
  {
    for (WebAppFragmentConfig fragment : sourceList) {
      if (isOther(fragment.getName(), names)) {
        return true;
      }
    }
    
    return false;
  }
  
  /*
  private boolean isAnyOther(String selfName,
                             Set<String> names,
                             List<WebAppFragmentConfig> sourceList)
  {
    for (WebAppFragmentConfig fragment : sourceList) {
      if (selfName != null && selfName.equals(fragment.getName())) {
        // server/1r20
        continue;
      }
      
      if (isOther(fragment.getName(), names)) {
        return true;
      }
    }
    
    return false;
  }
  */
  
  /*
      Map<WebAppFragmentConfig, Set<WebAppFragmentConfig>> parentsMap
        = new HashMap<WebAppFragmentConfig, Set<WebAppFragmentConfig>>();

      for (WebAppFragmentConfig config : namedFragments.values()) {
        if (config.getOrdering() == null)
          continue;

        List<WebAppFragmentConfig> children
          = getWebFragments(config,
                            config.getOrdering().getBefore(),
                            namedFragments,
                            anonFragments);

        List<WebAppFragmentConfig> parents
          = getWebFragments(config,
                            config.getOrdering().getAfter(),
                            namedFragments,
                            anonFragments);

        if (children != null && parents != null) {
          for (WebAppFragmentConfig fragmentConfig : children) {
            if (parents.contains(fragmentConfig))
              throw new ConfigException(L.l(
                "web-fragment '{0}' specifies conflicting ordering in its before and after tags. Consider using <absolute-ordering> in web.xml",
                config));
          }
        }


        if (children != null) {
          for (WebAppFragmentConfig child : children) {
            Set<WebAppFragmentConfig> parentSet = parentsMap.get(child);

            if (parentSet == null)
              parentSet = new HashSet<WebAppFragmentConfig>();

            parentSet.add(config);

            parentsMap.put(child, parentSet);
          }
        }

        if (parents != null) {
          Set<WebAppFragmentConfig> parentsSet = parentsMap.get(config);

          if (parentsSet == null)
            parentsSet = new HashSet<WebAppFragmentConfig>();

          parentsSet.addAll(parents);
          parentsMap.put(config, parentsSet);
        }
      }

      List<WebAppFragmentConfig> result = new ArrayList<WebAppFragmentConfig>();

      while (! parentsMap.isEmpty()) {
        int resultSize = 0;

        Set<WebAppFragmentConfig> removeSet
          = new HashSet<WebAppFragmentConfig>();

        for (
          Iterator<Map.Entry<WebAppFragmentConfig, Set<WebAppFragmentConfig>>>
            entries = parentsMap.entrySet().iterator(); entries.hasNext();) {
          Map.Entry<WebAppFragmentConfig, Set<WebAppFragmentConfig>> entry
            = entries.next();
          for (Iterator<WebAppFragmentConfig> iterator
            = entry.getValue().iterator(); iterator.hasNext();) {
            WebAppFragmentConfig config = iterator.next();

            if (result.contains(config))
              iterator.remove();
            else if (parentsMap.get(config) == null
              || parentsMap.get(config).isEmpty()) {
              result.add(config);
              removeSet.remove(config);
            }
          }

          if (entry.getValue().isEmpty())
            entries.remove();
        }

        for (WebAppFragmentConfig config : removeSet) {
          parentsMap.remove(config);
        }

        if (result.size() == resultSize)
          throw new ConfigException(L.l("web-fragments at '{0}' appear to have circular dependency. Consider using <absolute-ordering> in web.xml.", this));
      }

      for (WebAppFragmentConfig config : namedFragments.values()) {
        if (! result.contains(config))
          result.add(config);
      }

      if (anonFragments.size() > 0)
        result.addAll(anonFragments);

      return result;
    }
  }
  */

  private List<WebAppFragmentConfig> 
  getWebFragments(final WebAppFragmentConfig config,
                  Ordering ordering,
                  Map<String, WebAppFragmentConfig> namedFragments,
                  List<WebAppFragmentConfig> anonFragments)
  {
    if (ordering == null) {
      return null;
    }

    Map<String, WebAppFragmentConfig> others = new HashMap<>(namedFragments);

    if (config != null) {
      others.remove(config.getName());
    }

    ArrayList<WebAppFragmentConfig> result = new ArrayList<>();

    int othersIdx = -1;

    for (Object key : ordering.getOrder()) {
      if (key instanceof String) {
        WebAppFragmentConfig fragmentConfig = others.remove(key);

        if (fragmentConfig != null) {
          result.add(fragmentConfig);
        }
      } 
      else if (ordering.isOthers(key)) {
        othersIdx = result.size();
      }
    }

    if (othersIdx > -1) {
      // result.ensureCapacity(result.size() + others.size());

      result.addAll(othersIdx, others.values());
      result.addAll(anonFragments);

      anonFragments.clear();
    }

    return result;
  }

  public List<WebAppFragmentConfig> getFragments()
  {
    return _webFragments;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getBuilder().getId() + "]";
  }
}
