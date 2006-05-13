<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.codewave.de/jsp/functions" prefix="cwfn" %>

<fmt:setBundle basename="de.codewave.mytunesrss.MyTunesRSSWeb" />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>

    <title><fmt:message key="title" /> v${cwfn:sysprop('mytunesrss.version')}</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <link rel="stylesheet" type="text/css" href="${appUrl}/styles/mytunesrss.css" />
    <!--[if IE]>
      <link rel="stylesheet" type="text/css" href="styles/ie.css" />
    <![endif]-->

</head>

<body>

<div class="body">

    <h1 class="search"><span>MyTunesRSS</span></h1>

    <jsp:include page="/error.jsp" />

    <div class="link">
        <a href="${servletUrl}/browseAlbum">Browse by album</a>
    </div>

    <form name="browse" action="" method="post">
        <table class="select" cellspacing="0">
            <tr>
                <th>&nbsp;</th>
                <th colspan="5">
                    Artists
                    <c:if test="${!empty param.album}"> on "<c:out value="${param.album}" />"</c:if>
                </th>
            </tr>
            <c:forEach items="${artists}" var="artist" varStatus="loopStatus">
                <tr class="${cwfn:choose(loopStatus.index % 2 == 0, '', 'odd')}">
                    <td class="check"><input type="checkbox" name="artist" value="<c:out value="${artist.name}"/>" /></td>
                    <td class="artist2"><c:out value="${artist.name}" /></td>
                    <td class="album">
                        <a href="${servletUrl}/browseAlbum?artist=<c:out value="${cwfn:urlEncode(artist.name, 'UTF-8')}"/>">${artist.albumCount}&nbsp;album${cwfn:choose(artist.albumCount > 1, 's', '')}</a>
                    </td>
                    <td class="tracks">
                        <a href="${servletUrl}/browseTrack?artist=<c:out value="${cwfn:urlEncode(artist.name, 'UTF-8')}"/>">${artist.trackCount}&nbsp;track${cwfn:choose(artist.trackCount > 1, 's', '')}</a>
                    </td>
                    <td class="icon">
                        <a href="${servletUrl}/createRSS?artist=<c:out value="${cwfn:urlEncode(artist.name, 'UTF-8')}"/>"><img src="${appUrl}/images/rss.gif"
                                                                                                                               alt="rss" /></a>
                    </td>
                    <td class="icon">
                        <a href="${servletUrl}/createM3U/artist=<c:out value="${cwfn:urlEncode(artist.name, 'UTF-8')}"/>/mytunesrss.m3u"><img src="${appUrl}/images/m3u.gif"
                                                                                                                               alt="m3u" /></a>
                    </td>
                </tr>
            </c:forEach>
        </table>

        <div class="buttons">
            <input type="button" onClick="document.location.href='${servletUrl}/showPortal'" value="back to portal" />
            <input type="submit" onClick="document.forms['browse'].action = '${servletUrl}/createRSS'" value="RSS" />
            <input type="submit" onClick="document.forms['browse'].action = '${servletUrl}/createM3U/mytunesrss.m3u'" value="M3U" />
        </div>
    </form>

</div>

</body>

</html>
