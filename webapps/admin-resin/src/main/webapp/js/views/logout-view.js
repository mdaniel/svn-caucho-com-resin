$(function() {
  LogoutView.init();

  View.addView(LogoutView.getName(), LogoutView);
});

var LogoutView = {};

LogoutView.init = function() {
  console.log("LogoutView.init");
};

LogoutView.getName = function() {
  return "logout";
};

LogoutView.hide = function() {
  $('#' + View.getContainerId()).empty();
};

LogoutView.show = function() {  
  console.log("LogoutView.show");
  
  AdminService.getServerInfo()
    .done(function(data) {
      console.log("LogoutView.show: done " + JSON.stringify(data))
      
      var html = '' +
        '<div>' +
          '<h2>Log Out</h2>' +
        '</div>';
      
      $('#' + View.getContainerId()).append(html);
    })
    .fail(function(error) {
      console.log("LogoutView.show: error " + error)
    })
    .always(function() {
    });
};
