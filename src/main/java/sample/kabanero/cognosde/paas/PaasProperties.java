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
package sample.kabanero.cognosde.paas;

import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import sample.kabanero.cognosde.exceptions.CognosException;

/**
 * Contains all runtime properties stored in the PaaS subsystem in association
 * with the application.
 * 
 * @author Denilson Nastacio
 * 
 */
public class PaasProperties {

    /**
	 * K8S credential binding for Cognos Dashboard Embedded
	 */
	private static final String COGNOS_SERVICES = "cognos_binding";

	/**
	 * Credentials pulled out of credential binding
	 */
    private CognosEmbeddedCredentials cognosService;

    /**
     * Singleton instance.
     */
    private static PaasProperties instance;

    /**
     * Logging
     */
    private static final Logger   logger           = Logger.getLogger(PaasProperties.class.getName());

    /*
     * Public methods
     */

    /**
     * Singleton constructor.
     */
    public static synchronized PaasProperties getInstance() throws CognosException {
        if (null == instance) {
            instance = new PaasProperties();
            instance.readPaaSProperties();
        }
        return instance;
    }

    /*
     * Private methods
     */

    /**
     * Private constructor for singleton pattern
     */
    private PaasProperties() {
    }

    /**
	 * Retrieves the PaaS settings for this application instance, such as credential
	 * bindings.
	 */
    private void readPaaSProperties() throws CognosException {
        readPaaSServiceProperties();
    }

    /**
     * @throws CognosException
     */
    private void readPaaSServiceProperties() throws CognosException {
		String serviceBinding = System.getenv(COGNOS_SERVICES);
		if (serviceBinding == null) {
            String errMsg = MessageFormat.format("No {0} environment variable containing the service definitions.",
					COGNOS_SERVICES);
            throw new CognosException(errMsg);
        }

		StringReader jsonIoReader = new StringReader(serviceBinding);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            cognosService = jsonb.fromJson(jsonIoReader, CognosEmbeddedCredentials.class);
        } catch (IOException e) {
            String errMsg = MessageFormat.format("Unable to read environment properties: {0}", e.getMessage());
            logger.log(Level.SEVERE, errMsg, e);
        } catch (Exception e) {
            String errMsg = MessageFormat.format("Unable to close JsonB object: {0}", e.getMessage());
            logger.log(Level.SEVERE, errMsg, e);
        }
    }

    /**
     * @return the cognosService
     */
    public CognosEmbeddedCredentials getCognosService() {
        return cognosService;
    }

}
