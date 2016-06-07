<?php

class Test {
	public $foo;

	function test() {
		$filename = "/tmp/test2_".rand().".php";
		file_put_contents($filename, '<?php echo $this->foo;');
		$this->foo = "bar";
		include $filename;
	}
}

(new Test())->test();
