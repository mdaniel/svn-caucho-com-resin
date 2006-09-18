package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="profile_fields")
public class ProfileField {
  /**
   * CREATE TABLE `profile_fields` (
   *   `fid` int(10) NOT NULL auto_increment,
   *   `title` varchar(255) default NULL,
   *   `name` varchar(128) default NULL,
   *   `explanation` text,
   *   `category` varchar(255) default NULL,
   *   `page` varchar(255) default NULL,
   *   `type` varchar(128) default NULL,
   *   `weight` tinyint(1) NOT NULL default '0',
   *   `required` tinyint(1) NOT NULL default '0',
   *   `register` tinyint(1) NOT NULL default '0',
   *   `visibility` tinyint(1) NOT NULL default '0',
   *   `options` text,
   *   PRIMARY KEY  (`fid`),
   *   UNIQUE KEY `name` (`name`),
   *   KEY `category` (`category`)
   * );
   */

  @Id
  private int fid;
  private String title;
  private String name;
  private String explanation;
  private String category;
  private String page;
  private String type;
  private int weight;
  private int required;
  private int register;
  private int visibility;
  private String options;
}
