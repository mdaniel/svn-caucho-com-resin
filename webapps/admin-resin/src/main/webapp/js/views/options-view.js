$(function() {
  OptionsView.init();

  View.addView(OptionsView.getName(), OptionsView);
});

var OptionsView = {};

OptionsView.init = function() {
  console.log("OptionsView.init");
};

OptionsView.getName = function() {
  return "options";
};

OptionsView.hide = function() {
  $('#' + View.getContainerId()).empty();
};

OptionsView.show = function() {  
  console.log("OptionsView.show");
  
  AdminService.getServerInfo()
    .done(function(data) {
      console.log("OptionsView.show: done " + JSON.stringify(data))
      
      var html = '' +
        '<div>' +
          '<h2>Options</h2>' +
        '</div>';
      
      $('#' + View.getContainerId()).append(html);
    })
    .fail(function(error) {
      console.log("OptionsView.show: error " + error)
    })
    .always(function() {
    });
};
