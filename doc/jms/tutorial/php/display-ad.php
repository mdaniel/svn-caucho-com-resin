<table width=100%>
<tr><td align=center>
<?php

$ad_queue = message_get_queue("jms/AdQueue");
$control_queue = message_get_queue("jms/ControlQueue");

if (! $ad_queue) {
  echo "Unable to get ad queue!\n";
} elseif (! $control_queue) {
  echo "Unable to get control queue!\n";
} else {
  $ad = message_receive($ad_queue);

  if ($ad == null) {
    echo "No ads available on the queue";
  } else {
    echo "$ad";
  }

  if (! message_send($control_queue, "")) {
    echo "Unable to send message";
  }
}

?>
</td></tr>
<tr><td align=center>Content</td></tr>
</table>
