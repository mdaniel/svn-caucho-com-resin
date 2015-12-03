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

function formatMemory(val)
{
  if (val >= 1e9)
    return (val / 1e9).toFixed(1) + 'G';
  if (val >= 1e6)
    return (val / 1e6).toFixed(1) + 'M';
  if (val >= 1e3)
    return (val / 1e3).toFixed(1) + 'k'

  return val + "B";
}

function formatDate(val) {
  var now = new Date(Date.now());
  var date = new Date(val);

  var result;
  if (now.getFullYear() != date.getFullYear() ||
      now.getMonth() != date.getMonth() ||
      now.getDate() != date.getDate()) {
    result = date.getFullYear() + "-" + (date.getMonth() + 1 ) + "-"
               + date.getDate() + " " + date.getHours() + ":"
               + date.getMinutes() + ":" + date.getSeconds();
  }
  else {
    result = date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();
  }

  return result;
}

var lday = 1000 * 60 * 60 * 24;
var lhour = 1000 * 60 * 60;
var lmin = 1000 * 60;
var lsec = 1000;

function formatTimeout(ms) {
  var days = Math.floor(ms / lday);
  var hours = Math.floor((ms - lday * days) / lhour);
  var minutes = Math.floor((ms - lday * days - lhour * hours) / lmin);
  var seconds = Math.floor((ms - lday * days - lhour * hours - lmin * minutes)
                             / lsec);

  var result;
  if (days > 0)
    result = days + 'd ' + hours + 'h ' + minutes + 'm ' + seconds + 's ';
  else if (hours > 0)
   result = hours + 'h ' + minutes + 'm ' + seconds + 's ';
  else if (minutes > 0)
    result = minutes + 'm ' + seconds + 's ';
  else if (seconds > 0)
    result = seconds + 's ' + (ms - seconds * 1000) + 'ms';
  else
    result = ms + 'ms';

  return result;
}