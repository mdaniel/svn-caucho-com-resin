<?php

class MktimeTest extends PHPUnit_Framework_TestCase {

    public function testFinally() {
        class TestFall {
            public $curPos = 0;
            public $posFinally = -1;
            public $posEnd = -1;
            
            public function basicRun() {
                $curPos++;
                
                try {
                    $curPos++;
                    throw new Exception("Test");
                    $curPos++;
                } finally {
                    $posFinally = $curPos;
                }
                
                $posEnd = $curPos;
            }
        }

        $test = new TestFall();
        $test->basicRun();
        
        $this->assertThat($test->posFinally, new PHPUnit_Framework_Constraint_IsEqual(2));
        $this->assertThat($test->posEnd, new PHPUnit_Framework_Constraint_IsEqual(2));
    }
    
    public function testFinallyReturn() {
        class TestFall2 {
            public $curPos = 0;
            public $posFinally = -1;
            public $posEnd = -1;
            
            public function basicRun() {
                $this->basicRun2();
                $posEnd = $curPos;
            }
            
            public function basicRun2() {
                $curPos++;
                
                try {
                    $curPos++;
                    throw new Exception("Test");
                    $curPos++;
                    return;
                } finally {
                    $posFinally = $curPos;
                    $curPos++;
                }
                
                
            }
        }

        $test = new TestFall2();
        $test->basicRun();
        
        $this->assertThat($test->posFinally, new PHPUnit_Framework_Constraint_IsEqual(2));
        $this->assertThat($test->posEnd, new PHPUnit_Framework_Constraint_IsEqual(3));
    }
    
    public function testCatchFinally() {
        class TestFall {
            public $curPos = 0;
            public $posFinally = -1;
            public $posEnd = -1;
            
            public function basicRun() {
                $curPos++;
                
                try {
                    $curPos++;
                    throw new Exception("Test");
                    $curPos++;
                } catch (Exception $ex) {
                    $curPos++;
                } finally {
                    $posFinally = $curPos;
                }
                
                $posEnd = $curPos;
            }
        }

        $test = new TestFall();
        $test->basicRun();
        
        $this->assertThat($test->posFinally, new PHPUnit_Framework_Constraint_IsEqual(3));
        $this->assertThat($test->posEnd, new PHPUnit_Framework_Constraint_IsEqual(3));
    }
}
