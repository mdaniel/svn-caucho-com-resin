<?php
	$testString = '{"äöüß":"äöüß"}';
	$compressed = gzcompress($testString, 9);
	$uncompressed = gzuncompress($compressed);
	assert($testString === $uncompressed, "$testString !== $uncompressed");
?>