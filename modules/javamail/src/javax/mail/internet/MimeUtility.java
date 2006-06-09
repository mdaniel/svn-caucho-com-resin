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

/**
 * This is a utility class that provides various MIME related
 * functionality.
 *
 * There are a set of methods to encode and decode MIME headers as per
 * RFC 2047. A brief description on handling such headers is given
 * below:
 *
 * RFC 822 mail headers must contain only US-ASCII characters. Headers
 * that contain non US-ASCII characters must be encoded so that they
 * contain only US-ASCII characters. Basically, this process involves
 * using either BASE64 or QP to encode certain characters. RFC 2047
 * describes this in detail.
 *
 * In Java, Strings contain (16 bit) Unicode characters. ASCII is a
 * subset of Unicode (and occupies the range 0 - 127). A String that
 * contains only ASCII characters is already mail-safe. If the String
 * contains non US-ASCII characters, it must be encoded. An additional
 * complexity in this step is that since Unicode is not yet a widely
 * used charset, one might want to first charset-encode the String
 * into another charset and then do the transfer-encoding.
 *
 * Note that to get the actual bytes of a mail-safe String (say, for
 * sending over SMTP), one must do
 *
 * The setHeader and addHeader methods on MimeMessage and MimeBodyPart
 * assume that the given header values are Unicode strings that
 * contain only US-ASCII characters. Hence the callers of those
 * methods must insure that the values they pass do not contain non
 * US-ASCII characters. The methods in this class help do this.
 *
 * The getHeader family of methods on MimeMessage and MimeBodyPart
 * return the raw header value. These might be encoded as per RFC
 * 2047, and if so, must be decoded into Unicode Strings. The methods
 * in this class help to do this.
 *
 * Several System properties control strict conformance to the MIME
 * spec. Note that these are not session properties but must be set
 * globally as System properties.
 *
 * The mail.mime.decodetext.strict property controls decoding of MIME
 * encoded words. The MIME spec requires that encoded words start at
 * the beginning of a whitespace separated word. Some mailers
 * incorrectly include encoded words in the middle of a word. If the
 * mail.mime.decodetext.strict System property is set to "false", an
 * attempt will be made to decode these illegal encoded words. The
 * default is true.
 *
 * The mail.mime.encodeeol.strict property controls the choice of
 * Content-Transfer-Encoding for MIME parts that are not of type
 * "text". Often such parts will contain textual data for which an
 * encoding that allows normal end of line conventions is
 * appropriate. In rare cases, such a part will appear to contain
 * entirely textual data, but will require an encoding that preserves
 * CR and LF characters without change. If the
 * mail.mime.encodeeol.strict System property is set to "true", such
 * an encoding will be used when necessary. The default is false.
 *
 * In addition, the mail.mime.charset System property can be used to
 * specify the default MIME charset to use for encoded words and text
 * parts that don't otherwise specify a charset. Normally, the default
 * MIME charset is derived from the default Java charset, as specified
 * in the file.encoding System property. Most applications will have
 * no need to explicitly set the default MIME charset. In cases where
 * the default MIME charset to be used for mail messages is different
 * than the charset used for files stored on the system, this property
 * should be set.
 */
public class MimeUtility {

  public static final int ALL=-1;

