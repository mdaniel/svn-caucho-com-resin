<?php

require_once "WEB-INF/php/inc.php";

$title = "Resin Admin Login";

display_header("login.php", $title, null);

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
    <input name="j_password" width="40" type="secret"></input>
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

$digest_username = "admin";

include "digest.php";

if (! empty($digest)) {

?>

<?php
  /** XXX:
<p>
The following can now be added to the file
<code><b><?= $password_file ?></b></code>
to enable administration functionality. 
</p>

<pre>
&lt;authenticator>
 &lt;user name='<?= $digest_username ?>' password='<?= $digest ?>' roles='read,write'/>
&lt;/authenticator>
</pre>
  */
?>

<p>
The following can now be set in the resin.conf file
to enable administration functionality. 
</p>

<pre>
  &lt;resin:set var="resin_admin_password"  value="<?= $digest ?>"/&gt;
</pre>

<p>
By default, access to the administration application is limited
to the localhost.  The default behaviour can be changed in the 
resin.conf file.  To enable access to clients other than localhost:
</p>

<pre>
  &lt;resin:set var="resin_admin_localhost" value="false"/&gt;
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