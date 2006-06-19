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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Date;

import javax.activation.DataHandler;

import javax.mail.event.ConnectionEvent;
import javax.mail.event.ConnectionListener;
import javax.mail.event.FolderEvent;
import javax.mail.event.FolderListener;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import javax.mail.search.SearchTerm;

// XXX: revisit once we have an SPI
/**
 * Represents a mail folder.
 */
public abstract class Folder {
  public static final int HOLDS_MESSAGES = 1;
  public static final int HOLDS_FOLDERS = 2;
  public static final int READ_ONLY = 1;
  public static final int READ_WRITE = 2;

  /**
   * The owning store.
   */
  protected Store store;

  /**
   * The open mode of the folder.
   */
  protected int mode;

  private transient ArrayList _connectionListeners = new ArrayList();
  private transient ArrayList _folderListeners = new ArrayList();
  private transient ArrayList _messageCountListeners = new ArrayList();
  private transient ArrayList _messageChangedListeners = new ArrayList();

  /**
   * Create a folder from the given store.
   */
  protected Folder(Store store)
  {
    this.store = store;
  }

  /**
   * Returns the folder's name.
   */
  public abstract String getName();

  /**
   * Returns the folder's full name.
   */
  public abstract String getFullName();

  /**
   * Returns the URL name of the folder.
   */
  public URLName getURLName()
    throws MessagingException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the owning store.
   */
  public Store getStore()
  {
    return this.store;
  }

  /**
   * Returns the parent folder.
   */
  public abstract Folder getParent()
    throws MessagingException;

  /**
   * Returns true if the folder physically exists.
   */
  public abstract boolean exists()
    throws MessagingException;

  /**
   * Returns the list of sub-folders matching the folder.
   */
  public abstract Folder []list(String pattern)
    throws MessagingException;

  /**
   * Returns a list of subscribed folders.
   */
  public Folder []listSubscribed(String pattern)
    throws MessagingException
  {
    Folder []list = list(pattern);

    ArrayList subscribedList = new ArrayList();

    for (int i = 0; i < list.length; i++) {
      if (list[i].isSubscribed())
	subscribedList.add(list[i]);
    }

    Folder []subscribed = new Folder[subscribedList.size()];

    subscribedList.toArray(subscribed);

    return subscribed;
  }

  /**
   * Returns a list of folders.
   */
  public Folder []list()
    throws MessagingException
  {
    return list("%");
  }

  /**
   * Returns a list of subscribed folders.
   */
  public Folder []listSubscribed()
    throws MessagingException
  {
    return listSubscribed("%");
  }

  /**
   * Returns the Folder delimited.
   */
  public abstract char getSeparator()
    throws MessagingException;

  /**
   * Returns the folder type.
   */
  public abstract int getType()
    throws MessagingException;

  /**
   * Creates the folder.
   */
  public abstract boolean create(int type)
    throws MessagingException;

  /**
   * Returns true if the folder is subscribed.
   */
  public boolean isSubscribed()
  {
    return true;
  }

  /**
   * Sets the subscribed.
   */
  public void setSubscribed(boolean subscribe)
    throws MessagingException
  {
    throw new MethodNotSupportedException(getClass().getName());
  }

  /**
   * Returns true if the folder has new messages since the indication
   * was set.
   */
  public abstract boolean hasNewMessages()
    throws MessagingException;

  /**
   * Returns the folder with the given name.
   */
  public abstract Folder getFolder(String name)
    throws MessagingException;

  /**
   * Deletes the folder.
   */
  public abstract boolean delete(boolean recurse)
    throws MessagingException;

  /**
   * Renames the folder.
   */
  public abstract boolean renameTo(Folder f)
    throws MessagingException;

  /**
   * Opens the folder.
   */
  public abstract void open(int mode)
    throws MessagingException;

  /**
   * Closes the folder.
   */
  public abstract void close(boolean expunged)
    throws MessagingException;

  /**
   * Returns true if the folder is open.
   */
  public abstract boolean isOpen();

  /**
   * Returns the mode of the folder.
   */
  public int getMode()
  {
    if (! isOpen())
      throw new IllegalStateException("Only open folders have a mode.");
    
    return this.mode;
  }

  /**
   * Returns the permanent flags supported by the folder.
   */
  public abstract Flags getPermanentFlags();

  /**
   * Returns the number of messages in the folder.
   */
  public abstract int getMessageCount()
    throws MessagingException;

  /**
   * Returns the number of new messages in the folder.
   */
  public int getNewMessageCount()
    throws MessagingException
  {
    Message []message = getMessages();

    int count = 0;

    for (int i = 0; i < message.length; i++) {
      if (message[i] != null && message[i].isSet(Flags.Flag.RECENT))
	count++;
    }

    return count;
  }

  /**
   * Returns the number of unread messages in the folder.
   */
  public int getUnreadMessageCount()
    throws MessagingException
  {
    Message []message = getMessages();

    int count = 0;

    for (int i = 0; i < message.length; i++) {
      if (message[i] != null && ! message[i].isSet(Flags.Flag.SEEN))
	count++;
    }

    return count;
  }

  /**
   * Returns the number of deleted messages in the folder.
   */
  public int getDeletedMessageCount()
    throws MessagingException
  {
    Message []message = getMessages();

    int count = 0;

    for (int i = 0; i < message.length; i++) {
      if (message[i] != null && ! message[i].isSet(Flags.Flag.DELETED))
	count++;
    }

    return count;
  }

