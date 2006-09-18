package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="sequences")
public class Sequence {
  /**
   * CREATE TABLE `sequences` (
   *   `name` varchar(255) NOT NULL default '',
   *   `id` int(10) unsigned NOT NULL default '0',
   *   PRIMARY KEY  (`name`)
   * );
   */

  @Id
  private String name;
  private int id;
}
