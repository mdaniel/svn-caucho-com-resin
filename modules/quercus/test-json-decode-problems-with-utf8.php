<?php

$tmpfname1 = tempnam("", "UTF8Reader-problems");
$tmpfname2 = tempnam("", "UTF8Reader-problems");

$string = '"ß"';
//var_dump(json_decode($string));
file_put_contents($tmpfname1, $string);
$read1 = file_get_contents($tmpfname1);
//var_dump(json_decode($read1));
file_put_contents($tmpfname2, $read1);
$read2 = file_get_contents($tmpfname2);
//var_dump(json_decode($read2));
unlink($tmpfname1);
unlink($tmpfname2);
assert($string == $read2, "$string should have been returned, but it was: $read2");
?>