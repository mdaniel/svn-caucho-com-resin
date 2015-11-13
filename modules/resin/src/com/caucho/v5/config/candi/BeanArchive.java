package com.caucho.v5.config.candi;

import java.util.ArrayList;

import com.caucho.v5.vfs.Path;

public interface BeanArchive
{
  BeansConfig getBeansConfig();

  Path getRoot();

  void addClassName(String className);

  ArrayList<String> getClassNameList();

  boolean isScanComplete();

  void setScanComplete(boolean isScanComplete);

  DiscoveryMode getDiscoveryMode();

  boolean isClassExcluded(String className);

  boolean isImplicit();

  enum DiscoveryMode
  {
    NONE, ANNOTATED, ALL
  }
}
