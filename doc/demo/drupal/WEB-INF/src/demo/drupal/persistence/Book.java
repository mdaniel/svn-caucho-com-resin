package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="book")
public class Book {
  /**
   * CREATE TABLE `book` (
   *   `vid` int(10) unsigned NOT NULL default '0',
   *   `nid` int(10) unsigned NOT NULL default '0',
   *   `parent` int(10) NOT NULL default '0',
   *   `weight` tinyint(3) NOT NULL default '0',
   *   PRIMARY KEY  (`vid`),
   *   KEY `nid` (`nid`),
   *   KEY `parent` (`parent`)
   * );
   */

  @Id
  private int vid;
  private int nid;
  private int parent;
  private int weight;
}
