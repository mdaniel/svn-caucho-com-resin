<?php
/**
 * General include file for admin.
 *
 * @author Sam
 */

require_once "WEB-INF/php/graph_flot.php";
require_once "WEB-INF/php/MBeanServer.php";

import java.lang.System;
//import com.caucho.management.server.*;

global $g_resin;
global $g_is_professional;
global $g_mbean_server;
global $g_server;
global $g_server_id;
global $g_server_index;
global $g_label;
global $g_si;

global $g_tail_objects;

$g_tail_objects = array();

bindtextdomain("messages", "i18n");

if (function_exists('header')) {
  // kill the cache, all pages are uncached and private
  header("Expires: 01 Dec 1994 16:00:00 GMT");
  header("Cache-Control: max-age=0,private");
  header("Pragma: No-Cache");
}  

function admin_init($is_refresh=false, $fragment = false)
{
  global $g_server_id;
  global $g_server;
  global $g_page;

  if (! mbean_init()) {
    if ($g_server_id)
      $title = "Resin: $g_page for server $g_server_id";
    else
      $title = "Resin: $g_page for server default";

    display_header($g_page, $title, $g_server, $is_refresh);

    $mbean_server = new MBeanServer($g_server_id);

    if ($mbean_server->isConnected() == true)
      echo "<h3 class='fail'>Please register &lt;resin:AdminService/></h3>";
    else
      echo "<h3 class='fail'>Can't contact $g_server_id</h3>";

    return false;
  }

  if ($g_server_id)
    $title = "Resin: $g_page";
  else
    $title = "Resin: $g_page";

  if ($fragment)
    return true;
  else
    return display_header($g_page, $title, $g_server, $is_refresh, true);
}

function mbean_init()
{
  global $g_resin;
  global $g_is_professional;
  global $g_mbean_server;
  global $g_server;
  global $g_server_id;
  global $g_server_index;
  global $g_label;
  global $g_si;

  $is_valid = 1;

  $server_index = get_request_param("s");

  $new_s = get_request_param("new_s");
  if ($new_s !== NULL) {
    $server_index = $new_s;
  }

  $g_mbean_server = new MBeanServer();

  $server = server_find_by_index($g_mbean_server, $server_index);

  if ($server) {
    $g_server_index = $server->ClusterIndex;
  }

  if (! isset($g_server_index)) {
    $g_server = $g_mbean_server->getServer();
    $g_server_index = $g_server->SelfServer->ClusterIndex;
    $g_server_id = $g_server->Id;

    if (! $g_server_id)
      $g_server_id = "default";
  }
  else {
    $server = server_find_by_index($g_mbean_server, $g_server_index);
    $g_server_id = $server->Name;

    try {
      $mbean_server = new MBeanServer($g_server_id);

      $server = $mbean_server->getServer();

      if ($server) {
        $g_mbean_server = $mbean_server;
        $g_server = $server;
      }
      else {
        $is_valid = false;
      }
    } catch (Exception $e) {
      $is_valid = false;
    }
  }

  $g_si = sprintf("%02d", $g_server_index);
  $g_label = "$g_si - $g_server_id";

  if ($g_mbean_server) {
    $g_resin = $g_mbean_server->getResin();
    $g_server = $g_mbean_server->getServer();

    $g_is_professional = $g_resin->Professional;

    return $is_valid;
  }
  else
    return false;
}

function load_pages($suffix)
{
  $pages = load_dir_pages("WEB-INF/php", $suffix);

  $config = java("com.caucho.config.Config");
  $user_path = $config->getProperty("resin_admin_ext_path");

  if ($user_path)
    $pages = array_merge($pages, load_dir_pages($user_path, $suffix));

  return $pages;
}

function load_dir_pages($dir_name, $suffix)
{
  $dir = opendir($dir_name);

  $pages = array();

  while (($file = readdir($dir))) {
    $values = null;

    if (preg_match("/(.*)\." . $suffix . "$/", $file, $values)) {
      $name = $values[1];
      $pages[$name] = $dir_name . "/" . $file;
    }
  }

  closedir($dir);

  return $pages;
}

function format_datetime($date)
{
  return strftime("%a %b %d %H:%M:%S %Z %Y", $date->time / 1000);
}

function format_memory($memory)
{
  return sprintf("%.2fMeg", $memory / (1024 * 1024));
}

function format_hit_ratio($hit, $miss)
{
  $total = $hit + $miss;

  if ($total == 0)
    return "0.00% (0 / 0)";
  else
    return sprintf("%.2f%% (%d / %d)", 100 * $hit / $total, $hit, $total);
}

