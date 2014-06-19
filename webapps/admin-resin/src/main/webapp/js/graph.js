var Graph = function(title, containerId, isShowLegendAtBottom, cssClass, legendColumns) {
  this.COLOR_LIST = ["#ff3030",  // red
                     "#30b0ff",  // azure
                     "#906000",  // brown
                     "#ff9030",  // orange
                     "#3030ff",  // blue
                     "#000000",  // black
                     "#50b000",  // green
                     "#d030d0",  // magenta
                     "#008080",  // cyan
                     "#b03060",  // rose
                     "#e090ff",  // indigo
                     "#c0c0c0",  // gray
                     "#408040"]; // forest green
  
  if (cssClass == null) {
    cssClass = 'mini-graph';
  }
  
  if (legendColumns == null) {
    legendColumns = 1;
  }
  
  this._title = title;
  this._containerId = containerId;
  
  this._hostId = containerId + '-graph';
  this._legendId;
  
  this._valueMap = {};
  
  var html = '' +
    '<div>' + title + '</div>';
  
  $('#' + Main.escape(containerId)).append(html);
  
  var html = '' +
    '<div id="' + this._hostId + '" class="' + cssClass + '">' +
    '</div>';
  
  $('#' + Main.escape(containerId)).append(html);
  
  if (isShowLegendAtBottom) {
    this._legendId = containerId + '-mini-legend';
    this._legendColumns = legendColumns;
  }
  
  this.addPoint = function(key, point) {
    list = this._valueMap[key];
    
    if (list == null) {
      list = [];
      
      this._valueMap[key] = list;
    }
    
    list.push(point);
  };
  
  this.setPoints = function(key, points) {
    this._valueMap[key] = points;
  }
  
  this.plot = function() {
    var list = [];
    
    var qThis = this;
    
    var i = 0;
    $.each(this._valueMap, function(key, points) {
      var color = qThis.COLOR_LIST[i++ % qThis.COLOR_LIST.length];
      
      var map = {
        'label' : key,
        'color' : color,
        'data'  : points
      };
      
      list.push(map);
    });
    
    var options = {};
    
    options.xaxis = {
      'mode' : 'time',
      'timezone' : 'browser'
    };
    
    options.yaxis = {
      'tickFormatter' : this.tickFormatter
    };
    
    if (this._legendId != null) {
      options.legend = {
        'container' : ('#' + Main.escape(this._legendId)),
        'noColumns' : this._legendColumns
      };
    }
    
    $.plot('#' + Main.escape(this._hostId), list, options);
    
    this._valueMap = {};
  };
  
  this.tickFormatter = function(value, axis) {
    if (value >= 1e9) {
      return (value / 1e9).toFixed(1) + "G";
    }
    else if (value >= 1e6) {
      return (value / 1e6).toFixed(1) + "M";
    }
    else if (value > 1e3) {
      return (value / 1e3).toFixed(1) + "k";
    }
    else {
      return value.toFixed(axis.tickDecimals);
    }
  };
};

var GraphManager = function(updateInterval, periodStartTime, periodEndTime) {
  this._updateInterval = updateInterval;
  this._periodStartTime = periodStartTime;
  this._periodEndTime = periodEndTime;
  
  this._graphList = [];
  
  this._timerId = null;
  
  this.setPeriod = function(updateInterval, periodStartTime, periodEndTime) {
    this._updateInterval = updateInterval;
    this._periodStartTime = periodStartTime;
    this._periodEndTime = periodEndTime;
  };
  
  this.addGraph = function(graph, statNameMap) {
    var entry = new GraphEntry(graph, statNameMap);
    
    this._graphList.push(entry);
  };
  
  this.scheduleTimer = function() {
    console.log("GraphManager.scheduleTimer: " + this._updateInterval);
    
    this._timerId = window.setTimeout(this.start.bind(this), this._updateInterval);
  };
  
  this.start = function() {
    var endTime = this._periodEndTime;
    
    if (endTime == null) {
      endTime = new Date().getTime();
    }
    
    if (this._period == null) {
      this._period = endTime - this._periodStartTime;
    }
    
    this._graphList.forEach.bind(this);
    
    this._graphList.forEach(function(entry, index) {
      
      var statCount = 0;
            
      $.each(entry._statNameMap, function(name, value) {
        statCount++;
        
        StatService.getData(null, value, this._periodStartTime, endTime)
          .done(function(data) {
            console.log("GraphUpdateService.start: done " + JSON.stringify(data));
            
            data.forEach(function(stat) {
              entry._graph.addPoint(name, [stat._time, stat._sum / stat._count]);
            });
          })
          .fail(function(error) {
            console.log("GraphUpdateService.start: fail " + error);
          })
          .always(function() {
            statCount--;
            
            if (statCount == 0) {
              entry._graph.plot();
            }
          });
      });
    });
        
    if (this._updateInterval != null) {
      this._periodStartTime = endTime - this._period + this._updateInterval;
      
      this.scheduleTimer();
    }
  };
  
  this.stop = function() {
    window.clearTimeout(this._timerId);
  };
};

var GraphEntry = function(graph, statNameMap) {
  this._graph = graph;
  this._statNameMap = statNameMap;
};