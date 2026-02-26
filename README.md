# Executor API (Kubernetes Script Runner)

A small Spring Boot Kotlin service that accepts a shell script via HTTP, creates a separate Kubernetes Job to run it, and stores execution state in **PostgreSQL**. A background reconciler/poller updates unfinished executions by querying Kubernetes and when script is finished collects the outcome (Script outcome: Failed, Succeeded, Error).

The application is configured to run against a local Kubernetes cluster by default, but can be connected to any hosted cluster by adjusting Kubernetes configuration (kubeconfig or in-cluster settings).

## Project structure
```text
k8s/
└── all.yaml

src/
└── main/
    ├── kotlin/
    │   ├── controllers/
    │   │   └── ScriptExecutionController.kt # REST API endpoints
    │   ├── dto/                             # DTOs
    │   │   ├── CreateExecutionDTO.kt
    │   │   ├── ExecutionStatusDTO.kt
    │   │   ├── JobState.kt
    │   │   └── ScriptDTO.kt
    │   ├── entities/
    │   │   └── ExecutionEntity.kt            # DB entity
    │   ├── repositories/
    │   │   └── ExecutionRepository.kt        # DB repository
    │   ├── services/
    │   │   ├── ExecutionDBService.kt         # Service to interact with DB
    │   │   ├── ExecutorService.kt            # Service that combined both DB and Kubernetes to execute scripts
    │   │   └── KubernetesExecutorService.kt  # Service to interact with Kubernetes
    │   └── Main.kt
    │
    └── resources/
        ├── db/
        └── migration/
           └── V1__create_executions.sql
application.yml
Dockerfile
docker-compose.yml
build.gradle.kts
settings.gradle.kts
```


## What it does

- Has **POST** endpoint to execute request (script + optional resource requests)
- Persist an execution record in DB (status starts as `QUEUED`)
- Create a Kubernetes **Job** that runs the script
- A background reconciler:
    - loads unfinished executions from DB
    - checks Kubernetes job status and updates DB if the status is different from the one in DB
    - when finished: stores outcome (`SUCCEEDED`, `FAILED`, or ERROR) and marks execution as `FINISHED`
- Has **GET** endpoint to get execution status by request id of a script execution.
## Requirements

- Docker
- kind
- kubectl

> The Kubernetes manifests in `k8s/` create the namespace/resources and deploy the API + Postgres (depending on how you authored your YAML).

## Run locally on kind

These are the commands to run app locally in kind (docker should be running):

```bash
kind create cluster --name executor-cluster 

docker build -t executor-api:local .

kind load docker-image executor-api:local --name executor-cluster

kubectl apply -f k8s/

kubectl port-forward -n executor svc/executor-api 8080:8080
```
They create a cluster, build the image, load it into the cluster, apply configs and then expose the API on port 8080.

The API will be available at `http://localhost:8080/executions`.
## API 
### POST /executions
#### Content-Type: application/json.
#### Example Request (Successful Script)
```json
{
  "script": "echo Hello && sleep 1 && echo Done",
  "resources": {
    "cpu": "250m",
    "memory": "128Mi"
  }
}
```
This will return something like:
```json
{
  "id": "1234567890",
  "status": "QUEUED"
}
```
#### Example Request (Failed Script)
```json
{
  "script": "echo About to fail; exit 42",
  "resources": {
    "cpu": "100m",
    "memory": "64Mi"
  }
}
```
#### Invalid Command Example
```json
{
  "script": "non_existing_command",
  "resources": {
    "cpu": "250m",
    "memory": "128Mi"
  }
}
```
Example response:
```json
{"id":"285ac357-7737-4dea-bf7d-4fea1537c343","status":"FINISHED","createdAt":"2026-02-26T13:38:17.356446Z","outcome":"FAILED"}   
```

### GET /executions{id}
User can get status of a script execution by providing the id from the previous request response.

#### Example of successful response
```json
{
  "id": "b3f86f10-6b4f-4c6e-9bd0-4ff0c0a7f1aa",
  "status": "FINISHED",
  "outcome": "SUCCEEDED",
  "createdAt": "2026-02-26T01:23:45Z"
}
```

