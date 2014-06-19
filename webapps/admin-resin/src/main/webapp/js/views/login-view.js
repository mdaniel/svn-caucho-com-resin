$(function() {
  LoginView.init();

  View.addView(LoginView.getName(), LoginView);
});

var LoginView = {};

LoginView.init = function() {
  console.log("LoginView.init");
};

LoginView.getName = function() {
  return "login";
};

LoginView.hide = function() {
  $('#' + View.getContainerId()).empty();
};

LoginView.show = function() {  
  console.log("LoginView.show");
  
  AdminService.getServerInfo()
    .done(function(data) {
      console.log("LoginView.show: done " + JSON.stringify(data))
      
      var html = '' +
        '<div>' +
          '<h2>Log In</h2>' +
        '</div>';
      
      $('#' + View.getContainerId()).append(html);
    })
    .fail(function(error) {
      console.log("LoginView.show: error " + error)
    })
    .always(function() {
    });
};
