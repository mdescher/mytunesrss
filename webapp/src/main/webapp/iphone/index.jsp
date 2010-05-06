<html>
<head>

    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no, maximum-scale=1.0" />
    <script src="${appUrl}/js/jquery.js" type="text/javascript"></script>
    <script src="${appUrl}/js/jquery.json.js" type="text/javascript"></script>
    <script src="${appUrl}/iphone/js/mytunesrss-remote-api.js" type="text/javascript"></script>
    <script type="text/javascript">
        var $jQ = jQuery.noConflict();        
        var sid = "/${remoteApiSession}";
        var reqId = 0;
        var mytunesrssServer = "${appUrl}";

        function init() {
            checkSession();
        }

        function error(code, msg) {
            alert("Server error " + code + ": \"" + msg + "\"");
            init();
        }

        function clientError(msg) {
            alert("Client error: \"" + msg + "\"");
            init();
        }

        function checkSession() {
            if (sid != "undefined" && sid != null && sid.length > 1) {
                mytunesrss("LoginService.ping", null, loadInitialPage);
            } else {
                document.getElementById("content").setAttribute("src", "${appUrl}/iphone/login.jsp");
            }
        }

        function loadInitialPage(json) {
            if (!json.error && json.result) {
                document.getElementById("content").setAttribute("src", "${appUrl}/iphone/portal.jsp");
            } else {
                document.getElementById("content").setAttribute("src", "${appUrl}/iphone/login.jsp");
            }
        }

        function initScroll() {
            setTimeout("window.scrollTo(0, 1)", 100);
        }
    </script>

</head>
<body onload="init()">
<div id="dynascript"></div>
<iframe id="content" width="100%" height="100%" frameborder="0" marginheight="0" marginwidth="0"></iframe>
</body>
</html>
