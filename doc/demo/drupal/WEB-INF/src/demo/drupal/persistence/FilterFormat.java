package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="filter_formats")
public class FilterFormat {
  /**
   * CREATE TABLE `filter_formats` (
   *   `format` int(4) NOT NULL auto_increment,
   *   `name` varchar(255) NOT NULL default '',
   *   `roles` varchar(255) NOT NULL default '',
   *   `cache` tinyint(2) NOT NULL default '0',
   *   PRIMARY KEY  (`format`)
   * );
   */

  @Id
  private int format;
  private String name;
  private String roles;
  private int cache;
}
