<html>
<title>
Quercus&#153; Start Page
</title>

<!--
<?php

  function quercus_test()
  {
    return function_exists("resin_call_stack");
  }

?>
-->

<style type="text/css">
.message {
  margin: 10px;
  padding: 10px;
  border: 1px solid blue;
  background: #CCCCCC;
}

.footer {
  font-size: small;
  font-style: italic;
}

#failure {
    display: <?php echo "none"; ?>;
}

#failure_default_interpreter {
    display: none;
    display: <?php if (! quercus_test()) echo "block"; ?>;
}

#success {
    display: none;
    display: <?php if (quercus_test()) echo "block"; ?>;
}
</style>

<div>
Testing for Quercus&#153;...
</div>

<div class="message" id="failure">
PHP files doesn't appear to be interpreted by Quercus&#153;.
</div>

<div class="message" id="failure_default_interpreter">
PHP is being interpreted, but not by Quercus&#153;!  Please make sure the standard PHP interpreter is disabled.
</div>

<div class="message" id="success">
Congratulations!  Quercus&#153; seems to be working fine.  Have fun!
</div>

<div>
Documentation is available at <a href="http://quercus.caucho.com">http://quercus.caucho.com</a>
</div>

<hr/>

<div class="footer">
<p>Copyright 2007 <a href="http://www.caucho.com">Caucho Technology, Inc</a>.  All rights reserved.</a></p>
<p>Quercus&#153; is a registered trademark of Caucho Technology, Inc.</p>
</div>

</html>