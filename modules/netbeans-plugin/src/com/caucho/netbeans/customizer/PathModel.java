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


package com.caucho.netbeans.customizer;

import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;

import javax.swing.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * Path list model, supports adding, removing and moving URLs in the list.
 */
public class PathModel
  extends AbstractListModel
{

  private List data;

  public PathModel(List<URL> data/*, int type*/)
  {
    this.data = data;
  }

  public int getSize()
  {
    return data.size();
  }

  public Object getElementAt(int index)
  {
    URL url = (URL) data.get(index);
    if ("jar".equals(url.getProtocol())) { // NOI18N
      URL fileURL = FileUtil.getArchiveFile(url);
      if (FileUtil.getArchiveRoot(fileURL).equals(url)) {
        // really the root
        url = fileURL;
      }
      else {
        // some subdir, just show it as is
        return url.toExternalForm();
      }
    }
    if ("file".equals(url.getProtocol())) { // NOI18N
      File f = new File(URI.create(url.toExternalForm()));
      return f.getAbsolutePath();
    }
    else {
      return url.toExternalForm();
    }
  }

  public void removePath(int[] indices)
  {
    for (int i = indices.length - 1; i >= 0; i--) {
      data.remove(indices[i]);
    }
    fireIntervalRemoved(this, indices[0], indices[indices.length - 1]);
  }

  public void moveUpPath(int[] indices)
  {
    for (int i = 0; i < indices.length; i++) {
      Object p2 = data.get(indices[i]);
      Object p1 = data.set(indices[i] - 1, p2);
      data.set(indices[i], p1);
    }
    fireContentsChanged(this, indices[0] - 1, indices[indices.length - 1]);
  }

  public void moveDownPath(int[] indices)
  {
    for (int i = indices.length - 1; i >= 0; i--) {
      Object p1 = data.get(indices[i]);
      Object p2 = data.set(indices[i] + 1, p1);
      data.set(indices[i], p2);
    }
    fireContentsChanged(this, indices[0], indices[indices.length - 1] + 1);
  }

  public boolean addPath(File f)
  {
    try {
      URL url = f.toURI().toURL();
      return this.addPath(url);
    }
    catch (MalformedURLException mue) {
      return false;
    }
  }

  public boolean addPath(URL url)
  {
    if (FileUtil.isArchiveFile(url)) {
      url = FileUtil.getArchiveRoot(url);
    }
    else if (!url.toExternalForm().endsWith("/")) { // NOI18N
      try {
        url = new URL(url.toExternalForm() + "/"); // NOI18N
      }
      catch (MalformedURLException mue) {
        ErrorManager.getDefault().notify(mue);
      }
    }
    int oldSize = data.size();
    data.add(url);
    fireIntervalAdded(this, oldSize, oldSize);
    return true;
  }

  public List getData()
  {
    return data;
  }
}