function format_ago_td_pair($value, $date, $headers=null)
{
  $ago_class = format_ago_class($date);
  if ($ago_class)
    $ago_class="class='$ago_class'";

  if ($headers != null) {
	  echo "<td headers='$headers'>$value</td>\n";
	  echo "<td headers='$headers' $ago_class>";
  } else {
	  echo "<td>$value</td>\n";
	  echo "<td $ago_class>";
  }

  echo format_ago($date);
  echo "</td>\n";
}

function format_state_class($state)
{
  if ($state == "FAILED")
    return "fail";
  else
    return "";
}

function format_ago_class($date, $fail=3600, $warn=14400)
{
  if (! $date)
    return "";

  $event_time = $date->time / 1000;

  if ($event_time < 365 * 24 * 3600)
    return "";

  $now = time();
  $ago = $now - $event_time;

  if ($ago < $fail)
    return "fail";
  else if ($ago < $warn)
    return "warn";
  else
    return "";
}

function format_ago($date)
{
  if (! $date)
    return "";

  $event_time = $date->time / 1000;

  return format_ago_unixtime($event_time);
}

function format_ago_unixtime($event_time)
{
  if (! $event_time)
    return "";

  if ($event_time < 365 * 24 * 3600)
    return "";

  $now = time();
  $ago = $now - $event_time;

  return sprintf("%dh %02dm", $ago / 3600, $ago / 60 % 60);
}

// this is necessary to strip milliseconds, which strtotime doesn't handle
function java_iso8601_to_date($iso8601)
{
  $array = split('[T\.\+]', $iso8601);
  return strtotime($array[0] . "T" . $array[1] . "+" . $array[3]);
}

function format_miss_ratio($hit, $miss)
{
  $total = $hit + $miss;

  if ($total == 0)
    return "0.00% (0 / 0)";
  else
    return sprintf("%.2f%% (%s / %s)", 100 * $miss / $total,
                   format_count($miss),
                   format_count($total));
}

function format_count($count)
{
  if ($count < 100 * 1000)
    return sprintf("%d", $count);
  if ($count < 1000 * 1000)
    return sprintf("%.1fk", $count / 1000.0);
  if ($count < 1000 * 1000 * 1000)
    return sprintf("%.1fM", $count / (1000.0 * 1000.0));
  else
    return sprintf("%.1fG", $count / (1000.0 * 1000.0 * 1000.0));
}

function format_bytes($bytes)
{
  if(!empty($bytes)) {
    $s = array('B', 'kB', 'MB', 'GB', 'TB', 'PB');
    $e = floor(log($bytes)/log(1024));
    return sprintf('%.1f '.$s[$e], ($bytes/pow(1024, floor($e))));
  }
}

function indent($string, $count = 2)
{
  $lines = explode("\n", $string);
  $output = "";
  $indent = str_repeat(" ", $count);

  foreach ($lines as $line) {
    $output .= $indent . $line . "\n";
  }

  return $output;
}

function uri($path)
{
  global $home_uri;

  if (is_null($home_uri))
    $home_uri = rtrim(dirname($_SERVER['PHP_SELF']), '/\\');

  if (strncmp($path, "/", 1) === 0)
    return $path;
  else
    return $home_uri . "/" . $path;
}

if (is_null($target_uri))
  $target_uri = $_SERVER['PHP_SELF'];

$user_principal = quercus_servlet_request()->getUserPrincipal();
$is_read_role = quercus_servlet_request()->isUserInRole("read");
$is_write_role = quercus_servlet_request()->isUserInRole("write");
  
$display_header_script = NULL;
$display_header_title = NULL;
$is_display_footer = false;

/**
 * Displays JMX data to the output
 **/
