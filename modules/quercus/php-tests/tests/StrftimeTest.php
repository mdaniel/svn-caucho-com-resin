<?php

class StrftimeTest extends PHPUnit_Framework_TestCase {

    public function testRFormat() {
        // See http://bugs.caucho.com/view.php?id=4626 for details
        // %R and %r Modifier were missing from formats
        
        $now = time();
        
        $this->assertThat(
                strftime("%H:%M", $now), 
                new PHPUnit_Framework_Constraint_IsEqual(strftime("%R", $now)));
        
        $this->assertThat(
                strftime("%I:%M:%S %p", $now), 
                new PHPUnit_Framework_Constraint_IsEqual(strftime("%r", $now)));
    }

}
