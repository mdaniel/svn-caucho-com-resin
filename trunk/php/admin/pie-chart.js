/**
 * Derived from work by http://www.elated.com/, licensed under a Creative Commons Attribution 3.0 Unported License. 
 * http://www.elated.com/articles/snazzy-animated-pie-chart-html5-jquery/
 * */

         
var colors = [
              "#FDE982",
              "#FDE255",
              "#FDD682",
              "#FAD81D",
              "#FDC855",
              "#FAB41D",
              "#BB943F",
              "#A37209",
              "#BBA83F",
              "#A38B09",
             ];

var miscColor = "#c0c0c0"; // gray

function PieChartBuilder() {
};

PieChartBuilder.prototype.setTable = function(table) {
  this.table = table;
};

PieChartBuilder.prototype.setChartId = function(chartId) {
  this.chartId = chartId;
};

PieChartBuilder.prototype.setLabelClass = function(labelClass) {
  this.labelClass = labelClass;
};

PieChartBuilder.prototype.setDataClass = function(dataClass) {
  this.dataClass = dataClass;
};

PieChartBuilder.prototype.setLegendClass = function(legendClass) {
  this.legendClass = legendClass;
};

PieChartBuilder.prototype.setNumTopSlices = function(numTopSlices) {
  this.numTopSlices = numTopSlices;
};

PieChartBuilder.prototype.setLabelId = function(labelId) {
  this.labelId = labelId;
};

PieChartBuilder.prototype.create = function() {
  return pieChart(this.table, this.chartId, this.labelId,
                  this.labelClass, this.dataClass, this.legendClass,
                  this.numTopSlices);
};

