# viz-open-data
Example of [Kabanero](https://kabanero.io) java-microprofile application using a hosted service instance of [Cognos Dashboard Embedded](https://www.ibm.com/us-en/marketplace/cognos-dashboard-embedded) to visualize various data sets.

This purpose of this example is to show how an external service reference can be integrated into the Kabanero development flow, from local runs with `appsody run` to runs in a local cluster with `appsody deploy`. 

## Create the service instance

- Cognos Dashboard Embedded is exclusive to the IBM Cloud, so you need to [create an IBM Cloud account](https://cloud.ibm.com/registration)

- [Install the IBM Cloud CLI](https://cloud.ibm.com/docs/cli)

- [Create an IBM Cloud API Key](https://cloud.ibm.com/iam/apikeys). Copy the "API key" value and paste it somewhere safe. It will be used later to login via command-line interface and referenced as `${IBMCLOUD_API_KEY}` in the instructions. 

- Create a "Lite" plan instance of Cognos Dashboard Embedded. That is a free service that gets deleted after 30 days of inactivity.


```
ibmcloud login --apikey ${IBMCLOUD_API_KEY}
 
ibmcloud resource service-instance-create open-data-cognos-de dynamic-dashboard-embedded lite us-south -g default
```

- Create the credentials and store them in a properties file that you can reference later when running the application locally:

```
ibmcloud resource service-key-create cognos-dashboard-key Reader --instance-name open-data-cognos-de 

ibmcloud resource service-key cognos-dashboard-key -g default --output json

cognos_credentials="$(echo $(ibmcloud resource service-key cognos-dashboard-key -g default --output json | grep credentials -A 20) | sed "s|.*\({.*}\).*|cognos_binding=\1|")"
cat > ~/tmp/sample_cognos_binding.txt << EOF
${cognos_credentials}
EOF
```


```

Run the server with binding as an environment variable:

```
appsody run --docker-options="--env-file=/Users/nastacio/tmp/bindings.txt"
```



## Remote Deployment - IBM cloud



Configure kubectl to point to IBM Cloud cluster

```
eval $(ibmcloud ks cluster config --cluster kab -s)
```


Bind the service to the cluster

```
ibmcloud ks cluster service bind --cluster kab --service open-data-cognos-de -n default
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

