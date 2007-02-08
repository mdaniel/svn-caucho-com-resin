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

package com.caucho.quercus.lib;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PHP functions implemented from the mail module
 */
public class MailModule extends AbstractQuercusModule {
  private static final Logger log =
    Logger.getLogger(MailModule.class.getName());

  private static final L10N L = new L10N(MailModule.class);

  /**
   * Send mail using JavaMail.
   */
  public static boolean mail(Env env,
                             String to,
                             String subject,
                             StringValue message,
                             @Optional String additionalHeaders,
                             @Optional String additionalParameters)
  {
    Transport smtp = null;

    try {
      Properties props = new Properties();
      
      StringValue host = env.getIni("SMTP");
      if (host != null && ! host.toString().equals(""))
        props.put("mail.smtp.host", host.toString());

      StringValue port = env.getIni("smtp_port");
      if (port != null && ! port.toString().equals(""))
        props.put("mail.smtp.port", port.toString());

      StringValue user = env.getIni("sendmail_from");
      if (user != null && ! user.toString().equals(""))
        props.put("mail.from", user.toString());

      String username = env.getIniString("smtp_username");
      String password = env.getIniString("smtp_password");

      if (password != null && ! "".equals(password))
	props.put( "mail.smtp.auth", "true");
 
      Session mailSession = Session.getInstance(props, null);
      smtp = mailSession.getTransport("smtp");

      MimeMessage msg = new MimeMessage(mailSession);
      msg.setSubject(subject);
      msg.setContent(message.toString(), "text/plain");

      ArrayList<Address> addrList;
      addrList = addRecipients(msg, Message.RecipientType.TO, to);

      if (additionalHeaders != null)
        addHeaders(msg, additionalHeaders);

      Address []from = msg.getFrom();
      if (from == null || from.length == 0) {
        log.fine(L.l("mail 'From' not set, setting to Java System property 'user.name'"));
        msg.setFrom();
      }

      msg.saveChanges();

      if (addrList.size() == 0)
        throw new QuercusModuleException(L.l("mail has no recipients"));

      from = msg.getFrom();
      log.fine(L.l("sending mail, From: {0}, To: {1}", from[0], to));

      if (password != null && ! "".equals(password))
        smtp.connect(username, password);
      else
        smtp.connect();

      Address[] addr = new Address[addrList.size()];
      addrList.toArray(addr);

      smtp.sendMessage(msg, addr);

      log.fine("quercus-mail: sent mail to " + to);

      return true;
    } catch (RuntimeException e) {
      log.log(Level.FINER, e.toString(), e);

      throw e;
    } catch (MessagingException e) {
      log.log(Level.FINE, e.toString(), e);

      env.warning(e.getMessage());

      return false;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      env.warning(e.toString());

      return false;
    } finally {
      try {
        if (smtp != null)
          smtp.close();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  private static ArrayList<Address> addRecipients(MimeMessage msg,
                                                  Message.RecipientType type,
                                                  String to)
    throws MessagingException
  {
    String []split = to.split("[ \t,<>]");

    ArrayList<Address> addresses = new ArrayList<Address>();

    for (int i = 0; i < split.length; i++) {
      if (split[i].indexOf('@') > 0) {
        Address addr = new InternetAddress(split[i]);

        addresses.add(addr);
        msg.addRecipient(type, addr);
      }
    }

    return addresses;
  }

  private static void addHeaders(MimeMessage msg, String headers)
    throws MessagingException
  {
    int i = 0;
    int len = headers.length();

    CharBuffer buffer = CharBuffer.allocate();

    while (true) {
      char ch;

      for (;
           i < len && Character.isWhitespace(headers.charAt(i));
           i++) {
      }

      if (len <= i)
        return;

      buffer.clear();

      for (;
           i < len && (! Character.isWhitespace(ch = headers.charAt(i)) &&
                       ch != ':');
           i++) {
        buffer.append((char) ch);
      }

      for (;
           i < len && ((ch = headers.charAt(i)) == ' ' ||
                        ch == '\t' ||
                        ch == '\f' ||
                        ch == ':');
           i++) {
      }

      String name = buffer.toString();
      buffer.clear();

      for (;
           i < len && ((ch = headers.charAt(i)) != '\r' &&
                       ch != '\n');
           i++) {
        buffer.append((char) ch);
      }

      String value = buffer.toString();

      if (name.equals("From"))
        msg.setFrom(new InternetAddress(value));
      else
        msg.addHeader(name, value);
    }
  }
}

