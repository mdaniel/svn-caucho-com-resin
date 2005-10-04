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

package javax.mail.event;

/**
 * Represents a folder event.
 */
public class FolderEvent extends MailEvent {
  private static final int CREATED = 1;
  private static final int DELETED = 2;
  private static final int RENAMED = 3;

  protected transient Folder folder;
  protected transient  Folder newFolder;
  protected int type;

  public FolderEvent(Object source, Folder folder, int type)
  {
    super(source);

    this.folder = folder;
    this.type = type;
  }

  public FolderEvent(Object source, Folder folder, Folder newFolder, int type)
  {
    super(source);

    this.folder = folder;
    this.newFolder = newFolder;
    this.type = type;
  }

  /**
   * Returns the event folder.
   */
  public Folder getFolder()
  {
    return this.folder;
  }

  /**
   * Returns the event new folder.
   */
  public Folder getNewFolder()
  {
    return this.newFolder;
  }

  /**
   * Returns the event type.
   */
  public int getType()
  {
    return this.type;
  }

  /**
   * Dispatches the method.
   */
  public void dispatch(Object listenerObject)
  {
    FolderListener listener = (FolderListener) listenerObject;

    switch (this.type) {
    case CREATED:
      listener.folderCreated(this);
      break;
    case RENAMED:
      listener.folderRenamed(this);
      break;
    case DELETED:
      listener.folderDeleted(this);
      break;
    default:
      throw new UnsupportedOperationException(toString());
    }
  }

  public String toString()
  {
    switch (this.type) {
    case CREATED:
      return getClass().getName() + "[created]";
      
    case RENAMED:
      return getClass().getName() + "[renamed]";
      
    case DELETED:
      return getClass().getName() + "[deleted]";

    default:
      return super.toString();
    }
  }
}
