function hideElement(element) 
{
  element.style.display = 'none';
}

function showElement(element) 
{ 
  element.style.display = '';
}

function hideElementById(id) 
{
  hideElement(document.getElementById(id));
}

function showElementById(id) 
{ 
  showElement(document.getElementById(id));
}

function mapElementsByClass(cssClass, f)
{
  var tags = document.getElementsByTagName('*');
  var regex = new RegExp("(^|\\s)" + cssClass + "(\\s|$)");

  for (i = 0; i < tags.length; i++) {
    if (regex.test(tags[i].className)) {
      f(tags[i]);
    }
  }
}

function hideElementsByClass(cssClass)
{
  mapElementsByClass(cssClass, hideElement);
}

function showElementsByClass(cssClass)
{
  mapElementsByClass(cssClass, showElement);
}

function toggleElementsByClass(checkbox, cssClass)
{
  if (checkbox.checked)
  {
    showElementsByClass(cssClass);
  }
  else
  {
    hideElementsByClass(cssClass);
  }
}

function findElementLeft(element)
{
  var left = 0;

  while (element != null) {
    left += element.offsetLeft;
    element = element.offsetParent;
  }

  return left;
}

function findElementTop(element)
{
  var top = 0;

  while (element != null) {
    top += element.offsetTop;
    element = element.offsetParent;
  }

  return top;
}

/* class */ function Timer()
{
  var _ = this;

  _.id = -1;

  _.clear = function() {
    if (_.id != -1) {
      window.clearTimeout(_.id);
      _.id = -1;
    }
  };

  _.set = function(callback, timeout) {
    _.clear();

    _.id = window.setTimeout(callback, timeout);
  };
}

/* class */ function Popup()
{
  var _ = this;

  _.MAX_HEIGHT = 300;
  _.MAX_WIDTH = 400;
  _.xhr = new XMLHttpRequest();
  _.element = document.getElementById('popup');
  _.reference = null;
  _.showTimer = new Timer();
  _.hideTimer = new Timer();

  _.element.onmouseover = function() {
    _.hideTimer.clear();
  };

  _.element.onmouseout = function() {
    _.mouseOutHandler();
  };

  _.show = function(caller, reference, text) {

    _.showTimer.clear();

    var top = findElementTop(caller);
    var left = findElementLeft(caller);
    var windowWidth = document.documentElement.clientWidth;

    _.element.style.top = top + caller.offsetHeight + "px";

    if (left + _.MAX_WIDTH > windowWidth - 30) {
      _.element.style.left = windowWidth - _.MAX_WIDTH - 30 + "px";
    }
    else {
      _.element.style.left = left + "px";
    }

    _.element.style.width = _.MAX_WIDTH + "px";
    _.element.style.height = _.MAX_HEIGHT + "px";

    _.element.innerHTML = text;
    _.element.style.display = 'block';

    _.reference = reference;
  };

  _.refResponseHandler = function(caller, reference) {
    // 4 == DONE
    if (_.xhr.readyState == 4 && _.xhr.status == 200) {
      _.show(caller, reference, _.xhr.responseText);
    }
  };

  _.fetchReference = function(caller, reference) {
    _.xhr.onreadystatechange = function() {
      _.refResponseHandler(caller, reference); 
    };

    _.xhr.open("GET", "reference/?ref=" + reference, true);
    _.xhr.send();
  };

  _.mouseOverHandler = function(tag, reference) {
    _.hideTimer.clear();

    if (_.reference != reference) {
      _.hide();
    }

    if (_.reference == null) {
      var showTimeoutHandler = function() {
        _.fetchReference(tag, reference);
      };

      _.showTimer.set(showTimeoutHandler, 1000);
    }
  };

  _.hide = function() {
    _.element.scrollTop = 0;
    _.element.style.display = 'none';
    _.element.innerHTML = '';

    _.reference = null;
  };

  _.mouseOutHandler = function() {
    _.showTimer.clear();

    _.hideTimer.set(_.hide, 1000);
  };
}

