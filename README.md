# Orchestrator–Worker PoC (Spring Boot + Kotlin + AWS SQS)

A small proof‑of‑concept that demonstrates an orchestrator–worker pattern using Spring Boot (Kotlin) and AWS SQS (via Spring Cloud AWS). The orchestrator exposes a REST endpoint to start a "job", splits it into multiple worker tasks, and publishes those tasks to a worker queue. A separate worker application instance consumes and processes the tasks. For local development, ElasticMQ is used as an in‑memory SQS emulator via Docker.


## Key Features
- Spring Boot 3 (Kotlin) application split into two runtime modes:
  - Orchestrator: accepts API requests, creates and dispatches tasks to a worker queue
  - Worker: consumes tasks from the worker queue and processes them
- AWS SQS integration using Spring Cloud AWS (3.x)
- Local SQS emulator with ElasticMQ (docker‑compose)
- Clear separation via Spring profiles and a simple CLI switch (`--mode`)
- Integration tests with Testcontainers


## Project Structure
- src/main/kotlin/…/controller/CalculationController.kt
  - REST endpoint to start a calculation job (orchestrator mode only)
- src/main/kotlin/…/service/JobOrchestrator.kt
  - Listens on the control queue, splits jobs into multiple worker tasks, and publishes them to the worker queue
- src/main/kotlin/…/service/JobWorker.kt
  - Listens on the worker queue and processes tasks
- src/main/kotlin/…/service/WorkerJobPayload.kt
  - DTO for tasks sent to the worker queue
- src/main/kotlin/…/util/cli/CommandLineParser.kt
  - Parses `--mode` CLI arg: `orchestrator` or `worker`
- src/main/resources/application.yml
  - Profiles and local SQS configuration (ports, queue names, etc.)
- docker-compose.yml and docker/elasticmq/elasticmq.conf
  - Local ElasticMQ configuration


## Requirements
- Java 21 (project uses Gradle toolchain to ensure this)
- Docker Desktop (for ElasticMQ)
- Internet access to fetch dependencies from Maven Central

Optional:
- cURL or any REST client (e.g., Postman, HTTPie) to invoke the API


## Getting Started (Local Development)

1) Start the local SQS emulator (ElasticMQ)
- From the project root:
  - Linux/macOS: `docker compose up -d`
  - Windows (PowerShell): `docker compose up -d`
- ElasticMQ will be available at:
  - SQS endpoint: http://localhost:9324
  - Management UI: http://localhost:9325

2) Run the Orchestrator
- The application requires a run mode via `--mode` and uses Spring profiles for environment setup.
- In a first terminal, start the orchestrator with the `local` and `orchestrator` profiles:
  - Linux/macOS: `./gradlew bootRun --args="--mode orchestrator --spring.profiles.active=local"`
  - Windows (PowerShell): `./gradlew.bat bootRun --args="--mode orchestrator --spring.profiles.active=local"`
- Orchestrator default port (actuator/health etc. per profile): 9945

3) Run the Worker
- In a second terminal, start the worker with the `local` and `worker` profiles:
  - Linux/macOS: `./gradlew bootRun --args="--mode worker --spring.profiles.active=local"`
  - Windows (PowerShell): `./gradlew.bat bootRun --args="--mode worker --spring.profiles.active=local"`
- Worker default port (actuator/health etc. per profile): 9946

4) Trigger a Job
- There's a ready-to-run HTTP request file included in the repository that contains a POST request to start a calculation job:

  `src/test/resources/http/rest-api.http`

  - Open this file in your IDE (JetBrains HTTP Client or VS Code REST Client) and run the `POST http://localhost:9945/api/calculate/start` request. The file contains a sample JSON body (e.g. `["one","two","three"]`) and sends `Content-Type: application/json`.

  Quick run in editor (JetBrains / VS Code)
  - JetBrains (IntelliJ, GoLand, etc.): open the `rest-api.http` file, place the caret on the POST request block and click the green "Run" icon in the gutter (or use the contextual Run action above the request) to execute the request and inspect the response in the HTTP client tool window.
  - VS Code (REST Client extension): open the file, and click the `Send Request` link that appears above the request block to run it; the response will open in a new editor tab.

- Optional: if you prefer curl from the command line (not required if you use the included request file), you can run:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"inputs":["one","two","three"]}' \
  http://localhost:9945/api/calculate/start
```

- Response:
  ```json
  { "jobId": "<uuid>" }
  ```
- What happens under the hood:
  - Orchestrator publishes a `START_JOB` message with the generated job ID to the control queue
  - `JobOrchestrator` splits the job into multiple worker tasks (currently 4) and publishes them to the worker queue
  - `JobWorker` consumes the tasks and logs the processing


## Configuration
- Main config: `src/main/resources/application.yml`
- Important properties (local profile):
  - spring.cloud.aws.sqs.endpoint: `http://localhost:9324`
  - app.queues.control-queue: `job-control-queue`
  - app.queues.worker-queue: `job-worker-queue`
  - Server ports by profile:
    - default local: 9944
    - orchestrator: 9945
    - worker: 9946

