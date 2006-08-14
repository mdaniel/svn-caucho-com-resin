<?php

$result_queue = new JMSQueue("jms/OutboundQueue");
$request_queue = new JMSQueue("jms/InboundQueue");

if (! $result_queue) {
  echo "Unable to get result queue!\n";
} elseif (! $request_queue) {
  echo "Unable to get request queue!\n";
} else {
  $result = $result_queue->receive();

  if ($result == null) {
    echo "No results available on the queue\n";
  } else {
    echo "received result: <pre>\n";
    echo htmlspecialchars($result) . "\n";
    echo "</pre>\n";
  }

  $request = 
    "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-enveloper\">" .
    "<env:Body>" .
    "<m:hello xmlns:m=\"urn:hello\">" .
    "</m:hello>" .
    "</env:Body>" .
    "</env:Envelope>";

  if (! $request_queue->send($request)) {
    echo "Unable to send request\n";
  }
}

?>
