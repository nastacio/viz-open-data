<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<jsp:useBean scope="session" id="cognos"
	class="nastacio.cognosde.util.CognosEmbeddedUtil" />

<jsp:setProperty name="cognos" property="appUri" 
    value="<%= request %>" />

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Cognos Embedded Dashboard</title>
<script src="https://dde-us-south.analytics.ibm.com/daas/CognosApi.js"></script>

<script>
function init() {
	window.api = new CognosApi({
		cognosRootURL : 'https://dde-us-south.analytics.ibm.com/daas/',
		sessionCode : '${cognos.sessionCode}',
		node : document.getElementById('containerDivId')
	});

	window.api.initialize().then(function() {
		console.log('API created successfully.');

 		window.api.dashboard.openDashboard({
		    dashboardSpec: ${cognos.dashboardSpec}
		}).then(
		    function(dashboardAPI) {
		        console.log('Dashboard created successfully.');
		        window.dashboardAPI = dashboardAPI;
		    }
		).catch(
		    function(err) {
		        console.log(err);
		    }
		);

	}, function(err) {
		console.log('Failed to create API. ' + err.message);
	});

	window.onError = function(event) {
		console.log('onError:' + JSON.stringify(event));
	};
	// window.api.on(CognosApi.EVENTS.REQUEST_ERROR, window.onError);
}
</script>

<style>
html {
  height: 97vh;
}
body {
    height: 97vh;
	font-family: Helvetica, Arial, sans-serif;
	font-size: 14px;
}

.iframeContainer {  
	height: 97vh;
	width: 100%;
	display: flex;
	flex-direction: column;
}

.iframeContainer pre {
	display: flex;
	font-size: medium;
	white-space: pre-wrap;
}
</style>

</head>

<body onload="init()">
	<div class="iframeContainer" id="containerDivId"></div>
</body>

</html>
