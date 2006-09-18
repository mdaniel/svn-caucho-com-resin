package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="locales_target")
public class LocaleTarget {
  /**
   * CREATE TABLE `locales_target` (
   *   `lid` int(11) NOT NULL default '0',
   *   `translation` blob NOT NULL,
   *   `locale` varchar(12) NOT NULL default '',
   *   `plid` int(11) NOT NULL default '0',
   *   `plural` int(1) NOT NULL default '0',
   *   KEY `lid` (`lid`),
   *   KEY `lang` (`locale`),
   *   KEY `plid` (`plid`),
   *   KEY `plural` (`plural`)
   * );
   */

  private int lid;
  private String  translation; // blob
  private String locale;
  private int plid;
  private int plural;
}
