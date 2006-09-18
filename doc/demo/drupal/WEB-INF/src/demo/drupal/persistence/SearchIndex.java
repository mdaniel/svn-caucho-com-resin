package demo.drupal.persistence;

import javax.persistence.Table;

// XXX: no primary key @Entity
@Table(name="search_index")
public class SearchIndex {
  /**
   * CREATE TABLE `search_index` (
   *   `word` varchar(50) NOT NULL default '',
   *   `sid` int(10) unsigned NOT NULL default '0',
   *   `type` varchar(16) default NULL,
   *   `fromsid` int(10) unsigned NOT NULL default '0',
   *   `fromtype` varchar(16) default NULL,
   *   `score` float default NULL,
   *   KEY `sid_type` (`sid`,`type`),
   *   KEY `from_sid_type` (`fromsid`,`fromtype`),
   *   KEY `word` (`word`)
   * );
   */

  private String word;
  private int sid;
  private String type;
  private int fromsid;
  private String fromtype;
  private float  score;
}
