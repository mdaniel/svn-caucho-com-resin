package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="locales_source")
public class LocaleSource {
  /**
   * CREATE TABLE `locales_source` (
   *   `lid` int(11) NOT NULL auto_increment,
   *   `location` varchar(255) NOT NULL default '',
   *   `source` blob NOT NULL,
   *   PRIMARY KEY  (`lid`)
   * );
   */

  @Id
  private int lid;
  private String location;
  private String source; // blob
}