function display_jmx($group_mbeans)
{
  $type_partition = jmx_partition($group_mbeans, array("type"));
  ksort($type_partition);
  static $group_id = 0;
  static $data_id = 0;

  $javascript = "";

  $row = 0;

  echo "<div class='jmx'>";

  foreach ($type_partition as $type_name => $type_mbeans) {
    echo "<div id='jmx-${group_id}-type-${type_name}'";
    // echo " class='ui-widget-header ui-corner-all switch jmx-header'>\n";
    // echo " class='switch jmx-header'>\n";
    echo " class='switch jmx-header " . row_style($row++) . "'>\n";

    echo "$type_name";
    echo "</div>\n";

    echo "<div class='jmx-items toggle-jmx-${group_id}-type-${type_name}'>\n";

    foreach ($type_mbeans as $mbean) {
      $attr_list = $mbean->mbean_info->attributes;
      sort($attr_list);

      $attr_names = null;

      foreach ($attr_list as $attr) {
        $attr_names[] = $attr->name;
      }
      sort($attr_names);

      $start_id = ++$data_id;

      echo "<div id='jmx-${start_id}' ";
      echo " class='switch jmx-header'>";
      echo jmx_short_name($mbean->mbean_name, null);
      echo "</div>\n";

      echo "<div class='jmx-data-table toggle-jmx-${start_id}'>";
      echo "<table class='jmx-data'>\n";

      foreach ($attr_names as $attr_name) {
        echo "<tr>";
        echo "<th width='200px'>" . $attr_name . "</th>";

        //OS X 10.6.2 JDK 1.6 fix for #3782
        try {
          echo "<td>";
          $v = $mbean->$attr_name;
          display_jmx_data($v);
        } catch (Exception $e) {
          echo "Data unavailable due to error: ";
          var_dump($e);
        }

        echo "</td>\n";
        echo "</tr>\n";
      }
      echo "</table></div>";
    }

    echo "</div>";
  }

  echo "</div>";

  $group_id++;
}

function is_composite_data($v)
{
  $class_name = get_java_class_name($v);

  return $class_name == "com.caucho.quercus.lib.resin.CompositeDataBean";
}

function display_jmx_data($v)
{
  if (is_array($v)) {
    echo "<pre>{\n";
    foreach ($v as $k => $v) {
      echo "  ";
      if (is_string($k)) {
        echo htmlspecialchars($v);
        echo " => ";
      }

      if (is_string($v))
        echo htmlspecialchars("\"$v\",\n");
      else
        htmlvardump($v);
    }
    echo "}</pre>";
  }
  else if ($v === false)
    echo "false";
  else if ($v === true)
    echo "true";
  else if ($v === null)
    echo "null";
  elseif (is_composite_data($v)) {
    echo "<table class='jmx-composite-data'>\n";
    echo "<tbody>\n";
    foreach ($v->getKeys() as $key) {
      echo "<tr>\n";
      echo "<th>" . htmlspecialchars($key) . "</th>\n";
      echo "<td>";
      display_jmx_data($v->$key);
      echo "</td>\n";
      echo "</tr>\n";
    }
    echo "</tbody>\n";
    echo "</table>\n";
  }
  else {
    $v = (string) $v;

    $v = wordwrap($v);

    echo htmlspecialchars($v);
  }
}

function htmlvardump($var)
{
  ob_start();
  call_user_func_array('var_dump', $var);
  echo htmlspecialchars(ob_get_clean());
  ob_end_clean();
}

function jmx_partition($mbean_list, $keys)
{
  $env = null;

  foreach ($mbean_list as $mbean) {
    $exp = mbean_explode($mbean->mbean_name);

    $domain = $exp[':domain:'];

    if ($domain == "JMImplementation")
      continue;

    $name = "";

    foreach ($keys as $key) {
      if ($key == ":domain:")
        continue;

      $value = $exp[$key];

      if ($value) {
        if (strlen($name) > 0)
	  $name .= ",";

	$name .= $value;
      }
    }

    if (in_array(":domain:", $keys)) {
      $name = "${domain}:" . $name;
    }

    $env[$name][] = $mbean;
  }

  ksort($env);

  return $env;
}

function jmx_short_name($name, $exclude_array)
{
  $exp = mbean_explode($name);

  foreach ($exclude_array as $key) {
    unset($exp[$key]);
  }

  if (count($exp) > 0) {
    ksort($exp);

    $name = "";

    foreach ($exp as $key => $value) {
      if ($key == ':domain:')
        continue;

      if (strlen($name) > 0)
        $name .= ",";

      $name .= $key . '=' . $value;
    }

    if ($exp[':domain:']) {
      $name = $exp[':domain:'] . ":" . $name;
    }

    return $name;
  }
  else
    return $name;
}

/**
 * Outputs an html header.
 * A header is only output if this is the first call to display_header().
 * The first call establishes the title of the page.
 *
 * @param $script the script calling the function
 * @param $title a title to use if the header is output.
 *
 * @return true if the header was output, false if a header has already been output
 */
