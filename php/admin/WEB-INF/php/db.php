<?php
require_once "WEB-INF/php/inc.php";

function print_db_pools($db_pools)
{
  ob_start();
?>

<table class="data">
  <tr>
    <th>&nbsp;</th>
    <th colspan='5' id='connections'><?= info("Connections", "Database_Connections") ?></th>
    <th colspan='2' id='config'><?= info("Config", "Database_Connections") ?></th>
  </tr>
  <tr>
    <th id='name'><?= gettext('Name') ?></th>
    <th id='active'><?= gettext('Active')?></th>
    <th id='idle'><?= gettext('Idle')?></th>
    <th id='created'><?= gettext('Created')?></th>
    <th id='failed' colspan='2'><?= gettext('Failed')?></th>
    <th id='max-connections'>max-connections</th>
    <th id='idle-time'>idle-time</th>
  </tr>

<?php
  $row = 0;
  foreach ($db_pools as $pool) {
?>

  <tr class='<?= row_style($row++) ?>'>
    <td headers="name"><?= $pool->Name ?></td>
    <td headers="connections active"><?= $pool->ConnectionActiveCount ?></td>
    <td headers="connections idle"><?= $pool->ConnectionIdleCount ?></td>
    <td headers="connections created"><?= format_miss_ratio($pool->ConnectionCountTotal, $pool->ConnectionCreateCountTotal) ?></td>
    <td headers="connections failed"><?= $pool->ConnectionFailCountTotal ?></td>
    <td headers="connections failed" class='<?= format_ago_class($pool->LastFailTime) ?>'>
        <?= format_ago($pool->LastFailTime) ?></td>
    <td headers="config max-connections"><?= $pool->MaxConnections ?></td>
    <td headers="config idle-time"><?= sprintf("%.2fs", $pool->MaxIdleTime * 0.001) ?></td>
  </tr>
<?php
  }
?>
</table>
<?php
  return ob_get_clean();
}
?>
