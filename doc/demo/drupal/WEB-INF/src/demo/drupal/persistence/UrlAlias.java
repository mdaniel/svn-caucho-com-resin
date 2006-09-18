package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="url_alias")
public class UrlAlias {
  /**
   * CREATE TABLE `url_alias` (
   *   `pid` int(10) unsigned NOT NULL auto_increment,
   *   `src` varchar(128) NOT NULL default '',
   *   `dst` varchar(128) NOT NULL default '',
   *   PRIMARY KEY  (`pid`),
   *   UNIQUE KEY `dst` (`dst`),
   *   KEY `src` (`src`)
   * );
   */

  @Id
  private int pid;
  private String src;
  // unique:
  private String dst;
}