function display_header($script, $title, $server,
                        $is_refresh = false,
                        $allow_remote = false)
{
  global $g_server_id;
  global $g_server_index;
  global $g_page;
  global $g_next_url;
  global $g_next_url_params;
  global $user_principal;

  global $display_header_script;
  global $display_header_title;

  if (! empty($display_header_script)) {
    return;
  }

  $title = $title . " for server " . $g_server_id;

  $next_url = "?q=${g_page}&s=${g_server_index}";

  $GET_array = get_filtered_GET();

  foreach ($GET_array as $key => $value) {
    if (! preg_match("/^[sq]{1}$/", $key)) {
      $next_url .= "&${key}=${value}";
    }
  }

  $g_next_url = $next_url;
  $new_s = $_POST["new_s"];
  $s = get_request_param("s");

  if ($new_s !== NULL && $new_s != $s) {
    $next_url = preg_replace('/([?|&]s=)\d+/', '${1}' . $new_s, $next_url);
    header("Location: ${next_url}");
    return false;
  }

  $display_header_script = $script;
  $display_header_title = $title;
?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html lang="en" xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title><?= $title ?></title>
  <link rel='stylesheet' href='jquery-ui/jquery.ui.all.css' type='text/css' />
  <link rel='stylesheet' href='<?= uri("default.css") ?>' type='text/css' />
  <link rel="stylesheet" type="text/css" href="colorbox/colorbox.css" media="screen" />

  <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<?php
if ($is_refresh) {
  echo "<meta http-equiv=\"refresh\" content=\"60\" />\n";
}
?>
  <script type="text/javascript" src="jquery.js"></script>
  <script type="text/javascript" src="colorbox/jquery.colorbox.js"></script>
  <script language='javascript' type='text/javascript'>
    function hide(id) { document.getElementById(id).style.display = 'none'; }
    function show(id) { document.getElementById(id).style.display = 'block'; }
    function show_n(id) { document.getElementById(id).style.display = ''; }
    function show_i(id) { document.getElementById(id).style.display = 'inline'; }
    function sel(id) { document.getElementById(id).className = 'selected'; }
    function unsel(id) { document.getElementById(id).className = ''; }
    function showInline(id) { document.getElementById(id).style.display = 'inline'; }
    function setValue(id, v) { document.getElementById(id).value = v; }
    function selectChoice(root, name)
    {
      var textInput = document.getElementById(root + "_" + name + "_text");
      var choice = document.getElementById(root + "_" + name + "_choice");
      var infoId = root + "_" + name + "_" + textInput.value + "_info";
      infoId = infoId.replace(/\./g, "_");

      if (textInput.value != "")
        hide(infoId);

      textInput.value = choice.options[choice.selectedIndex].value;

      infoId = root + "_" + name + "_" + textInput.value + "_info";
      infoId = infoId.replace(/\./g, "_");
      show(infoId);
    }
  </script>
</head>

<body>
<?
if ($user_principal) {
?>
<div id="status-bar">
<div style='float: left; width: 80%; padding: 0; margin: 0;'>
<?
if (! empty($server)) {
  $server_name = $server->Id ? $server->Id : "default";
}
else {
  $server_name = "default";
}
?>
<ul class='status'>
  <li class="server status-item"><?php display_servers($server); ?></li>
  <li class="status-item"><?php display_health(); ?></li>
  <li class="status-item status-log"><?php display_status_log($server); ?></li>
  <li class="status-item status-log"><?php display_graph_control(); ?></li>
</ul>
</div>
<div style='float: right; width: 20%; text-align: right;'>
  <span class='status-item'><a target="caucho-wiki" href="http://wiki4.caucho.com/Admin: <?= ucfirst($g_page) ?>"><?= gettext('help')?></a></span>
  <span class='status-item'><a href="<?= $g_next_url ?>"><?= gettext('refresh')?></a></span>
  <span class='status-item logout'><a href="?q=index.php&amp;logout=true"><?= gettext('logout')?></a></span>
</div>
</div>
<? } ?>

<div class="top-header">
  <img src='<?= uri("images/caucho-logo.png") ?>' width='300' alt="Caucho Technology"/>
</div>
<table id="layout">
<tr>
  <td class="leftnav" valign="top">
    <ul class="leftnav">
    <?php
      display_pages();
    ?>
    </ul>
  </td>
  <td valign='top'>
<?php
  if (! $server && $g_server_id) {
    echo "<h3 class='fail'>Can't contact $g_server_id</h3>";
    return false;
  }

  return true;
}

