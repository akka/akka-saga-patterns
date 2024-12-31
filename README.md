# Akka Saga Patterns

This demo project demonstrates more sophisticated use of Akka components to implement a distributed system that uses both Saga flavors: choreography and orchestration. 

## Prerequisites

- A [Akka account](https://console.akka.io/register)
- Java 21 (we recommend [Eclipse Adoptium](https://adoptium.net/marketplace/))
- [Apache Maven](https://maven.apache.org/install.html)
- [Docker Engine](https://docs.docker.com/get-started/get-docker/)
- [`curl` command-line tool](https://curl.se/download.html)

## Concepts

### Designing

To understand the Akka concepts behind this example, see [Development Process](https://doc.akka.io/concepts/development-process.html) in the documentation.

### Developing

This project demonstrates the use of Workflow and Event Sourced Entity components. For more information, see [Developing Services](https://doc.akka.io/java/index.html).

## Build

Use Maven to build your project:

```shell
mvn compile
```

## Run Locally

To start your service locally, run:

```shell
mvn compile exec:java
```

This command will start your Akka service.

## Exercising the service

Create wallet

```shell
curl -i -X POST http://localhost:9000/wallet/1/create/100  
```

Deposit (only 1 request will update the balance, the other will be deduplicated)

```shell
curl http://localhost:9000/wallet/1/deposit \
  -i -X PATCH \
  --header "Content-Type: application/json" \
  --data '{"amount": 100, "commandId": "12345"}' 
```  
```shell
curl http://localhost:9000/wallet/1/deposit \
  -i -X PATCH \
  --header "Content-Type: application/json" \
  --data '{"amount": 100, "commandId": "12345"}' 
```

Get wallet

```shell
curl http://localhost:9000/wallet/1
```

Create cinema show

```shell
curl http://localhost:9000/cinema-show/show1 \
  -i -X POST \
  --header "Content-Type: application/json" \
  --data '{"title": "Pulp Fiction", "maxSeats": 10}'
```

Get cinema show

```shell
curl http://localhost:9000/cinema-show/show1
```

Make reservation

```shell
curl http://localhost:9000/cinema-show/show1/reserve \
  -i -X PATCH \
  --header "Content-Type: application/json" \
  --data '{"walletId": "1", "expenseId": "123", "seatNumber": 3}'
```

Verify wallet balance

```shell
curl http://localhost:9000/wallet/1
```

Verify seat status

```shell
curl http://localhost:9000/cinema-show/show1/seat-status/3
```

## Run integration tests

To run the integration tests located in `src/it/java`:

```shell
mvn integration-test
```

## Troubleshooting

If you encounter issues, ensure that:

- The Akka service is running and accessible on port 9000.
- Your `curl` commands are formatted correctly.

## Need help?

For questions or assistance, please refer to our [online support resources](https://doc.akka.io/support/index.html).

## Deploying

You can use the [Akka Console](https://console.akka.io) to create a project and see the status of your service.

Build container image:

```shell
mvn clean install -DskipTests
```

Install the `akka` CLI as documented in [Install Akka CLI](https://doc.akka.io/reference/cli/index.html).

Deploy the service using the image tag from above `mvn install`:

```shell
akka service deploy order-saga akka-order-saga:tag-name --push
```

Refer to [Deploy and manage services](https://doc.akka.io/operations/services/deploy-service.html)
for more information.