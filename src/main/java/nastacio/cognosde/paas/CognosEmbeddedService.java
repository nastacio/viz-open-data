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
package nastacio.cognosde.paas;

/**
 * @author Denilson Nastacio
 *
 */
public class CognosEmbeddedService {

    public String label;
    public String name;
    public String plan;
    public String provider;
    public String syslog_drain_url;
    public CognosEmbeddedCredentials credentials;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CognosEmbeddedService [label=" + label + ", name=" + name + ", plan=" + plan + ", provider=" + provider
                + ", syslog_drain_url=" + syslog_drain_url + ", creds=" + credentials.toString() + "]";
    }

}