function javascript_create_tab($tab_name)
{
  $javascript = 'var tabs = $("#' . $tab_name . '").tabs();' . "\n";
  $javascript .= 'tabs.find(".ui-tabs-nav").sortable({axis:\'x\'});' . "\n";
  $javascript .= '$("#'. $tab_name . '").show();' . "\n";

  return $javascript;
}  

function display_pages()
{
  global $g_pages;
  global $g_page;
  global $g_server_index;

  $names = array_keys($g_pages);
  sort($names);

  $names = array_diff($names, array('index', 'summary'));
  array_unshift($names, 'summary');
  array_unshift($names, 'index');

  foreach ($names as $name) {
    $page_name = gettext($name);
    $img = "";

    if (file_exists("images/${page_name}_16.png")) {
      $img = "<img src='images/${page_name}_16.png' width='16' height='16' alt='${page_name}' />&nbsp;";
    }
    else {
      $img = "<img src='images/pixel.gif' width='16' height='16' noborder alt='${page_name}' />&nbsp;";
    }

    if ($g_page == $name) {
      echo "<li class='selected'>$img$page_name</li>";
    } else {
      echo "<li><a href='?q=$name&amp;s=$g_server_index'>$img$page_name</a></li>";
    }
  }
}

function print_title_image($page_name)
{
  if (file_exists("images/${page_name}_32.png")) {
    return "<img src='images/${page_name}_32.png' width='32' height='32' alt='${page_name}' />&nbsp;";
  }
  else {
    return "";
  }
}  

function print_title($page_name, $text)
{
  echo "<h1>" . print_title_image($page_name) . gettext($text);
  print_help($page_name);
  echo "</h1>\n";
}  

function display_status_log($server)
{
  $mbean_server = new MBeanServer($server->SelfServer->Name);

  if ($mbean_server) {
    $mbean = $mbean_server->getLogService();
  }

  //
  // recent messages
  //

  if ($mbean) {
    $now = time();

    $messages = $mbean->findMessages(($now - 24 * 3600) * 1000, $now * 1000);

    if (! empty($messages)) {
      $messages = array_reverse($messages);

      if (count($messages) > 3) {
        $messages = array_slice($messages, 0, 3);
      }

      $first_message = $messages[0]->message;

      if (strlen($first_message) > 60) {
        $first_message
          = htmlspecialchars(substr($messages[0]->message, 0, 57)) . "...";
      }
      else {
        $first_message = htmlspecialchars($messages[0]->message);
      }

      echo "Latest Log: \n";

      echo "<span class=\"menu-switch {$messages[0]->level}\" id=\"status-log\">";
      echo "<span id='first-log-message'>${first_message}</span></span>";

      echo "<table class='toggle-status-log data'>\n";

      echo "<tbody class='scroll'>\n";
      foreach ($messages as $message) {
        echo "<tr class='{$message->level}'>";

        echo "  <td class='date'>";
        echo strftime("%Y-%m-%d %H:%M:%S", $message->timestamp / 1000);
        echo "</td>";
        echo "  <td class='level'>{$message->level}</td>";
        echo "  <td class='message'>" . htmlspecialchars(wordwrap($message->message, 90));
        echo "  </td>";

        echo "</tr>";
      }

      echo "</tbody>\n";
      echo "</table>\n";
    } else {
      echo "&nbsp;\n";
    }
  }
}

