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

package com.caucho.quercus.lib;

import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.MessagingException;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.caucho.util.L10N;

import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Env;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

/**
 * PHP functions implemented from the mail module
 */
public class QuercusMailModule extends AbstractQuercusModule {
  private static final Logger log =
    Logger.getLogger(QuercusMailModule.class.getName());
  
  private static final L10N L = new L10N(QuercusMailModule.class);

  public static boolean mail(Env env,
			     String to,
			     String subject,
			     String message,
			     @Optional String additionalHeaders,
			     @Optional String additionalParameters)
  {
    Transport smtp = null;

    System.out.println("MAILING: " + to);

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

      Session mailSession = Session.getInstance(props, null);
      smtp = mailSession.getTransport("smtp");

      MimeMessage msg = new MimeMessage(mailSession);
      msg.setFrom();
      msg.setSubject(subject);
      msg.setText(message);

      Address addr = new InternetAddress(to);
      msg.addRecipient(Message.RecipientType.TO, addr);

      if (additionalHeaders != null)
	addHeaders(msg, additionalHeaders);

      smtp.connect();

      smtp.sendMessage(msg, new Address[] { addr });

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

  private static void addHeaders(MimeMessage msg, String headers)
    throws MessagingException
  {
    int i = 0;
    int len = headers.length();

    while (true) {
      char ch;
      
      for (;
	   i < len && Character.isWhitespace(headers.charAt(i));
	   i++) {
      }

      if (len <= i)
	return;

      StringBuilder name = new StringBuilder();

      for (;
	   i < len && (! Character.isWhitespace(ch = headers.charAt(i)) &&
		       ch != ':');
	   i++) {
	name.append((char) ch);
      }

      for (;
	   i < len && (Character.isWhitespace(ch = headers.charAt(i)) ||
		       ch == ':');
	   i++) {
      }

      StringBuilder value = new StringBuilder();
      
      for (;
	   i < len && ((ch = headers.charAt(i)) != '\r' &&
		       ch != '\n');
	   i++) {
	value.append((char) ch);
      }

      msg.addHeader(name.toString(), value.toString());
    }
  }
}

