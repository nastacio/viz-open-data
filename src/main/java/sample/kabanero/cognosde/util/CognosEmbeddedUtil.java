/********************************************************* {COPYRIGHT-TOP} ****
*  Copyright 2019 Denilson Nastacio
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
***************************************************************************** {COPYRIGHT-END} **/
package sample.kabanero.cognosde.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;

import sample.kabanero.cognosde.exceptions.CognosException;
import sample.kabanero.cognosde.paas.CognosEmbeddedCredentials;
import sample.kabanero.cognosde.paas.PaasProperties;

/**
 * 
 * 
 * @author Denilson Nastacio
 */
public class CognosEmbeddedUtil {

	/**
	 * 
	 */
	private static final String APPLICATION_JSON = "application/json";

	private CognosEmbeddedSession cachedSession;
	private Object cachedSessionLock = new Object();

	private String appUri;

	/**
	 * Java logger.
	 */
	private static final Logger logger = Logger.getLogger(CognosEmbeddedUtil.class.getName());

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getSessionCode() throws CognosException {
		String result = null;
		synchronized (cachedSessionLock) {
			if (cachedSession == null) {
				cachedSession = getCognosSession();
			}
			result = cachedSession.getSessionCode();
		}

		return result;
	}

	/**
	 * 
	 * @return
	 * @throws CognosException
	 */
	public String getDashboardSpec() throws CognosException {
		StringBuilder sb = new StringBuilder();

		try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("/dashboard-tabbed-spec.json");
				BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			String errMsg = MessageFormat.format("Unable to dashboard specification due to {0}", e.getMessage());
			throw new CognosException(errMsg, e);
		}

