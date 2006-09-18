package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="filters")
public class Filter {
  /**
   * CREATE TABLE `filters` (
   *   `format` int(4) NOT NULL default '0',
   *   `module` varchar(64) NOT NULL default '',
   *   `delta` tinyint(2) NOT NULL default '0',
   *   `weight` tinyint(2) NOT NULL default '0',
   *   KEY `weight` (`weight`)
   * );
   */

  private int format;
  private String module;
  private int delta;
  private int weight;
}
