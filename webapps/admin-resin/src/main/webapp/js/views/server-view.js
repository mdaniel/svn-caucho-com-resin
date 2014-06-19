$(function() {
  ServerView.init();

  View.addView(ServerView.getName(), ServerView);
});

var ServerView = {};

ServerView.init = function() {
  console.log("ServerView.init");
};

ServerView.getName = function() {
  return "server";
};

ServerView.hide = function() {
  $('#' + View.getContainerId()).empty();
};

ServerView.show = function() {
  console.log("ServerView.show");
  
  AdminService.getServerInfo()
    .done(function(data) {
      console.log("ServerView.show: done " + JSON.stringify(data))
      
      var html = '' +
        '<div>' +
          '<h2>Server</h2>' +
          '<h3>Server Info</h3>' +
          '<table>' +
            '<tr><th>Server ID</th>' + '<td>' + data._serverId + '</td></tr>' +
            '<tr><th>User</th>' + '<td>' + data._user + '</td></tr>' +
            '<tr><th>Machine</th>' + '<td>' + data._machine + '</td></tr>' +
            '<tr><th>Resin</th>' + '<td>' + data._resinVersion + '</td></tr>' +
            '<tr><th>JDK</th>' + '<td>' + data._jdkVersion + '</td></tr>' +
            '<tr><th>OS</th>' + '<td>' + data._osVersion + '</td></tr>' +
            '<tr><th>Watchdog Message</th>' + '<td>' + data._watchdogMessage + '</td></tr>' +
          '</table>' +
          '<h3>Server State</h3>' +
          '<table>' +
            '<tr><th>State</th><td>' + data._state + '</td></tr>' +
            '<tr><th>Heap</th><td>' + data._freeHeap + ' free (' + data._totalHeap + ' total)</td></tr>' +
            '<tr><th>Physical</th><td>' + data._freePhysical + ' free (' + data._totalPhysical + ' total)</td></tr>' +
            '<tr><th>Swap</th><td>' + data._freeSwap + ' free (' + data._totalSwap + ' total)</td></tr>' +
            '<tr><th>File Descriptors</th><td>' + data._fileDescriptors + ' total (' + data._maxFileDescriptors + ' max)</td></tr>' +
            '<tr><th>CPU Load</th><td>' + Main.formatDouble(data._cpuLoadAverage) + '</td></tr>' +
          '</table>' +
        '</div>';
      
      $('#' + View.getContainerId()).append(html);
    })
    .fail(function(error) {
      console.log("ServerView.show: error " + error)
    })
    .always(function() {
    });
};
