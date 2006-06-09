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
import javax.mail.*;
import java.text.*;
import java.util.*;

/**
 * Formats and parses date specification based on the
 * draft-ietf-drums-msg-fmt-08 dated January 26, 2000. This is a
 * followup spec to RFC822.
 *
 * This class does not take pattern strings. It always formats the
 * date based on the specification below.
 *
 * 3.3 Date and Time Specification
 *
 * Date and time occur in several header fields of a message. This
 * section specifies the syntax for a full date and time
 * specification. Though folding whitespace is permitted throughout
 * the date-time specification, it is recommended that only a single
 * space be used where FWS is required and no space be used where FWS
 * is optional in the date-time specification; some older
 * implementations may not interpret other occurrences of folding
 * whitespace correctly.
 *
 * date-time = [ day-of-week "," ] date FWS time [CFWS]
 * day-of-week = ([FWS] day-name) / obs-day-of-week
 * day-name = "Mon" / "Tue" / "Wed" / "Thu" / "Fri" / "Sat" / "Sun"
 * date = day month year
 * year = 4*DIGIT / obs-year
 * month = (FWS month-name FWS) / obs-month
 * month-name = "Jan" / "Feb" / "Mar" / "Apr" / "May" / "Jun" / "Jul" / "Aug" / "Sep" / "Oct" / "Nov" / "Dec"
 * day = ([FWS] 1*2DIGIT) / obs-day
 * time = time-of-day FWS zone
 * time-of-day = hour ":" minute [ ":" second ]
 * hour = 2DIGIT / obs-hour
 * minute = 2DIGIT / obs-minute
 * second = 2DIGIT / obs-second
 * zone = (( "+" / "-" ) 4DIGIT) / obs-zone
 *
 * The day is the numeric day of the month. The year is any numeric
 * year in the common era.
 *
 * The time-of-day specifies the number of hours, minutes, and
 * optionally seconds since midnight of the date indicated.
 *
 * The date and time-of-day SHOULD express local time.
 *
 * The zone specifies the offset from Coordinated Universal Time (UTC,
 * formerly referred to as "Greenwich Mean Time") that the date and
 * time-of-day represent. The "+" or "-" indicates whether the
 * time-of-day is ahead of or behind Universal Time. The first two
 * digits indicate the number of hours difference from Universal Time,
 * and the last two digits indicate the number of minutes difference
 * from Universal Time. (Hence, +hhmm means +(hh * 60 + mm) minutes,
 * and -hhmm means -(hh * 60 + mm) minutes). The form "+0000" SHOULD
 * be used to indicate a time zone at Universal Time. Though "-0000"
 * also indicates Universal Time, it is used to indicate that the time
 * was generated on a system that may be in a local time zone other
 * than Universal Time.
 *
 * A date-time specification MUST be semantically valid. That is, the
 * day-of-the week (if included) MUST be the day implied by the date,
 * the numeric day-of-month MUST be between 1 and the number of days
 * allowed for the specified month (in the specified year), the
 * time-of-day MUST be in the range 00:00:00 through 23:59:60 (the
 * number of seconds allowing for a leap second; see [STD-12]), and
 * the zone MUST be within the range -9959 through +9959.
 *
 * Since: JavaMail 1.2 See Also:Serialized Form
 */
public class MailDateFormat extends SimpleDateFormat {

  public MailDateFormat()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Formats the given date in the format specified by
   * draft-ietf-drums-msg-fmt-08 in the current TimeZone.
   */
  public StringBuffer format(Date date, StringBuffer dateStrBuf,
			     FieldPosition fieldPosition)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Parses the given date in the format specified by
   * draft-ietf-drums-msg-fmt-08 in the current TimeZone.
   */
  public Date parse(String text, ParsePosition pos)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Don't allow setting the calendar
   */
  public void setCalendar(Calendar newCalendar)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Don't allow setting the NumberFormat
   */
  public void setNumberFormat(NumberFormat newNumberFormat)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
