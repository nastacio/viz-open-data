/********************************************************* {COPYRIGHT-TOP} ****
*  Copyright 2018 Denilson Nastacio
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
package nastacio.cognosde.util;

import java.util.List;

/**
 * @author Denilson Nastacio
 *
 */
public class CognosEmbeddedSession {

	private String serviceInstanceId;
	private String sessionId;
	private String sessionCode;
	private long createTime;
	private long expiresIn;
	private List<SessionKey> keys;

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @param sessionId
	 *            the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * @return the sessionCode
	 */
	public String getSessionCode() {
		return sessionCode;
	}

	/**
	 * @param sessionCode
	 *            the sessionCode to set
	 */
	public void setSessionCode(String sessionCode) {
		this.sessionCode = sessionCode;
	}

	/**
	 * @return the keys
	 */
	public List<SessionKey> getKeys() {
		return keys;
	}

	/**
	 * @param keys
	 *            the keys to set
	 */
	public void setKeys(List<SessionKey> keys) {
		this.keys = keys;
	}

	/**
	 * @return the createTime
	 */
	public long getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime
	 *            the createTime to set
	 */
	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	/**
	 * @return the expiresIn
	 */
	public long getExpiresIn() {
		return expiresIn;
	}

	/**
	 * @param expiresIn
	 *            the expiresIn to set
	 */
	public void setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
	}

	/**
	 * @return the serviceInstanceId
	 */
	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	/**
	 * @param serviceInstanceId
	 *            the serviceInstanceId to set
	 */
	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

}
