package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

import java.util.Date;

@Entity
@Table(name="watchdog")
public class Watchdog {
  /**
   * CREATE TABLE `watchdog` (
   *   `wid` int(5) NOT NULL auto_increment,
   *   `uid` int(10) NOT NULL default '0',
   *   `type` varchar(16) NOT NULL default '',
   *   `message` longtext NOT NULL,
   *   `severity` tinyint(3) unsigned NOT NULL default '0',
   *   `link` varchar(255) NOT NULL default '',
   *   `location` varchar(128) NOT NULL default '',
   *   `referer` varchar(128) NOT NULL default '',
   *   `hostname` varchar(128) NOT NULL default '',
   *   `timestamp` int(11) NOT NULL default '0',
   *   PRIMARY KEY  (`wid`)
   * );
   */

  @Id
  private int wid;
  private int uid;
  private String type;
  private String message;
  private int severity;
  private String link;
  private String location;
  private String referer;
  private String hostname;
  private Date timestamp;
}
