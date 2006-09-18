package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="poll_choices")
public class PollChoice {
  /**
   * CREATE TABLE `poll_choices` (
   *   `chid` int(10) unsigned NOT NULL auto_increment,
   *   `nid` int(10) unsigned NOT NULL default '0',
   *   `chtext` varchar(128) NOT NULL default '',
   *   `chvotes` int(6) NOT NULL default '0',
   *   `chorder` int(2) NOT NULL default '0',
   *   PRIMARY KEY  (`chid`),
   *   KEY `nid` (`nid`)
   * );
   */

  @Id
  private int chid;
  private int nid;
  private String chtext;
  private int chvotes;
  private int chorder;
}
