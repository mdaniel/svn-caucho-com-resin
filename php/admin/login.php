<?php

require_once "WEB-INF/php/inc.php";

$title = "Resin Admin Login";

display_header("login.php", $title, null);

if ($error) {
  echo "<h3 class='fail'>Error: $error</h3>";
}

?><form method="POST" action="j_security_check?j_uri=status.php">
<table border='0'>
<tr>
  <th>Username: </th>
  <td>
    <input name="j_username" width="40"></input>
  </td>
</tr>
<tr>
  <th>Password: </th>
  <td>
    <input name="j_password" width="40" type="password"></input>
  </td>
</tr>
<tr>
  <td>
    <input type="submit"></input>
  </td>
  <td></td>
</tr>
</table>
</form>

<?php

$digest_username = null;

include "digest.php";

if (! empty($digest)) {

?>

<?php
  /** XXX:
<p>
The following can now be added to the file
<code><b>resin.conf</b></code>
to enable administration functionality. 
</p>

<pre>
&lt;resin xmlns="http://caucho.com/ns/resin">

  &lt;management>
     &lt;user name="<?= $digest_username ?>" password="<?= $digest ?>"/>
  &lt;/management>

  ...

&lt;/resin>
</pre>
  */
?>

<p>
The following can now be set in the resin.conf file
to enable administration functionality. 
</p>

<pre>
&lt;resin xmlns="http://caucho.com">

  &lt;management path="admin">
     &lt;user name="<?= $digest_username ?>" password="<?= $digest ?>"/>
     ...
  &lt;/management>

  ...

&lt;/resin>
</pre>

<p>
By default, access to the administration application is limited
to the localhost.  The default behaviour can be changed in the 
resin.conf file.  To enable access to clients other than localhost:
</p>

<pre>
  &lt;resin:set var="resin_admin_external" value="true"/&gt;
</pre>

<p>
Once the file has been updated, you can
<a href="<?= $login_uri ?>">continue to the administration area</a>.
</p>

<p>
When prompted, use the username and password you provided.
</p>

<?php
}

  display_footer("login.php");
?>