		return sb.toString();
	}

	/**
	 * Issues Cognos service request to create session object.
	 * 
	 * @param result the session code in the session object.
	 * 
	 * @return
	 * 
	 * @throws CognosException
	 */
	private CognosEmbeddedSession getCognosSession() throws CognosException {

		CognosEmbeddedSession result = null;

		try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("/session-context.json")) {
			if (is != null) {
				result = parseSessionContext(is);
				if ((result != null) && (!isSessionValid(result))) {
					result = null;
				}
			}

			if (result == null) {
				result = createNewCognosSession();
			}
		} catch (IOException e) {
			String errMsg = MessageFormat.format("Unable to process existing cached session, due to {0}",
					e.getMessage());
			throw new CognosException(errMsg);
		}

		return result;
	}

	/**
	 * Create a new session via request to Cognos Embedded Service infrastructure.
	 * 
	 * curl -X POST "https://dde-us-south.analytics.ibm.com/daas/v1/session" -H
	 * "accept: application/json" -H "authorization: Basic <base64
	 * client_id:client_secret>" -H "Content-Type: application/json" -d "{
	 * \"expiresIn\": 3600, \"webDomain\":
	 * \"https://dde-us-south.analytics.ibm.com\"}"
	 * 
	 * @return the created session
	 * 
	 * @throws CognosException if the session cannot be created for whatever reason.
	 */
	private CognosEmbeddedSession createNewCognosSession() throws CognosException, IOException {
		CognosEmbeddedSession result = null;

		PaasProperties paasProperties = PaasProperties.getInstance();
		CognosEmbeddedCredentials cognosService = paasProperties.getCognosService();

		String hostname = appUri;
		String payload = "{ \"expiresIn\": 3600, \"webDomain\": \"" + hostname + "\"}";
		String authStr = createCognosAuthStr(cognosService);
		URI uri = getCognosEndpointUri(cognosService, null);
		HttpURLConnection conn = postJsonPayload(uri, payload, authStr);
		try {
			int responseCode = conn.getResponseCode();
			if (HttpURLConnection.HTTP_CREATED == responseCode) {
				result = parseSessionContext(conn.getInputStream());
			} else {
				String errMsg = MessageFormat.format("Unable to acquire session context. Http response status is {0}.",
						responseCode);
				throw new CognosException(errMsg);
			}
		} finally {
			conn.disconnect();
		}

		return result;
	}

	/**
	 * Reads a Cognos session context from an input stream.
	 * 
	 * @param is the input stream.
	 * 
	 * @return the parsed session
	 * 
	 * @throws CognosException if the session cannot be created for whatever reason.
	 */
	private CognosEmbeddedSession parseSessionContext(InputStream is) throws CognosException {
		CognosEmbeddedSession s;
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			String contentStr = sb.toString();
	        Jsonb jsonb = JsonbBuilder.create();
			s = jsonb.fromJson(contentStr, CognosEmbeddedSession.class);
			logger.info(() -> MessageFormat.format("Read session context: {0}", contentStr));
		} catch (IOException e) {
			throw new CognosException(e);
		}
		return s;
	}

	/**
	 * 
	 * @param cognosService
	 * @param path
	 * 
	 * @return
	 * @throws CognosException
	 */
	private URI getCognosEndpointUri(CognosEmbeddedCredentials cognosService, String path) throws CognosException {
		URI uri;
		try {
			String uriStr = cognosService.api_endpoint_url + "v1/session";
			if (path != null) {
				uriStr += path;
			}
			uri = new URI(uriStr);
		} catch (URISyntaxException e1) {
			String errMsg = MessageFormat.format("Invalid credentials API endpoint {0}. Detailed message is {1}",
					cognosService.api_endpoint_url, e1.getMessage());
			throw new CognosException(errMsg);
		}
		return uri;
	}

	/**
	 * @param session
	 * @return
	 */
	private boolean isSessionValid(CognosEmbeddedSession session) throws CognosException {
		boolean result = false;

		URI uri = getCognosEndpointUri(PaasProperties.getInstance().getCognosService(), "/" + session.getSessionId());
		String authStr = createCognosAuthStr(PaasProperties.getInstance().getCognosService());
		HttpsURLConnection huc;
		try {
			final URL targetUrl = uri.toURL();
			huc = (HttpsURLConnection) (targetUrl.openConnection());
			huc.addRequestProperty("Accept", APPLICATION_JSON);
			huc.addRequestProperty("Authorization", "Basic " + authStr);

			huc.setRequestMethod("GET");
			huc.setDoOutput(true);
			int httpResponseCode = huc.getResponseCode();
			if (httpResponseCode == 200) {
				CognosEmbeddedSession sc = parseSessionContext(huc.getInputStream());
				long expirationTime = sc.getCreateTime() + sc.getExpiresIn() * 1000;
				if (expirationTime > System.currentTimeMillis()) {
					result = true;
					logger.info(() -> MessageFormat.format("Session {0} still valid until {1}", session.getSessionId(),
							new Date(expirationTime)));
				} else {
					logger.info(() -> MessageFormat.format("Session {0} expired on {1}", session.getSessionId(),
							new Date(expirationTime)));

				}
			} else {
				logger.info(() -> MessageFormat.format("Session information not available for session id {0}",
						session.getSessionId()));
			}
		} catch (IOException e) {
			String errMsg = MessageFormat.format("Unable to query session contents from {0}, due to {1}",
					uri.toASCIIString(), e.getMessage());
			throw new CognosException(errMsg, e);
		}

		return result;
	}

	/**
	 * 
	 * @param uri
	 * @param payload
	 * @param authStr
	 * @return
	 * @throws IOException
	 */
	private HttpURLConnection postJsonPayload(URI uri, String payload, String authStr) throws CognosException {
		HttpURLConnection huc;
		try {
			final URL targetUrl = uri.toURL();
			huc = (HttpURLConnection) (targetUrl.openConnection());
			huc.addRequestProperty("Accept", APPLICATION_JSON);
			huc.addRequestProperty("Content-Type", APPLICATION_JSON);
			huc.addRequestProperty("Authorization", "Basic " + authStr);

			huc.setRequestMethod("POST");
			huc.setDoOutput(true);
			if (!payload.isEmpty()) {
				huc.getOutputStream().write(payload.getBytes("UTF-8"));
			}
		} catch (IOException e) {
			String errMsg = MessageFormat.format("Unable to post json payload due to {0}", e.getMessage());
			throw new CognosException(errMsg, e);
		}

		return huc;
	}

	/**
	 * @param cognosService
	 * @return
	 */
	private String createCognosAuthStr(CognosEmbeddedCredentials cognosService) {
		return Base64.getEncoder().encodeToString(
				(cognosService.client_id + ":" + cognosService.client_secret).getBytes());
	}

	/**
	 * @param value
	 * @return
	 */
	String sessionEncrypt(String data, SessionKey sessionKey) throws CognosException {
		String encPrefix = "{enc}";

		String result = null;

		try {
			String nStr = sessionKey.getN();
			String eStr = sessionKey.getE();
			BigInteger modulus = new BigInteger(Base64.getUrlDecoder().decode(nStr));
			BigInteger exponent = new BigInteger(Base64.getUrlDecoder().decode(eStr));
			RSAPublicKeySpec rpks = new RSAPublicKeySpec(modulus, exponent);
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(rpks);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] encryptedbytes = cipher.doFinal(data.getBytes());
			result = encPrefix + new String(Base64.getEncoder().encode(encryptedbytes));
		} catch (GeneralSecurityException e) {
			String errMsg = MessageFormat.format("Unable to encrypt the session parameters due to {0}", e.getMessage());
			throw new CognosException(errMsg, e);
		}

		return result;
	}

	public void setAppUri(HttpServletRequest appUri) {
		this.appUri = appUri.getScheme() + "://" + appUri.getServerName() + ":" + appUri.getServerPort();
	}

}
