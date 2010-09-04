function ToggleSwitch(context) {
  // right triangle -- e == east
  this.toggleShowIcon = "ui-icon-circle-triangle-e";
  // down triangle -- s == south
  this.toggleHideIcon = "ui-icon-circle-triangle-s";

  this.state = "hidden";
  this.context = context;
  this.toggleSwitch = $("<span></span>");

  this.toggleSwitch.addClass("ui-icon");
  this.toggleSwitch.css("display", "inline-block");
  this.toggleSwitch.addClass(this.toggleShowIcon);
  this.context.prepend(this.toggleSwitch);

  var id = this.context.attr("id");
  this.toggleTargets = $(".toggle-" + id);
  this.toggleTargets.hide();
}

ToggleSwitch.prototype.init = function() {
  var _ = this;

  this.context.click(function() {
    _.handleClickToggle();
  });

  // in case a toggle target is controlled by multiple switches, make sure
  // they all change state
  this.toggleTargets.bind("toggle", function(event, caller) {
    _.handleExternalToggle(caller);
  });
};

ToggleSwitch.prototype.setShownState = function() {
  this.toggleSwitch.removeClass(this.toggleShowIcon);
  this.toggleSwitch.addClass(this.toggleHideIcon);
  this.state = "shown";
};

ToggleSwitch.prototype.showTargets = function() {
  this.toggleTargets.show();
};

ToggleSwitch.prototype.setHiddenState = function() {
  this.toggleSwitch.removeClass(this.toggleHideIcon);
  this.toggleSwitch.addClass(this.toggleShowIcon);
  this.state = "hidden";
};

ToggleSwitch.prototype.hideTargets = function() {
  this.toggleTargets.hide();
};

ToggleSwitch.prototype.handleClickToggle = function() {
  if (this.state == "hidden") {
    this.setShownState();
    this.showTargets();
  }
  else {
    this.setHiddenState();
    this.hideTargets();
  }

  this.toggleTargets.trigger("toggle", this);
};

ToggleSwitch.prototype.handleExternalToggle = function(caller) {
  if (this == caller)
    return;

  var shownCount = 0;
  var hiddenCount = 0;

  this.toggleTargets.each(function() { 
    var target = $(this);

    if (target.css("display") == "none")
      hiddenCount++;
    else
      shownCount++;
  });

  if (shownCount == 0)
    this.setHiddenState();
  else if (hiddenCount == 0)
    this.setShownState();
};

function initializeToggleSwitches() {
  $(".switch").each(function() {
    var toggleSwitch = new ToggleSwitch($(this));
    toggleSwitch.init();
  });
}

function init() {
  initializeToggleSwitches();
}
