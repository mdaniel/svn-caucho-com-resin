var View = {};

View._views = {
};

View._defaultView = null;
View._currentView = null;

View.getContainerId = function() {
  return "view";
};

View.addView = function(name, view) {
  View._views[name] = view;
  
  console.log("View.addView: " + name + ", " + view);
  
  if (view.isDefault != null && view.isDefault()) {
    View._defaultView = view;
    
    if (View._currentView == null) {
      View.setView(view);
    }
  }
  else if (Main.getQueryParameters().view == name) {
    View.setView(view);
  }
};

View.getViews = function() {  
  return View._views;
};

View.setView = function(name) {  
  var oldView = View._currentView;
  var view = name;
  
  if (typeof name == "string" && name.length > 0) {
    view = View._views[name];
  }
  else if (name != null) {
    name = name.getName();
  }
  
  console.log("View.setView: " + name + ", " + view);
  
  if (view != null) {    
    View._currentView = view;    
  }
  else {
    View._currentView = View._defaultView;
  }
  
  if (oldView != View._currentView) {
    if (oldView != null) {
      oldView.hide();
    }
        
    View._currentView.show();
  }
};