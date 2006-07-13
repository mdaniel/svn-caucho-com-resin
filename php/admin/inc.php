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

  if (strstr($path, "?") === FALSE)
    $rand = "?.rand=" . mt_rand();
  else
    $rand = "&.rand=" . mt_rand();

  if (strncmp($path, "/", 1) === 0)
    return $path . $rand;
  else
    return $home_uri . "/" . $path . $rand;
}

function redirect($relative_url)
{
  header("Location: http://"
         . $_SERVER['HTTP_HOST']
         . rtrim(dirname($_SERVER['PHP_SELF']), '/\\')
         . "/" . $relative_url);
}

if (is_null($target_uri))
  $target_uri = $_SERVER['PHP_SELF'];

$is_read_role = $request->isUserInRole("read");
$is_write_role = $request->isUserInRole("write");

$decorator_header_script = NULL;
$decorator_header_title = NULL;
$is_decorator_footer = false;

/**
 * Outputs an html header.
 * A header is only output if this is the first call to decorator_header().
 * The first call establishes the title of the page.
 * 
 * @param $script the script calling the function
 * @param $title a title to use if the header is output.
 *
 * @return true if the header was output, false if a header has already been output
 */
function decorator_header($script, $title)
{
  global $decorator_header_script, $decorator_header_title;

  if (! empty($decorator_header_script))
    return;

  $decorator_header_script = $script;
  $decorator_header_title = $title;

?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Strict//EN" "http://www.w3.org/TR/html4/strict.dtd">

<html>
<head>
  <title><?= $title ?></title>
  <link rel='stylesheet' href='<?= uri("default.css") ?>' type='text/css' />
  <link rel='shortcut icon' href='<?= uri("images/dragonfly.ico") ?>'>
  <link rel='icon' href='<?= uri("images/dragonfly-tiny.png") ?>' type='image/png'>
</head>

<body>
<h1><?= $title ?></h1>

<?php

  return true;
}

/**
 * Returns the title for the page, established by the first call to decorator_header().
 */
function decorator_header_title()
{
  global $decorator_header_title;

  return $decorator_header_title;
?>

<?php
}

/**
 * Outputs an html footer if needed.
 */
function decorator_footer($script)
{
  global $decorator_header_script, $is_decorator_footer;

  if ($is_decorator_footer)
    return;

  if ($script !== $decorator_header_script)
    return;

  $is_decorator_footer = true;
?>
<hr />
<p>
<em><?= resin_version() ?></em>
</p>

</body>
</html>
<?php
}
?>
