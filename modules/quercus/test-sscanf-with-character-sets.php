<?php
sscanf ( "J7", '%[_Z-A]%d', $startCol, $startRow );
assert ( $startCol === 'J', "expected \$startCol to be 'J', but was: '$startCol'" );
assert ( $startRow === 7, "expected \$startRow to be 7, but was: '$startRow'" );

sscanf ( "_7", '%[_-]%d', $startCol2, $startRow2 );
assert ( $startCol2 === '_', "expected \$startCol2 to be '_', but was: '$startCol2'" );
assert ( $startRow2 === 7, "expected \$startRow2 to be 7, but was: '$startRow2'" );

sscanf ( "-7", '%[-]%[0-9]', $startCol3, $startRow3 );
assert ( $startCol3 === '-', "expected \$startCol3 to be '-', but was: '$startCol3'" );
assert ( $startRow3 === '7', "expected \$startRow3 to be 7, but was: '$startRow3'" );

?>