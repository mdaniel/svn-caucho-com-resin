/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
import javax.activation.*;
import javax.mail.*;
import java.io.*;
import java.util.*;

/**
 * This class represents a MIME body part.
 */
public class MimeBodyPart extends BodyPart implements MimePart {

  /**
   * Byte array that holds the bytes of the content of this Part.
   */
  protected byte[] content;

  /**
   * If the data for this body part was supplied by an InputStream
   * that implements the SharedInputStream interface, contentStream is
   * another such stream representing the content of this body
   * part. In this case, content will be null.  Since: JavaMail 1.2
   */
  protected InputStream contentStream;

  /**
   * The DataHandler object representing this Part's content.
   */
  protected DataHandler dh;

  /**
   * The InternetHeaders object that stores all the headers of this body part.
   */
  protected InternetHeaders headers = new InternetHeaders();

  /**
   * An empty MimeBodyPart object is created. This body part maybe
   * filled in by a client constructing a multipart message.
   */
  public MimeBodyPart()
  {
    this.content = null;
    this.contentStream = null;
    this.dh = null; /* XXX */;
  }

  /**
   * Constructs a MimeBodyPart by reading and parsing the data from
   * the specified input stream. The parser consumes data till the end
   * of the given input stream. The input stream must start at the
   * beginning of a valid MIME body part and must terminate at the end
   * of that body part.
   *
   * Note that the "boundary" string that delimits body parts must not
   * be included in the input stream. The intention is that the
   * MimeMultipart parser will extract each body part's bytes from a
   * multipart stream and feed them into this constructor, without the
   * delimiter strings.
   *
   * is - the body part Input Stream
   */
  public MimeBodyPart(InputStream is) throws MessagingException
  {
    try {
      // XXX: read headers off the top
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      byte[] buf = new byte[1024];

      int totalread = 0;

      while(true)
	{
	  int numread = is.read(buf, 0, buf.length);

	  if (numread == -1) {
	    content = baos.toByteArray();
	    break;
	  }

	  baos.write(buf, 0, numread);
	  totalread += numread;
	}

      // 
      if (is instanceof SharedInputStream) {
	SharedInputStream sis = (SharedInputStream)is;
	contentStream = sis.newStream(0, totalread);
      } else {
	contentStream = null;
      }
    }
    catch (IOException e) {
      throw new MessagingException(e.getMessage());
    }
  }

