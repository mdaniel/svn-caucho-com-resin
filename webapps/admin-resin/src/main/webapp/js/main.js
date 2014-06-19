$(function() {
  Main.init();
  
  console.log(Main.getQueryParameters());
});

var Main = {};

Main._isLoggedIn = false;

Main._defaultView = null;
Main._currentView = null;

Main.init = function() {
  console.log("Main.init");
  
  Main._defaultView = DashboardView.getName();
  
  window.onhashchange = Main.onhashchange;
};

Main.getCurrentServerId = function() {
  return Main.getQueryParameters().serverId;
};

Main.getUrl = function() {
  return $("#baseUrl").val();
};

Main.login = function(user, pass) {
  console.log("Main.login: " + user + ", " + pass);
  
  Main._isLoggedIn = true;
};

Main.logout = function() {
  console.log("Main.logout");
};

Main.isLoggedIn = function() {
  return Main._isLoggedIn == true;
};

Main.onhashchange = function() {
  console.log("Main.onhashchange: " + location.hash);
  
  var viewName = Main.getQueryParameters().view;
  
  View.setView(viewName);
};

Main.updateUrl = function(paramMap) {
  var oldParamMap = Main.getQueryParameters();
  
  $.each(paramMap, function(key, value) {
    oldParamMap[key] = value;
  });
  
  var url = window.location.href;
  var p = url.indexOf("#");
  
  if (p >= 0) {
    url = url.substring(0, p);
  }
  
  url += "#";
  
  var i = 0;
  $.each(oldParamMap, function(key, value) {
    if (i++ > 0) {
      url += "&";
    }
    
    url += key + "=" + value;
  });
  
  window.location.href = url;
};

Main.getQueryParameters = function() {
  var map = {};

  var url = window.location.href;
  var p = url.indexOf("#");
  
  if (p < 0) {
    return map;
  }
  
  var fragment = url.substring(p + 1);
  var list = fragment.split("&");
  
  list.forEach(function(element, index, array) {
    var i = element.indexOf("=");
    
    if (i >= 0) {
      var key = element.substring(0, i);
      var value = element.substring(i + 1);
      
      map[key] = value;
    }
  });
    
  return map;
};

Main.escape = function(str) {
  if (str) {
    return str.replace(/([ #;?%&,.+*~\':"!^$[\]()=>|\/@])/g,'\\$1');      
  }
  else {
    return str;
  }
};

Main.escapeHtml = function(str) {
  if (str == null) {
    return str;
  }
  
  str = str.toString();
  
  str = str.replace('<', '&lt;');
  str = str.replace('>', '&gt;');
  str = str.replace('&', '&amp;');
  
  return str;
};

Main.formatDouble = function(value, digits) {
  if (digits == null) {
    digits = 2;
  }
  
  return Number(value).toFixed(digits);
};
