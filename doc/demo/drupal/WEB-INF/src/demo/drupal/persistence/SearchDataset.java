package demo.drupal.persistence;

import javax.persistence.Table;

// XXX: no primary key @Entity
@Table(name="search_dataset")
public class SearchDataset {
  /**
   * CREATE TABLE `search_dataset` (
   *   `sid` int(10) unsigned NOT NULL default '0',
   *   `type` varchar(16) default NULL,
   *   `data` longtext NOT NULL,
   *   KEY `sid_type` (`sid`,`type`)
   * );
   */

  private int sid;
  private String type;
  private String  data;
}
