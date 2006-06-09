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
import java.io.ObjectStreamException;

import java.util.Enumeration;
import java.util.Date;

import javax.activation.DataHandler;

import javax.mail.search.SearchTerm;

/**
 * This class models an email message. This is an abstract
 * class. Subclasses provide actual implementations.
 *
 * Message implements the Part interface. Message contains a set of
 * attributes and a "content". Messages within a folder also have a
 * set of flags that describe its state within the folder.
 *
 * Message defines some new attributes in addition to those defined in
 * the Part interface. These attributes specify meta-data for the
 * message - i.e., addressing and descriptive information about the
 * message.
 *
 * Message objects are obtained either from a Folder or by
 * constructing a new Message object of the appropriate
 * subclass. Messages that have been received are normally retrieved
 * from a folder named "INBOX".
 *
 * A Message object obtained from a folder is just a lightweight
 * reference to the actual message. The Message is 'lazily' filled up
 * (on demand) when each item is requested from the message. Note that
 * certain folder implementations may return Message objects that are
 * pre-filled with certain user-specified items. To send a message, an
 * appropriate subclass of Message (e.g., MimeMessage) is
 * instantiated, the attributes and content are filled in, and the
 * message is sent using the Transport.send method.
 *
 * See Also:Part
 */
public abstract class Message implements Part {

  /**
   * True if the message has been expunged.
   */
  protected boolean expunged;

  /**
   * Returns the message's owning folder.
   */
  protected Folder folder;

  /**
   * Returns the message's number.
   */
  protected int msgnum;

  /**
   * Returns the message's session.
   */
  protected Session session;

  /**
   * Creates a message.
   */
  protected Message()
  {
  }

  /**
   * Creates a message with a folder and an identifying number..
   */
  protected Message(Folder folder, int msgnum)
  {
    this.folder = folder;
    this.msgnum = msgnum;
  }

  /**
   * Creates a message with a session
   */
  protected Message(Session session)
  {
    this.session = session;
  }

  /**
   * Returns a from address.
   */
  public abstract Address []getFrom()
    throws MessagingException;

  /**
   * Sets the from attribute using mail.user.
   */
  public abstract void setFrom()
    throws MessagingException;

  /**
   * Sets the from attribute with an address.
   */
  public abstract void setFrom(Address address)
    throws MessagingException;

  /**
   * Adds new from addresses.
   */
  public abstract void addFrom(Address []addresses)
    throws MessagingException;

  /**
   * Returns the recipient addresses.
   */
  public abstract Address []getRecipients(RecipientType type)
    throws MessagingException;

  /**
   * Returns all the recipient addresses.
   */
  public Address []getAllRecipients()
    throws MessagingException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the recipients of the given type.
   */
  public abstract void setRecipients(RecipientType type,
				     Address []addresses)
    throws MessagingException;

  /**
   * Sets the recipient of the given type.
   */
  public void setRecipient(RecipientType type, Address address)
    throws MessagingException
  {
    setRecipients(type, new Address[] { address });
  }

  /**
   * Adds a set of recipients.
   */
  public abstract void addRecipients(RecipientType type,
				     Address []addresses)
    throws MessagingException;

  /**
   * Adds a single recipient.
   */
  public void addRecipient(RecipientType type,
			   Address address)
    throws MessagingException
  {
    addRecipients(type, new Address[] { address });
  }

  /**
   * Returns the reply-to addresses.
   */
  public Address []getReplyTo()
    throws MessagingException
  {
    return getFrom();
  }

  /**
   * Sets the reply-to address.
   */
  public void setReplyTo(Address []addresses)
    throws MessagingException
  {
    throw new MethodNotSupportedException(getClass().getName());
  }

  /**
   * Returns the message subject.
   */
  public abstract String getSubject()
    throws MessagingException;

  /**
   * Sets the subject.
   */
  public abstract void setSubject(String subject)
    throws MessagingException;

  /**
   * Gets the date the message was sent.
   */
  public abstract Date getSentDate()
    throws MessagingException;

  /**
   * Sets the date the message was sent.
   */
  public abstract void setSentDate(Date date)
    throws MessagingException;

  /**
   * Sets the date the message was received.
   */
  public abstract Date getReceivedDate()
    throws MessagingException;

  /**
   * Returns the message flags.
   */
  public abstract Flags getFlags()
    throws MessagingException;

  /**
   * Check if the flag is set.
   */
  public boolean isSet(Flags.Flag flag)
    throws MessagingException
  {
    return getFlags().contains(flag);
  }

  /**
   * Sets a given set of flags.
   */
  public abstract void setFlags(Flags flags, boolean set)
    throws MessagingException;

  /**
   * Sets a specific flag.
   */
  public void setFlag(Flags.Flag flag, boolean set)
    throws MessagingException
  {
    setFlags(new Flags(flag), set);
  }

  /**
   * Returns the message number.
   */
  public int getMessageNumber()
  {
    return this.msgnum;
  }

  /**
   * Sets the message number.
   */
  protected void setMessageNumber(int msgnum)
  {
    this.msgnum = msgnum;
  }

  /**
   * Returns the message folder.
   */
  public Folder getFolder()
  {
    return this.folder;
  }

  /**
   * Returns true if the message is expunged.
   */
  public boolean isExpunged()
  {
    return this.expunged;
  }

  /**
   * Sets the expunged flag.
   */
  protected void setExpunged(boolean expunged)
  {
    this.expunged = expunged;
  }

  /**
   * Generate a message as a reply-to.
   */
  public abstract Message reply(boolean replyToAll)
    throws MessagingException;

  /**
   * Saves changes to the message to the store.
   */
  public abstract void saveChanges()
    throws MessagingException;

  /**
   * Return true if the message matches the search.
   */
  public boolean match(SearchTerm search)
    throws MessagingException
  {
    return search.match(this);
  }

  protected Session getSession()
  {
    return this.session;
  }

  /**
   * This inner class defines the types of recipients allowed by the
   * Message class. The currently defined types are TO, CC and
   * BCC. Note that this class only has a protected constructor,
   * thereby restricting new Recipient types to either this class or
   * subclasses. This effectively implements an enumeration of the
   * allowed Recipient types. The following code sample shows how to
   * use this class to obtain the "TO" recipients from a message.
   *
   * See Also:Message.getRecipients(javax.mail.Message.RecipientType),
   * Message.setRecipients(javax.mail.Message.RecipientType,
   * javax.mail.Address[]),
   * Message.addRecipients(javax.mail.Message.RecipientType,
   * javax.mail.Address[]), Serialized Form
   */
  public static class RecipientType implements java.io.Serializable {
    public static final RecipientType BCC = new RecipientType("Bcc");
    public static final RecipientType CC = new RecipientType("Cc");
    public static final RecipientType TO = new RecipientType("To");
    
    protected String type;

    protected RecipientType(String type)
    {
      this.type = type;
    }

    protected Object readResolve()
      throws java.io.ObjectStreamException
    {
      if (this.type.equals(BCC.type))
	return BCC;
      else if (this.type.equals(CC.type))
	return CC;
      else if (this.type.equals(TO.type))
	return TO;
      else
	throw new IllegalStateException(this.type + " is an unknown RecipientType");
    }

    public String toString()
    {
      return "Recipient[" + this.type + "]";
    }
  }
}
