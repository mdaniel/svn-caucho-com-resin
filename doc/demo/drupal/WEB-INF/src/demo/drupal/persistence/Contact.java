package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="contact")
public class Contact {
  /**
   * CREATE TABLE `contact` (
   *   `cid` int(10) unsigned NOT NULL auto_increment,
   *   `category` varchar(255) NOT NULL default '',
   *   `recipients` longtext NOT NULL,
   *   `reply` longtext NOT NULL,
   *   `weight` tinyint(3) NOT NULL default '0',
   *   `selected` tinyint(1) NOT NULL default '0',
   *   PRIMARY KEY  (`cid`),
   *   UNIQUE KEY `category` (`category`)
   * );
   */

  @Id
  private int cid;
  private String category;
  private String recipients;
  private String reply;
  private int weight;
  private int selected;
}

