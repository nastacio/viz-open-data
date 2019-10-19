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

/**
 * @author Denilson Nastacio
 *
 */
public class SessionKey {

    private String kty;
    private String use;
    private String kid;
    private String alg;
    private String n;
    private String e;

    /**
     * @return the kty
     */
    public String getKty() {
        return kty;
    }
    /**
     * @param kty the kty to set
     */
    public void setKty(String kty) {
        this.kty = kty;
    }
    /**
     * @return the use
     */
    public String getUse() {
        return use;
    }
    /**
     * @param use the use to set
     */
    public void setUse(String use) {
        this.use = use;
    }
    /**
     * @return the kid
     */
    public String getKid() {
        return kid;
    }
    /**
     * @param kid the kid to set
     */
    public void setKid(String kid) {
        this.kid = kid;
    }
    /**
     * @return the alg
     */
    public String getAlg() {
        return alg;
    }
    /**
     * @param alg the alg to set
     */
    public void setAlg(String alg) {
        this.alg = alg;
    }
    /**
     * @return the n
     */
    public String getN() {
        return n;
    }
    /**
     * @param n the n to set
     */
    public void setN(String n) {
        this.n = n;
    }

    /**
     * @return the e
     */
    public String getE() {
        return e;
    }

    /**
     * @param e
     *            the e to set
     */
    public void setE(String e) {
        this.e = e;
    }
}
