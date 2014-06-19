$(function() {
  Nav.init();
});

var Nav = {};

Nav.init = function() {
  Nav.show();
};

Nav.show = function() {  
  var html = '' +
    '<ul>' +
      '<li><div><a href="#view=' + DashboardView.getName() + '">Cluster Dashboard</a></div></li>' +
      '<li>' +
        '<div>' +
          '<a href="#view=' + ServerView.getName() + '">Server</a>' +
          '<select id="navigation-select">' +
          '</select>' +
        '</div>' +
      '<ul>' +
        '<li><div><a href="#view=jmx">JMX/MBeans</a></li></div>' +
        '<li><div><a href="#view=memory">Memory</a></li></div>' +
        '<li><div><a href="#view=meters">Meters</a></li></div>' +
        '<li><div><a href="#view=threads">Threads</a></li></div>' +
      '</ul>' +
      '</li>' +
      '<li><div><a href="#view=options">Options</a></div></li>' +
      '<li><div><a href="#view=logout">Logout</a></div></li>' +
    '</ul>';

  $('#navigation').append(html);
  
  AdminService.getServers()
    .done(function(data) {
      console.log("Nav.show: done " + JSON.stringify(data));
      
      $('#navigation-select').empty();
      
      data.forEach(function(element, index) {
        var html = '' +
          '<option value="' + element + '">' + element + '</option>';
        
        $('#navigation-select').append(html);        
      });
      
    })
    .fail(function(error) {
    
    })
    .always(function() {
    });
};

Nav.hide = function() {
  $('#navigation').empty();
};