<?php
/**
 * Redirect to the summary page
 */

if ($_REQUEST['logout'] == 'true') {
  quercus_servlet_request()->getSession()->invalidate();
  header("Location: index.php");
}
else {
  require "WEB-INF/php/inc.php";

  $g_pages = load_pages("json");

  $g_page = $_GET['q'];

  if ($g_pages[$g_page]) {
    include_once($g_pages[$g_page]);
  }
  else {
    header($_SERVER['SERVER_PROTOCOL'] . " 404 Not Found");
    header("Status: 404 Not Found");
  }
}

?>