  /**
   * Returns the messages with the given number.
   */
  public abstract Message getMessage(int msgnum)
    throws MessagingException;

  /**
   * Returns the given messages.
   */
  public Message []getMessages(int start, int end)
    throws MessagingException
  {
    int length = end - start + 1;
    
    Message []messages = new Message[length];

    for (int i = 0; i < length; i++)
      messages[i] = getMessage(start + i);

    return messages;
  }

  /**
   * Returns the message objects for the given numbers.
   */
  public Message []getMessages(int []msgnums)
    throws MessagingException
  {
    Message []messages = new Message[msgnums.length];

    for (int i = 0; i < msgnums.length; i++) {
      messages[i] = getMessage(i);
    }

    return messages;
  }

  /**
   * Returns all messages.
   */
  public Message []getMessages()
    throws MessagingException
  {
    return getMessages(1, getMessageCount());
  }

  /**
   * Adds new messages to the folder.
   */
  public abstract void appendMessages(Message []msgs)
    throws MessagingException;

  /**
   * Prefetch the messages.
   */
  public void fetch(Message []msgs, FetchProfile fp)
    throws MessagingException
  {
  }

  /**
   * Sets the flags for the messages.
   */
  public void setFlags(Message []msgs, Flags flag, boolean value)
    throws MessagingException
  {
    for (int i = 0; i < msgs.length; i++) {
      if (msgs[i] != null)
	msgs[i].setFlags(flag, value);
    }
  }

  /**
   * Sets the flags for the messages.
   */
  public void setFlags(int start, int end, Flags flag, boolean value)
    throws MessagingException
  {
    setFlags(getMessages(start, end), flag, value);
  }

  /**
   * Sets the flags for the messages.
   */
  public void setFlags(int []msgnums, Flags flag, boolean value)
    throws MessagingException
  {
    setFlags(getMessages(msgnums), flag, value);
  }

  /**
   * Copies the messages to another folder.
   */
  public void copyMessages(Message []messages, Folder folder)
    throws MessagingException
  {
    folder.appendMessages(messages);
  }

  /**
   * Permanently remove deleted messages.
   */
  public abstract Message []expunge()
    throws MessagingException;

  /**
   * Search for matching messages.
   */
  public Message []search(SearchTerm term)
    throws MessagingException
  {
    return search(term, getMessages());
  }

  /**
   * Search for matching messages.
   */
  public Message []search(SearchTerm term, Message []msgs)
    throws MessagingException
  {
    ArrayList matchList = new ArrayList();

    for (int i = 0; i < msgs.length; i++) {
      if (msgs[i] != null && msgs[i].match(term))
	matchList.add(msgs[i]);
    }

    Message []match = new Message[matchList.size()];

    matchList.toArray(match);

    return match;
  }

  /**
   * Adds a connection listener.
   */
  public void addConnectionListener(ConnectionListener listener)
  {
    _connectionListeners.add(listener);
  }

  /**
   * Removes a connection listener.
   */
  public void removeConnectionListener(ConnectionListener listener)
  {
    _connectionListeners.remove(listener);
  }

  /**
   * Notify all folder listeners of an event.
   */
  protected void notifyConnectionListeners(int type)
  {
    ConnectionEvent event = new ConnectionEvent(this, type);

    for (int i = 0; i < _connectionListeners.size(); i++) {
      event.dispatch(_connectionListeners.get(i));
    }
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
   * Notify all folder listeners of an event.
   */
  protected void notifyFolderListeners(int type)
  {
    FolderEvent event = new FolderEvent(this.store, this, type);

    for (int i = 0; i < _folderListeners.size(); i++) {
      event.dispatch(_folderListeners.get(i));
    }
  }

  /**
   * Notify renamed listeners about a renaming.
   */
  protected void notifyFolderRenamedListeners(Folder folder)
  {
    FolderEvent event = new FolderEvent(this.store, this,
					folder, FolderEvent.RENAMED);

    for (int i = 0; i < _folderListeners.size(); i++) {
      event.dispatch(_folderListeners.get(i));
    }
  }

  /**
   * Adds a MessageCount listener.
   */
  public void addMessageCountListener(MessageCountListener listener)
  {
    _messageCountListeners.add(listener);
  }

  /**
   * Removes a MessageCount listener.
   */
  public void removeMessageCountListener(MessageCountListener listener)
  {
    _messageCountListeners.remove(listener);
  }

  /**
   * Adds a MessageChanged listener.
   */
  public void addMessageChangedListener(MessageChangedListener listener)
  {
    _messageChangedListeners.add(listener);
  }

  /**
   * Removes a MessageChanged listener.
   */
  public void removeMessageChangedListener(MessageChangedListener listener)
  {
    _messageChangedListeners.remove(listener);
  }

  /**
   * Notifies MessageChanged listeners.
   */
  public void notifyMessageChangedListeners(int type, Message msg)
  {
    MessageChangedEvent event = new MessageChangedEvent(this, type, msg);

    for (int i = 0; i < _messageChangedListeners.size(); i++) {
      event.dispatch(_messageChangedListeners.get(i));
    }
  }
  // XXX: implement

  protected void finalize()
    throws Throwable
  {
    super.finalize();

    close(false);
  }

  public String toString()
  {
    String fullName = getFullName();

    if (fullName != null)
      return fullName;
    else
      return super.toString();
  }
}
