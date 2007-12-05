<?php
$hessian = java_bean("hessian");
$rest = java_bean("rest");
$soap = java_bean("soap");
$vm = java_bean("vm");
?>

From Hessian: <?= $hessian->hello() ?><br>
From REST: <?= $rest->hello() ?><br>
From SOAP: <?= $soap->hello() ?><br>
From VM: <?= $vm->hello() ?><br>

<ul>
<li><a href="demo.jsp">JSP</a></li>
<li><a href="index.xtp">Tutorial</a></li>
<ul>