function display_health()
{
  global $g_server;
  $resin = $g_server->Cluster->Resin;

  // $clusters = $resin->Clusters;
  $clusters = array($g_server->Cluster);

  $down_servers = array();
  $error = "";

  foreach ($clusters as $c) {
    $servers = $c->Servers;
    $triads = array();

    for ($i = 0; $i < min(3, count($servers)); $i++) {
      $triad = $c->Servers[$i];
      if ($triad) {
        array_push($triads, array($triad, new MBeanServer($triad->Name)));
      }
    }

    foreach ($servers as $s) {
      if ($s->Name == "")
        $display_name = "{$c->Name}:default";
      else
        $display_name = "{$c->Name}:{$s->Name}";

      $error = "";

      foreach ($triads as $triad_pair) {
        list($triad, $triad_mbean_server) = $triad_pair;

        if ($s->SelfServer->Name == $triad->Name)
          continue;

        $s_mbean_server = new MBeanServer($s->Name);
        $s_server = $s_mbean_server->getServer();
        $s_triad_server = $s_server->SelfServer->Cluster->Servers[$triad->ClusterIndex];

        $triad_server = $triad_mbean_server->getServer();
        $triad_cluster = $triad_server->SelfServer->Cluster;
        $triad_cluster_server = $triad_cluster->Servers[$s->ClusterIndex];

        if (! $s_triad_server || ! $s_triad_server->isHeartbeatActive()) {
          $error .= "\"${display_name}\" cannot contact triad \"" . $triad->Name . "\"\n";
        }

        if (! $triad_cluster_server || ! $triad_cluster_server->isHeartbeatActive()) {
          $error .= "Triad server \"{$triad->Name}\" cannot contact \"${display_name}\n";
        }
      }

      if ($error) {
        array_push($down_servers, array($display_name, $error));
      }
    }
  }

  $health = (count($down_servers) == 0);

  if ($health) {
  	echo "System Health: ";
    print_check_or_x($health);
  }
  else {
    echo "System Health: ";
    print_check_or_x($health);
    echo " (" . count($down_servers) . ") ";
    echo "<span class='menu-switch' id='down-servers'>";
    echo "<ul class='toggle-down-servers' style='display: none'>";
    foreach ($down_servers as $down_server) {
      list($display_name, $error) = $down_server;
      $title = htmlspecialchars($error);

      global $g_server_index;

      $next_url = "?q=heartbeat&amp;s=" . $g_server_index;
      echo "<li class='fail' title='$title'><a href='$next_url'>$display_name</a></li>";
    }
    echo "</ul>";
    echo "</span>";
  }
}

function display_servers($server)
{
  global $g_next_url;
  global $g_server_index;
  global $g_server;

  if (! $server) {
    $server = $g_server;
  }

  echo "<form class='status-item' name='servers' method='post' action='${g_next_url}'>";
  echo "<label for='new_s'>Server</label>: ";
  echo "<select id='new_s' name='new_s' onchange='document.forms.servers.submit();' class='status-item'>\n";

  $self_server = $server->SelfServer;

  foreach ($self_server->Cluster->Servers as $cluster_server) {
    $id = $cluster_server->Name;
    if (! $id)
      $id = "default";

    echo "  <option";
    if ($cluster_server->ClusterIndex == $g_server_index)
      echo " selected='selected'";

    echo " value=\"" . $cluster_server->ClusterIndex . "\">";
    printf("%02d - %s\n", $cluster_server->ClusterIndex, $id);
    echo "  </option>";
  }
  echo "</select>";
  echo "</form>";
}

/**
 * Returns the title for the page, established by the first call to display_header().
 */
function display_header_title()
{
  global $display_header_title;

  return $display_header_title;
?>

<?php
}

/**
 * Outputs an html footer if needed.
 */
function display_footer($script, $javascript="")
{
  global $display_header_script, $is_display_footer;

  if ($is_display_footer)
    return;

  if ($script !== $display_header_script)
    return;

  $is_display_footer = true;

?>
  <div id="busyIndicator">
    <img src="images/loading.gif" alt="loading..."/>
  </div>

</td></tr></table>

<div id="footer">
<hr />
<p>
&nbsp;<em><?= resin_version() ?></em>
</p>
</div>

<script type="text/javascript" src="jquery-ui.js"></script>
<script type="text/javascript" src="resin-admin.js"></script>
<script type="text/javascript" src="pie-chart.js"></script>
<script type="text/javascript" src="flot/jquery.flot.js"></script>
<script type="text/javascript" src="flot/jquery.flot.symbol.js"></script>
<script type="text/javascript" src="flot/jquery.flot.selection.js"></script>
<script type="text/javascript">
<!--
//  $(document).ready(function() {
    init();

    <?= $javascript ?>

    $("#busyIndicator").hide();
//  });
-->
</script>
</body>
</html>
<?php
flush();
  display_tail();
  ?>

<?php
}

function display_left_navigation($current_server)
{
  global $g_page;

  $mbean_server = new MBeanServer();

  if (! $mbean_server)
    return;

  $resin = $mbean_server->getResin();
  $server = $mbean_server->getServer();

  if (! $current_server)
    $current_server = $server;

  foreach ($resin->Clusters as $cluster) {
    if (empty($cluster->Servers))
      continue;

    echo "<div class='nav-cluster'>$cluster->Name</div>\n";

    $client_names = array();

    foreach ($cluster->Servers as $client) {
      $client_names[] = $client->Name;
    }

    sort($client_names);

    foreach ($client_names as $client) {
      $name = $client;
      if ($name == "")
        $name = "default";

      if (! $client)
        $client = '""';

      $client_server = $mbean_server->lookup("resin:type=ClusterServer,name=$client");

      if ($client == $current_server->Id) {
        echo "<div class='nav-this'>$name</div>\n";
      }
      else if ($client_server && ! $client_server->ping()) {
        echo "<div class='nav-dead'>$name</div>\n";
      }
      else {
        echo "<div class='nav-server'><a href='?q=$g_page&server-id=$name'>";
        echo "$name</a></div>\n";
      }
    }
  }
}

