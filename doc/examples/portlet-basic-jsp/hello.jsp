<%@ page session="false" %>

<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>

<portlet:defineObjects/>

<c:choose>
<c:when test="${'edit' eq renderRequest.portletMode}">

<portlet:actionUrl var="submitUrl" portletMode="edit"/>

<form method='POST' action='${submitUrl}'>
Name: <input type='text' name='identity' value='${identity}'/>
<br/>
Color: <input type='text' name='color' value='${color}'/>
<br/>
<input type='submit'/>
</form>

</c:when>
<c:otherwise>

<portlet:renderUrl var="editUrl" portletMode="edit"/>

Hello, ${identity}.
Your favorite color is ${color}
<p>
<a href='${editUrl}'>Edit</a>

</c:otherwise>
</c:choose>


