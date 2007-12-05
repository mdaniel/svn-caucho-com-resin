<?php
$hessian = java_bean("hessian");
$rest = java_bean("rest");
$soap = java_bean("soap");
$vm = java_bean("vm");
?>
<pre>
From Hessian: <?= $hessian->hello() ?>
From REST: <?= $rest->hello() ?>
From SOAP: <?= $soap->hello() ?>
From VM: <?= $vm->hello() ?>
</pre>

<ul>
<li><a href="demo.jsp">JSP</a></li>
<li><a href="index.xtp">Tutorial</a></li>
<ul>