function row_style($i)
{
  switch ($i % 2) {
  case 0: return 'ra';
  case 1: return 'rb';
  default: return '';
  }
}

function info($name,$wiki="")
{
  if (! $wiki)
    $wiki = $name;

  echo $name;
  echo "<sup><small><a href='http://wiki4.caucho.com/Admin: $wiki' target='caucho-wiki' class='info'>?</a></small></sup>";
}

function print_help($wiki)
{
  echo "<sup><small><a href='http://wiki4.caucho.com/Admin: $wiki' target='caucho-wiki' class='info'>?</a></small></sup>";
}

function sort_host($a, $b)
{
  return strcmp($a->URL, $b->URL);
}

function sort_webapp($a, $b)
{
  return strcmp($a->ContextPath, $b->ContextPath);
}

function display_timeout($timeout)
{
  if ($timeout == 0)
    return "0s";
  else if ($timeout % (24 * 3600 * 1000) == 0) {
    return ($timeout / (24 * 3600 * 1000)) . "d";
  }
  else if ($timeout % (3600 * 1000) == 0) {
    return ($timeout / (3600 * 1000)) . "h";
  }
  else if ($timeout % (60 * 1000) == 0) {
    return ($timeout / (60 * 1000)) . "m";
  }
  else if ($timeout % (1000) == 0) {
    return ($timeout / (1000)) . "s";
  }
  else {
    return $timeout . "ms";
  }
}


function server_find_by_index($g_mbean_server, $index)
{
  $server = $g_mbean_server->getServer();

  foreach ($server->Cluster->Servers as $cluster_server) {
    if ($cluster_server->ClusterIndex == $index) {
      return $cluster_server;
    }
  }

  return null;
}

function server_find_by_id($mbean_server, $id)
{
  $server = $mbean_server->getServer();

  if (! $id) {
    $id = "default";
  }

  foreach ($server->Cluster->Servers as $cluster_server) {
    $server_id = $cluster_server->Name;

    if (! $server_id) {
      $server_id = "default";
    }

    if ($server_id == $id) {
      return $cluster_server;
    }
  }

  return null;
}
    
function print_ok($message)
{
	if (is_null($message))
		$message = "OK";
  echo "<span style='color:#00c000'>&#x2713;</span>&nbsp;$message";
}
    
function print_fail($message)
{
	if (is_null($message))
		$message = "FAIL";
	echo "<span style='color:#c00000'>&#x2717;&nbsp;$message</span>";
}
    
function print_warn($message)
{
	if (is_null($message))
		$message = "WARNING";
	echo "<span style='color:#cc8811'>!&nbsp;$message</span>";
}

function print_unknown($message)
{
	if (is_null($message))
		$message = "UNKNOWN";
	echo "<span style='color:#909090'>?&nbsp;$message</span>";
}

function print_check_or_x($status)
{
  if ($status) {
    echo "<span style='color:#00c000'>&#x2713; OK</span>";
  }
  else {
    echo "<span style='color:#c00000'>&#x2717; ERROR</span>";
  }
}

function print_health($status, $message=null)
{
  if ($status == "OK") {
    print_ok($message);
  }
  else if ($status == "CRITICAL" || $status == "FATAL") {
    print_fail($message);
  }
  else if ($status == "WARNING") {
    print_warn($message);
  }
  else if ($status == "UNKNOWN") {
    print_unknown($message);
  }
  else {
    echo $message;
  }
}