  /**
   * Decode the given input stream. The Input stream returned is the
   * decoded input stream. All the encodings defined in RFC 2045 are
   * supported here. They include "base64", "quoted-printable",
   * "7bit", "8bit", and "binary". In addition, "uuencode" is also
   * supported.
   */
  public static InputStream decode(InputStream is, String encoding)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Decode "unstructured" headers, that is, headers that are defined
   * as '*text' as per RFC 822.
   *
   * The string is decoded using the algorithm specified in RFC 2047,
   * Section 6.1.1. If the charset-conversion fails for any sequence,
   * an UnsupportedEncodingException is thrown. If the String is not
   * an RFC 2047 style encoded header, it is returned as-is
   *
   * Example of usage:
   *
   * MimePart part = ... String rawvalue = null; String value = null; try 
   *
   * { if ((rawvalue = part.getHeader("X-mailer")[0]) != null) value =
   * MimeUtility.decodeText(rawvalue); } catch
   * (UnsupportedEncodingException e) { // Don't care value = rawvalue;
   * } catch (MessagingException me) { } return value;
  */
  public static String decodeText(String etext)
    throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * The string is parsed using the rules in RFC 2047 for parsing an
   * "encoded-word". If the parse fails, a ParseException is
   * thrown. Otherwise, it is transfer-decoded, and then
   * charset-converted into Unicode. If the charset-conversion fails,
   * an UnsupportedEncodingException is thrown.
   */
  public static String decodeWord(String eword)
    throws ParseException, UnsupportedEncodingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Wrap an encoder around the given output stream. All the encodings
   * defined in RFC 2045 are supported here. They include "base64",
   * "quoted-printable", "7bit", "8bit" and "binary". In addition,
   * "uuencode" is also supported.
   */
  public static OutputStream encode(OutputStream os, String encoding)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Wrap an encoder around the given output stream. All the encodings
   * defined in RFC 2045 are supported here. They include "base64",
   * "quoted-printable", "7bit", "8bit" and "binary". In addition,
   * "uuencode" is also supported. The filename parameter is used with
   * the "uuencode" encoding and is included in the encoded output.
   */
  public static OutputStream encode(OutputStream os, String encoding,
				    String filename) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Encode a RFC 822 "text" token into mail-safe form as per RFC 2047.
   *
   * The given Unicode string is examined for non US-ASCII
   * characters. If the string contains only US-ASCII characters, it
   * is returned as-is. If the string contains non US-ASCII
   * characters, it is first character-encoded using the platform's
   * default charset, then transfer-encoded using either the B or Q
   * encoding. The resulting bytes are then returned as a Unicode
   * string containing only ASCII characters.
   *
   * Note that this method should be used to encode only
   * "unstructured" RFC 822 headers.
   *
   * Example of usage:
   *
   * MimePart part = ... String rawvalue = "FooBar Mailer, Japanese
   * version 1.1" try
   *
   * { // If we know for sure that rawvalue contains only US-ASCII //
   * characters, we can skip the encoding part
   * part.setHeader("X-mailer", MimeUtility.encodeText(rawvalue)); }
   * catch (UnsupportedEncodingException e) { // encoding failure }
   * catch (MessagingException me) { // setHeader() failure }
  */
  public static String encodeText(String text)
    throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Encode a RFC 822 "text" token into mail-safe form as per RFC 2047.
   *
   * The given Unicode string is examined for non US-ASCII
   * characters. If the string contains only US-ASCII characters, it
   * is returned as-is. If the string contains non US-ASCII
   * characters, it is first character-encoded using the specified
   * charset, then transfer-encoded using either the B or Q
   * encoding. The resulting bytes are then returned as a Unicode
   * string containing only ASCII characters.
   *
   * Note that this method should be used to encode only
   * "unstructured" RFC 822 headers.
   */
  public static String encodeText(String text, String charset, String encoding)
    throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Encode a RFC 822 "word" token into mail-safe form as per RFC 2047.
   *
   * The given Unicode string is examined for non US-ASCII
   * characters. If the string contains only US-ASCII characters, it
   * is returned as-is. If the string contains non US-ASCII
   * characters, it is first character-encoded using the platform's
   * default charset, then transfer-encoded using either the B or Q
   * encoding. The resulting bytes are then returned as a Unicode
   * string containing only ASCII characters.
   *
   * This method is meant to be used when creating RFC 822
   * "phrases". The InternetAddress class, for example, uses this to
   * encode it's 'phrase' component.
   */
  public static String encodeWord(String word)
    throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Encode a RFC 822 "word" token into mail-safe form as per RFC 2047.
   *
   * The given Unicode string is examined for non US-ASCII
   * characters. If the string contains only US-ASCII characters, it
   * is returned as-is. If the string contains non US-ASCII
   * characters, it is first character-encoded using the specified
   * charset, then transfer-encoded using either the B or Q
   * encoding. The resulting bytes are then returned as a Unicode
   * string containing only ASCII characters.
   */
  public static String encodeWord(String word, String charset, String encoding)
    throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Fold a string at linear whitespace so that each line is no longer
   * than 76 characters, if possible. If there are more than 76
   * non-whitespace characters consecutively, the string is folded at
   * the first whitespace after that sequence. The parameter used
   * indicates how many characters have been used in the current line;
   * it is usually the length of the header name.
   *
   * Note that line breaks in the string aren't escaped; they probably
   * should be.
   */
  public static String fold(int used, String s)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the default charset corresponding to the system's current
   * default locale. If the System property mail.mime.charset is set,
   * a system charset corresponding to this MIME charset will be
   * returned.
   */
  public static String getDefaultJavaCharset()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the content-transfer-encoding that should be applied to the
   * input stream of this datasource, to make it mailsafe.
   *
   * The algorithm used here is: If the primary type of this
   * datasource is "text" and if all the bytes in its input stream are
   * US-ASCII, then the encoding is "7bit". If more than half of the
   * bytes are non-US-ASCII, then the encoding is "base64". If less
   * than half of the bytes are non-US-ASCII, then the encoding is
   * "quoted-printable". If the primary type of this datasource is not
   * "text", then if all the bytes of its input stream are US-ASCII,
   * the encoding is "7bit". If there is even one non-US-ASCII
   * character, the encoding is "base64".
   */
  public static String getEncoding(DataSource ds)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Convert a MIME charset name into a valid Java charset name.
   */
  public static String javaCharset(String charset)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Convert a java charset into its MIME charset name.
   * Note that a future version of JDK (post 1.2) might provide this functionality, in which case, we may deprecate this method then.
   */
  public static String mimeCharset(String charset)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * A utility method to quote a word, if the word contains any
   * characters from the specified 'specials' list.
   *
   * The HeaderTokenizer class defines two special sets of delimiters
   * - MIME and RFC 822.
   *
   * This method is typically used during the generation of RFC 822
   * and MIME header fields.
   */
  public static String quote(String word, String specials)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Unfold a folded header. Any line breaks that aren't escaped and
   * are followed by whitespace are removed.
   */
  public static String unfold(String s)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
