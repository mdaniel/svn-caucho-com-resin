$(function() {
  DashboardView.init();

  View.addView(DashboardView.getName(), DashboardView);
});

var DashboardView = {};

DashboardView.init = function() {
  console.log("DashboardView.init");
};

DashboardView.getName = function() {
  return "dashboard";
};

DashboardView.isDefault = function() {
  return true;
};

DashboardView.hide = function() {
  $('#' + View.getContainerId()).empty();
};

DashboardView.show = function() {
  console.log("DashboardView.show");
  
  var html = '' +
    '<h2>Cluster Dashboard</h2>' +
    '<div id="' + DashboardView.getName() + '-servers' + '"></div>';
  
  $('#' + View.getContainerId()).append(html);
  
  AdminService.getServers()
    .done(function(data) {
      console.log("DashboardView.show: done " + data)
      
      data.forEach(function(element, index, array) {
        DashboardView.createServerBlock(element);
        //DashboardView.createServerBlock(element);
        //DashboardView.createServerBlock(element);
      });
    })
    .fail(function(error) {
      console.log("DashboardView.show: error " + error)
    })
    .always(function() {
    });
};

DashboardView.createServerBlock = function(serverId) {
  console.log("DashboardView.createServerBlock: " + serverId);
  
  var html = '' +
    '<a href="#view=' + ServerView.getName() + '&serverId=' + serverId + '"' +
      '<div>' +
        '<h3>' + serverId + '</h3>' +
        '<div><img src="images/commons/High-contrast-network-server.png" height="96px"></div>' +
        '<div id="' + DashboardView.getName() + '-' + serverId + '-machine' + '"></div>' +
        '<div id="' + DashboardView.getName() + '-' + serverId + '-load' + '"></div>' +
      '</div>' +
    '</a>';
  
  $('#' + DashboardView.getName() + '-servers').append(html); 
  
  AdminService.getServerInfo(serverId)
    .done(function(data) {
      console.log("DashboardView.createServerBlock: done " + JSON.stringify(data))
            
      $('#' + Main.escape(DashboardView.getName() + '-' + serverId + '-machine')).append(data._machine);
      
      $('#' + Main.escape(DashboardView.getName() + '-' + serverId + '-load')).append("Load: " + Main.formatDouble(data._cpuLoadAverage));

    })
    .fail(function(error) {
      console.log("DashboardView.createServerBlock: error " + error)
    })
    .always(function() {
    });
};