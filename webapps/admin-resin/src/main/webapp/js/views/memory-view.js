$(function() {
  MemoryView.init();

  View.addView(MemoryView.getName(), MemoryView);
});

var MemoryView = {};

MemoryView.init = function() {
  console.log("MemoryView.init");
};

MemoryView.getName = function() {
  return "memory";
};

MemoryView.hide = function() {
  $('#' + View.getContainerId()).empty();
};

MemoryView.show = function() {
  var serverId = Main.getQueryParameters().serverId;
  
  console.log("MemoryView.show: " + serverId);
  
  var html = '' +
    '<div>' +
      '<h2>Memory</h2>' +
      '<div id="' + MemoryView.getName() + '-memory">' + '</div>' +
      '<div id="' + MemoryView.getName() + '-free-graph">' + '</div>' +
      '<div id="' + MemoryView.getName() + '-gc-graph">' + '</div>' +
    '</div>';
      
  $('#' + View.getContainerId()).append(html);
  
  AdminService.getMemoryState(serverId)
    .done(function(data) {
      console.log("MemoryView.show: done " + JSON.stringify(data))
      
      var html = '' +
        '<table>' +
          '<tr>' +
            '<th>Pool</th>' +
            '<th>% free</th>' +
            '<th>Free</th>' +
            '<th>Used</th>' +
            '<th>Max</th>' +
            '<th>Committed</th>' +
          '</tr>' +
          '<tr>' +
            '<td>CodeCache</td>' +
            '<td>' + Main.formatDouble((data._codeCacheFree / data._codeCacheMax * 100)) + '%</td>' +
            '<td>' + Main.formatDouble((data._codeCacheFree / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._codeCacheUsed / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._codeCacheMax / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._codeCacheCommitted / (1024 * 1024))) + 'M</td>' +
          '</tr>' +
          '<tr>' +
            '<td>Eden</td>' +
            '<td>' + Main.formatDouble((data._edenFree / data._edenMax * 100)) + '%</td>' +
            '<td>' + Main.formatDouble((data._edenFree / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._edenUsed / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._edenMax / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._edenCommitted / (1024 * 1024))) + 'M</td>' +
          '</tr>' +
          '<tr>' +
            '<td>PermGen</td>' +
            '<td>' + Main.formatDouble((data._permGenFree / data._permGenMax * 100)) + '%</td>' +
            '<td>' + Main.formatDouble((data._permGenFree / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._permGenUsed / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._permGenMax / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._permGenCommitted / (1024 * 1024))) + 'M</td>' +
          '</tr>' +
          '<tr>' +
            '<td>Survivor</td>' +
            '<td>' + Main.formatDouble((data._survivorFree / data._survivorMax * 100)) + '%</td>' +
            '<td>' + Main.formatDouble((data._survivorFree / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._survivorUsed / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._survivorMax / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._survivorCommitted / (1024 * 1024))) + 'M</td>' +
          '</tr>' +
          '<tr>' +
            '<td>Tenured</td>' +
            '<td>' + Main.formatDouble((data._tenuredFree / data._tenuredMax * 100)) + '%</td>' +
            '<td>' + Main.formatDouble((data._tenuredFree / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._tenuredUsed / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._tenuredMax / (1024 * 1024))) + 'M</td>' +
            '<td>' + Main.formatDouble((data._tenuredCommitted / (1024 * 1024))) + 'M</td>' +
          '</tr>' +
        '</table>';
      
      $('#' + Main.escape(MemoryView.getName() + '-memory')).append(html);
    })
    .fail(function(error) {
      console.log("MemoryView.show: error " + error)
    })
    .always(function() {
    });
};
