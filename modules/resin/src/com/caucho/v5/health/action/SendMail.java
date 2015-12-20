/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.caucho.v5.amp.misc.MailService;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthCheckResult;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.health.ResinHealthCheckImpl;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.check.HealthCheck;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.QDate;

/**
 * Health action to send an email containing the results of the health check.  
 * <p>
 * The SMTP server should be configured using 
 * a &lt;mail&gt; resource, as demonstrated in the following example.  See 
 * Resin documentation on &lt;mail&gt; for additional configuration.
 * <p> 
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <mail name="healthMailer">
 *   <from>resin@yourdomain.com</from>
 *   <smtp-host>localhost</smtp-host>
 *   <smtp-port>25</smtp-port>
 * </mail>
 * 
 * <health:SendMail mail="${healthMailer}">
 *   <to>admin@yourdomain.com</to>
 *   <to>another_admin@yourdomain.com</to>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:SendMail>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class SendMail extends HealthActionBase
{
  private static final L10N L = new L10N(SendMail.class);
  private static final Logger log
    = Logger.getLogger(SendMail.class.getName());

  private MailService _mailService = new MailService();
  
  public SendMail()
  {
    try {
      setFrom("resin@localhost");
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
    }
  }
  
  @PostConstruct
  public void init()
  {
    _mailService.init();
    
    super.init();
  }
  
  /**
   * Add a "TO:" address; a mail recipient
   * @throws AddressException for an invalid email address
   */
  @Configurable
  public void addTo(String to)
    throws AddressException
  {
    _mailService.addTo(new InternetAddress(to));
  }

  /**
   * Set the "FROM:" address, defaults to "resin@localhost"
   * @throws AddressException for an invalid email address
   */
  @Configurable
  public void setFrom(String from)
    throws AddressException
  {
    _mailService.addFrom(new InternetAddress(from));
  }

  /**
   * Set the javax.mail.Session to send; configure using a &lt;mail&gt; resource
   */
  @Configurable
  public void setMail(Session session)
  {
    if (session == null)
      throw new ConfigException(L.l("referenced mail session not found"));
    
    _mailService.setSession(session);
  }
  
  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    HealthSubSystem healthService = healthEvent.getHealthSystem();
    HealthCheckResult summaryResult = healthService.getSummaryResult();
    
    // TODO: construct more informative subject and body?
    
    String subject = L.l("Resin health {0} from {1} at {2}",
                         summaryResult.getStatus(),
                         SystemManager.getCurrent().getId(),
                         CauchoUtil.getLocalHost());
    
    StringBuilder sb = new StringBuilder();
    
    sb.append(summaryResult.getDescription());
    sb.append("\n");
    
    for (HealthCheck healthCheck : healthService.getHealthChecks()) {
      HealthCheckResult lastResult = healthService.getLastResult(healthCheck);
      
      if (lastResult == null)
        continue;
      
      if (healthCheck instanceof ResinHealthCheckImpl)
        continue;
      
      sb.append(healthCheck.getName());
      sb.append("[");
      sb.append(lastResult.getDescription());
      sb.append("] at ");
      sb.append(QDate.formatLocal(lastResult.getTimestamp()));
      sb.append("\n");
    }
    
    _mailService.send(subject, sb.toString());
    
    return new HealthActionResult(ResultStatus.OK, 
                                  L.l("Email {0} sent to {0}",
                                      subject,
                                      Arrays.toString(_mailService.getToAddresses())));
  }
}
