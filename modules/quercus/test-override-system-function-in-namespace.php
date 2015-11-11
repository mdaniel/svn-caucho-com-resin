<?php
namespace Test {
	function file_exists($name) {
		return 'overriden_method';
	}
	class Test {
		public function test() {
			assert(file_exists("name") === 'overriden_method');
		}
	}
}

namespace {
	$test = new Test\Test();
	$test->test();
}