function display_health_status($s)
{
  $si = sprintf("%02d", $s->ClusterIndex);
  $server_id = $s->Name;
  if ($server_id == "")
  	$server_id = "default";

  $mbean_server = new MBeanServer($server_id);

  $label = $si . " - " . $server_id;
  echo "<h2>" . gettext('Server') . ": $label</h2>\n";

  echo "<table class='data'>\n";
  echo "<tr><th scope='col' class='item'>" . gettext('Status') . "</th>";
  echo "<th scope='col' class='item'>" . gettext('Check') . "<span id='sw_health_status_${si}' class='switch'></span></th>";
  echo "<th scope='col' class='item'>" . gettext('Message') . "</th></tr>\n";

  echo "<tr><td>";

  if ($mbean_server) {
    $health = $mbean_server->lookup("resin:type=HealthCheck,name=Resin");
  }

  if (! $health) {
    print_health("CRITICAL");
    $message = gettext('cannot connect to server ') . $label;
  }
  else {
    print_health($health->Status);
    $message = $health->Message;
  }

  echo "</td><td>" . gettext('Overall') . "</td><td>$message</td></tr>\n";

  if ($mbean_server && $health) {
    $health_list = $mbean_server->query("resin:type=HealthCheck,*");
    foreach ($health_list as $s_health) {
    	if ($s_health->Name == 'Resin')
    		continue;
      echo "<tr class='toggle-sw_health_status_${si}'>";
      echo "<td>";
      print_health($s_health->Status);
      echo "</td>";
      echo "<td>";
      echo $s_health->Name;
      echo "</td>";
      echo "<td>";
      echo nl2br(htmlspecialchars(wordwrap($s_health->Message, 90)));
      echo "</td>";
      echo "</tr>";
    }
  }

  echo "</table><br/>";
}

function get_stats_service($mbean_server = null)
{
  global $g_mbean_server;

  if (! $mbean_server) {
    $mbean_server = $g_mbean_server;
  }

  if (! $mbean_server) {
    return;
  }

  return $mbean_server->getStatService();
}

function display_add_tail($tail)
{
  global $g_tail_objects;

  $g_tail_objects[] = $tail;
}

function display_tail()
{
  global $g_tail_objects;

  foreach ($g_tail_objects as $tail) {
    $tail->execute();
  }
}

function require_professional($msg = "This feature requires Resin Professional and a valid license.")
{
  global $g_is_professional;

  if (! $g_is_professional) {
    echo "<div style=\"display:inline-block;margin:.5em;\">\n";
    echo "<div class=\"req-pro-title\"><span style=\"color: red\">&#x2717;&nbsp;&nbsp;</span>";
    echo "Resin Professional Feature</div>\n";

    if (isset($msg)) {
      echo "<div class=\"req-pro-message\">${msg}</div>\n";
    }

    echo "<div class=\"req-pro-link\">Please download  
    <a href='http://www.caucho.com/download'> 
    Resin Professional</a> and request a free 
    <a href='http://www.caucho.com/evaluation-license/'>evaluation license</a>.
    </div>\n";

    echo "</div>\n";

    return false;
  }

  return true;
}

function get_filtered_GET()
{
  $array = filter_input_array(INPUT_GET, FILTER_SANITIZE_SPECIAL_CHARS);
  
  return $array;
}

function get_request_param($name, $default = NULL)
{
  $value = filter_input(INPUT_GET, $name, FILTER_SANITIZE_SPECIAL_CHARS);
  if ($value) {
    return $value;
  }
  
  $value = filter_input(INPUT_POST, $name, FILTER_SANITIZE_SPECIAL_CHARS);
  if ($value) {
    return $value;
  }
  
  return $default;
}

function debug($obj)
{
  if (is_string($obj))
    System::out->println($obj);
  else
    System::out->println(var_export($obj,1));
}

function format_seconds($seconds)
{
  if (round($seconds) == 0)
    return "0 seconds";

  $minute = 60;
  $hour = $minute * 60;
  $day = $hour * 24;
  $week = $day * 7;

  $weeks = floor($seconds/$week);
  $seconds -= $weeks * $week;

  $days = floor($seconds/$day);
  $seconds -= $days * $day;

  $hours = floor($seconds/$hour);
  $seconds -= $hours * $hour;

  $minutes = floor($seconds/$minute);
  $seconds -= $minutes * $minute;

  $seconds = round($seconds);

  $sb = "";
  if ($weeks == 1)
    $sb .= $weeks . " week ";
  if ($weeks > 1)
    $sb .= $weeks . " weeks ";
  if ($days == 1)
    $sb .= $days . " day ";
  if ($days > 1)
    $sb .= $days . " days ";
  if ($hours == 1)
    $sb .= $hours . " hour ";
  if ($hours > 1)
    $sb .= $hours . " hours ";
  if ($minutes == 1)
    $sb .= $minutes . " minute ";
  if ($minutes > 1)
    $sb .= $minutes . " minutes ";
  if ($seconds == 1)
    $sb .= $seconds . " second";
  if ($seconds > 1)
    $sb .= $seconds . " seconds";

  return $sb;
}

?>
