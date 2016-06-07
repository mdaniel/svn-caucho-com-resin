<?php

$tmpfname1 = tempnam("", "UTF8Reader-problems");

$string = "äöüß";
file_put_contents($tmpfname1, $string);
$read1 = file_get_contents($tmpfname1);
assert($string == json_decode(json_encode([0 => $read1]))[0]);
?>