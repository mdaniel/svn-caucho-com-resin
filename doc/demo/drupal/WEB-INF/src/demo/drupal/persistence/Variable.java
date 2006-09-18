package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="variable")
public class Variable {
  /**
   * CREATE TABLE `variable` (
   *   `name` varchar(48) NOT NULL default '',
   *   `value` longtext NOT NULL,
   *   PRIMARY KEY  (`name`)
   * );
   */

  @Id
  private String name;
  private String value;
}
