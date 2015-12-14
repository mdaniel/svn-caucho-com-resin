<?php

class ParentClass {
	private $parent;
	public $name;
	
	public function __construct($name, $parent = null) {
		$this->name = $name;
		$this->parent = $parent;
	}
	
	public function getParent() {
		return $this->parent;
	}
	
}

class ChildClass extends ParentClass {
	private $parent;
	
	public function getParent() {
		return $this->parent;
	}
	
	public function setParent($parent) {
		$this->parent = $parent;
	}
}

$parent1 = new ParentClass('parent1');
$parent2 = new ParentClass('parent2');
$child = new ChildClass('child1', $parent2);
$child->setParent($parent2);
// echo $child->getParent()->name;

$property = new ReflectionProperty('ParentClass', 'parent');
$property->setAccessible(true);
$property->setValue($child, $parent1);
assert('parent2' == $child->getParent()->name, 'parent value of ParentClass should be changed, not of ChildClass');

?>