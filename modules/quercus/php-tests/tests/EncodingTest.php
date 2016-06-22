<?php
class EncodingTest extends PHPUnit_Framework_TestCase
{
    public function testAnotherEncodingProblem()
    {
    	$array = ['Straße' => 'asdf'];
    	$temp_file = tempnam(sys_get_temp_dir(), 'test');
    	file_put_contents($temp_file, 'Straße');
    	$string = file_get_contents($temp_file);
    	unlink($temp_file);
    	$this->assertTrue(array_key_exists('Straße', $array));
    	$this->assertTrue(array_key_exists($string, $array));
    }
    
    public function testKeepUnicodeEscaping() 
    {
    	$this->assertEquals(7, strlen("m\u00b2"));
    	$this->assertEquals(7, strlen('m\u00b2'));
    }
    
    public function testReadingFromBinaryFile()
    {
    	$tmpfname1 = tempnam("", "UTF8Reader-problems");
    	$tmpfname2 = tempnam("", "UTF8Reader-problems");
    	
    	$string = "äöüß";
    	file_put_contents($tmpfname1, $string);
    	$read1 = file_get_contents($tmpfname1);
    	file_put_contents($tmpfname2, $read1);
    	$read2 = file_get_contents($tmpfname2);
    	unlink($tmpfname1);
    	unlink($tmpfname2);
    	$this->assertEquals($string, $read2, "$string should have been returned, but it was: $read2");
    }
    
    public function testFailing()
    {
    	$this->assertTrue(false);
    }
}
?>
