<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/tags" prefix="mt" %>
<%@ taglib uri="http://www.codewave.de/jsp/functions" prefix="cwfn" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="backUrl" scope="request">${servletUrl}/showPlaylistManager/${auth}/<mt:encrypt key="${encryptionKey}">index=${param.index}</mt:encrypt></c:set>
<c:set var="browseArtistUrl" scope="request">${servletUrl}/browseArtist/${auth}/page=1</c:set>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>

    <jsp:include page="incl_head.jsp"/>

    <script type="text/javascript">

        $jQ(document).ready(function() {
            $jQ("#confirmDeletePlaylist").dialog({
                autoOpen:false,
                modal:true,
                buttons:{
                    "<fmt:message key="no"/>" : function() {
                        $jQ("#confirmDeletePlaylist").dialog("close");
                    },
                    "<fmt:message key="yes"/>" : function() {
                        var serverCall = $jQ("#confirmDeletePlaylist").dialog("option", "serverCall");
                        $jQ("#confirmDeletePlaylist").dialog("close");
                        document.location.href = serverCall;
                    }
                }
            });
        });

        function loadAndEditPlaylist(id) {
            jsonRpc('${servletUrl}', "EditPlaylistService.startEditPlaylist", [id], function() {
                document.location.href = "${servletUrl}/showResource/${auth}/<mt:encrypt key="${encryptionKey}">resource=EditPlaylist</mt:encrypt>/backUrl=${mtfn:encode64(backUrl)}";
            }, "${remoteApiSessionId}");
        }

    </script>

</head>

<body class="plmanager">

    <div class="body">
    
        <div class="head">
            <h1 class="manager">
                <a class="portal" href="${servletUrl}/showPortal/${auth}"><span><fmt:message key="portal"/></span></a>
                <span><fmt:message key="myTunesRss"/></span>
            </h1>
        </div>
        
        <div class="content">
        
            <div class="content-inner">
    
                <ul class="menu">
                    <li><a href="${servletUrl}/startNewPlaylist/${auth}/backUrl=${cwfn:encode64(browseArtistUrl)}"><fmt:message key="newPlaylist"/></a></li>
                    <li><a href="${servletUrl}/editSmartPlaylist/${auth}"><fmt:message key="newSmartPlaylist"/></a></li>
                </ul>
            
                <jsp:include page="/incl_error.jsp" />
            
                <table cellspacing="0" class="tracklist">
                    <tr>
                        <th class="active"><fmt:message key="playlists"/></th>
						<c:if test="${!empty playlists}">
							<th colspan="3"><fmt:message key="tracks"/></th>
						</c:if>
                    </tr>
                    <c:forEach items="${playlists}" var="playlist" varStatus="loopStatus">
                        <tr class="${cwfn:choose(loopStatus.index % 2 == 0, 'even', 'odd')}">
                            <td class="${fn:toLowerCase(playlist.type)}"><c:out value="${playlist.name}" /></td>
                            <td class="tracks"><a href="${servletUrl}/browseTrack/${auth}/<mt:encrypt key="${encryptionKey}">playlist=${playlist.id}</mt:encrypt>/backUrl=${mtfn:encode64(backUrl)}">${playlist.trackCount}</a></td>
                            <td class="actions">
                                <c:choose>
                                    <c:when test="${playlist.type == 'MyTunesSmart'}">
                                        <a class="edit" href="${servletUrl}/editSmartPlaylist/${auth}/<mt:encrypt key="${encryptionKey}">playlistId=${playlist.id}</mt:encrypt>/backUrl=${mtfn:encode64(backUrl)}">Edit</a>
                                        </c:when>
                                    <c:otherwise>
                                        <a class="edit" onclick="loadAndEditPlaylist('${playlist.id}')">Edit</a>
                                    </c:otherwise>
                                </c:choose>
                                <c:choose>
                                    <c:when test="${deleteConfirmation}">
                                        <a class="delete" onclick="$jQ('#confirmDeletePlaylist').dialog('option', 'serverCall', '${servletUrl}/deletePlaylist/${auth}/<mt:encrypt key="${encryptionKey}">playlist=${playlist.id}</mt:encrypt>');$jQ('#playlistName').text('${mtfn:escapeJs(playlist.name)}');$jQ('#confirmDeletePlaylist').dialog('open')">Delete</a>
                                    </c:when>
                                    <c:otherwise>
                                        <a class="delete" href="${servletUrl}/deletePlaylist/${auth}/<mt:encrypt key="${encryptionKey}">playlist=${playlist.id}</mt:encrypt>">Delete</a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty playlists}">
                        <tr><td colspan="3" class="empty"><fmt:message key="noPlaylists"/></td></tr>
                    </c:if>
                </table>
            
                <c:if test="${!empty pager}">
                    <c:set var="pagerCommand" scope="request" value="${servletUrl}/showPlaylistManager/${auth}/index={index}" />
                    <c:set var="pagerCurrent" scope="request" value="${cwfn:choose(!empty param.index, param.index, '0')}" />
                    <jsp:include page="incl_bottomPager.jsp" />
                </c:if>
                
            </div>
            
        </div>
        
        <div class="footer">
            <div class="inner"></div>
        </div>
            
    </div>
        
    <div id="confirmDeletePlaylist" title="<fmt:message key="confirmDeletePlaylistTitle"/>" style="display:none">
        <fmt:message key="dialog.confirmDeletePlaylist"><fmt:param><span id="playlistName"></span></fmt:param></fmt:message>
    </div>

</body>

</html>