Environment/Profiles
- The application uses Spring profiles to separate local and role-specific settings:
  - `local` activates local ElasticMQ and default ports
  - `orchestrator` and `worker` are added programmatically based on `--mode`
- Always combine `--mode` with a Spring profile when running locally, e.g. `--spring.profiles.active=local`


## Build, Test, and Package
- Run tests:
  - Linux/macOS: `./gradlew test`
  - Windows: `./gradlew.bat test`
- Build a jar:
  - Linux/macOS: `./gradlew bootJar`
  - Windows: `./gradlew.bat bootJar`
- Run from the built jar (after `bootJar`):
  - Orchestrator:
    - `java -jar build/libs/orchestrator-worker-poc-0.0.1-SNAPSHOT.jar --mode orchestrator --spring.profiles.active=local`
  - Worker:
    - `java -jar build/libs/orchestrator-worker-poc-0.0.1-SNAPSHOT.jar --mode worker --spring.profiles.active=local`


## Queues and Messaging
- Control queue: receives `START_JOB` and `ERROR_RETRY` messages for orchestration
- Worker queue: receives `WORKER_TASK` (and potential `WORKER_TASK_RETRY`) messages for processing
- Message headers used:
  - `job-id` (required)
  - `message-type` (e.g., START_JOB, WORKER_TASK, WORKER_TASK_RETRY, ERROR_RETRY)
  - `task-id` (for worker tasks)


## Troubleshooting
- ElasticMQ not reachable
  - Ensure Docker is running and `docker compose up -d` succeeded
  - Verify ports 9324/9325 are free and accessible
- Orchestrator or worker fails to start due to CLI parsing
  - The `--mode` argument is required and must be `orchestrator` or `worker`
- No messages consumed by worker
  - Confirm both instances run with `--spring.profiles.active=local`
  - Check logs for `SqsListener` bindings and queue names
- Port conflicts
  - Adjust ports in `application.yml` or use different profiles


## Technology Stack
- Kotlin 1.9.x, Spring Boot 3.5.x
- Spring Cloud AWS SQS 3.3.x
- Gradle 8 (via wrapper)
- Testcontainers, JUnit 5
- ElasticMQ for local SQS


## Contributing
This is a PoC and not intended for production. Feel free to open issues or pull requests if you want to extend it.


## License
No explicit license provided. If you plan to publish this repository publicly (e.g., GitHub), consider adding a LICENSE file (e.g., MIT, Apache 2.0) to clarify usage terms.

---
Last updated: 2025-09-21


## Running multiple instances in IntelliJ (one orchestrator + two workers)

If you want to run one Orchestrator and two Worker instances from IntelliJ (for local manual testing), create three Spring Boot run configurations that use the same main class but different program arguments and ports. The project already contains a Spring Boot run configuration named `OrchestratorWorkerPocApplication` in `.idea/workspace.xml` that you can duplicate — the main class is `de.laboranowitsch.poc.orchestratorworkerpoc.OrchestratorWorkerPocApplication`.

Step-by-step (create three Spring Boot run configurations)
- Open Run | Edit Configurations... in IntelliJ.
- Click the + button and choose **Spring Boot** (or duplicate the existing `OrchestratorWorkerPocApplication`).

Create the Orchestrator config
- Name: `Orchestrator`
- Main class: `de.laboranowitsch.poc.orchestratorworkerpoc.OrchestratorWorkerPocApplication`
- Module: select the project's main module (e.g. `orchestrator-worker-poc.main`)
- Program arguments:
  --mode orchestrator --spring.profiles.active=local --server.port=9945
- (Optional) VM options: leave empty
- Save the configuration.

Create two Worker configs
- Duplicate the Orchestrator config or add two new Spring Boot configs and adjust:
  - Name: `Worker-1`
    Program arguments: --mode worker --spring.profiles.active=local --server.port=9946
  - Name: `Worker-2`
    Program arguments: --mode worker --spring.profiles.active=local --server.port=9947
- These different ports avoid port conflicts when running multiple workers on the same machine.

Allow multiple instances
- In the Run/Debug Configurations dialog, for each of the above configurations make sure the configuration allows running multiple instances:
  - Find the option labeled like **Single instance only** and uncheck it (or enable **Allow parallel run** depending on your IntelliJ version). This lets you run more than one copy of the same Spring Boot app.

Run them
- Run `Orchestrator` first, then `Worker-1` and `Worker-2` (order doesn't strictly matter, but starting the orchestrator first makes it easier to observe job dispatch).
- Use the Run tool window to inspect logs for each instance. The orchestrator will bind to port 9945 and each worker to its configured port.

Tips and alternatives
- If you prefer `bootRun` via Gradle, create Gradle run configurations and pass `--args='--mode worker --spring.profiles.active=local --server.port=9946'` (Gradle run configs accept extra arguments). The Spring Boot run configuration is usually more convenient for debugging.
- If you accidentally get port conflicts, choose different `--server.port` values for the second worker.
- The project's `.idea/workspace.xml` contains a `OrchestratorWorkerPocApplication` run configuration; you can duplicate/modify it instead of creating a new one from scratch.

Security note
- These run configurations use local/test credentials and the `local` profile. Do not store production AWS credentials in the project or run these local configs in a production environment.
