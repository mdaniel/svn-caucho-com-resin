package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="search_total")
public class SearchTotal {
  /**
   * CREATE TABLE `search_total` (
   *   `word` varchar(50) NOT NULL default '',
   *   `count` float default NULL,
   *   PRIMARY KEY  (`word`)
   * );
   */

  @Id
  private String word;
  private float count;
}
