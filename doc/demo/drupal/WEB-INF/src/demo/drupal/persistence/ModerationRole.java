package demo.drupal.persistence;

import javax.persistence.Table;

// XXX: no primary key @Entity
@Table(name="moderation_roles")
public class ModerationRole {
  /**
   * CREATE TABLE `moderation_roles` (
   *   `rid` int(10) unsigned NOT NULL default '0',
   *   `mid` int(10) unsigned NOT NULL default '0',
   *   `value` tinyint(4) NOT NULL default '0',
   *   KEY `idx_rid` (`rid`),
   *   KEY `idx_mid` (`mid`)
   * );
   */

  private int rid;
  private int mid;
  private int value;
}
