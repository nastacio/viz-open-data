# Open Data Sets with Kabanero

This project is an example of a [Kabanero](https://kabanero.io) application connecting  to a remote service, showing how the credentials for that service can be obtained and made available to the application during coding sessions.

The application was developed using the java-microprofile collection and the service instance is a [Cognos Dashboard Embedded](https://www.ibm.com/us-en/marketplace/cognos-dashboard-embedded) instance, paired with a few open data sets from the [U.N. Data](http://data.un.org/) program.   

This purpose of this example is to show how an external service reference can be integrated into the Kabanero development flow, so that the application code can access the service references whether the application is running in a developer's laptop with `appsody run` or running in a remote cluster.

## Prerequisites

- Go through the [Appsody Quick Start](https://appsody.dev/docs/getting-started/quick-start). That will ensure you have prerequisites installed and working before proceeding, as well as some basic notion of how Appsody can help you create and develop applications.


## Overview

In a running state, we want a simple arrangement, where the application is interfacing with the service instance using the service credentials. For simplicity, the service instance is configured with pointers to static CSV files instead of using a database connection. The added complexity of configuring the service to use a database connection would not add to the purpose of this sample.


![Components](src/main/webapp/img/component-diagram.png "Component Diagram for the sample") 

The CSV files are obtained directly from [http://data.un.org](http://data.un.org), but that website does not allow automated retrieval of the data by a remote service, so we uploaded without modifications to GitHub, which allows directly downloads from a remote service.

The CSV files are placed in the `/src/main/webapp/csv` directory and are accessed by the service instance using the dashboard specification listed in `/src/main/resources/dashboard-tabbed-spec.json`, where you can see data URLs like `https://raw.githubusercontent.com/nastacio/viz-open-data/master/src/main/webapp/csv/OpenData-UN-RandD.csv`.

 

## Setup the service instance

Cognos Dashboard Embedded is exclusive to the IBM Cloud, so you need to [create an IBM Cloud account](https://cloud.ibm.com/registration).

- [Install the IBM Cloud CLI](https://cloud.ibm.com/docs/cli)

- [Create an IBM Cloud API Key](https://cloud.ibm.com/iam/apikeys). Copy the "API key" value and paste it somewhere safe. It will be used later to login via command-line interface and referenced as `${IBMCLOUD_API_KEY}` in the instructions. 

With the IBM Cloud CLI and the API Key setup, it is time to create a "Lite" plan instance of Cognos Dashboard Embedded (that is a free service that gets deleted after 30 days of inactivity) .

Type the following commands in a terminal:


```
ibmcloud login --apikey ${IBMCLOUD_API_KEY}
 
ibmcloud resource service-instance-create open-data-cognos-de dynamic-dashboard-embedded lite us-south -g default
```

### Create a service key

The service instance can have multiple credentials with different roles. For this sample, we want a key with the "Reader" role.

Once again, from a terminal, create the credentials and store them in a properties file that can be referenced later when running the application locally:

```
ibmcloud resource service-key-create cognos-dashboard-key Reader --instance-name open-data-cognos-de 

ibmcloud resource service-key cognos-dashboard-key -g default --output json

cognos_credentials="$(echo $(ibmcloud resource service-key cognos-dashboard-key -g default --output json | grep credentials -A 20) | sed "s|.*\({.*}\).*|cognos_binding=\1|")"
cat > ~/tmp/sample_cognos_binding.txt << EOF
${cognos_credentials}
EOF
```

Note that the credentials were written in the form of a `key=value`, which will later be passed to the Appsody CLI.

The structure of a service credential is specific to the service and therefore needs specific code inside the application code to be parsed. For this particular service, the structure looks like this:

```
{
  "api_endpoint_url": "https://us-south.dynamic-dashboard-embedded.cloud.ibm.com/daas/",
  "apikey": "...",
  "client_id": "...",
  "client_secret": "...",
  "iam_apikey_description": "Auto-generated for key 5c3845e2-e198-4843-8ce7-811d2c054b3b",
  "iam_apikey_name": "cognos-dashboard-key",
  "iam_role_crn": "...",
  "iam_serviceid_crn": "crn:v1:bluemix:public:iam-identity::..."
}
```

### Bind the service to the cluster

This step makes the service credentials available to the cluster:

```
ibmcloud ks cluster service bind --cluster kab --service open-data-cognos-de -n default
```

Internally, this command simply creates a Kubernetes `Secret` object, prepending the word "binding-" to the service instance name to generate the secret name.

We can inspect the contents of the Secret, first changing the configuration context of the `kubectl` CLI using the following command: 

```
cluster_name=<put your cluster name here>
eval $(ibmcloud ks cluster config --cluster ${cluster_name} -s)
```

Note that users of Docker Desktop can change the configuration context of `kubectl` by simply right-clicking the Docker icon and choosing "Kubernetes", then "_<clustername>_".

With `kubectl` pointing to the remote cluster, use the secret  name to inspect its contents:

```
kubectl get secret binding-open-data-cognos-de -o json

{
    "apiVersion": "v1",
    "data": {
        "binding": "<base64 encoding of the service credentials"
    },
    "kind": "Secret",
    "metadata": {
        "name": "binding-open-data-cognos-de",
        "namespace": "default",
        ...
    },
    "type": "Opaque"
}
```
  

## Setup the application

The application in this repo has already gone through those steps, but for reference, it was created with canonical development sequence for an Appsody application, as described in this [guide](https://kabanero.io/guides/collection-microprofile).


## Referencing service credentials in an Appsody application

We want the local development environment to mimic the environment when the application is running inside a Kubernetes cluster, so that the application developer does not have to write different code for the different environments.

Service credentials are created as a [secret](https://kubernetes.io/docs/concepts/configuration/secret/) inside a Kubernetes cluster. A Kubernetes secret can be exposed to an application in [many different ways](https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets), and the [Appsody Application Operator](https://github.com/appsody/appsody-operator) can create deployment configurations using those same strategies.

For this example, we are using the approach of environment variables, since they can be equally injected into the containers created by Appsody when running the application in a local development environment. [This Appsody issue](https://github.com/appsody/appsody/issues/509) will eventually make possible to use volume mounts during development as well.

Important: Common characters such as "-" and "." are not valid in environment variable names. Make sure to check the [specification](https://pubs.opengroup.org/onlinepubs/000095399/basedefs/xbd_chap08.html) for Unix environment variables when choosing names.

### Mapping a service credential as an environment variable

Appsody applications are created in a cluster as an `AppsodyApplication` application,
which are handled by the Appsody Application Operator.   

We want the Appsody Application Operator to pull out the `binding` key from the `binding-open-data-cognos-de` secret created by the `ibmcloud ks cluster service bind` invocation, then make it available as an environment variable named `cognos_binding` for the application, which can be achieved with the following snippet in the app-deploy.yml file created by Appsody in the root directory of the application:

```
  env:
    - name: cognos_binding
      valueFrom:
        secretKeyRef:
          name: binding-open-data-cognos-de
          key: binding
```

This snippet will name the environment variable `cognos_binding` and set it to the *decoded* value of the `binding` key in the `binding-open-data-cognos-de` secret.

Refer to the [Appsody Application Operator User Guide](https://github.com/appsody/appsody-operator/blob/master/doc/user-guide.md) for further details on configuration options.


### Running the application
			
With the environment variable in a file, we can instruct `appsody run` to pass that file to container running the application, as follows:

```
appsody run --docker-options="--env-file=$(echo ~)/tmp/bindings.txt"
```

The `--env-file` option instructs docker to export each `env_var_name=value` entry in the file as an environment variable, so you can have multiple secrets in the same file.



## Remote Deployment - IBM cloud


With the application configured to connect to the remote service, it is possible to use `appsody deploy` to deploy the application to a cluster.

The command will use `kubectl` to interface with the cluster, so we need to configure `kubectl` to point to the cluster:

```
cluster_name=<put your cluster  name here>
eval $(ibmcloud ks cluster config --cluster ${cluster_name} -s)
```
 
Since the cluster is remote to your local docker daemon, the `deploy` command must also specify the `--push` parameter, so that the image is pushed to a remote repository accessible by the cluster.

```
appsody deploy --push --tag your_docker_repo/viz-open-data:0.0.1
```

Once the application deployment is complete, `appsody` will display the address for the newly deployed application.

In the case of a free-tier cluster in the IBM cloud, the external IP address is not in the Kubernetes service object for the application:

```
kubectl get services viz-open-data
```

So you need to get the public IP address from the cluster workers:


```
cluster_name=<put your cluster  name here>

public_ip=$(ibmcloud cs workers --cluster ${cluster_name} --json | grep publicIP | cut -d "\"" -f 4)
public_port=$(kubectl get service viz-open-data -o jsonpath={.spec.ports[0].nodePort})

echo "http://${public_ip}:${public_port}"

```

## Special notes on making a Java application trust a remote certificate

Some application frameworks have settings to automatically use pre-built CA certificates, but that is not the case for the java-microprofile collection in Liberty  The purpose of this example is to show how Appsody applications can reference external services, so that beyond the credentials and address for the service, the application will have to trust the service certificate.

### Using the OpenJDK CA database

A well-written application should not automatically accept untrusted certificates, so we need to instruct the Java application to use either an already existing CA database or create a new one. The default template for the java-microprofile stack currently does not set a default trust store for outbound communications (this should change soon, when Open Liberty delivers [this feature](https://github.com/OpenLiberty/open-liberty/issues/9016)) .

We can start with the first option, using the CA database shipped with Open JDK as the trust store for outbound communications. Upon inspection of the docker image used by the java-microprofile stack, we can find that database located at `/opt/java/openjdk/jre/lib/security/cacerts`, so we need to make the corresponding modifications to the server.xml file bundled with the application under `src/main/liberty/config`:


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

If we wanted to reference a service that is not present in a CA database for some reason, then a new trust store has to created with the OpenJDK `keytool` utility.

The new key needs to be added to the new trust store, referenced in the server.xml file, and included in the running server directory. 

Since "/project/user-app" is a mounted folder inside the container after `appsody run`, it may be more convenient to create the new repository from inside such container.

Launch the application with `appsody run` and ssh into it:

```
docker exec -it viz-open-data-dev /bin/sh

keytool -importkeystore -srckeystore /opt/java/openjdk/jre/lib/security/cacerts -srcstorepass changeit -keystore /project/user-app/src/main/liberty/config/resources/security/truststore.p12 -storetype PKCS12 -deststorepass mpKeystore 
```

Still in the shell inside the container, add the new key to the new trust store. Note that the key location may defer in your system, but adjust it to match the location of the service certificate. This [Stackoverflow thread](https://stackoverflow.com/questions/7885785/using-openssl-to-get-the-certificate-from-a-server) covers a number of techniques for obtaining a copy of the certificate used to secure traffic to a remote server.

This is just an example for illustration purposes, since this particular key is already signed by a trusted CA and therefore already implicitly trusted by the client application:

```
keytool -importcert -file /project/user-app/src/main/resources/us-southdynamic-dashboard-embeddedcloudibmcom.crt -alias ussouthcde -keystore  /project/user-app/src/main/liberty/config/resources/security/truststore.p12 -noprompt -storetype PKCS12 -storepass mpKeystore
```

Now replace `/opt/java/openjdk/jre/lib/security/cacerts` in server.xml with `truststore.p12`. Note that this is not an absolute path anymore, which means Open Liberty will look for it in the default runtime location (`/project/target/liberty/wlp/usr/servers/defaultServer/resources/security`) . Therefore, we need to tell the `appsody build` process to place the new trust store in that location, by making a modification to pom.xml.

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

With these changes in place, the new `truststore.p12` file will be placed in the running container and referenced from the Open Liberty server configuration, allowing the application to connect to the remote service. 


## Credits

The project was inspired by the session ["Open Data: The New Oil Fueling Civic Tech"](https://allthingsopen.org/talk/open-data-the-new-oil-fueling-civic-tech/), presented by [Jason Hibbets](https://twitter.com/jhibbets) at the ATO2019 conference.