<?php
/**
 * Provides the most important status information about the Resin server.
 *
 * @author Sam
 */


require_once "WEB-INF/php/inc.php";

$server_id = $_GET["server-id"];

if ($server_id) {
  $mbean_server = new MBeanServer($server_id);

  if (! $mbean_server) {
    $title = "Resin: WebApp for server $server_id";

    display_header("webapp.php", $title, $server);

    echo "<h3 class='fail'>Can't contact $server_id</h3>";
    return;
  }
}
else
  $mbean_server = new MBeanServer();

if ($mbean_server) {
  $resin = $mbean_server->lookup("resin:type=Resin");
  $server = $mbean_server->lookup("resin:type=Server");
}

$title = "Resin: WebApp";

if (! empty($server->Id))
  $title = $title . " for server " . $server->Id;
?>

<?php

display_header("webapp.php", $title, $server, true);

function sort_host($a, $b)
{
  return strcmp($a->URL, $b->URL);
}

if (! $server) {
  echo "<h2 class='fail'>Can't contact '$server_id'</h2>";
  return;
}

$webapp = $mbean_server->lookup($_GET['id']);

$request = quercus_get_request();
$is_secure = $request->isSecure();
$user = quercus_get_request()->getUserPrincipal();
$action = $_POST['action'];

if (! $action) {
}
else if ($request->isSecure()
         && $_POST['name'] == $_GET['id']) {
    
  if ($action == 'start') {
    $webapp->start();

    echo "<div class='warn'>Starting '${_GET['id']}' by $user</div>";
  }
  else if ($action == 'stop') {
    $webapp->stop();

    echo "<div class='warn'>Stopping '${_GET['id']}' by $user</div>";
  }
  else if ($action == 'restart') {
    $webapp->restart();

    echo "<div class='warn'>Restarting '${_GET['id']}' by $user</div>";
  }
}

if ($webapp) {
  display_webapp($mbean_server, $webapp);
}
else {
  display_webapp_summary($mbean_server);
}

/**
 * single-page webapp
 */
function display_webapp($mbean_server, $webapp)
{
  $session = $webapp->SessionManager;
  
  echo <<<END
<h2>WebApp</h2>

<table class="data">
  <tr>
    <th>Web-App</th>
    <th>State</th>
    <th>Active</th>
    <th>Sessions</th>
    <th>Uptime</th>
    <th colspan='2'>500</th>
  </tr>
END;

echo "<tr class='" . row_style($count++) . "'>\n";
echo " <td class='item'>\n";

$context_path = empty($webapp->ContextPath) ? "/" : $webapp->ContextPath;

echo $context_path;

echo " </td>";

echo "  <td class='" . format_state_class($webapp->State) . "'>\n";
echo  $webapp->State;
echo "  </td>\n";
echo "  <td>" . $webapp->RequestCount . "</td>\n";
echo "  <td>" . $session->SessionActiveCount . "</td>\n";
echo "  <td class='" . format_ago_class($webapp->StartTime) . "'>\n";
echo "    " . format_ago($webapp->StartTime) . "\n";
echo "   </td>\n";

        format_ago_td_pair($webapp->Status500CountTotal,
                           $webapp->Status500LastTime);

echo "</tr>\n";
echo "</table>";

echo "<h3>Actions</h3>\n";

if (quercus_get_request()->isSecure()) {
  $disabled = "";
}
else {
  $disabled = "disabled='true'";
}

$name = $webapp->mbean_name;

echo "<form method='POST' style='display:inline'>\n";
echo "<input type='hidden' name='action' value='start'>\n";
echo "<input type='hidden' name='name' value='$name'>\n";
echo "<input type='submit' name='submit' value='start' $disabled>\n";
echo "</form>";

echo "<form method='POST' style='display:inline'>\n";
echo "<input type='hidden' name='action' value='stop'>\n";
echo "<input type='hidden' name='name' value='$name'>\n";
echo "<input type='submit' name='submit' value='stop' $disabled>\n";
echo "</form>";

echo "<form method='POST' style='display:inline'>";
echo "<input type='hidden' name='action' value='restart'>";
echo "<input type='hidden' name='name' value='$name'>\n";
echo "<input type='submit' name='submit' value='restart' $disabled>";
echo "</form>";
  
}

/**
 * summary of all the webapps
 */
function display_webapp_summary($mbean_server)
{
?>

<!-- Applications -->
<h2>WebApps</h2>

<table class="data">
  <tr>
    <th>Web-App</th>
    <th>State</th>
    <th>Active</th>
    <th>Sessions</th>
    <th>Uptime</th>
    <th colspan='2'>500</th>
  </tr>
<?php

if ($mbean_server) {
  $hosts = $mbean_server->query("resin:*,type=Host");
}

usort($hosts, "sort_host");

foreach ($hosts as $host) {
  $hostName = empty($host->HostName) ? "default" : $host->HostName;
?>

  <tr title='<?= $hostObjectName ?>'><td class='group' colspan='7'><?= $host->URL ?></td></tr>
<?php
function sort_webapp($a, $b)
{
  return strcmp($a->ContextPath, $b->ContextPath);
}

$webapps = $host->WebApps;

usort($webapps, "sort_webapp");
$count = 0;
foreach ($webapps as $webapp) {

  $session = $webapp->SessionManager;
?>

  <tr class='<?= row_style($count++) ?>'>
    <td class='item'>
<?php
$context_path = empty($webapp->ContextPath) ? "/" : $webapp->ContextPath;
$object_name = $webapp->mbean_name;

echo "<a href='webapp.php?id=" . $object_name . "'>" . $context_path . "</a>";
?>       
    </td>
    <td class='<?= format_state_class($webapp->State) ?>'>
       <?= $webapp->State ?>
    </td>
    <td><?= $webapp->RequestCount ?></td>
    <td><?= $session->SessionActiveCount ?></td>
    <td class='<?= format_ago_class($webapp->StartTime) ?>'>
      <?= format_ago($webapp->StartTime) ?>
    </td>
    <?php
        format_ago_td_pair($webapp->Status500CountTotal,
                           $webapp->Status500LastTime);
    ?>
  </tr>
<?php
  } // webapps
} // hosts
?>

</table>
<?php
} // summary
?>

<?php display_footer("webapp.php"); ?>
