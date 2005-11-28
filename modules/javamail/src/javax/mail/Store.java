/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package javax.mail;

import java.util.ArrayList;

import javax.mail.event.StoreListener;
import javax.mail.event.StoreEvent;
import javax.mail.event.FolderListener;
import javax.mail.event.FolderEvent;

/**
 * Represents a mail store
 */
public abstract class Store extends Service {
  private transient ArrayList _storeListeners
    = new ArrayList();
  
  private transient ArrayList _folderListeners
    = new ArrayList();
  
  public Store(Session session, URLName urlName)
  {
    super(session, urlName);
  }

  /**
   * Returns the root namespace for the user.
   */
  public abstract Folder getDefaultFolder()
    throws MessagingException;

  /**
   * Returns the folder with the given name.
   */
  public abstract Folder getFolder(String name)
    throws MessagingException;

  /**
   * Returns the folder with the given name.
   */
  public abstract Folder getFolder(URLName url)
    throws MessagingException;

  /**
   * Returns the personal namespaces.
   */
  public Folder []getPersonalNamespaces()
    throws MessagingException
  {
    return new Folder[] { getDefaultFolder() };
  }

  /**
   * Returns the user's namespaces.
   */
  public Folder []getUserNamespaces(String user)
    throws MessagingException
  {
    return new Folder[0];
  }

  /**
   * Returns shared namespaces.
   */
  public Folder []getSharedNamespaces()
    throws MessagingException
  {
    return new Folder[0];
  }

  /**
   * Adds a store listener.
   */
  public void addStoreListener(StoreListener listener)
  {
    _storeListeners.add(listener);
  }

  /**
   * Removes a store listener.
   */
  public void removeStoreListener(StoreListener listener)
  {
    _storeListeners.remove(listener);
  }

  /**
   * Adds a folder listener.
   */
  public void addFolderListener(FolderListener listener)
  {
    _folderListeners.add(listener);
  }

  /**
   * Removes a folder listener.
   */
  public void removeFolderListener(FolderListener listener)
  {
    _folderListeners.remove(listener);
  }

  /**
   * Notify the store listeners.
   */
  protected void notifyStoreListeners(int type, String message)
  {
    StoreEvent event = new StoreEvent(this, type, message);

    for (int i = 0; i < _storeListeners.size(); i++) {
      event.dispatch(_storeListeners.get(i));
    }
  }

  /**
   * Notify the folder listeners.
   */
  protected void notifyFolderListeners(int type, Folder folder)
  {
    FolderEvent event = new FolderEvent(this, folder, type);

    for (int i = 0; i < _folderListeners.size(); i++) {
      event.dispatch(_folderListeners.get(i));
    }
  }

  /**
   * Notify the folder listeners.
   */
  protected void notifyFolderRenamedListeners(Folder oldFolder,
					      Folder newFolder)
  {
    FolderEvent event = new FolderEvent(this, oldFolder, newFolder,
					FolderEvent.RENAMED);

    for (int i = 0; i < _folderListeners.size(); i++) {
      event.dispatch(_folderListeners.get(i));
    }
  }
}
