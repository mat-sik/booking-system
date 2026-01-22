# Booking System

This project implements a booking system that allows users to reserve services for specific dates and times. Users can
choose a service, specify a desired date, and book a time slot for a specified duration (e.g., booking a service on
10.10.2024 from 13:00 for 1 hour).

The project is composed of multiple microservices. It leverages the **CQRS** (Command Query Responsibility Segregation)
to separate booking placing related logic from querying logic. **Kafka** topic partitions are used to guarantee no
concurrent bookings for the same service and day. The bookings are stored in the **Cassandra** database. This
architecture allows the system to be highly scalable and available. We can create more partitions to increase concurrency 
of booking processing, and we can add more cassandra nodes to take the write and read pressure of the cassandra cluster, 
while at the same time providing higher availability.

The microservices were implemented using **Hexagonal Architecture** so that the code is as maintainable as possible.

The project has full observability support. Each microservice has **Open Telemetry** tracing, logs and metrics
instrumentation. The project also leverages custom instrumentation to make the microservices as observable as possible.

The observability stack used is **Otel Collector**, **Tempo**, **Loki**, **Prometheus** and **Grafana**.

The project and all of its components are fully containerized, and can be deployed using **Docker Compose** or 
on **Kubernetes** cluster. **Kubernetes** deployment utilizes **Helm** and was tested on **Minikube**, but it can be 
used on any cluster, the only difference would be the PVs configuration.

There are load-tests implemented with **Locust**, they mimic real user behavior, and we can simulate desired concurrent
users.

All the features have integration tests that utilize **Testcontainers**.

# How to Try Out the App

To quickly get started with the app, follow the steps below:

