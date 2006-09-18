package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="role")
public class Role {
  /**
   * CREATE TABLE `role` (
   *   `rid` int(10) unsigned NOT NULL auto_increment,
   *   `name` varchar(32) NOT NULL default '',
   *   PRIMARY KEY  (`rid`),
   *   UNIQUE KEY `name` (`name`)
   * );
   */

  @Id
  private int rid;
  private String name;
}
