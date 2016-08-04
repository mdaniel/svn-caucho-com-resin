<?php

class MktimeTest extends PHPUnit_Framework_TestCase {

    public function testMktimeSpill() {
        $tests = array(
            // Testdata: hours, minutes, seconds, month, day, year
            array(22, 21, 1, 4, 21, 2016, "2016-04-21 22:21:01"), // Simple case
            array(30, 0, 0, 4, 21, 2016, "2016-04-22 06:00:00"), // Rollover hour
            array(-1, 0, 0, 4, 21, 2016, "2016-04-20 23:00:00"), // Negative hour
            array(-25, 0, 0, 4, 21, 2016, "2016-04-19 23:00:00"), // Negative hour > 24
            array(2, 67, 0, 4, 21, 2016, "2016-04-21 03:07:00"), // Rollover minute
            array(2, -5, 0, 4, 21, 2016, "2016-04-21 01:55:00"), // Negative minute
            array(2, -125, 0, 4, 21, 2016, "2016-04-20 23:55:00"), // Negative minute > 120
            array(2, 0, 65, 4, 21, 2016, "2016-04-21 02:01:05"), // Rollover second
            array(2, 1, -30, 4, 21, 2016, "2016-04-21 02:00:30"), // Negative minute
            array(2, 1, -150, 4, 21, 2016, "2016-04-21 01:58:30"), // Negative minute > 120
            array(2, 0, 0, 4, 31, 2016, "2016-05-01 02:00:00"), // Rollover day
            array(2, 0, 0, 4, -1, 2016, "2016-03-30 02:00:00"), // Negative day
            array(2, 0, 0, 4, -56, 2016, "2016-02-04 02:00:00"), // Negative day > 30
            array(2, 0, 0, 13, 4, 2016, "2017-01-04 02:00:00"), // Rollover month
            array(2, 0, 0, -1, 4, 2016, "2015-11-04 02:00:00"), // Negative month
            array(2, 0, 0, -13, 4, 2016, "2014-11-04 02:00:00"), // Negative month > 12
        );

        foreach ($tests as $test) {
            $mktime = mktime($test[0], $test[1], $test[2], $test[3], $test[4], $test[5]);
            $timeString = strftime("%Y-%m-%d %H:%M:%S", $mktime);
            $this->assertThat(
                    $timeString, 
                    new PHPUnit_Framework_Constraint_IsEqual($test[6]), 
                    sprintf("mktime(%d, %d, %d, %d, %d, %d)\t=> Should be: %s -- IS: %s", $test[0], $test[1], $test[2], $test[3], $test[4], $test[5], $test[6], $timeString));
        }
    }
    
    public function testGmmktime() {
        // This timestamp is the unixtimestamp for 2016-06-16T17:49:14+00:00: 1466099354
        $this->assertThat(
                gmmktime(17, 49, 14, 6, 16, 2016), 
                new PHPUnit_Framework_Constraint_IsEqual(1466099354));
    }

}
