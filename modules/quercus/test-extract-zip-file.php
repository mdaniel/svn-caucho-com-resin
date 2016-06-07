<?php
$dbf = dbase_open('/Users/immanuelscheerer/Downloads/Buerolagen_A_Staedte_region/Buerolagen_A_Staedte_region.dbf', 0);
$dbf_data = dbase_get_record_with_names($dbf, 1);
//var_dump($dbf);
//var_dump($dbf_data);
dbase_close($dbf);
?>