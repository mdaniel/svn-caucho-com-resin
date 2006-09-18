package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="aggregator_category")
public class AggregatorCategory {
  /**
   * CREATE TABLE `aggregator_category` (
   *   `cid` int(10) NOT NULL auto_increment,
   *   `title` varchar(255) NOT NULL default '',
   *   `description` longtext NOT NULL,
   *   `block` tinyint(2) NOT NULL default '0',
   *   PRIMARY KEY  (`cid`),
   *   UNIQUE KEY `title` (`title`)
   * );
   */

  @Id
  private int cid;
  private String title;
  private String description;
  private int block;

  /**
   *
DROP TABLE IF EXISTS `aggregator_category_feed`;
CREATE TABLE `aggregator_category_feed` (
  `fid` int(10) NOT NULL default '0',
  `cid` int(10) NOT NULL default '0',
  PRIMARY KEY  (`fid`,`cid`)
);
*/

  /**
DROP TABLE IF EXISTS `aggregator_category_item`;
CREATE TABLE `aggregator_category_item` (
  `iid` int(10) NOT NULL default '0',
  `cid` int(10) NOT NULL default '0',
  PRIMARY KEY  (`iid`,`cid`)
);
*/

}
