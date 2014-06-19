$(function() {
  AdminService.init();
});

var AdminService = {};

AdminService.init = function() {
  console.log("AdminService.init");
};

AdminService.getName = function() {
  return "admin";
};

AdminService.getCurrentServerId = function() {
  var params = {
    "service" : AdminService.getName(),
    "method" : "getCurrentServerId",
    "url" : Main.getUrl()
  };
  
  return Jamp.sendRequest(params);
};

AdminService.getServers = function() {
  var params = {
    "service" : AdminService.getName(),
    "method" : "getServers",
    "url" : Main.getUrl()
  };
  
  return Jamp.sendRequest(params);
};

AdminService.getServerInfo = function(serverId) {
  var params = {
    "service" : AdminService.getName(),
    "method" : "getServerInfo",
    "url" : Main.getUrl(),
    "args" : [serverId]
  };
  
  return Jamp.sendRequest(params);
};

AdminService.getThreadingInfo = function(serverId) {
  var params = {
    "service" : AdminService.getName(),
    "method" : "getThreadingInfo",
    "url" : Main.getUrl(),
    "args" : [serverId]
  };
  
  return Jamp.sendRequest(params);
};

AdminService.getThreadScoreboard = function(serverId) {
  var params = {
    "service" : AdminService.getName(),
    "method" : "getThreadScoreboard",
    "url" : Main.getUrl(),
    "args" : [serverId]
  };
  
  return Jamp.sendRequest(params);
};

AdminService.getThreadDumps = function(serverId) {
  var params = {
    "service" : AdminService.getName(),
    "method" : "getThreadDumps",
    "url" : Main.getUrl(),
    "args" : [serverId]
  };
  
  return Jamp.sendRequest(params);
};

AdminService.getMemoryState = function(serverId) {
  var params = {
    "service" : AdminService.getName(),
    "method" : "getMemoryState",
    "url" : Main.getUrl(),
    "args" : [serverId]
  };
  
  return Jamp.sendRequest(params);
};

AdminService.jmxQueryMBeans = function(query) {
  var params = {
    "service" : AdminService.getName(),
    "method" : "jmxQueryMBeans",
    "url" : Main.getUrl(),
    "args" : [query]
  };
  
  return Jamp.sendRequest(params);
};
