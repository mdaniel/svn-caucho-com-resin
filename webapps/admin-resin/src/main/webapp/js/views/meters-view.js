$(function() {
  MetersView.init();

  View.addView(MetersView.getName(), MetersView);
});

var MetersView = {};

MetersView._graphManager = null;

MetersView.init = function() {
  console.log("MetersView.init");
};

MetersView.getName = function() {
  return "meters";
};

MetersView.hide = function() {
  $('#' + View.getContainerId()).empty();
};

MetersView.show = function() {
  var serverId = Main.getQueryParameters().serverId;
  
  console.log("MetersView.show: " + serverId);
  
  var html = '' +
    '<div>' +
      '<h2>Meters</h2>' +
      '<div id="' + MetersView.getName() + '-meters' + '"></div>' +
    '</div>';
      
  $('#' + View.getContainerId()).append(html);
  
  StatService.getMeterGraphPages(serverId)
    .done(function(data) {
      console.log("MetersView.show: done " + JSON.stringify(data));
      
      var updateIntervalMs = 1000 * 60;
      _graphManager = new GraphManager(updateIntervalMs);
      
      var pageCount = 0;
      var containerId = MetersView.getName() + '-meters';
      
      data.forEach(function(page, index) {
        MetersView.createPage(page, containerId, pageCount++, _graphManager);
      });
      
      _graphManager.start();

    })
    .fail(function(error) {
      console.log("MetersView.show: error " + error)
    })
    .always(function() {
    });
};

MetersView.createPage = function(page, containerId, pageCount, graphManager) {  
  var html = '' +
    '<div id="' + containerId + '-' + pageCount + '">' +
      '<p>' + page._name + '</p>' +
    '</div>';
  
  $('#' + Main.escape(containerId)).append(html);
  
  var sectionCount = 0;
  
  page._meterSections.forEach(function(section, index) {
    MetersView.createSection(section, containerId + '-' + pageCount, sectionCount++, graphManager);
  });
}

MetersView.createSection = function(section, containerId, sectionCount, graphManager) {
  var html = '' +
    '<div id="' + containerId + '-' + sectionCount + '">' +
      '<p>' + section._name + '</p>' +
    '</div>';
  
  $('#' + Main.escape(containerId)).append(html);
  
  var meterCount = 0;
  
  section._meterGraphs.forEach(function(meterGraph, index) {
    MetersView.createGraph(meterGraph, containerId + '-' + sectionCount, meterCount++, graphManager);
  });
};

MetersView.createGraph = function(meterGraph, containerId, meterCount, graphManager) {
  var html = '' +
    '<div id="' + containerId + '-' + meterCount + '" class="graph-container">' +
    '</div>';
  
  $('#' + Main.escape(containerId)).append(html);
  
  var graph = new Graph(meterGraph._name, containerId + '-' + meterCount);
  
  var statMap = {};
  meterGraph._meterNames.forEach(function(meterName, index) {
    meterName = '00|' + meterName;
    
    statMap[meterName] = meterName;
  });
  
  graphManager.addGraph(graph, statMap);
};
