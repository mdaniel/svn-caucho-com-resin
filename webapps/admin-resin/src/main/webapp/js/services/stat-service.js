$(function() {
  StatService.init();
});

var StatService = {};

StatService.init = function() {
  console.log("StatService.init");
};

StatService.getName = function() {
  return "stat";
};

StatService.getMeterGraphPages = function() {
  var params = {
    "service" : StatService.getName(),
    "method" : "getMeterGraphPages",
    "url" : Main.getUrl()
  };
  
  return Jamp.sendRequest(params);
};

StatService.getData = function(serverId, name, beginTime, endTime, stepTime) {
  if (beginTime == null) {
    beginTime = -1;
  }
  
  if (endTime == null) {
    endTime = -1;
  }
  
  if (stepTime == null) {
    stepTime = -1;
  }
  
  if (beginTime < 0) {
    beginTime = new Date().getTime() - 1000 * 60 * 60;
  }
  
  if (endTime < 0) {
    endTime = beginTime + 1000 * 60 * 60;
  }
  
  if (stepTime < 0) {
    stepTime = Math.round((endTime - beginTime) / 1000);
    
    if (stepTime <= 0) {
      stepTime = 1;
    }
  }
  
  var params = {
    "service" : StatService.getName(),
    "method" : "getData",
    "url" : Main.getUrl(),
    "args" : [serverId, name, beginTime, endTime, stepTime]
  };
  
  return Jamp.sendRequest(params);
};
