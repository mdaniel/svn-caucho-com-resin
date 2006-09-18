package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="directory")
public class Directory {
  /**
   * CREATE TABLE `directory` (
   *   `link` varchar(255) NOT NULL default '',
   *   `name` varchar(128) NOT NULL default '',
   *   `mail` varchar(128) NOT NULL default '',
   *   `slogan` longtext NOT NULL,
   *   `mission` longtext NOT NULL,
   *   `timestamp` int(11) NOT NULL default '0',
   *   PRIMARY KEY  (`link`)
   * );
   */

  @Id
  private String link;
  private String name;
  private String mail;
  private String slogan;
  private String mission;
  private int timestamp;
}
