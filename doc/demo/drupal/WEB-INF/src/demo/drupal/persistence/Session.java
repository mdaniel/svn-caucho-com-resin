package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="sessions")
public class Session {
  /**
   * CREATE TABLE `sessions` (
   *   `uid` int(10) unsigned NOT NULL default '0',
   *   `sid` varchar(32) NOT NULL default '',
   *   `hostname` varchar(128) NOT NULL default '',
   *   `timestamp` int(11) NOT NULL default '0',
   *   `cache` int(11) NOT NULL default '0',
   *   `session` longtext,
   *   PRIMARY KEY  (`sid`),
   *   KEY `uid` (`uid`),
   *   KEY `timestamp` (`timestamp`)
   * );
   */

  @Id
  private String sid;
  private int uid;
  private String hostname;
  private int timestamp;
  private int cache;
  private String session; // bytes[] ???
}
