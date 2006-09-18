package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="aggregator_item")
public class AggregatorItem {
  /**
   * CREATE TABLE `aggregator_item` (
   *   `iid` int(10) NOT NULL auto_increment,
   *   `fid` int(10) NOT NULL default '0',
   *   `title` varchar(255) NOT NULL default '',
   *   `link` varchar(255) NOT NULL default '',
   *   `author` varchar(255) NOT NULL default '',
   *   `description` longtext NOT NULL,
   *   `timestamp` int(11) default NULL,
   *   PRIMARY KEY  (`iid`)
   * );
   */

  @Id
  private int iid;
  private int fid;
  private String title;
  private String link;
  private String author;
  private String description;
  private int timestamp;
}
