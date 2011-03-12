<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/tags" prefix="mt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.codewave.de/jsp/functions" prefix="cwfn" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mttag" %>

<c:set var="imageSize" value="128" />  

<%--
  ~ Copyright (c) 2011. Codewave Software Michael Descher.
  ~ All rights reserved.
  --%>

<%--@elvariable id="appUrl" type="java.lang.String"--%>
<%--@elvariable id="servletUrl" type="java.lang.String"--%>
<%--@elvariable id="permFeedServletUrl" type="java.lang.String"--%>
<%--@elvariable id="auth" type="java.lang.String"--%>
<%--@elvariable id="encryptionKey" type="javax.crypto.SecretKey"--%>
<%--@elvariable id="authUser" type="de.codewave.mytunesrss.User"--%>
<%--@elvariable id="globalConfig" type="de.codewave.mytunesrss.MyTunesRssConfig"--%>
<%--@elvariable id="config" type="de.codewave.mytunesrss.servlet.WebConfig"--%>
<%--@elvariable id="photos" type="java.util.List<de.codewave.mytunesrss.datastore.statement.Track>"--%>

<c:set var="backUrl" scope="request">${servletUrl}/browsePhotoAlbum/${auth}/<mt:encrypt key="${encryptionKey}">index=${param.index}</mt:encrypt>/backUrl=${param.backUrl}</c:set>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>

    <jsp:include page="incl_head.jsp"/>

    <script type="text/javascript">
        $jQ(document).ready(function() {
            $jQ("img").fullsize();
        });
    </script>

    <!-- stolen code starts here -->

    <style type="text/css">

    .thumbwrap {
        border: 1px solid #999;
        padding: 15px 8px 0 8px;
        background-color: #f4f4f4;
        margin: 0;
    }
    .thumbwrap li {
        display: -moz-inline-box;
        display: inline-block;
        /*\*/ vertical-align: top; /**/
        margin: 0 7px 15px 7px;
        border: 1px solid #999;
        padding: 0;
    }
    /*  Moz: NO border qui altrimenti difficolta' con width, table altrimenti problemi a text resize (risolubili con refresh) */
    .thumbwrap li>div {
        /*\*/ display: table; table-layout: fixed; /**/
        width: ${imageSize}px;
    }
    .thumbwrap a {
        display: block;
        text-decoration: none;
        color: #000;
        background-color: #ffe;
        cursor: pointer;
    }
    /*\*/
    .thumbwrap>li .wrimg {
        display: table-cell;
        vertical-align: middle;
        width: ${imageSize}px;
        height: ${imageSize}px;
    }
    /**/
    .thumbwrap img {
        border: solid 1px #66f;
        vertical-align: middle;
    }
    .thumbwrap a:hover {
        background-color: #dfd;
    }
    /*\*//*/
    * html .thumbwrap li .wrimg {
        display: block;
        font-size: 1px;
    }
    * html .thumbwrap .wrimg span {
        display: inline-block;
        vertical-align: middle;
        height: ${imageSize}px;
        width: 1px;
    }
    /* top ib e hover Op < 9.5 */
    @media all and (min-width: 0px) {
        html:first-child .thumbwrap li div {
            display: block;
        }
        html:first-child .thumbwrap a {
            display: inline-block;
            vertical-align: top;
        }
        html:first-child .thumbwrap {
            border-collapse: collapse;
            display: inline-block; /* non deve avere margin */
        }
    }
    </style>
    <!--[if lt IE 8]><style>
    .thumbwrap li {
        width: ${imageSize + 2}px;
        w\idth: ${imageSize}px;
        display: inline;
    }
    .thumbwrap {
        _height: 0;
        zoom: 1;
        display: inline;
    }
    .thumbwrap li .wrimg {
        display: block;
        /* evita hasLayout per background position */
        width: auto;
        height: auto;
    }
    .thumbwrap .wrimg span {
        vertical-align: middle;
        height: ${imageSize}px;
        zoom: 1;
    }
    </style><![endif]-->

    <!-- stolen code ends here -->

</head>

<body class="browse">

    <div class="body">
    
        <div class="head">    
            <h1 class="browse">
                <a class="portal" href="${servletUrl}/showPortal/${auth}"><span><fmt:message key="portal"/></span></a>
                <span><fmt:message key="myTunesRss"/></span>
            </h1>
        </div>
        
        <div class="content">
            
            <div class="content-inner">
                
                <ul class="menu">
                    <li class="back">
                        <a href="${mtfn:decode64(param.backUrl)}"><fmt:message key="back"/></a>
                    </li>
                </ul>
                
                <jsp:include page="/incl_error.jsp" />
                
                <ul class="thumbwrap">
                <c:forEach items="${photos}" var="photo" varStatus="loopStatus">
                    <li>
                        <div><span class="wrimg"><span></span><img src="${servletUrl}/showImage/${auth}/<mt:encrypt key="${encryptionKey}">hash=${photo.imageHash}/size=${imageSize}</mt:encrypt>" longdesc="${mtfn:playbackLink(pageContext, photo, '')}"/></span></div>
                    </li>
                </c:forEach>
                </ul>

                <c:if test="${!empty pager}">
                    <c:set var="pagerCommand"
                           scope="request">${servletUrl}/browsePhoto/${auth}/<mt:encrypt key="${encryptionKey}">photoalbum=${param.photoalbum}</mt:encrypt>/index={index}/backUrl=${param.backUrl}</c:set>
                    <c:set var="pagerCurrent" scope="request" value="${cwfn:choose(!empty param.index, param.index, '0')}" />
                    <jsp:include page="incl_bottomPager.jsp" />
                </c:if>
                
            </div>
            
        </div>
        
        <div class="footer">
            <div class="inner"></div>
        </div>
    
    </div>

    <jsp:include page="incl_select_flashplayer_dialog.jsp"/>

    <jsp:include page="incl_functions_menu.jsp"/>

</body>

</html>