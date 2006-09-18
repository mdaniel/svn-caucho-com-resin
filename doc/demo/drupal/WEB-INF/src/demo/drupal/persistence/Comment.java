package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="comments")
public class Comment {
  /**
   * CREATE TABLE `comments` (
   *   `cid` int(10) NOT NULL auto_increment,
   *   `pid` int(10) NOT NULL default '0',
   *   `nid` int(10) NOT NULL default '0',
   *   `uid` int(10) NOT NULL default '0',
   *   `subject` varchar(64) NOT NULL default '',
   *   `comment` longtext NOT NULL,
   *   `hostname` varchar(128) NOT NULL default '',
   *   `timestamp` int(11) NOT NULL default '0',
   *   `score` mediumint(9) NOT NULL default '0',
   *   `status` tinyint(3) unsigned NOT NULL default '0',
   *   `format` int(4) NOT NULL default '0',
   *   `thread` varchar(255) NOT NULL default '',
   *   `users` longtext,
   *   `name` varchar(60) default NULL,
   *   `mail` varchar(64) default NULL,
   *   `homepage` varchar(255) default NULL,
   *   PRIMARY KEY  (`cid`),
   *   KEY `lid` (`nid`)
   * );
   */

  @Id
  private int cid;
  private int pid;
  private int nid;
  private int uid;
  private String subject;
  private String  comment;
  private String hostname;
  private int timestamp;
  private int score;
  private int status;
  private int format;
  private String thread;
  private String users;
  private String name;
  private String mail;
  private String homepage;
}
