<?php

if (array_key_exists("message", $_POST)) {
  $queue = new JMSQueue("jms/Queue");

  if (! $queue) {
    echo "Unable to get message queue!\n";
  } else {
    if ($queue->send($_POST["message"]) == TRUE) {
      echo "Successfully sent message '" . $_POST["message"] . "'";
    } else {
      echo "Unable to send message '" . $_POST["message"] . "'";
    }
  }
}

?>
<form method=POST action="">
  <input type="text" name="message" />
  <br />
  <input type="submit" value="Send message" />
</form>

<p>
<a href="view-log">See all messages sent so far.</a>
</p>
