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
	                dashboardAPI.setMode(dashboardAPI.MODES.EDIT);
	                dashboardAPI.toggleProperties();
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
  height: 900px;
}
body {
     min-height: 900px;
	font-family: Helvetica, Arial, sans-serif;
	font-size: 14px;
}

option:disabled {
	color: #00669E;
	font-weight: bolder;
}

.leftPanel {
	height: 100%;
	width: 400px;
	background-color: #d0d2d3;
	float: left;
	position: relative;
	box-shadow: 5px 5px 10px #264A60;
	border: 2px solid #3C4646;
	margin: 2px 10px 2px 2px;
	display: flex;
	flex-direction: column;
}

.leftPanel>div {
	padding: 10px;
}

.select {
	color: #3D4852;
	font-weight: bold;
}

.select>select {
	width: 100%;
	border-radius: 5px;
	-moz-border-radius: 5px;
	-webkit-border-radius: 5px;
	border: 2px solid #264A60;
	font-size: 14px;
	padding: 2px;
}

.input {
	font-size: 12px;
	font-family: monospace;
	height: 100%;
	border-radius: 5px;
	-moz-border-radius: 5px;
	-webkit-border-radius: 5px;
	border: 2px solid #264A60;
	background-color: white;
	margin: 10px;
	overflow-y: auto;
}

.group {
	padding: 10px;
}

.button {
	background-color: #00669E;
	color: white;
	padding: 10px 15px;
	text-align: center;
	text-decoration: none;
	display: inline-block;
	font-size: 16px;
	bottom: 10px;
	right: 10px;
	box-shadow: 3px 3px 10px #264A60;
	cursor: hand;
	border-radius: 10px;
	-moz-border-radius: 10px;
	-webkit-border-radius: 10px;
	border: 2px solid #264A60;
	float: right;
}

.iframeContainer {  
	height: 900px; 
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
    ${cognos.sessionCode}
	<div class="iframeContainer" id="containerDivId"></div>
</body>

</html>
