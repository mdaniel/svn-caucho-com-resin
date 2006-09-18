package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="users")
public class User {
  /**
   * CREATE TABLE `users` (
   *   `uid` int(10) unsigned NOT NULL default '0',
   *   `name` varchar(60) NOT NULL default '',
   *   `pass` varchar(32) NOT NULL default '',
   *   `mail` varchar(64) default '',
   *   `mode` tinyint(1) NOT NULL default '0',
   *   `sort` tinyint(1) default '0',
   *   `threshold` tinyint(1) default '0',
   *   `theme` varchar(255) NOT NULL default '',
   *   `signature` varchar(255) NOT NULL default '',
   *   `created` int(11) NOT NULL default '0',
   *   `access` int(11) NOT NULL default '0',
   *   `login` int(11) NOT NULL default '0',
   *   `status` tinyint(4) NOT NULL default '0',
   *   `timezone` varchar(8) default NULL,
   *   `language` varchar(12) NOT NULL default '',
   *   `picture` varchar(255) NOT NULL default '',
   *   `init` varchar(64) default '',
   *   `data` longtext,
   *   PRIMARY KEY  (`uid`),
   *   UNIQUE KEY `name` (`name`),
   *   KEY `access` (`access`)
   * );
   */

  @Id
  private int uid;
  private String name;
  private String pass;
  private String mail;
  private int mode;
  private int sort;
  private int threshold;
  private String theme;
  private String signature;
  private int created;
  private int access;
  private int login;
  private int status;
  private String timezone;
  private String language;
  private String picture;
  private String init;
  private String data;

  public String toString()
  {
    return getClass().getSimpleName() + "[" + uid + "," + name + "]";
  }
}

/**
DROP TABLE IF EXISTS `users_roles`;
CREATE TABLE `users_roles` (
  `uid` int(10) unsigned NOT NULL default '0',
  `rid` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`uid`,`rid`)
);
*/
