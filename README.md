# viz-open-data

This project is an example of a [Kabanero](https://kabanero.io) application that can connect to an instance of a remote service.

The application was developed using the java-microprofile collection and the service instance is a [Cognos Dashboard Embedded](https://www.ibm.com/us-en/marketplace/cognos-dashboard-embedded) , paired with a few open data sets from the [undata program](http://data.un.org/).

This purpose of this example is to show how an external service reference can be integrated into the Kabanero development flow, so that the application code can access the service references whether the application is running on the developer's laptop with `appsody run` or running on a remote cluster after being deployed with `appsody deploy` or with a [Tektoncd pipeline](https://github.com/tektoncd).

## Overview

This is what you will be building:


 

## Create the service instance

Cognos Dashboard Embedded is exclusive to the IBM Cloud, so you need to [create an IBM Cloud account](https://cloud.ibm.com/registration).

In order to simplify these instructoon 

- [Install the IBM Cloud CLI](https://cloud.ibm.com/docs/cli)

- [Create an IBM Cloud API Key](https://cloud.ibm.com/iam/apikeys). Copy the "API key" value and paste it somewhere safe. It will be used later to login via command-line interface and referenced as `${IBMCLOUD_API_KEY}` in the instructions. 

- Create a "Lite" plan instance of Cognos Dashboard Embedded. That is a free service that gets deleted after 30 days of inactivity.


```
ibmcloud login --apikey ${IBMCLOUD_API_KEY}
 
ibmcloud resource service-instance-create open-data-cognos-de dynamic-dashboard-embedded lite us-south -g default
```

### Create a service key

- Create the credentials and store them in a properties file that you can reference later when running the application locally:

```
ibmcloud resource service-key-create cognos-dashboard-key Reader --instance-name open-data-cognos-de 

ibmcloud resource service-key cognos-dashboard-key -g default --output json

cognos_credentials="$(echo $(ibmcloud resource service-key cognos-dashboard-key -g default --output json | grep credentials -A 20) | sed "s|.*\({.*}\).*|cognos_binding=\1|")"
cat > ~/tmp/sample_cognos_binding.txt << EOF
${cognos_credentials}
EOF
```

### Bind the service to the cluster

```
ibmcloud ks cluster service bind --cluster kab --service open-data-cognos-de -n default
```




## Iterate in local development

We want the local development environment to mimic the environment when the application is running inside a Kubernetes cluster, so that the application developer does not have to write different code for the different environments.

### Using service credentials in an Appsody application

The best-practice is to look at how service credentials are mapped in a Kubernetes cluster, and then recreate that mapping in the local development workstation.

Service credentials are created as a [secret](https://kubernetes.io/docs/concepts/configuration/secret/) inside a Kubernetes cluster. A Kubernetes secret can be exposed to an application in [many different ways](https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets) , and currently the [Appsody Application Operator](https://github.com/appsody/appsody-operator) always injects Kubernetes secrets as environment variables for the container running the application, as described in the [Appsody Application Operator User Guide](https://github.com/appsody/appsody-operator/blob/master/doc/user-guide.md).

## Mapping service credential to K8S

Determining how create the environment variable with the Cognos Dashboard Embedded credential inside `app-deploy.yaml`:

In essence, we want the Appsody operator to pull out the `binding` key from the `binding-open-data-cognos-de` secret created by the `ibmcloud ks cluster service bind` step in the README.md, then make it available as an environment variable named `cognos-binding` for the application;

### Map a secret as an environment variable

If we wanted the whole "binding" key from the `binding-open-data-cognos-de` secret, we could have used:

```
  envFrom:
    - secretRef:
        name: binding-open-data-cognos-de
```

### Map a secret key with a different name

For applications using multiple bindings and in situations where the key name inside a secret clashes with the key name in a different secret, you want to tell the Appsody operator to assign different environment variable names to each secret key, which can be achieved with a `secretKeyRef` element: 

```
  env:
    - name: cognos-binding
      valueFrom:
        secretKeyRef:
          name: binding-open-data-cognos-de
          key: binding
```




```
appsody run --docker-options="--env-file=$(echo ~)/tmp/bindings.txt"
```



## Remote Deployment - IBM cloud



Configure kubectl to point to IBM Cloud cluster

```
eval $(ibmcloud ks cluster config --cluster kab -s)
```


 

Deploy application

```
appsody deploy --push
```


Show application URL

```
public_ip=$(ibmcloud cs workers --cluster ${cluster_name} --json | grep publicIP | cut -d "\"" -f 4)

public_port=$(kubectl get service viz-open-data -o jsonpath={.spec.ports[0].nodePort})

echo "http://${public_ip}:${public_port}"

```


## Credits

The project was inspired by the session ["Open Data: The New Oil Fueling Civic Tech"](https://allthingsopen.org/talk/open-data-the-new-oil-fueling-civic-tech/), presented by [Jason Hibbets](https://twitter.com/jhibbets) at the ATO2019 conference.