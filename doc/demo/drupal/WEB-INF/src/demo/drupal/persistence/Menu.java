package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="menu")
public class Menu {
  /**
   * CREATE TABLE `menu` (
   *   `mid` int(10) unsigned NOT NULL default '0',
   *   `pid` int(10) unsigned NOT NULL default '0',
   *   `path` varchar(255) NOT NULL default '',
   *   `title` varchar(255) NOT NULL default '',
   *   `description` varchar(255) NOT NULL default '',
   *   `weight` tinyint(4) NOT NULL default '0',
   *   `type` int(2) unsigned NOT NULL default '0',
   *   PRIMARY KEY  (`mid`)
   * );
   */

  @Id
  private int mid;
  private int pid;
  private String path;
  private String title;
  private String description;
  private int weight;
  private int type;
}
