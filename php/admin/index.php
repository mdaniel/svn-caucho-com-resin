<?php
/**
 * Redirect to the status.php page
 */

require "WEB-INF/php/inc.php";

$g_pages = load_pages();

$g_page = $_GET['q'];

if (! $g_pages[$g_page]) {
  $g_page = "status";
}

if (! admin_init()) {
  return;
}

include_once($g_pages[$g_page]);

display_footer($g_page);

?>
