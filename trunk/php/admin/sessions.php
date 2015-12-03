<?php

require_once "WEB-INF/php/inc.php";

admin_init("", false, true);

$r_host = $_GET['host'];

if ($r_host === '')
  $r_host = 'default';

$manager_suffix = $_GET['manager-suffix'];

$r_webapp = $_GET['webapp'];

if ($r_webapp === "")
  $r_webapp = "/";

$query = "resin:type=WebApp,Host=" . $r_host . ",name=" . $r_webapp;

$beans = $g_mbean_server->query($query);

$webapp = $beans[0];

$session_manager = $webapp->SessionManager;

if ($session_manager->SessionActiveCount == 0) {
  echo "No Active Sessions";

  return;
}

$sessions_json = $session_manager->sessionsAsJsonString();

//[{"AccessTime":1335326457442,"CreationTime":1335326237342,"IdleIsSet":false,"IdleTimeout":1800000,"LastSaveLength":112,"LastUseTime":1335326459502,"New":false,"SessionId":"aaacRWFwH0LzGcuiiiGBt","UseCount":1.0,"Valid":true}]

$session_table_id = "session-table-" . $manager_suffix;

$properties
  = array("SessionId", "CreationTime", "AccessTime", "LastUseTime",
  "IdleTimeout", "IdleIsSet", "New", "Valid",
  "UseCount", "LastSaveLength");

$properties_text
  = array("Session Id", "Creation Time", "Access Time", "Last UseTime",
  "Idle Timeout", "IdleIsSet", "New", "Valid",
  "Use Count", "Last Save Length");

$properties_format = array("CreationTime" => "date",
  "AccessTime" => "date",
  "LastUseTime" => "date",
  "LastSaveLength" => "memory",
  "IdleTimeout" => "timeout"
);

$properties_header = array (
);

echo "<table id='${session_table_id}' class='data-detail'>\n";
echo " <tr>\n";

foreach ($properties_text as $property) {
  echo"  <th scope='col' title='" . gettext('$property') . "'>";
  echo gettext("$property");
  echo "  </th>\n";
}

echo " </tr>\n";

echo " </table>\n";
echo "<script language='javascript'>\n";
echo ("var sessions = jQuery.parseJSON('$sessions_json');\n");
echo ("var table = $('#${session_table_id}');\n");
echo ("var data = '';\n");
echo ("for (var i = 0; i < sessions.length; i++)\n");
echo ("{\n");
echo ("  var session = sessions[i];\n");
echo ("  data += '<tr>';\n");

foreach ($properties as $property) {
  echo ("  data += '<td>';\n");
  $format = $properties_format[$property];

  if ($format === "date")
    echo ("  data += formatDate(session.${property});\n");
  else if ($format === "memory")
    echo ("  data += formatMemory(session.${property});\n");
  else if ($format === "timeout")
    echo ("  data += formatTimeout(session.${property});\n");
  else
    echo ("  data += session.${property};\n");
  echo ("  data += '</td>';\n");
}

echo ("  data += '</tr>';\n");
echo ("}\n");
echo ("table.append(data);\n");
echo "</script>";
?>
