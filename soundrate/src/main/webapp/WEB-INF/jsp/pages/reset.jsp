<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setBundle basename="i18n/strings/strings"/>
<c:set var="context" value="${pageContext.request.contextPath}"/>
<c:set var="token" value="${requestScope.token}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#2962FF">
    <link rel="icon" href="${context}/favicon.ico">
    <link rel="stylesheet" type="text/css" href="${context}/content/semantic/dist/semantic.min.css">
    <script src="https://code.jquery.com/jquery-3.4.1.min.js"></script>
    <script src="${context}/content/semantic/dist/semantic.min.js"></script>
    <script src="${context}/content/javascript/sign-user.js"></script>
    <script src="${context}/content/javascript/search.js"></script>
    <script src="${context}/content/javascript/reset-password.js"></script>
    <title><fmt:message key="page.reset"/></title>
</head>
<body>
    <c:import url="/header"/>
    <c:if test="${empty sessionScope.username}">
        <c:import url="/sign-in-modal"/>
    </c:if>
    <div class="ui container">
        <c:choose>
            <c:when test="${not empty sessionScope.username}">
                <div class="ui placeholder segment">
                    <div class="ui large icon header">
                        <i class="ui circular exclamation red icon"></i>
                        <fmt:message key="error.cannotReset"/>
                    </div>
                </div>
            </c:when>
            <c:when test="${empty requestScope.token}">
                <div class="ui placeholder segment">
                    <div class="ui large icon header">
                        <i class="ui circular exclamation red icon"></i>
                        <fmt:message key="error.invalidLink"/>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="ui one column stackable center aligned page grid">
                    <div class="ui twelve wide column">
                        <div class="ui attached info message">
                            <p><fmt:message key="message.resetPassword"/></p>
                        </div>
                        <form class="ui form attached fluid segment" id="reset-password-form"
                              onsubmit="return false" data-token="${requestScope.token}">
                            <div class="two fields">
                                <div class="required field">
                                    <label><fmt:message key="label.passwordField"/></label>
                                    <div class="ui left icon input">
                                        <i class="lock icon"></i>
                                        <input type="password" name="password"
                                               placeholder="<fmt:message key="label.passwordField"/>" id="reset-password">
                                    </div>
                                </div>
                                <div class="required field">
                                    <label><fmt:message key="label.matchPasswordField"/></label>
                                    <div class="ui left icon input">
                                        <i class="lock icon"></i>
                                        <input type="password" name="password-match"
                                               placeholder="<fmt:message key="label.matchPasswordField"/>" id="reset-password-match">
                                    </div>
                                </div>
                            </div>
                            <button class="ui primary fluid button" type="button" id="reset-button">
                                <fmt:message key="label.submit"/>
                            </button>
                            <div class="ui success message">
                                <div class="ui header">
                                    <fmt:message key="label.success"/>
                                </div>
                                <p><fmt:message key="message.passwordResetSuccess"/></p>
                            </div>
                            <div class="ui error message"></div>
                        </form>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</body>
</html>