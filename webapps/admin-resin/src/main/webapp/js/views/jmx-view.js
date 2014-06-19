$(function() {
  JmxView.init();

  View.addView(JmxView.getName(), JmxView);
});

var JmxView = {};

JmxView.init = function() {
  console.log("JmxView.init");
};

JmxView.getName = function() {
  return "jmx";
};

JmxView.hide = function() {
  $('#' + View.getContainerId()).empty();
};

JmxView.show = function() {
  var serverId = Main.getQueryParameters().serverId;
  
  console.log("JmxView.show: " + serverId);
  
  var html = '' +
    '<div>' +
      '<h2>JMX (MBeans)</h2>' +
      '<div>' +
        '<h3>JVM</h3>' +
        '<div id="' + JmxView.getName() + '-jvm' + '"></div>' +
      '</div>' +
      '<div>' +
        '<h3>Resin</h3>' +
        '<div id="' + JmxView.getName() + '-resin' + '"></div>' +
      '</div>' +
      '<div>' +
        '<h3>App</h3>' +
        '<div id="' + JmxView.getName() + '-app' + '"></div>' +
      '</div>' +
    '</div>';
      
  $('#' + View.getContainerId()).append(html);
  
  AdminService.jmxQueryMBeans('*:*')
    .done(function(data) {
      console.log("JmxView.show: done " + JSON.stringify(data))
      
      data.sort();
      
      var helperFun = function(mbean, prefix, map) {
        var typeMap = map[prefix];
        
        if (typeMap == null) {
          typeMap = {};
          
          map[prefix] = typeMap;
        }
        
        var mbeanType = MBean.getType(mbean);
        
        var list = typeMap[mbeanType];
        
        if (list == null) {
          list = [];
          
          typeMap[mbeanType] = list;
        }
        
        list.push(mbean);
      };
      
      var jvmMap = {};
      var resinMap = {};
      var appMap = {};
      
      data.forEach(function(element, index) {        
        var p = element.ObjectName.indexOf(':');
        var prefix = element.ObjectName.substring(0, p);
        
        if (prefix.lastIndexOf('java', 0) === 0
            || prefix.lastIndexOf('com.sun', 0) === 0) {
          helperFun(element, prefix, jvmMap);
        }
        else if (prefix.lastIndexOf('resin', 0) === 0) {
          helperFun(element, prefix, resinMap);
        }
        else {
          helperFun(element, prefix, appMap);
        }
      });
      
      var showMBeans = function(map, name, containerId) {
        $.each(map, function(prefix, innerMap) {
          $.each(innerMap, function(type, list) {
            list.forEach(function(mbean, index) {
              var html = '' +
                '<div id="' + containerId + '-accordion' + '">' +
                  '<h4>' + MBean.getType(mbean) + '</h4>' +
                  '<table id="' + containerId + '-' + prefix + '-' + type + '"></table>' +
                '</div>';
              
              $('#' + Main.escape(containerId)).append(html);
              
              $.each(mbean, function(key, value) {
                var html = '' +
                  '<tr>' +
                    '<th>' + Main.escapeHtml(key) + '</th>' +
                    '<td>' + Main.escapeHtml(value) + '</td>' +
                  '</tr>';
                
                $('#' + Main.escape(containerId + '-' + prefix + '-' + type)).append(html);
              });
            });
          });
        });
      };
      
      showMBeans(jvmMap, 'JVM', JmxView.getName() + '-jvm');
      showMBeans(resinMap, 'Resin', JmxView.getName() + '-resin');
      showMBeans(appMap, 'App', JmxView.getName() + '-app');
    })
    .fail(function(error) {
      console.log("JmxView.show: error " + error)
    })
    .always(function() {
    });
};

var MBean = {};

MBean.getType = function(obj) {
  var name = obj.ObjectName;
  
  var p = name.indexOf('type=');
  
  if (p < 0) {
    return "";
  }
  
  var end = name.indexOf(',', p + 'type='.length);
  
  if (end < 0 || end > name.length) {
    end = name.length;
  }
    
  return name.substring(p + 'type='.length, end);
};