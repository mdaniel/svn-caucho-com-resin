<?php

require_once "WEB-INF/php/inc.php";

$title = "Resin Admin Login";

display_header("login.php", $title, null);

if ($error) {
  echo "<h3 class='fail'>Error: $error</h3>";
}

?><form method="POST" action="j_security_check?j_uri=index.php">
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

// destroy old temporary config file as soon as the user logs in or
// tries to generate a new password
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
  unlink("admin-users.xml");
}

if (! empty($digest)) {
  // generate temporary config file
  $file = fopen("admin-users.xml", "w");
  resin_var_dump("$file");
  fwrite($file, <<<EOF
<management xmlns="http://caucho.com/ns/resin">
  <user name="$digest_username" password="$digest"/>
</management>
EOF
);
  fclose($file);
?>

<ol>
<li>
Download and save the file below as
<em>/etc/resin/admin-users.xml</em> (If your resin.xml file is
not in /etc/resin, save this file as admin-users.xml in the same
directory as your resin.xml.)
<br/>
<br/>
<ul>
<li><a href="admin-users.xml">admin-users.xml</a></li>
</ul>
<br/>
</li>

<li>
By default, access to the administration application is limited
to the localhost.  The default behaviour can be changed in the 
resin.xml file.  To enable access to clients other than localhost:

<pre>
  &lt;resin:set var="resin_admin_external" value="true"/&gt;
</pre>
</li>

<li>
Once the file has been saved, you can
<a href="<?= $login_uri ?>">continue to the administration area</a>.
This will trigger a server restart, so just refresh your browser
until you see the login page again.
</li>

<li>
When prompted, use the username and password you provided.
</li>

<?php
}

  display_footer("login.php");
?>
