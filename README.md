# ISP Mini Project (Java)

Netwise is a small Internet Service Provider portal with an HTML/CSS/JavaScript frontend and a Java backend. The Java application serves both the frontend and a same-origin HTTP API, so customer, subscription, usage, invoice, and complaint changes all go through the same in-memory backend state.

## Requirements

- JDK 17 or later
- No external Java libraries

## Run the application

Open PowerShell in the project root:

```powershell
New-Item -ItemType Directory -Force backend/out | Out-Null
$javaFiles = Get-ChildItem backend/src/isp -Filter *.java
javac -d backend/out $javaFiles.FullName
java -cp backend/out isp.Main
```

Then open [http://127.0.0.1:8080](http://127.0.0.1:8080). Stop the server with Ctrl+C. To use another host or port, pass them as arguments, for example `java -cp backend/out isp.Main 127.0.0.1 8090`.

Run from the project root so the Java server can find the `frontend/` folder. The project data is held in memory and resets when the server stops.

## Features

- Customer directory: add, search, edit, and remove customer records; removal also clears that customer's subscriptions, usage, invoices, and tickets.
- Plan catalog: add, edit, and remove Basic, Home Plus, Premium, and custom plans; customers can subscribe or switch plans.
- Usage: track data usage against the active plan, show monthly history, and validate usage records.
- Billing: generate an invoice when a customer subscribes or switches plans, view balances, mark demo invoices as paid, and export CSV statements and receipts.
- Support: create tickets, search them, view details, and update ticket status from the admin view.
- Admin report: generate and download a report in a Java thread pool, show progress, and cancel the task.
- Demo customer/admin views, responsive layout, and browser-only portal appearance preferences.

## API overview

The UI calls these endpoints on the Java server:

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/state` | Load the shared customer, plan, subscription, usage, invoice, and complaint state |
| `POST` | `/api/customers` | Add a customer |
| `PUT` | `/api/customers/{id}` | Edit a customer |
| `DELETE` | `/api/customers/{id}` | Remove customer and associated records |
| `POST` | `/api/plans` | Add an internet plan |
| `PUT` | `/api/plans/{id}` | Edit a plan |
| `DELETE` | `/api/plans/{id}` | Remove a plan that has no active subscribers |
| `POST` | `/api/subscriptions` | Subscribe or switch a plan |
| `POST` | `/api/customers/{id}/usage` | Add data usage |
| `POST` | `/api/complaints` | Create a support ticket |
| `PATCH` | `/api/complaints/{id}` | Update ticket status |
| `POST` | `/api/bills/{id}/pay` | Mark a demo invoice paid |
| `POST` | `/api/reports` | Start an asynchronous admin report |
| `GET` | `/api/reports/{id}` | Read or wait for report progress/result |
| `POST` | `/api/reports/{id}/cancel` | Interrupt a running report |

## Syllabus mapping

- **OOP:** encapsulated `Customer`, `Subscription`, `Complaint`, `Invoice`, and plan classes, with associations between customers and their records.
- **Inheritance and polymorphism:** abstract `InternetPlan` with `BasicPlan`, `HomePlan`, and `PremiumPlan`; overridden plan behavior; `super`, an interface (`PlanDetails`), and final identifiers/constants.
- **Collections:** arrays for allowed status values, `List` for customers and records, `Set` for unique complaint types, `Map` for ID lookups and usage, and `Vector` for the plan catalog.
- **Strings and regex:** validated email/phone patterns, status/search strings, `StringBuilder` report generation, and `StringBuffer` invoice summaries.
- **Exceptions:** checked domain exceptions for missing customers/plans and invalid customer, usage, or complaint data; unchecked validation errors are translated to HTTP error responses.
- **Multithreading:** fixed thread pool for reports, synchronized service and job state, `wait`/`notifyAll` for report progress communication, and interruption when a report is cancelled.

## Team areas

- Frontend and API integration: `frontend/`
- Customer, plan, and subscription domain: `Customer.java`, `InternetPlan.java`, `BasicPlan.java`, `HomePlan.java`, `PremiumPlan.java`, `Subscription.java`, `IspService.java`
- Usage and billing domain: `Usage.java`, `Bill.java`, `Invoice.java`
- Support, exceptions, and concurrency: `Complaint.java`, `ComplaintManager.java`, custom exception classes, `ReportService.java`
- HTTP integration and startup: `IspHttpServer.java`, `Json.java`, `Main.java`
