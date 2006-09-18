package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="profile_values")
public class ProfileValue {
  /**
   * CREATE TABLE `profile_values` (
   *   `fid` int(10) unsigned default '0',
   *   `uid` int(10) unsigned default '0',
   *   `value` text,
   *   KEY `uid` (`uid`),
   *   KEY `fid` (`fid`)
   * );
   */

  @Id
  private int fid;
  private int uid;
  private String value;
}
