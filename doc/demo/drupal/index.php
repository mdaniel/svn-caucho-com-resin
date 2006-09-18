<?php
$drupal = jndi_lookup('java:comp/env/persistence/PersistenceContext/drupal');

$query = $drupal->createQuery("SELECT o FROM Node o");

?>
<table border='1'>
  <tr>
    <th>id</th>
    <th>type</th>
    <th>title</th>
    <th>status</th>
    <th>created</th>
    <th>changed</th>
    <th>comment</th>
    <th>promote</th>
    <th>moderate</th>
    <th>sticky</th>
    <th>user</th>
    <th>vocabulary</th>
  </tr>

<?php
foreach ($query->resultList as $node) {
?>
  <tr>
    <td><?= $node->id ?></td>
    <td><?= $node->type ?></td>
    <td><?= $node->title ?></td>
    <td><?= $node->status ?></td>
    <td><?= $node->created ?></td>
    <td><?= $node->changed ?></td>
    <td><?= $node->comment ?></td>
    <td><?= $node->promote ?></td>
    <td><?= $node->moderate ?></td>
    <td><?= $node->sticky ?></td>
    <td><?= $node->user ?></td>
    <td><?= $node->vocabulary ?></td>
  </tr>
<?php
}
?>

</table>
