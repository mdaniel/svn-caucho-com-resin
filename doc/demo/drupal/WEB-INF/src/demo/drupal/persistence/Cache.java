package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="cache")
public class Cache {
  /**
   * CREATE TABLE `cache` (
   *   `cid` varchar(255) NOT NULL default '',
   *   `data` longblob,
   *   `expire` int(11) NOT NULL default '0',
   *   `created` int(11) NOT NULL default '0',
   *   `headers` text,
   *   PRIMARY KEY  (`cid`),
   *   KEY `expire` (`expire`)
   * );
   */

  @Id
  private String cid;
  private byte[] data;
  private int expire;
  private int created;
  private String headers;
}
