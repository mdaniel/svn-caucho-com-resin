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

package javax.mail.internet;
import javax.mail.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.net.InetAddress;

import java.util.Date;
import java.util.Enumeration;

import javax.activation.DataHandler;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.MessagingException;

/**
 * Represents a mime-type mail message.
 */
public class MimeMessage extends Message {
  protected byte []content;
  protected Flags flags;
  protected InternetHeaders headers = new InternetHeaders();
  protected boolean modified;
  protected boolean saved;

  /**
   * If the data for this message was supplied by an InputStream that
   * implements the SharedInputStream interface, contentStream is
   * another such stream representing the content of this message. In
   * this case, content will be null.  Since: JavaMail 1.2
   */
  protected InputStream contentStream;

  /**
   * The DataHandler object representing this Message's content.
   */
  protected DataHandler dh;

  
  /**
   * Creates a message with a session
   */
  public MimeMessage(Session session)
  {
    super(session);
  }

  /**
   * Constructs a MimeMessage by reading and parsing the data from the
   * specified MIME InputStream. The InputStream will be left
   * positioned at the end of the data for the message. Note that the
   * input stream parse is done within this constructor itself.  This
   * method is for providers subclassing MimeMessage.  folder - The
   * containing folder.is - the message input streammsgnum - Message
   * number of this message within its folder
   */
  protected MimeMessage(Folder folder, InputStream is, int msgnum)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructs an empty MimeMessage object with the given Folder and
   * message number.  This method is for providers subclassing
   * MimeMessage.
   */
  protected MimeMessage(javax.mail.Folder folder, int msgnum){
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructs a MimeMessage from the given InternetHeaders object
   * and content. This method is for providers subclassing
   * MimeMessage.  folder - The containing folder.headers - The
   * headerscontent - The message contentmsgnum - Message number of
   * this message within its folder
   */
  protected MimeMessage(Folder folder,
			InternetHeaders headers,
			byte[] content, int msgnum)
    throws javax.mail.MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructs a new MimeMessage with content initialized from the
   * source MimeMessage. The new message is independent of the
   * original.  Note: The current implementation is rather
   * inefficient, copying the data more times than strictly necessary.
   * source - the message to copy content from JavaMail 1.2
   */
  public MimeMessage(MimeMessage source) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructs a MimeMessage by reading and parsing the data from the
   * specified MIME InputStream. The InputStream will be left
   * positioned at the end of the data for the message. Note that the
   * input stream parse is done within this constructor itself.  The
   * input stream contains an entire MIME formatted message with
   * headers and data.  session - Session object for this messageis -
   * the message input stream
   */
  public MimeMessage(Session session, InputStream is) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns a from address.
   */
  public Address []getFrom()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the from attribute using mail.user.
   */
  public void setFrom()
    throws MessagingException
  {
    String user = null;

    if (user == null)
      user = this.session.getProperty("mail.from");

    if (user == null) {
      String userName = System.getProperty("user.name");
      String hostName = "localhost";

      try {
	InetAddress addr = InetAddress.getLocalHost();

	hostName = addr.getCanonicalHostName();
      } catch (Throwable e) {
      }

      user = userName + '@' + hostName;
    }

    addFrom(new Address[] { new InternetAddress(user) });
  }

  /**
   * Sets the from attribute using mail.user.
   */
  public void addFrom(Address []address)
    throws MessagingException
  {
    // XXX: is this correct?
    setFrom(address[0]);
  }

  /**
   * Adds new from addresses.
   */
  public void setFrom(Address address)
    throws MessagingException
  {
    InternetAddress addr;

    if (! (address instanceof InternetAddress))
	throw new MessagingException("expected internet address");

    addr = (InternetAddress) address;

    setHeader("From", addr.getAddress());
  }

  /**
   * Returns the recipient addresses.
   */
  public Address []getRecipients(RecipientType type)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the recipients of the given type.
   */
  public void setRecipients(RecipientType type, Address []addresses)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds a set of recipients.
   */
  public void addRecipients(RecipientType type, Address []addresses)
    throws MessagingException
  {
    for (int i = 0; i < addresses.length; i++) {
      InternetAddress addr;

      if (! (addresses[i] instanceof InternetAddress))
	throw new MessagingException("expected internet address");

      addr = (InternetAddress) addresses[i];

      if (type == RecipientType.TO) {
	addHeader("To", addr.getAddress());
      }
      else {
	addHeader("Unknown", addr.getAddress());
      }
    }
  }

  /**
   * Returns the message subject.
   */
  public String getSubject()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the subject.
   */
  public void setSubject(String subject)
    throws MessagingException
  {
    setHeader("Subject", subject);
  }

  /**
   * Gets the date the message was sent.
   */
  public Date getSentDate()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the date the message was sent.
   */
  public void setSentDate(Date date)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the date the message was received.
   */
  public Date getReceivedDate()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the message flags.
   */
  public Flags getFlags()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets a given set of flags.
   */
  public void setFlags(Flags flags, boolean set)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generate a message as a reply-to.
   */
  public Message reply(boolean replyToAll)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Saves changes to the message to the store.
   */
  public void saveChanges()
    throws MessagingException
  {
  }

  /**
   * Returns the size of the part in bytes.
   */
  public int getSize()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the number of lines of the part.
   */
  public int getLineCount()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the content-type of the part.
   */
  public String getContentType()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the part is of the specified mime-type.
   */
  public boolean isMimeType(String mimeType)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the disposition.
   */
  public String getDisposition()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the part's disposition.
   */
  public void setDisposition(String disposition)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the part's description.
   */
  public String getDescription()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the part's description.
   */
  public void setDescription(String description)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the part's filename.
   */
  public String getFileName()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the p art's filename.
   */
  public void setFileName(String filename)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns an input stream to the content.
   */
  public InputStream getInputStream()
    throws IOException, MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns a DataHandler for the part.
   */
  public DataHandler getDataHandler()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the content as an object.
   */
  public Object getContent()
    throws IOException, MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the handler for the part's content.
   */
  public void setDataHandler(DataHandler handler)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the part's content.
   */
  public void setContent(Object obj, String type)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the content as a text string.
   */
  public void setText(String text)
    throws MessagingException
  {
    // XXX: encoding
    
    this.content = text.getBytes();
  }

  /**
   * Sets the multipart-content as the content.
   */
  public void setContent(Multipart multipart)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Output a bytestream for the part.
   */
  public void writeTo(OutputStream os)
    throws IOException, MessagingException
  {
    writeTo(os, new String[0]);
  }

  /**
   * Output a bytestream for the part.
   */
  public void writeTo(OutputStream os, String []ignoreList)
    throws IOException, MessagingException
  {
    if (! this.saved)
      saveChanges();
    
    Enumeration e = this.headers.getNonMatchingHeaderLines(ignoreList);

    while (e.hasMoreElements()) {
      String line = (String) e.nextElement();

      os.write(line.getBytes());
      os.write('\r');
      os.write('\n');
    }
    
    os.write('\r');
    os.write('\n');

    if (this.content != null)
      os.write(this.content, 0, this.content.length);
  }

  /**
   * Returns the headers for the given name.
   */
  public String []getHeader(String headerName)
    throws MessagingException
  {
    return this.headers.getHeader(headerName);
  }

  /**
   * Sets the given header.
   */
  public void setHeader(String headerName,
			String headerValue)
    throws MessagingException
  {
    this.headers.setHeader(headerName, headerValue);
  }

  /**
   * Adds a header.
   */
  public void addHeader(String headerName,
			String headerValue)
    throws MessagingException
  {
    this.headers.addHeader(headerName, headerValue);
  }

  /**
   * Removes a header.
   */
  public void removeHeader(String headerName)
    throws MessagingException
  {
    this.headers.removeHeader(headerName);
  }

  /**
   * Returns a part's headers.
   */
  public Enumeration getAllHeaders()
    throws MessagingException
  {
    return this.headers.getAllHeaders();
  }

  /**
   * Returns matching headers.
   */
  public Enumeration getMatchingHeaders(String []headerNames)
    throws MessagingException
  {
    return this.headers.getMatchingHeaders(headerNames);
  }

  /**
   * Returns non-matching headers.
   */
  public Enumeration getNonMatchingHeaders(String []headerNames)
    throws MessagingException
  {
    return this.headers.getNonMatchingHeaders(headerNames);
  }

  /**
   * Returns a part's headers.
   */
  public Enumeration getAllHeaderLines()
    throws MessagingException
  {
    return this.headers.getAllHeaderLines();
  }

  /**
   * Returns matching headers.
   */
  public Enumeration getMatchingHeaderLines(String []headerNames)
    throws MessagingException
  {
    return this.headers.getMatchingHeaderLines(headerNames);
  }

  /**
   * Returns non-matching headers.
   */
  public Enumeration getNonMatchingHeaderLines(String []headerNames)
    throws MessagingException
  {
    return this.headers.getNonMatchingHeaderLines(headerNames);
  }
}