function pieChart(table, chartId, labelId,
                  labelClass, dataClass, legendClass, 
                  numTopSlices) {

  // Config settings
  var chartSizePercent = 60;                        // The chart radius relative to the canvas width/height (in percent)
  var sliceBorderWidth = 1;                         // Width (in pixels) of the border around each slice
  var sliceBorderStyle = "#fff";                    // Colour of the border around each slice
  var sliceGradientColour = "#FDE982";                 // Colour to use for one end of the chart gradient
  var maxPullOutDistance = 0.05;                       // How far, in % of the radius, to pull slices out when clicked
  var pullOutFrameStep = 0.10;                        // How percent to move a slice with each animation frame
  var pullOutFrameInterval = 40;                    // How long (in ms) between each animation frame
  var pullOutLabelPadding = 0.30;                     // Padding between pulled-out slice and its label (% of radius) 
  var pullOutLabelFont = "bold 16px 'Trebuchet MS', Verdana, sans-serif";  // Pull-out slice label font
  var pullOutValueFont = "bold 12px 'Trebuchet MS', Verdana, sans-serif";  // Pull-out slice value font
  var pullOutValuePrefix = "";                     // Pull-out slice value prefix
  var pullOutShadowColour = "rgba( 0, 0, 0, .5 )";  // Colour to use for the pull-out slice shadow
  var pullOutShadowOffsetX = 5;                     // X-offset (in pixels) of the pull-out slice shadow
  var pullOutShadowOffsetY = 5;                     // Y-offset (in pixels) of the pull-out slice shadow
  var pullOutShadowBlur = 5;                        // How much to blur the pull-out slice shadow
  var pullOutBorderWidth = 2;                       // Width (in pixels) of the pull-out slice border
  var pullOutBorderStyle = "#333";                  // Colour of the pull-out slice border
  var chartStartAngle = -.5 * Math.PI;              // Start the chart at 12 o'clock instead of 3 o'clock

  // Declare some variables for the chart
  var canvas;                       // The canvas element in the page
  var currentPullOutSlice = -1;     // The slice currently pulled out (-1 = no slice)
  var currentPullOutDistance = 0;   // How many pixels the pulled-out slice is currently pulled out in the animation
  var animationId = 0;              // Tracks the interval ID for the animation created by setInterval()
  var chartData = [];               // Chart data (labels, values, and angles)
  var chartColours = [];            // Chart colours (pulled from the HTML table)
  var totalValue = 0;               // Total of all the values in the chart
  var canvasWidth;                  // Width of the canvas, in pixels
  var canvasHeight;                 // Height of the canvas, in pixels
  var centreX;                      // X-coordinate of centre of the canvas/chart
  var centreY;                      // Y-coordinate of centre of the canvas/chart
  var chartRadius;                  // Radius of the pie chart, in pixels

  // Set things up and draw the chart
  init();


  /**
   * Set up the chart data and colours, as well as the chart and table click handlers,
   * and draw the initial pie chart
   */

  function init() {

    // Get the canvas element in the page
    canvas = document.getElementById(chartId);

    // Exit if the browser isn't canvas-capable
    if ( typeof canvas.getContext === 'undefined' ) return;

    // Initialise some properties of the canvas and chart
    canvasWidth = canvas.width;
    canvasHeight = canvas.height;
    centreX = canvasWidth / 2;
    centreY = canvasHeight / 2;
    chartRadius = Math.min( canvasWidth, canvasHeight ) / 2 * ( chartSizePercent / 100 );

    // Grab the data from the table,
    // and assign click handlers to the table data cells
    
    var currentRow = -1;
    var chartRows = table.find('tr.chart-data');

    // If we're just 1 over, just do the extra one.  2 or more, we'll smash them 
    // together on the chart.
    if (chartRows.length > numTopSlices + 1) {
      var miscRows = chartRows.slice(numTopSlices);
      chartRows = chartRows.slice(0, numTopSlices);

      miscChartData = [];
      miscChartData['label'] = "miscellaneous";
      miscChartData['value'] = 0.0;

      miscRows.each( function(i) {
        var row = $(this);
        var valueString = row.find("td." + dataClass).text();
        // remove the percent at the end
        valueString = valueString.substring(0, valueString.length - 1);
        var value = parseFloat(valueString);
        totalValue += value;

        miscChartData['value'] += value;

        // Store the slice index in this cell, and attach a click handler to it
        $(this).data( 'slice', numTopSlices );
        $(this).hover( handleTableClick );

        row.find("td." + legendClass).css("background-color", miscColor);
      });

      chartColours[numTopSlices] = miscColor;
      chartData[numTopSlices] = miscChartData;
    }

    chartRows.each( function(i) {
      var row = $(this);

      chartData[i] = [];
      chartData[i]['label'] = row.find("td." + labelClass).text();

      var valueString = row.find("td." + dataClass).text();
      // remove the percent at the end
      valueString = valueString.substring(0, valueString.length - 1);
      var value = parseFloat(valueString);
      totalValue += value;
      chartData[i]['value'] = value;

      // Store the slice index in this cell, and attach a click handler to it
      $(this).data( 'slice', i );
      $(this).hover( handleTableClick );

      // Set color for this slice
      chartColours[i] = colors[i];

      row.find("td." + legendClass).css("background-color", chartColours[i]);
    } );

    // Now compute and store the start and end angles of each slice in the chart data

    var currentPos = 0; // The current position of the slice in the pie (from 0 to 1)

    for ( var slice in chartData ) {
      chartData[slice]['startAngle'] = 2 * Math.PI * currentPos;
      chartData[slice]['endAngle'] = 2 * Math.PI * ( currentPos + ( chartData[slice]['value'] / totalValue ) );
      currentPos += chartData[slice]['value'] / totalValue;
    }

    // All ready! Now draw the pie chart, and add the click handler to it
    drawChart();
    $(canvas).mousemove( handleChartClick );
  }


  /**
   * Process mouse clicks in the chart area.
   *
   * If a slice was clicked, toggle it in or out.
   * If the user clicked outside the pie, push any slices back in.
   *
   * @param Event The click event
   */

  function handleChartClick ( clickEvent ) {

    // Get the mouse cursor position at the time of the click, relative to the canvas
    var offset = $(this).offset();
    var mouseX = clickEvent.pageX - offset.left;
    var mouseY = clickEvent.pageY - offset.top;

    // Was the click inside the pie chart?
    var xFromCentre = mouseX - centreX;
    var yFromCentre = mouseY - centreY;
    var distanceFromCentre = Math.sqrt( Math.pow( Math.abs( xFromCentre ), 2 ) + Math.pow( Math.abs( yFromCentre ), 2 ) );

    if ( distanceFromCentre <= chartRadius ) {

      // Yes, the click was inside the chart.
      // Find the slice that was clicked by comparing angles relative to the chart centre.

      var clickAngle = Math.atan2( yFromCentre, xFromCentre ) - chartStartAngle;
      if ( clickAngle < 0 ) clickAngle = 2 * Math.PI + clickAngle;
                  
      for ( var slice in chartData ) {
        if ( clickAngle >= chartData[slice]['startAngle'] && clickAngle <= chartData[slice]['endAngle'] ) {

          // Slice found. Pull it out or push it in, as required.
          activateSlice ( slice );
          return;
        }
      }
    }

    // User must have clicked outside the pie. Push any pulled-out slice back in.
    pushIn();
  }


  /**
   * Process mouse clicks in the table area.
   *
   * Retrieve the slice number from the jQuery data stored in the
   * clicked table cell, then toggle the slice
   *
   * @param Event The click event
   */

  function handleTableClick ( clickEvent ) {
    var slice = $(this).data('slice');
    activateSlice ( slice );
  }


  /**
   * Activate a slice, deactivating any previous slice.
   *
   * @param Number The slice index (between 0 and the number of slices - 1)
   */

  function activateSlice ( slice ) {
    if ( slice != currentPullOutSlice ) {
      pushIn();
      startPullOut ( slice );
    }
  }

 
  /**
   * Start pulling a slice out from the pie.
   *
   * @param Number The slice index (between 0 and the number of slices - 1)
   */

  function startPullOut ( slice ) {

    // Exit if we're already pulling out this slice
    if ( currentPullOutSlice == slice ) return;

    // Record the slice that we're pulling out, clear any previous animation, then start the animation
    currentPullOutSlice = slice;
    currentPullOutDistance = 0;
    clearInterval( animationId );
    animationId = setInterval( function() { animatePullOut( slice ); }, pullOutFrameInterval );

    // Highlight the corresponding row in the key table
    table.find('td').removeClass('highlight');
    var row = null;
    
    if (slice < numTopSlices) {
      row = table.find('tr.chart-data:eq(' + (slice) + ')');
    } else {
      row = table.find('tr.chart-data:gt(' + (slice - 1) + ')');
    }

    row.find("td").each(function() { 
      $(this).addClass("highlight");
    });

    $("#" + labelId).text(chartData[slice]['label']).css('background-color', chartColours[slice]);
  }

 
  /**
   * Draw a frame of the pull-out animation.
   *
   * @param Number The index of the slice being pulled out
   */

  function animatePullOut ( slice ) {

    // Pull the slice out some more
    currentPullOutDistance += maxPullOutDistance * pullOutFrameStep;

    // If we've pulled it right out, stop animating
    if ( currentPullOutDistance >= maxPullOutDistance ) {
      clearInterval( animationId );
      return;
    }

    // Draw the frame
    drawChart();
  }

 
  /**
   * Push any pulled-out slice back in.
   *
   * Resets the animation variables and redraws the chart.
   * Also un-highlights all rows in the table.
   */

  function pushIn() {
    $("#" + labelId).text("");
    currentPullOutSlice = -1;
    currentPullOutDistance = 0;
    clearInterval( animationId );
    drawChart();
    table.find('td').removeClass('highlight');
  }
 
 
  /**
   * Draw the chart.
   *
   * Loop through each slice of the pie, and draw it.
   */

  function drawChart() {

    // Get a drawing context
    var context = canvas.getContext('2d');
        
    // Clear the canvas, ready for the new frame
    context.clearRect ( 0, 0, canvasWidth, canvasHeight );

    // Draw each slice of the chart, skipping the pull-out slice (if any)
    for ( var slice in chartData ) {
      if ( slice != currentPullOutSlice ) drawSlice( context, slice );
    }

    // If there's a pull-out slice in effect, draw it.
    // (We draw the pull-out slice last so its drop shadow doesn't get painted over.)
    if ( currentPullOutSlice != -1 ) drawSlice( context, currentPullOutSlice );
  }


  /**
   * Draw an individual slice in the chart.
   *
   * @param Context A canvas context to draw on  
   * @param Number The index of the slice to draw
   */

  function drawSlice ( context, slice ) {

    // Compute the adjusted start and end angles for the slice
    var startAngle = chartData[slice]['startAngle']  + chartStartAngle;
    var endAngle = chartData[slice]['endAngle']  + chartStartAngle;
    var midAngle = (startAngle + endAngle) / 2;
    var valueRadius = ( chartRadius + pullOutLabelPadding * chartRadius);
      
    if ( slice == currentPullOutSlice ) {

      // We're pulling (or have pulled) this slice out.
      // Offset it from the pie centre, draw the text label,
      // and add a drop shadow.

      var actualPullOutDistance = currentPullOutDistance * easeOut( currentPullOutDistance/maxPullOutDistance, .8 );
      startX = centreX + Math.cos(midAngle) * actualPullOutDistance * chartRadius;
      startY = centreY + Math.sin(midAngle) * actualPullOutDistance * chartRadius;
      valueRadius = ( chartRadius + chartRadius * maxPullOutDistance + pullOutLabelPadding * chartRadius);
    } else {

      // This slice isn't pulled out, so draw it from the pie centre
      startX = centreX;
      startY = centreY;
    }

    var valueText = pullOutValuePrefix + chartData[slice]['value'].toFixed(3); 
      + " (" + ( parseInt( chartData[slice]['value'] / totalValue * 100 + .5 ) ) +  "%)"

    context.fillStyle = chartColours[slice];
    context.textAlign = "center";
    context.font = pullOutValueFont;
    context.fillText(valueText, 
                     centreX + Math.cos(midAngle) * valueRadius, 
                     centreY + Math.sin(midAngle) * valueRadius);
    context.shadowOffsetX = pullOutShadowOffsetX;
    context.shadowOffsetY = pullOutShadowOffsetY;
    context.shadowBlur = pullOutShadowBlur;

    // Set up the gradient fill for the slice
    var sliceGradient = context.createLinearGradient( 0, 0, canvasWidth*.75, canvasHeight*.75 );
    sliceGradient.addColorStop( 0, sliceGradientColour );
    sliceGradient.addColorStop( 1, chartColours[slice] );

    // Draw the slice
    context.beginPath();
    context.moveTo( startX, startY );
    context.arc( startX, startY, chartRadius, startAngle, endAngle, false );
    context.lineTo( startX, startY );
    context.closePath();
    context.fillStyle = sliceGradient;
    context.shadowColor = ( slice == currentPullOutSlice ) ? pullOutShadowColour : "rgba( 0, 0, 0, 0 )";
    context.fill();
    context.shadowColor = "rgba( 0, 0, 0, 0 )";

    // Style the slice border appropriately
    if ( slice == currentPullOutSlice ) {
      context.lineWidth = pullOutBorderWidth;
      context.strokeStyle = pullOutBorderStyle;
    } else {
      context.lineWidth = sliceBorderWidth;
      context.strokeStyle = sliceBorderStyle;
    }

    // Draw the slice border
    context.stroke();
  }


  /**
   * Easing function.
   *
   * A bit hacky but it seems to work! (Note to self: Re-read my school maths books sometime)
   *
   * @param Number The ratio of the current distance travelled to the maximum distance
   * @param Number The power (higher numbers = more gradual easing)
   * @return Number The new ratio
   */

  function easeOut( ratio, power ) {
    return ( Math.pow ( 1 - ratio, power ) + 1 );
  }

};
