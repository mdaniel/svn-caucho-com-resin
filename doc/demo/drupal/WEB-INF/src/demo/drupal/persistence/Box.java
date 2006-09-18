package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="boxes")
public class Box {
  /**
   * CREATE TABLE `boxes` (
   *   `bid` tinyint(4) NOT NULL auto_increment,
   *   `title` varchar(64) NOT NULL default '',
   *   `body` longtext,
   *   `info` varchar(128) NOT NULL default '',
   *   `format` int(4) NOT NULL default '0',
   *   PRIMARY KEY  (`bid`),
   *   UNIQUE KEY `info` (`info`)
   * );
   */

  @Id
  private int bid;
  private String title;
  private String body;
  private String info;
  private int format;
}
