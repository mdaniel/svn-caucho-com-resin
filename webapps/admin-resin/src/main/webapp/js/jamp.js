var Jamp = {};

//
// param map: {"service", "method", "args", "id", "url"}
//
Jamp.sendRequest = function(paramMap) {
  var service = paramMap.service;
  var method = paramMap.method;
  
  var args = paramMap.args;

  var id = paramMap.id;
  var url = paramMap.url;

  url += "/" + service;

  if (id != null) {
    url += "/" + id;
  }

  url += "?m=" + method;
  
  if (args != null) {
    for (var i = 0; i < args.length; i++) {
      url += "&p" + i + "=" + args[i];
    }
  }
  
  url += "&jsoncallback=?";
  
  console.log(url);
  
  return $.getJSON(url);
};
