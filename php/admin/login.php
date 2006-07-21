<?php
/**
 * Redirect to a target uri.
 *
 * @author Sam
 */

require_once "inc.php";

header("Location: " . uri($_REQUEST['target'])); 
?>
