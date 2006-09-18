package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="locales_meta")
public class LocaleMeta {
  /**
   * CREATE TABLE `locales_meta` (
   *   `locale` varchar(12) NOT NULL default '',
   *   `name` varchar(64) NOT NULL default '',
   *   `enabled` int(2) NOT NULL default '0',
   *   `isdefault` int(2) NOT NULL default '0',
   *   `plurals` int(1) NOT NULL default '0',
   *   `formula` varchar(128) NOT NULL default '',
   *   PRIMARY KEY  (`locale`)
   * );
   */

  @Id
  private String locale;
  private String name;
  private int enabled;
  private int isdefault;
  private int plurals;
  private String formula;
}
