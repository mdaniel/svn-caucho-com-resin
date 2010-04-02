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

