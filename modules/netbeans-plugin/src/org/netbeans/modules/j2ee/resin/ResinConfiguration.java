/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package org.netbeans.modules.j2ee.resin;

import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.model.XpathEvent;
import javax.enterprise.deploy.model.XpathListener;
import javax.enterprise.deploy.spi.DConfigBeanRoot;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.exceptions.BeanNotFoundException;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;


public class ResinConfiguration
  implements DeploymentConfiguration, XpathListener
{

  private DeployableObject deplObj;

  public ResinConfiguration(DeployableObject deplObj)
  {
    this.deplObj = deplObj;
  }

  public void init(File file)
  {
    try {
      FileObject folder = FileUtil.toFileObject(file.getParentFile());
      if (folder == null) {
        ErrorManager.getDefault()
          .log(ErrorManager.INFORMATIONAL,
               "The parent folder does not exist!"); // NOI18N
        return;
      }
      PrintWriter pw = null;
      FileLock lock = null;
      try {
        String name = file.getName();
        FileObject fo = folder.getFileObject(name);
        if (fo == null) {
          fo = folder.createData(name);
        }
        lock = fo.lock();
        pw = new PrintWriter(new OutputStreamWriter(fo.getOutputStream(lock)));
        pw.println("<Resin path=\"/mypath\"/>"); // NOI18N
      }
      finally {
        if (pw != null) {
          pw.close();
        }
        if (lock != null) {
          lock.releaseLock();
        }
      }
    }
    catch (IOException ex) {
      ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
    }

    // web.xml represented as DDBean model
    DDBeanRoot root = deplObj.getDDBeanRoot();
    if (root != null) {
      // here we will listen to resource reference changes
      root.addXpathListener("/web-app/resource-ref", this); // NOI18N
    }
  }

  public String getContextPath()
    throws ConfigurationException
  {
    // TODO: replace this with reading the context path from the server specific DD
    return "/mypath";
  }

  public void setContextPath(String contextPath)
    throws ConfigurationException
  {
    // TODO: here put the code that will store the context path in the server specific DD
  }

  // XpathListener implementation -------------------------------------------

  public void fireXpathEvent(XpathEvent xpe)
  {
    DDBean eventDDBean = xpe.getBean();
    if ("/web-app/resource-ref".equals(eventDDBean.getXpath())) { // NIO18N
      // new resource reference added
      if (xpe.isAddEvent()) {
        String[] name = eventDDBean.getText("res-ref-name"); // NOI18N
        String[] type = eventDDBean.getText("res-type");     // NOI18N
        String[] auth = eventDDBean.getText("res-auth");     // NOI18N
        // TODO: take appropriate steps here
      }
    }
  }

  // JSR-88 methods ---------------------------------------------------------

  public DeployableObject getDeployableObject()
  {
    return deplObj;
  }

  public void save(OutputStream os)
    throws ConfigurationException
  {
  }

  public DConfigBeanRoot getDConfigBeanRoot(DDBeanRoot dDBeanRoot)
    throws ConfigurationException
  {
    return null;
  }

  public void removeDConfigBean(DConfigBeanRoot dConfigBeanRoot)
    throws BeanNotFoundException
  {
  }

  public void restore(InputStream is)
    throws ConfigurationException
  {
  }

  public DConfigBeanRoot restoreDConfigBean(InputStream is,
                                            DDBeanRoot dDBeanRoot)
    throws ConfigurationException
  {
    return null;
  }

  public void saveDConfigBean(OutputStream os, DConfigBeanRoot dConfigBeanRoot)
    throws ConfigurationException
  {
  }
}
