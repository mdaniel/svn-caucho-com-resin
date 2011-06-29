function ToggleCookie() {
  this.shown = {};
  this.cookieName = "toggle";
}

ToggleCookie.prototype.init = function() {
  var cookies = document.cookie.split(';');

  for (var i = 0; i < cookies.length; i++) {
    var cookie = $.trim(cookies[i]);

    if (cookie.indexOf(this.cookieName + "=") == 0) {
      var shown = cookie.substr((this.cookieName + "=").length).split(",");

      for (var j = 0; j < shown.length; j++) {
        this.shown[shown[j]] = true;
      }

      break;
    }
  }
};

ToggleCookie.prototype.isShown = function(id) {
  return this.shown[id] == true;
};

ToggleCookie.prototype.setShown = function(id) {
  this.shown[id] = true;
  this.persist();
};

ToggleCookie.prototype.setHidden = function(id) {
  delete this.shown[id];
  this.persist();
};

ToggleCookie.prototype.persist = function() {
  var shownArray = [];
  for (var key in this.shown)
    shownArray.push(key);

  document.cookie = this.cookieName + "=" + shownArray.join(",");
};

function ToggleSwitch(context) {
  // right triangle -- e == east
  this.toggleShowIcon = "ui-icon-circle-triangle-e";
  // down triangle -- s == south
  this.toggleHideIcon = "ui-icon-circle-triangle-s";

  this.state = "hidden";
  this.context = context;
  this.contextId = this.context.attr("id");
  this.toggleSwitch = $("<span></span>");

  this.toggleSwitch.addClass("ui-icon");
  this.toggleSwitch.css("display", "inline-block");
  this.toggleSwitch.addClass(this.toggleShowIcon);
  this.context.prepend(this.toggleSwitch);

  this.toggleTargets = $(".toggle-" + this.contextId);

  if (ToggleSwitch.cookie.isShown(this.contextId)) {
    this.setShownState();
    this.showTargets();
  }
  else {
    this.setHiddenState();
    this.hideTargets();
  }
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

  ToggleSwitch.cookie.setShown(this.contextId);
};

ToggleSwitch.prototype.showTargets = function() {
  this.toggleTargets.show();
};

ToggleSwitch.prototype.setHiddenState = function() {
  this.toggleSwitch.removeClass(this.toggleHideIcon);
  this.toggleSwitch.addClass(this.toggleShowIcon);
  this.state = "hidden";

  ToggleSwitch.cookie.setHidden(this.contextId);
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
  ToggleSwitch.cookie = new ToggleCookie();
  ToggleSwitch.cookie.init();

  $(".switch").each(function() {
    var toggleSwitch = new ToggleSwitch($(this));
    toggleSwitch.init();
  });

  $(".menu-switch").each(function() {
    var toggleSwitch = new ToggleSwitch($(this));

    toggleSwitch.showTargets = function() {
      this.toggleTargets.css('left',  '0px').show();
    };

    toggleSwitch.init();
  });
}

function initializeValidators() {
  $("form").each(function() {
    var required = $(this).find(".required");

    $(this).submit(function() {
      var areRequiredGiven = true;

      for (var i = 0; i < required.length; i++) {
        if (required[i].value == "") {
          required.effect("highlight");
          areRequiredGiven = false;
        }
      }

      if (! areRequiredGiven) {
        return false;
      }

      return true;
    });
  });
}

function init() {
  initializeValidators();
  initializeToggleSwitches();
}

function color_revscale(o, c, d)
{
  for (var i = 0; i < c.length; ++i) {
    var v = o[c.charAt(i)];
    var delta = 255 - v;
    o[c.charAt(i)] = v + delta * d;
  }

  return o.normalize();
}

function color_baseline(o)
{
  //  return color_revscale(o, "rgb", 0.7).add("a", -0.5);
  return o.add("a", -0.6);
}

