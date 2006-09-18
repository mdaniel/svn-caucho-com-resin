package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="authmap")
public class Authmap {
  /**
   * CREATE TABLE `authmap` (
   *   `aid` int(10) unsigned NOT NULL auto_increment,
   *   `uid` int(10) NOT NULL default '0',
   *   `authname` varchar(128) NOT NULL default '',
   *   `module` varchar(128) NOT NULL default '',
   *   PRIMARY KEY  (`aid`),
   *   UNIQUE KEY `authname` (`authname`)
   * );
   */

  @Id
  private int aid;
  private int uid;
  private String authname;
  private String module;
}

