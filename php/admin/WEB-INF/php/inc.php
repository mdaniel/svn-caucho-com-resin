<?php
/**
 * General include file for admin.
 *
 * @author Sam
 */

// kill the cache, all pages are uncached and private
header("Expires: 01 Dec 1994 16:00:00 GMT"); 
header("Cache-Control: max-age=0,private"); 
header("Pragma: No-Cache"); 


function format_datetime($date)
{
  return strftime("%a %b %d %H:%M:%S %Z %Y", $date->time / 1000);
}

function format_memory($memory)
{
  return sprintf("%.2fMeg", $memory / (1024 * 1024))
}

function format_hit_ratio($hit, $miss)
{
  $total = $hit + $miss;

  if ($total == 0)
    return "0.00% (0 / 0)";
  else
    return sprintf("%.2f%% (%d / %d)", 100 * $hit / $total, $hit, $total);
}

function format_miss_ratio($hit, $miss)
{
  $total = $hit + $miss;

  if ($total == 0)
    return "0.00% (0 / 0)";
  else
    return sprintf("%.2f%% (%d / %d)", 100 * $miss / $total, $miss, $total);
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

function uri_nocache($path)
{
  global $home_uri;

  if (is_null($home_uri))
    $home_uri = rtrim(dirname($_SERVER['PHP_SELF']), '/\\'); 

  /*
  if (strstr($path, "?") === FALSE)
    $rand = "?.rand=" . mt_rand();
  else
    $rand = "&.rand=" . mt_rand();
  */

  if (strncmp($path, "/", 1) === 0)
    return $path . $rand;
  else
    return $home_uri . "/" . $path . $rand;
}

function redirect($relative_url)
{
  $uri = uri($relative_url);

  header("Location: http://" . $_SERVER['HTTP_HOST'] . $uri);
}

function redirect_nocache($relative_url)
{
  $uri = uri_nocache($relative_url);

  header("Location: http://" . $_SERVER['HTTP_HOST'] . $uri);
}

if (is_null($target_uri))
  $target_uri = $_SERVER['PHP_SELF'];

$is_read_role = $request->isUserInRole("read");
$is_write_role = $request->isUserInRole("write");

$display_header_script = NULL;
$display_header_title = NULL;
$is_display_footer = false;

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
function display_header($script, $title, $server)
{
  global $display_header_script, $display_header_title;

  if (! empty($display_header_script))
    return;

  $display_header_script = $script;
  $display_header_title = $title;

  $logout_uri = uri("logout.php");
?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Strict//EN" "http://www.w3.org/TR/html4/strict.dtd">

<html>
<head>
  <title><?= $title ?></title>
  <link rel='stylesheet' href='<?= uri("default.css") ?>' type='text/css' />

  <script language='javascript' type='text/javascript'>
    function hide(id) { document.getElementById(id).style.display = 'none'; }
    function show(id) { document.getElementById(id).style.display = ''; }
  </script>
</head>

<body>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
<tr>
  <td width="160" align="center"><img src='<?= uri("images/caucho-white.jpg") ?>' width='150' height='63'>
  </td>

  <td width="10">
  </td>

  <td valign="top">
   <ul class="status">
<? if (! empty($server)) { ?>
   <li class="server">Server: <?= $server ?></li>
<? }  ?>
   <li>Last Refreshed: <?= strftime("%Y-%m-%d %H:%M:%S", time()) ?></li>
   <li><a href="<?= $script ?>">refresh</a></li>
   </ul>
  </td>
</tr>

<tr>
  <td width="150" background='<?= uri("images/left_background.gif") ?>'>
   <img src='<?= uri("images/pixel.gif") ?>' height="14">
  </td>

  <td width="10">
  </td>

  <td>
  </td>
</tr>


<tr>
  <td class="leftnav" valign="top">
    <?php display_left_navigation(); ?>
  </td>

  <td width="10">
  </td>
  <td>

<ul class="tabs">
<?
if ($script == "status.php") {
  ?><li class="selected">Summary</li><?
} else {
  ?><li><a href="status.php">Summary</a></li><?
}

if ($script == "thread.php") {
  ?><li class="selected">Threads</li><?
} else {
  ?><li><a href="thread.php">Threads</a></li><?
}

if ($script == "profile.php") {
  ?><li class="selected">Profile</li><?
} else {
  ?><li><a href="profile.php">Profile</a></li><?
}

if ($script == "heap.php") {
  ?><li class="selected">Heap</li><?
} else {
  ?><li><a href="heap.php">Heap</a></li><?
}

if ($script == "cache.php") {
  ?><li class="selected">Cache</li><?
} else {
  ?><li><a href="cache.php">Cache</a></li><?
}

if ($script == "cluster.php") {
  ?><li class="selected">Cluster</li><?
} else {
  ?><li><a href="cluster.php">Cluster</a></li><?
}
?>
</ul>

<?php
  return true;
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
function display_footer($script)
{
  global $display_header_script, $is_display_footer;

  if ($is_display_footer)
    return;

  if ($script !== $display_header_script)
    return;

  $is_display_footer = true;

?>
</td></tr></table>
<hr />
<p>
<em><?= resin_version() ?></em>
</p>

</td></tr></table>

</body>
</html>
<?php
}

function display_left_navigation()
{
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
  echo "<sup><small><a href='http://wiki.caucho.com/Admin:$wiki' class='info'>?</a></small></sup>";
}

?>
