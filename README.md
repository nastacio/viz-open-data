# viz-open-data
Example of Kabanero java-microprofile application using a Cognos dashboard to visualize open data

## Setup

Create instance of Cognos Dashboard Embedded:

https://cloud.ibm.com/catalog/services/ibm-cognos-dashboard-embedded

```
ibmcloud resource service-instance-create open-data-cognos-de dynamic-dashboard-embedded lite us-south -g default

ibmcloud resource service-alias-create open-data-cognos-de --instance-name open-data-cognos-de -s dev
```


Create a service key for the service instance:

```
ibmcloud service key-create open-data-cognos-de dashboard_key
```


```
curl -X POST "https://dde-us-south.analytics.ibm.com/daas/v1/session" -H "accept: application/json" -H  "authorization: Basic <base64 client_id:client_secret>" -H  "Content-Type: application/json" -d "{  \"expiresIn\": 3600,  \"webDomain\": \"https://dde-us-south.analytics.ibm.com\"}"
```

Configure kubectl to point to IBM Cloud cluster
eval $(ibmcloud ks cluster config --cluster kab -s)


```
public_ip=$(ibmcloud cs workers --cluster ${cluster_name} --json | grep publicIP | cut -d "\"" -f 4)

public_port=$(kubectl get service viz-open-data -o jsonpath={.spec.ports[0].nodePort})

echo "http://${public_ip}:${public_port}"

```


Bind the service to the cluster

ibmcloud ks cluster service bind --cluster kab -n default --service open-data-cognos-de 


Reference to "envFrom" "secretKeyRef"
https://github.com/appsody/appsody-operator/blob/master/doc/user-guide.md


Create truststore and add Cognos service certificate
```
keytool -importkeystore -srckeystore /opt/java/openjdk/jre/lib/security/cacerts -keystore /Users/nastacio/github.com/viz-open-data/src/main/liberty/config/resources/security/truststore.jks -deststoretype JKS -deststorepass mpKeystore -srcstorepass changeit

keytool -importcert -file /Users/nastacio/github.com/viz-open-data/src/main/resources/us-southdynamic-dashboard-embeddedcloudibmcom.crt -keystore  /Users/nastacio/github.com/viz-open-data/src/main/liberty/config/resources/security/truststore.jks -noprompt -storetype JKS -storepass mpKeystore

keytool -importcert -file /project/user-app/src/main/resources/us-southdynamic-dashboard-embeddedcloudibmcom.crt -alias ussouthcde -keystore  /project/user-app/src/main/liberty/config/resources/security/truststore.p12 -noprompt -storetype PKCS12 -storepass mpKeystore

```

Run the server with binding as an environment variable:

```
appsody run --docker-options="--env-file=/Users/nastacio/tmp/bindings.txt"
```


Add Kabanero repository to appsody list of repositories;

```
appsody repo add kabanero https://github.com/kabanero-io/collections/releases/download/0.2.0/kabanero-index.yaml
```
