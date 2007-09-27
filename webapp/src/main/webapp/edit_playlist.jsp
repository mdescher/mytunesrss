<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/tags" prefix="mt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.codewave.de/jsp/functions" prefix="cwfn" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %>

<c:set var="backUrl">${servletUrl}/editPlaylist/${auth}/<mt:encrypt key="${encryptionKey}">allowEditEmpty=${param.allowEditEmpty}/index=${param.index}</mt:encrypt>/backUrl=${param.backUrl}</c:set>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>

    <jsp:include page="incl_head.jsp"/>

    <script type="text/javascript">
        function getElementParams(elements, separator) {
            var elementNames = elements.split(",");
            var buffer = '';
            for (var i = 0; i < elementNames.length; i++) {
                buffer += elementNames[i] + "=" + getElementValue(self.document.getElementById(elementNames[i]));
                if (i + 1 < elementNames.length) {
                    buffer += separator;
                }
            }
            return buffer;
        }
        function getElementValue(element) {
            if (element.type == 'text') {
                return element.value;
            }
            if (element.type == 'select-one') {
                return element.options[element.options.selectedIndex].value;
            }
            return '';
        }
    </script>

</head>

<body>

<div class="body">

    <h1 class="manager">
        <a class="portal" href="${servletUrl}/showPortal/${auth}"><fmt:message key="portal"/></a> <span><fmt:message key="myTunesRss"/></span>
    </h1>

    <c:if test="${states.addToPlaylistMode}">
        <ul class="links">
            <li style="float:right;">
                <a href="${mtfn:decode64(param.backUrl)}"><fmt:message key="back"/></a>
            </li>
        </ul>
    </c:if>

    <jsp:include page="/incl_error.jsp" />

    <form id="playlist" action="${servletUrl}/savePlaylist/${auth}" method="post">
        <table class="portal" cellspacing="0">
            <tr>
                <td class="playlistManager">
                    <fmt:message key="playlistName"/> <input type="text" name="name" value="<c:out value="${playlist.name}"/>" />
                </td>
                <td class="links">
                    <a class="add" href="${servletUrl}/continuePlaylist/${auth}" style="background-image:url('${appUrl}/images/add_more.gif');"><fmt:message key="addMoreSongs"/></a>
                </td>
        </table>

        <input type="hidden" name="backUrl" value="${param.backUrl}" />
        <input type="hidden" name="allowEditEmpty" value="${param.allowEditEmpty}" />
        <table cellspacing="0">
            <c:if test="${!states.addToPlaylistMode}">
                <tr>
                    <th class="active" colspan="4">Anzeigefilter</th>
                </tr>
                <tr>
                    <td colspan="4">
                        <table cellspacing="0">
                            <tr>
                                <td>Text:</td>
                                <td><input id="filterText" type="text" name="filterText" value="${displayFilter.text}"/></td>
                            </tr>
                            <tr>
                                <td>Typ:</td>
                                <td>
                                    <select id="filterType" name="filterType">
                                        <option value="">keine Einschränkung</option>
                                        <option value="audio" <c:if test="${displayFilter.type eq 'audio'}">selected="selected"</c:if>>nur Audiodateien</option>
                                        <option value="video" <c:if test="${displayFilter.type eq 'video'}">selected="selected"</c:if>>nur Videodateien</option>
                                    </select>
                                </td>
                            </tr>
                            <tr>
                                <td>Schutz:</td>
                                <td>
                                    <select id="filterProtected" name="filterProtected">
                                        <option value="">keine Einschränkung</option>
                                        <option value="protected" <c:if test="${displayFilter.protected eq 'protected'}">selected="selected"</c:if>>nur geschützte Dateien</option>
                                        <option value="unprotected" <c:if test="${displayFilter.protected eq 'unprotected'}">selected="selected"</c:if>>nur freie Dateien</option>
                                    </select>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <input type="button" value="Filter anwenden" onclick="self.document.location.href='${servletUrl}/editPlaylist/${auth}/<mt:encrypt key="${encryptionKey}">allowEditEmpty=${param.allowEditEmpty}</mt:encrypt>/index=${param.index}/backUrl=${param.backUrl}/' + getElementParams('filterText,filterType,filterProtected', '/')"/>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </c:if>
            <tr>
                <c:choose>
                    <c:when test="${!empty tracks}">
                        <th class="check"><input type="checkbox" name="none" value="none" onclick="selectAll('item', '${trackIds}', this)" /></th>
                        <th class="active" colspan="3"><fmt:message key="playlistContent"/></th>
                    </c:when>
                    <c:otherwise>
                        <th class="active" colspan="4"><fmt:message key="playlistContent"/></th>
                    </c:otherwise>
                </c:choose>
            </tr>
            <c:forEach items="${tracks}" var="track" varStatus="trackLoop">
                <tr class="${cwfn:choose(trackLoop.index % 2 == 0, 'even', 'odd')}">
                    <td class="check">
                        <input type="checkbox" id="item${track.id}" name="track" value="${track.id}" />
                    </td>
                    <td>
                        <c:if test="${track.protected}"><img src="${appUrl}/images/protected${cwfn:choose(trackLoop.index % 2 == 0, '', '_odd')}.gif" alt="<fmt:message key="protected"/>" style="vertical-align:middle"/></c:if>
                        <c:if test="${track.video}"><img src="${appUrl}/images/movie${cwfn:choose(trackLoop.index % 2 == 0, '', '_odd')}.gif" alt="<fmt:message key="video"/>" style="vertical-align:middle"/></c:if>
                        <c:out value="${cwfn:choose(mtfn:unknown(track.name), '(unknown)', track.name)}" />
                    </td>
                    <td>
                        <c:out value="${cwfn:choose(mtfn:unknown(track.artist), '(unknown)', track.artist)}" />
                    </td>
                    <td class="icon">
                        <a href="${servletUrl}/removeFromPlaylist/${auth}/<mt:encrypt key="${encryptionKey}">allowEditEmpty=${param.allowEditEmpty}/track=${track.id}</mt:encrypt>/backUrl=${param.backUrl}">
                            <img src="${appUrl}/images/delete${cwfn:choose(trackLoop.index % 2 == 0, '', '_odd')}.gif" alt="delete" /> </a>
                    </td>
                </tr>
            </c:forEach>
        </table>
        <c:if test="${!empty pager}">
            <c:set var="pagerCommand"
                   scope="request">${servletUrl}/editPlaylist/${auth}/<mt:encrypt key="${encryptionKey}">allowEditEmpty=${param.allowEditEmpty}</mt:encrypt>/index={index}/backUrl=${param.backUrl}</c:set>
            <c:set var="pagerCurrent" scope="request" value="${cwfn:choose(!empty param.index, param.index, '0')}" />
            <jsp:include page="incl_bottomPager.jsp" />
        </c:if>

        <div class="buttons">
            <input type="button" onClick="document.forms['playlist'].action = '${servletUrl}/removeFromPlaylist/${auth}';document.forms['playlist'].submit()" value="<fmt:message key="removeSelected"/>" />
            <input type="submit"
                   onClick="document.forms['playlist'].action = '${servletUrl}/savePlaylist/${auth}';document.forms['playlist'].elements['backUrl'].value = '${mtfn:encode64(backUrl)}'"
                   value="<fmt:message key="savePlaylist"/>" />
            <input type="button" onClick="document.location.href = '${servletUrl}/cancelEditPlaylist/${auth}/backUrl=${param.backUrl}'" value="<fmt:message key="doCancel"/>" />
        </div>
    </form>

</div>

</body>

</html>