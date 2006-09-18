package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="accesslog")
public class Accesslog {
  /**
   * CREATE TABLE `accesslog` (
   *   `aid` int(10) NOT NULL auto_increment,
   *   `sid` varchar(32) NOT NULL default '',
   *   `title` varchar(255) default NULL,
   *   `path` varchar(255) default NULL,
   *   `url` varchar(255) default NULL,
   *   `hostname` varchar(128) default NULL,
   *   `uid` int(10) unsigned default '0',
   *   `timer` int(10) unsigned NOT NULL default '0',
   *   `timestamp` int(11) unsigned NOT NULL default '0',
   *   PRIMARY KEY  (`aid`),
   *   KEY `accesslog_timestamp` (`timestamp`)
   * );
   */

  @Id
  private int aid;
  private String sid;
  private String title;
  private String path;
  private String url;
  private String hostname;
  private int uid;
  private int timer;
  private int timestamp;
}

