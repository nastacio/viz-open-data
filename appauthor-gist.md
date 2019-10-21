# General notes on the creation of the application

Add Kabanero repository to appsody list of repositories;

```
appsody repo add kabanero https://github.com/kabanero-io/collections/releases/download/0.2.0/kabanero-index.yaml
```

Initialize application. From an empty directory:

```
appsody init kabanero/java-microprofile
```

## Create all files under "sample.kabanero.cognosde"

Follow [Cognos Dashboard Embedded docs](https://cloud.ibm.com/docs/services/cognos-dashboard-embedded).


## Mapping service credential to K8S

Determining how create the environment variable with the Cognos Dashboard Embedded credential inside `app-deploy.yaml`:

[Reference to "envFrom" and "secretKeyRef"](https://github.com/appsody/appsody-operator/blob/master/doc/user-guide.md) . In essence, we want the Appsody operator to pull out the `binding` key from the `binding-open-data-cognos-de` secret created by the `ibmcloud ks cluster service bind` step in the README.md, then make it available as an environment variable named `cognos-binding` for the application;

### Map a secret as an environment variable

If we wanted the whole "binding" key from the `binding-open-data-cognos-de` secret, we could have used:

```
  envFrom:
    - secretRef:
        name: binding-open-data-cognos-de
```

### Map a secret key with a different name

For applications using multiple bindings and in situations where the key name inside a secret clashes with the key name in a different secret, you want to tell the Appsody operator to assign different environment variable names to each secret key, which can be achieved with a `secretKeyRef` element inside `app-deploy.yaml`: 

```
  env:
    - name: cognos-binding
      valueFrom:
        secretKeyRef:
          name: binding-open-data-cognos-de
          key: binding
```



## Make the application trust a remote certificate

The purpose of this example is to show how Appsody applications can reference external services, so that beyond the credentials and address for the service, the application will have to trust the service certificate.

### Using the OpenJDK CA database

A well-written application should not automatically accept untrusted certificates, so we need to instruct the Java application to use either an already existing CA database or create a new one. We can start with the first option, using the CA shipped with Open JDK. 

In order to reference that CA database, located under `/opt/java/openjdk/jre/lib/security/cacerts` in the running container, we need to make the corresponding modifications to the server.xml file bundled with the application:


```
<server description="Liberty server">
    ...

	<keyStore id="defaultTrustStore" 
	    password="changeit"
		readOnly="false" 
		type="JKS" 
		location="/opt/java/openjdk/jre/lib/security/cacerts">
	</keyStore>

	<ssl id="defaultSSLSettings" 
	     keyStoreRef="defaultKeyStore"
		 trustStoreRef="defaultTrustStore"></ssl>

	<sslDefault 
	    sslRef="defaultSSLSettings" 
	    outboundSSLRef="defaultSSLSettings"></sslDefault>

    ...
    
</server>
```

### Using a new CA database

If we wanted to reference a service that is not present in a CA database for some reason, then a 
new truststore has to created with the OpenJDK `keytool` utility, the new key added to the new truststore, then referenced in the server.xml file, and included in the running server directory by maven.

Since "/project/user-app" is a mounted folder inside the container after `appsody run`, it may be more convenient to create the new repository from inside such cointainer.


Launch the application with `appsody run` and ssh into it:

```
docker exec -it viz-open-data-dev /bin/sh

keytool -importkeystore -srckeystore /opt/java/openjdk/jre/lib/security/cacerts -keystore /project/user-app/src/main/liberty/config/resources/security/truststore.jks -deststoretype JKS -deststorepass mpKeystore -srcstorepass changeit
```

Still in the shell inside the container: add the new key to the new truststore. This is just an example for illustration purposes, since that key is already signed by a trusted CA and therefore already implicitly trusted by the client application:

```
keytool -importcert -file /project/user-app/src/main/resources/us-southdynamic-dashboard-embeddedcloudibmcom.crt -alias ussouthcde -keystore  /project/user-app/src/main/liberty/config/resources/security/truststore.p12 -noprompt -storetype PKCS12 -storepass mpKeystore
```

Now replace `/opt/java/openjdk/jre/lib/security/cacerts` in server.xml with `truststore.p12`. Note that this is not an absolute path anymore, which means Open Liberty will look for it in the default runtime location (`/project/target/liberty/wlp/usr/servers/defaultServer/resources/security`) . Therefore, we need to tell the appsody build process to place the new truststore in that location, by making a modification to pom.xml.

Add a plugin execution for the `maven-resources-plugin`, under the `build/plugins` element:

```
			<plugin>
				<artifactId>maven-resources-plugin</artifactId>
				<version>2.6</version>
				<executions>
					<execution>
						<id>copy-resources</id>
						<phase>install</phase>
						<goals>
							<goal>copy-resources</goal>
						</goals>
						<configuration>
							<outputDirectory>${basedir}/target/liberty/wlp/usr/servers/defaultServer/resources/security</outputDirectory>
							<resources>
								<resource>
									<directory>${basedir}/src/main/liberty/config/resources/security</directory>
									<filtering>false</filtering>
									<includes>
										<include>truststore.p12</include>
									</includes>
								</resource>
							</resources>
						</configuration>
					</execution>
				</executions>
			</plugin>
```