  /**
   * Constructs a MimeBodyPart using the given header and content
   * bytes.  Used by providers.
   *
   * headers - The header of this part
   * content - bytes representing the body of this part.
   */
  public MimeBodyPart(InternetHeaders headers, byte[] content)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Add this value to the existing values for this header_name. Note
   * that RFC 822 headers must contain only US-ASCII characters, so a
   * header that contains non US-ASCII characters must be encoded as
   * per the rules of RFC 2047.
   */
  public void addHeader(String name, String value)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Add a header line to this body part
   */
  public void addHeaderLine(String line) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Use the specified file to provide the data for this part. The
   * simple file name is used as the file name for this part and the
   * data in the file is used as the data for this part. The encoding
   * will be chosen appropriately for the file data.
   */
  public void attachFile(File file) throws IOException, MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Use the specified file to provide the data for this part. The
   * simple file name is used as the file name for this part and the
   * data in the file is used as the data for this part. The encoding
   * will be chosen appropriately for the file data.
   */
  public void attachFile(String file) throws IOException, MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get all header lines as an Enumeration of Strings. A Header line
   * is a raw RFC 822 header line, containing both the "name" and
   * "value" field.
   */
  public Enumeration getAllHeaderLines() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return all the headers from this Message as an Enumeration of
   * Header objects.
   */
  public Enumeration getAllHeaders() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the content as a java object. The type of the object
   * returned is of course dependent on the content itself. For
   * example, the native format of a text/plain content is usually a
   * String object. The native format for a "multipart" content is
   * always a Multipart subclass. For content types that are unknown
   * to the DataHandler system, an input stream is returned as the
   * content.
   *
   * This implementation obtains the content from the
   * DataHandler. That is, it invokes getDataHandler().getContent();
   * If the content is a Multipart or Message object and was created
   * by parsing a stream, the object is cached and returned in
   * subsequent calls so that modifications to the content will not be
   * lost.
   */
  public Object getContent() throws IOException, MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the value of the "Content-ID" header field. Returns null
   * if the field is unavailable or its value is absent.  This
   * implementation uses getHeader(name) to obtain the requisite
   * header field.
   */
  public String getContentID() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the languages specified in the Content-Language header of
   * this MimePart. The Content-Language header is defined by RFC
   * 1766. Returns null if this header is not available or its value
   * is absent.  This implementation uses getHeader(name) to obtain
   * the requisite header field.
   */
  public String[] getContentLanguage() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the value of the "Content-MD5" header field. Returns null
   * if this field is unavailable or its value is absent.  This
   * implementation uses getHeader(name) to obtain the requisite
   * header field.
   */
  public String getContentMD5() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Produce the raw bytes of the content. This method is used when
   * creating a DataHandler object for the content. Subclasses that
   * can provide a separate input stream for just the Part content
   * might want to override this method.
   */
  protected InputStream getContentStream() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the value of the RFC 822 "Content-Type" header
   * field. This represents the content type of the content of this
   * body part. This value must not be null. If this field is
   * unavailable, "text/plain" should be returned.  This
   * implementation uses getHeader(name) to obtain the requisite
   * header field.
   */
  public String getContentType() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return a DataHandler for this body part's content.  The
   * implementation provided here works just like the the
   * implementation in MimeMessage.
   */
  public DataHandler getDataHandler() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the "Content-Description" header field of this body
   * part. This typically associates some descriptive information with
   * this part. Returns null if this field is unavailable or its value
   * is absent.
   *
   * If the Content-Description field is encoded as per RFC 2047, it
   * is decoded and converted into Unicode. If the decoding or
   * conversion fails, the raw data is returned as is.
   *
   * This implementation uses getHeader(name) to obtain the requisite
   * header field.
   */
  public String getDescription() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the value of the "Content-Disposition" header field. This
   * represents the disposition of this part. The disposition
   * describes how the part should be presented to the user.
   *
   * If the Content-Disposition field is unavailable, null is returned.
   *
   * This implementation uses getHeader(name) to obtain the requisite
   * header field.
   */
  public String getDisposition() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the content transfer encoding from the
   * "Content-Transfer-Encoding" header field. Returns null if the
   * header is unavailable or its value is absent.
   *
   * This implementation uses getHeader(name) to obtain the requisite
   * header field.
   */
  public String getEncoding() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the filename associated with this body part.
   *
   * Returns the value of the "filename" parameter from the
   * "Content-Disposition" header field of this body part. If its not
   * available, returns the value of the "name" parameter from the
   * "Content-Type" header field of this body part. Returns null if
   * both are absent.
   *
   * If the mail.mime.encodefilename System property is set to true,
   * the MimeUtility.decodeText method will be used to decode the
   * filename. While such encoding is not supported by the MIME spec,
   * many mailers use this technique to support non-ASCII characters
   * in filenames. The default value of this property is false.
   */
  public String getFileName() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get all the headers for this header_name. Note that certain
   * headers may be encoded as per RFC 2047 if they contain non
   * US-ASCII characters and these should be decoded.
   */
  public String[] getHeader(String name) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get all the headers for this header name, returned as a single
   * String, with headers separated by the delimiter. If the delimiter
   * is null, only the first header is returned.
   */
  public String getHeader(String name, String delimiter)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return a decoded input stream for this body part's "content".
   *
   * This implementation obtains the input stream from the
   * DataHandler. That is, it invokes
   * getDataHandler().getInputStream();
   */
  public InputStream getInputStream() throws IOException, MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the number of lines for the content of this Part. Return
   * -1 if this number cannot be determined.
   *
   * Note that this number may not be an exact measure of the content
   * length and may or may not account for any transfer encoding of
   * the content.
   *
   * This implementation returns -1.
   */
  public int getLineCount() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get matching header lines as an Enumeration of Strings. A Header
   * line is a raw RFC 822 header line, containing both the "name" and
   * "value" field.
   */
  public Enumeration getMatchingHeaderLines(String[] names)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return matching headers from this Message as an Enumeration of
   * Header objects.
   */
  public Enumeration getMatchingHeaders(String[] names)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get non-matching header lines as an Enumeration of Strings. A
   * Header line is a raw RFC 822 header line, containing both the
   * "name" and "value" field.
   */
  public Enumeration getNonMatchingHeaderLines(String[] names)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return non-matching headers from this Message as an Enumeration
   * of Header objects.
   */
  public Enumeration getNonMatchingHeaders(String[] names)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return an InputStream to the raw data with any
   * Content-Transfer-Encoding intact. This method is useful if the
   * "Content-Transfer-Encoding" header is incorrect or corrupt, which
   * would prevent the getInputStream method or getContent method from
   * returning the correct data. In such a case the application may
   * use this method and attempt to decode the raw data itself.
   *
   * This implementation simply calls the getContentStream method.
   */
  public InputStream getRawInputStream() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the size of the content of this body part in bytes. Return
   * -1 if the size cannot be determined.
   *
   * Note that this number may not be an exact measure of the content
   * size and may or may not account for any transfer encoding of the
   * content.
   *
   * This implementation returns the size of the content array (if not
   * null), or, if contentStream is not null, and the available method
   * returns a positive number, it returns that number as the
   * size. Otherwise, it returns -1.
   */
  public int getSize() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Is this Part of the specified MIME type? This method compares
   * only the primaryType and subType. The parameters of the content
   * types are ignored.
   *
   * For example, this method will return true when comparing a Part
   * of content type "text/plain" with "text/plain; charset=foobar".
   *
   * If the subType of mimeType is the special character '*', then the
   * subtype is ignored during the comparison.
   */
  public boolean isMimeType(String mimeType) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Remove all headers with this name.
   */
  public void removeHeader(String name) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Save the contents of this part in the specified file. The content
   * is decoded and saved, without any of the MIME headers.
   */
  public void saveFile(File file) throws IOException, MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Save the contents of this part in the specified file. The content
   * is decoded and saved, without any of the MIME headers.
   */
  public void saveFile(String file) throws IOException, MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * This method sets the body part's content to a Multipart object.
   */
  public void setContent(Multipart mp) throws javax.mail.MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * A convenience method for setting this body part's content.
   *
   * The content is wrapped in a DataHandler object. Note that a
   * DataContentHandler class for the specified type should be
   * available to the JavaMail implementation for this to work
   * right. That is, to do setContent(foobar, "application/x-foobar"),
   * a DataContentHandler for "application/x-foobar" should be
   * installed. Refer to the Java Activation Framework for more
   * information.
   */
  public void setContent(Object o, String type) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the "Content-ID" header field of this body part. If the cid
   * parameter is null, any existing "Content-ID" is removed.
   */
  public void setContentID(String cid) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the Content-Language header of this MimePart. The
   * Content-Language header is defined by RFC 1766.
   */
  public void setContentLanguage(String[] languages) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the "Content-MD5" header field of this body part.
   */
  public void setContentMD5(String md5) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * This method provides the mechanism to set this body part's
   * content. The given DataHandler object should wrap the actual
   * content.
   */
  public void setDataHandler(DataHandler dh) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the "Content-Description" header field for this body part. If
   * the description parameter is null, then any existing
   * "Content-Description" fields are removed.
   *
   * If the description contains non US-ASCII characters, it will be
   * encoded using the platform's default charset. If the description
   * contains only US-ASCII characters, no encoding is done and it is
   * used as is.
   *
   * Note that if the charset encoding process fails, a
   * MessagingException is thrown, and an UnsupportedEncodingException
   * is included in the chain of nested exceptions within the
   * MessagingException.
   */
  public void setDescription(String description) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the "Content-Description" header field for this body part. If
   * the description parameter is null, then any existing
   * "Content-Description" fields are removed.
   *
   * If the description contains non US-ASCII characters, it will be
   * encoded using the specified charset. If the description contains
   * only US-ASCII characters, no encoding is done and it is used as
   * is.
   *
   * Note that if the charset encoding process fails, a
   * MessagingException is thrown, and an UnsupportedEncodingException
   * is included in the chain of nested exceptions within the
   * MessagingException.
   */
  public void setDescription(String description, String charset)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the "Content-Disposition" header field of this body part. If
   * the disposition is null, any existing "Content-Disposition"
   * header field is removed.
   */
  public void setDisposition(String disposition) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the filename associated with this body part, if possible.
   *
   * Sets the "filename" parameter of the "Content-Disposition" header
   * field of this body part. For compatibility with older mailers,
   * the "name" parameter of the "Content-Type" header is also set.
   *
   * If the mail.mime.encodefilename System property is set to true,
   * the MimeUtility.encodeText method will be used to encode the
   * filename. While such encoding is not supported by the MIME spec,
   * many mailers use this technique to support non-ASCII characters
   * in filenames. The default value of this property is false.
   */
  public void setFileName(String filename) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the value for this header_name. Replaces all existing header
   * values with this new value. Note that RFC 822 headers must
   * contain only US-ASCII characters, so a header that contains non
   * US-ASCII characters must be encoded as per the rules of RFC 2047.
   */
  public void setHeader(String name, String value) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Convenience method that sets the given String as this part's
   * content, with a MIME type of "text/plain". If the string contains
   * non US-ASCII characters, it will be encoded using the platform's
   * default charset. The charset is also used to set the "charset"
   * parameter.
   *
   * Note that there may be a performance penalty if text is large,
   * since this method may have to scan all the characters to
   * determine what charset to use.
   *
   * If the charset is already known, use the setText method that
   * takes the charset parameter.
   */
  public void setText(String text) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Convenience method that sets the given String as this part's
   * content, with a MIME type of "text/plain" and the specified
   * charset. The given Unicode string will be charset-encoded using
   * the specified charset. The charset is also used to set the
   * "charset" parameter.
   */
  public void setText(String text, String charset) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Convenience method that sets the given String as this part's
   * content, with a primary MIME type of "text" and the specified
   * MIME subtype. The given Unicode string will be charset-encoded
   * using the specified charset. The charset is also used to set the
   * "charset" parameter.
   */
  public void setText(String text, String charset, String subtype)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Examine the content of this body part and update the appropriate
   * MIME headers. Typical headers that get set here are Content-Type
   * and Content-Transfer-Encoding. Headers might need to be updated
   * in two cases: - A message being crafted by a mail application
   * will certainly need to activate this method at some point to fill
   * up its internal headers. - A message read in from a Store will
   * have obtained all its headers from the store, and so doesn't need
   * this. However, if this message is editable and if any edits have
   * been made to either the content or message structure, we might
   * need to resync our headers. In both cases this method is
   * typically called by the Message.saveChanges method.
   */
  protected void updateHeaders() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Output the body part as an RFC 822 format stream.
   */
  public void writeTo(OutputStream os) throws IOException, MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