1. **Set Up the Environment**  
   1. Follow the instructions in the [Docker Compose section](#docker-compose) to set up the application locally with
      Docker.
   2. Follow the instruction in the [Kubernetes with Minikube section](#Kubernetes-Deployment) to set up the application locally with Kubernetes
      on Minikube.

2. **Explore the API with Swagger UI**  
Visit [Swagger UI](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/mat-sik/booking-system/refs/heads/main/booking-service/api-docs.yaml)
to interact with the app's API. The API documentation is automatically pulled from the `api-docs.yaml` file hosted on
my GitHub repository.

3. **Or observer artificial traffic from Locust**
Explore grafana on http://localhost:3000 and run [**Locust** tests](#load-testing).

**CORS Configuration**  
The app is configured with CORS to allow seamless integration with Swagger UI, so you can test the API directly from the
Swagger interface.

# Request Types

## Command Requests

Command requests are used to modify the state of the cassandra database. In other words, they are responsible for
creating and deleting bookings.

## Query Requests

Query requests are used to retrieve the point in time state of the cassandra database.

# Diagram

![Application Architecture](./diagrams/bookings.drawio.png)

# Command Service

The **Command Service** consumes records from **Kafka** Topic Partitions, with each partition acting as a log - a source
of truth about the bookings for a given service and a day. The log preserves the chronological order of command
requests,
which is essential for ensuring fairness and consistency. By maintaining this order, it prevents race conditions and
conflicts between concurrent booking requests.

Each partition consumer has a cache that is in sync with the cassandra table partition for a given service and day.
On each command request both cassandra and the cache state is updated. This allows the command service to not have to
query cassandra for existing bookings each time a new booking is being created. The cache size should not be a problem,
because of the limited amount of all possible occupied time ranges in a day.

Each instance of **Command Service** can be assigned to multiple partitions, that is it can have multiple consumer threads
each with its own cache. We can also increase the amount of Command Service instances. So we can have 3
**Command Service** instances each running 4 consumer threads, this way we could use for example 12 partitions for
the topic(But choosing a greater value upfront is a good idea).

The **Command Service** was implemented using **Hexagonal Architecture** so that the code is as maintainable as
possible.

# Topic Partitioning

The partition key is constructed using the date in **ISO-8601** format and the service ID, represented as a UUID.
By concatenating these two fields, the system ensures that bookings for the same service and date
are routed to the same partition. This approach maintains the chronological order of bookings for a specific service on
a given date, allowing the system to reliably determine which user successfully booked the service first.

This partition key also enables seamless scaling of processing. The processing speed is primarily influenced by the
number of bookings for a specific date and service. Since only the first records are relevant (for example, in the case
of 1,000,000 requests and only 1,000 time slots, we only care about processing the 1,000), performance should remain
strong even under the worst-case scenario.

We can also mitigate scenarios where a disproportionately high number of requests hit a single partition by increasing
the total partition count. This reduces the probability that requests unrelated to the busy service and date will hash
to the same partition as the high-traffic workload.

# Query Service

The **Query Service** allows users to see their bookings. This serves as a way for checking whether a given booking
request was successful.

The service is also used for providing the users with information which bookings are currently not booked. This
information might very quickly become stale, because of the async nature of booking processing.

The **Booking Service** interacts with **Query Service** via **gRPC** to perform users query requests.

# Booking Service

The **Booking Services** serves as an **API Gateway** for the application. It provides REST API to interact with the system.

# Swagger API Documentation

The **Booking Service** API is fully documented using Swagger. You can explore the available endpoints, their
parameters, and responses directly in the Swagger UI. This provides an interactive interface to test the API and 
understand the request/response structures.

- [Swagger UI](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/mat-sik/booking-system/refs/heads/main/booking-service/api-docs.yaml)
- [Raw API File](booking-service/api-docs.yaml)

# Helper Modules

To streamline the integration of microservices, I created several helper modules:

### `commons-models`

This module defines common data models for bookings, providing a standardized structure that is shared across the
system. This includes the BookingPartitionKey - service UUID and day.

It also contains the **Cassandra** table structures used for writing in **Command Service** and reading in
the **Query Service**. Because of the nature of the **Cassandra** query per table, the **Command Service** needs to
write to multiple tables, we use batch statements for the consistency.

### `commons-kafka`

This includes Command requests as well as serializers and deserializers. It ensures consistent **Kafka** message formats
and configurations across the **Command Service** and **Booking Service**.

### `commons-grpc-query-service`

This module contains the proto file and common

This module includes common Jackson configurations and classes that represent API responses. The response classes are
used, for example, in the **Query Service** and **Booking Service** to ensure consistent response structures.

# **Docker**

## Building **Docker** images

Execute the following commands from the root directory of the repository to build each service's Docker image:

### Query Service

```shell
docker build -f ./query-service/Dockerfile . -t booking-system-query-service
```

### Booking Service

```shell
docker build -f ./booking-service/Dockerfile . -t booking-system-booking-service
```

### Command Service

```shell
docker build -f ./command-service/Dockerfile . -t booking-system-command-service
```

## **Docker Compose**

### Prerequisites

- Docker
- Docker compose

All required services can be started seamlessly using Docker Compose.

```shell
docker compose up -d
```

# **Kubernetes Deployment**

## Prerequisites

- Minikube installed and running
- kubectl configured
- Helm 3.x installed
- Docker images built for: `query-service`, `command-service`, `booking-service`

## Setup

### 1. Create and Configure Namespace

Create the booking-system namespace:

```bash
kubectl create namespace booking-system
```

Set it as the default namespace for your context:

```bash
kubectl config set-context minikube --namespace=booking-system
```

### 2. Load Docker Images into Minikube

Load your built images into Minikube's Docker environment:

```bash
for img in query-service command-service booking-service; do
  minikube image load booking-system-$img
done
```

### 3. Prepare Persistent Volume Directories

Create the required directories on Minikube for persistent volumes with proper ownership:

```bash
minikube ssh

sudo rm -rf /mnt/data &&

# Cassandra data
sudo mkdir -p /mnt/data/cassandra-0 &&
sudo chown 999:999 /mnt/data/cassandra-0 &&

# Prometheus data
sudo mkdir -p /mnt/data/prometheus-0 &&
sudo chown 65534:65534 /mnt/data/prometheus-0 &&

# Tempo data
sudo mkdir -p /mnt/data/tempo-0 &&
sudo chown 10001:10001 /mnt/data/tempo-0 &&

# Loki data
sudo mkdir -p /mnt/data/loki-0 &&
sudo chown 10001:10001 /mnt/data/loki-0 &&

# General purpose persistent volumes
sudo mkdir -p /mnt/data/pv-0 &&
sudo mkdir -p /mnt/data/pv-1 &&
sudo mkdir -p /mnt/data/pv-2 &&
sudo mkdir -p /mnt/data/pv-3 &&
sudo mkdir -p /mnt/data/pv-4 &&

# Set proper permissions
sudo chmod 700 /mnt/data/* &&

exit
```

## Deployment

### Build Helm Dependencies

Build dependencies for all Helm charts:

```bash
helm dependency build ./helm/infra
helm dependency build ./helm/observability/grafana
helm dependency build ./helm/observability/loki
helm dependency build ./helm/observability/opentelemetry-collector
helm dependency build ./helm/observability/prometheus
helm dependency build ./helm/observability/tempo
```

### Install Infrastructure and Observability Stack

Install the infrastructure components:

```bash
helm install booking-system-infra ./helm/infra -n booking-system
```

Install the observability stack:

```bash
helm install booking-system-observability-grafana ./helm/observability/grafana -n booking-system
helm install booking-system-observability-loki ./helm/observability/loki -n booking-system
helm install booking-system-observability-opentelemetry-collector ./helm/observability/opentelemetry-collector -n booking-system
helm install booking-system-observability-prometheus ./helm/observability/prometheus -n booking-system
helm install booking-system-observability-tempo ./helm/observability/tempo -n booking-system
```

### Deploy Application Services

Apply Kubernetes manifests for application services:

```bash
kubectl apply -f ./k8s -n booking-system
```

## Uninstallation

### Remove Helm Releases

```bash
helm uninstall booking-system-infra -n booking-system
helm uninstall booking-system-observability-grafana -n booking-system
helm uninstall booking-system-observability-loki -n booking-system
helm uninstall booking-system-observability-opentelemetry-collector -n booking-system
helm uninstall booking-system-observability-prometheus -n booking-system
helm uninstall booking-system-observability-tempo -n booking-system
```

### Clean Up Resources

Delete application resources and persistent volume claims:

```bash
kubectl delete -f ./k8s -n booking-system
kubectl delete pvc broker-kafka-broker-controller-0 -n booking-system
kubectl delete pvc cassandra-cassandra-0 -n booking-system
kubectl delete pvc storage-booking-system-observability-tempo-0 -n booking-system
```

## Port Forwarding for Local Access

### Grafana Dashboard

Access Grafana at http://localhost:3000 (login: admin / password: admin):

```bash
kubectl port-forward service/booking-system-observability-grafana 3000:80 -n booking-system
```

### Booking Service API

Access the booking service at http://localhost:8080:

```bash
kubectl port-forward service/booking-service 8080:8080 -n booking-system
```

### Cassandra Database

Access Cassandra at localhost:9042:

```bash
kubectl port-forward service/cassandra 9042:9042 -n booking-system
```

## Component Management

### Cassandra

#### Cluster Monitoring

Check pod status:

```bash
kubectl get pods -n booking-system -o wide -w
```

View cluster node status:

```bash
kubectl exec cassandra-0 -n booking-system -- nodetool status
```

View detailed node information:

```bash
kubectl exec cassandra-0 -n booking-system -- nodetool info
```

#### Scaling the Cluster

To increase the Cassandra cluster size:

1. **Create additional persistent volumes** - Ensure you have PV definitions in `values.yaml` and corresponding
   directories on Minikube (e.g., `/mnt/data/cassandra-1`, `/mnt/data/cassandra-2`)

2. **Update the StatefulSet** - Increase the replica count in your Cassandra StatefulSet

3. **Update seed nodes** - Modify the `CASSANDRA_SEEDS` environment variable:
   ```yaml
   - name: CASSANDRA_SEEDS
     value: cassandra-0.cassandra-headless.booking-system.svc.cluster.local,cassandra-1.cassandra-headless.booking-system.svc.cluster.local
   ```

### Kafka

#### Cluster Diagnostics

View environment variables (note: `node_id` is set in the command block):

```bash
kubectl exec -n booking-system kafka-broker-controller-0 -- env
```

View `node_id` from the process environment:

```bash
kubectl exec -n booking-system kafka-broker-controller-0 -- cat /proc/1/environ | tr '\0' '\n' | grep NODE
```

Check metadata quorum status:

```bash
kubectl exec -n booking-system kafka-broker-controller-0 -- /opt/kafka/bin/kafka-metadata-quorum.sh \
  --bootstrap-server kafka-broker-controller-0.kafka-broker-controller.booking-system.svc.cluster.local:9092 \
  describe --status
```

Get the cluster ID:

```bash
kubectl exec -n booking-system kafka-broker-controller-0 -- /opt/kafka/bin/kafka-cluster.sh \
  cluster-id --bootstrap-server kafka-broker-controller-0.kafka-broker-controller.booking-system.svc.cluster.local:9092
```

List all brokers and their API versions:

```bash
kubectl exec -n booking-system kafka-broker-controller-0 -- /opt/kafka/bin/kafka-broker-api-versions.sh \
  --bootstrap-server kafka-broker-controller-0.kafka-broker-controller.booking-system.svc.cluster.local:9092
```

## Network Inspection

View all services:

```bash
kubectl get svc -n booking-system
```

View endpoint slices:

```bash
kubectl get endpointslice -n booking-system
```

## Load Testing

Navigate to the load tests directory and set up the environment:

```bash
cd load-tests
source venv/bin/activate
pip install -r requirements.txt
```

Run Locust load tests (ensure booking service is accessible on localhost:8080):

```bash
cd load-tests
locust -f locustfile.py --host=http://localhost:8080
```

Access the Locust web interface at http://localhost:8089
