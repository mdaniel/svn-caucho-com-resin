<?php // 
//Eine Beispielklasse
class MyClass {

    /**
     * Eine Testfunktion
     *
     * Der erste Parameter muss ein Objekt des Typs OtherClass sein
     */
    public function test(OtherClass $otherclass) {
//        echo $otherclass->var;
    }

    /**
     * Eine weitere Testfunktion
     *
     * Der erste Parameter muss ein Array sein
     */
    public function test_array(array $input_array) {
//        print_r($input_array);
    }

    /**
     * Der erste Parameter muss ein Iterator sein
     */
    public function test_interface(Traversable $iterator) {
//        echo get_class($iterator);
    }

    /**
     * Der erste Parameter muss ein callable sein
     */
    public function test_callable(callable $callback, $data) {
//        call_user_func($callback, $data);
    }

    /**
     *  Akzeptiert NULL Werte 
     */
    public function test_nullable_stdclass(stdClass $obj = NULL) {
        
    }

}

// Eine weitere Beispielklasse
class OtherClass {

    public $var = 'Hallo Welt';

}

class TypeHintingTest extends PHPUnit_Framework_TestCase {

    private $error_detected = false;
    private $myclass;
    private $otherclass;

    public function errorDetected($errorno, $errstr, $errfile, $errline) {
        $this->error_detected = true;
    }

    protected function setUp() {
        $this->myclass = new MyClass;
        $this->otherclass = new OtherClass;
        $this->error_detected = false;
        set_error_handler(array($this, 'errorDetected'));
    }

    protected function tearDown() {
        restore_error_handler();
    }

    public function testOtherClassString() {
        // Fatal Error: Argument 1 must be an object of class OtherClass
        $this->myclass->test('hello');
        $this->assertTrue($this->error_detected);
    }

    public function testOtherClassStdClass() {
        // Fatal Error: Argument 1 must be an instance of OtherClass
        $foo = new stdClass;
        $this->myclass->test($foo);
        $this->assertTrue($this->error_detected);
    }

    public function testOtherClassNULL() {
        // Fatal Error: Argument 1 must not be null
        $this->myclass->test(null);
        $this->assertTrue($this->error_detected);
    }

    public function testOtherClassOtherClass() {
        // Funktioniert: Gibt Hallo Welt aus
        $this->myclass->test($this->otherclass);
        $this->assertFalse($this->error_detected);
    }

    public function testArrayString() {
        // Fatal Error: Argument 1 must be an array
        $this->myclass->test_array('a string');
        $this->assertTrue($this->error_detected);
    }

    public function testArrayArray() {
        // Funktioniert: Gibt das Array aus
        $this->myclass->test_array(array('a', 'b', 'c'));
        $this->assertFalse($this->error_detected);
    }

    public function testInterfaceImplementation() {
        // Funktioniert: Gibt das ArrayObject aus
        $this->myclass->test_interface(new ArrayObject(array()));
        $this->assertFalse($this->error_detected);
    }

    public function testCallableCallable() {
        // Funktioniert: Gibt int(1) aus
        $this->myclass->test_callable('var_dump', 1);
        $this->assertFalse($this->error_detected);
    }

    public function testNullableStdClassNULL() {
        $this->myclass->test_nullable_stdclass(NULL);
        $this->assertFalse($this->error_detected);
    }

    public function testNullableStdClassStdClass() {
        $this->myclass->test_nullable_stdclass(new stdClass);
        $this->assertFalse($this->error_detected);
    }

    public function testNullableStdClassArray() {
        $this->myclass->test_nullable_stdclass(array());
        $this->assertTrue($this->error_detected);
    }

}
