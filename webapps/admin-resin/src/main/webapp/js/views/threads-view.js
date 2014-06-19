$(function() {
  ThreadsView.init();

  View.addView(ThreadsView.getName(), ThreadsView);
});

var ThreadsView = {};

ThreadsView.init = function() {
  console.log("ThreadsView.init");
};

ThreadsView.getName = function() {
  return "threads";
};

ThreadsView.hide = function() {
  $('#' + View.getContainerId()).empty();
};

ThreadsView.show = function() {
  var serverId = Main.getCurrentServerId();
  
  console.log("ThreadsView.show: " + serverId);
  
  var html = '' +
    '<div>' +
      '<h2>Threads</h2>' +
      '<div id="' + ThreadsView.getName() + '-' + 'summary' + '"></div>' +
      '<div id="' + ThreadsView.getName() + '-' + 'scoreboard' + '"></div>' +
      '<div id="' + ThreadsView.getName() + '-' + 'resin-graph' + '"></div>' +
      '<div id="' + ThreadsView.getName() + '-' + 'jvm-graph' + '"></div>' +
      '<div id="' + ThreadsView.getName() + '-' + 'dump' + '"></div>' +
    '</div>';
      
  $('#' + View.getContainerId()).append(html);
  
  ThreadsView.updateSummary();
  ThreadsView.updateScoreboard();
  //ThreadsView.updateResinGraph();
  //ThreadsView.updateJvmGraph();
  ThreadsView.updateDump();
};

ThreadsView.updateSummary = function() {
  AdminService.getThreadingInfo()
    .done(function(data) {
      console.log("ThreadsView.updateSummary: done " + JSON.stringify(data))
      
      var html = '' +
        '<div>' +
          '<h3>Summary</h3>' +
          '<table>' +
            '<tr>' +
              '<th colspan="3">Resin Threads</th>' +
              '<th colspan="6">JVM Threads</th>' +
            '</tr>' +
            '<tr>' +
              '<th>Active</th>' +
              '<th>Idle</th>' +
              '<th>Total</th>' +
              '<th>Runnable</th>' +
              '<th>Native</th>' +
              '<th>Blocked</th>' +
              '<th>Waiting</th>' +
              '<th>Total</th>' +
              '<th>Peak</th>' +
            '</tr>' +
            '<tr>' +
              '<td>' + data._activeResinThreads + '</td>' +
              '<td>' + data._idleResinThreads + '</td>' +
              '<td>' + data._totalResinThreads + '</td>' +
              '<td>' + data._runnableJvmThreads + '</td>' +
              '<td>' + data._nativeJvmThreads + '</td>' +
              '<td>' + data._blockedJvmThreads + '</td>' +
              '<td>' + data._waitingJvmThreads + '</td>' +
              '<td>' + data._totalJvmThreads + '</td>' +
              '<td>' + data._peakJvmThreads + '</td>' +
            '</tr>' +
          '</table>' +
        '</div>';
      
      $('#' + Main.escape(ThreadsView.getName() + '-' + 'summary')).append(html);
    })
    .fail(function(error) {
      console.log("ThreadsView.updateSummary: error " + error)
    })
    .always(function() {
    });
};

ThreadsView.updateScoreboard = function() {
  AdminService.getThreadScoreboard()
    .done(function(data) {
      console.log("ThreadsView.updateScoreboard: done " + JSON.stringify(data))
      
      var html = '' +
        '<div>' +
          '<h3>Scoreboard</h3>' +
          '<div id="' + ThreadsView.getName() + '-scoreboard-key"></div>' +
          '<div id="' + ThreadsView.getName() + '-scoreboard-table"></div>' +
        '</div>';
      
      $('#' + Main.escape(ThreadsView.getName() + '-' + 'scoreboard')).append(html);
      
      $.each(data._scoreboardMap, function(key, value) {
        var innerHtml = '' +
          '<div>' +
            '<div>' + key + '</div>' +
            '<div>' + value + '</div>' +
          '</div>';
        
        $('#' + Main.escape(ThreadsView.getName() + '-scoreboard-key')).append(innerHtml);
      });

    })
    .fail(function(error) {
      console.log("ThreadsView.updateScoreboard: error " + error)
    })
    .always(function() {
    });
};

ThreadsView.updateDump = function() {
  AdminService.getThreadDumps()
    .done(function(data) {
      console.log("ThreadsView.updateDump: done " + JSON.stringify(data))
      
      var helperFun = function(map, category, dump) {
        var list = map[category];
        
        if (list == null) {
          list = [];
          
          map[category] = list;
        }
        
        list.push(dump);
      };
      
      var map = {};
      
      data.forEach(function(element, index, list) {      
        if (element._isIdlePoolThread) {
          helperFun(map, "idle", element);
        }
        else if (element._isNative) {
          helperFun(map, "jni", element);
        }
        else if (element._state == "WAITING" || element._state == "TIMED_WAITING") {
          helperFun(map, "wait", element);
        }
        else {
          helperFun(map, "misc", element);
        }
      });
      
      $.each(map, function(key, value) {
        var html = '' +
        '<div>' +
          '<h3>' + key + '</h3>' +
          '<table id="' + ThreadsView.getName() + '-dump-' + key + '"' +
            '<tr>' +
              '<td>ID</td>' +
              '<td>Name</td>' +
              '<td>State</td>' +
              '<td>Method</td>' +
              '<td>Request URL</td>' +
            '</tr>' +
          '</table>' +
        '</div>';
        
        $('#' + Main.escape(ThreadsView.getName() + '-' + 'dump')).append(html);

        var list = value;
        
        list.forEach(function(element, index) {
          var html = '' +
            '<tr>' +
              '<td>' + element._id + '</td>' +
              '<td>' + element._name + '</td>' +
              '<td>' + element._state + '</td>' +
              '<td>' +
                '<div id="' + ThreadsView.getName() + '-dump-' + key + '-' + element._id + '-accordion">' +
                  '<div>' + (element._appClassName != null ? Main.escapeHtml(element._appClassName + '.' + element._appMethodName) + '()' : '') + '</div>' +
                  '<div>' +
                    '<pre class="stack-trace">' + Main.escapeHtml(element._stackTrace) + '</pre>' +
                  '</div>' +
                '</div>' +
              '</td>' +
              '<td>' + (element._url != null ? element._url : '') + '</td>' +
            '</tr>';
          
          $('#' + Main.escape(ThreadsView.getName() + '-dump-' + key)).append(html);
        });
      });
    })
    .fail(function(error) {
      console.log("ThreadsView.updateDump: error " + error)
    })
    .always(function() {
    });
};
