# ISP Mini Project (Java)

Netwise is a small Internet Service Provider portal with an HTML/CSS/JavaScript frontend and a Java backend. The backend serves the frontend and HTTP API, and persists portal state in PostgreSQL using JDBC.

## Requirements

- JDK 17 or later (your installed JDK 25 is suitable)
- Apache Maven 3.9 or later
- PostgreSQL running locally, with an `isp_project` database created in pgAdmin

## Run the application

Open PowerShell in the project root. Set the password you chose for PostgreSQL in the current PowerShell session (do not put it in source files):

```powershell
$securePassword = Read-Host "PostgreSQL password for user postgres" -AsSecureString
$env:ISP_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
mvn compile exec:java
```

Then open [http://127.0.0.1:8080](http://127.0.0.1:8080). Stop the server with Ctrl+C. The password remains in the current PowerShell process environment and is not written to the project.

Run from the project root so Maven and the Java server can find the project files. The JDBC driver is downloaded by Maven on the first build. Defaults are `jdbc:postgresql://localhost:5432/isp_project` and user `postgres`; override them with `ISP_DB_URL` and `ISP_DB_USER`. The server creates `isp_app_state` automatically. Existing demo records are inserted only when the database has no saved application state.

For repeat sessions, set the password again in each new PowerShell window, or configure a user-level environment variable through Windows Settings. Never commit credentials.

## Postman

Import `postman/Netwise ISP.postman_collection.json` in Postman. Start the backend first, then send **Health** and **Get full portal state**. The collection includes sample customer, usage, subscription, complaint, and simulated-payment calls. Postman talks to the Java HTTP API; only the Java app connects to PostgreSQL. After a write request, call **Get full portal state**, then restart the backend and call it again to confirm saved state reloads.

The current JDBC learning implementation stores the portal's complete JSON state in one PostgreSQL `JSONB` row (`isp_app_state`). This keeps the existing in-memory domain model and API intact while demonstrating JDBC connection management, prepared statements, PostgreSQL persistence, and restart recovery. It is a simple persistence design, not a normalized relational schema; a later database-design phase can split customers, plans, subscriptions, invoices, payments, usage, and complaints into related tables.

## Features

- Customer directory: add, search, edit, and remove customer records; removal clears active service records while issued invoices remain as a billing archive.
- Plan catalog: add, edit, and remove Basic, Home Plus, Premium, and custom plans; customers can subscribe or switch plans.
- Usage: track data usage against the active plan, show monthly history, and validate usage records.
- Billing: preserve uniquely numbered invoices with a plan/price/tax/period snapshot, calculate overdue status, record simulated payments in payment history, and export statements and receipts.
- One invoice is issued per customer per billing period. The normal due date is the 25th; an invoice first issued after that date is due on the 25th of the following month. Unpaid invoices become overdue after the due date.
- Plan changes on an active subscription are scheduled for the first day of the next billing period. Existing invoices are never recalculated, and no partial-month charge is applied. A first-time subscription is billed at the full monthly price for its current billing period.
- Support: create tickets, search them, view details, and update ticket status from the admin view.
- Admin report: generate and download a report in a Java thread pool, archive it as a UTF-8 text file, show progress, and cancel the task.
- Demo customer/admin views, responsive layout, and browser-only portal appearance preferences.

## API overview

The UI calls these endpoints on the Java server:

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/health` | Check that the API server is running |
| `GET` | `/api/state` | Load the shared customer, plan, subscription, usage, invoice, and complaint state |
| `GET` | `/api/customers/{id}` | Find one customer by numeric ID |
| `GET` | `/api/customers?email={email}` | Find one customer by email; demonstrates overloaded service lookup |
| `POST` | `/api/customers` | Add a customer |
| `PUT` | `/api/customers/{id}` | Edit a customer |
| `DELETE` | `/api/customers/{id}` | Remove customer and associated records |
| `POST` | `/api/plans` | Add an internet plan |
| `PUT` | `/api/plans/{id}` | Edit a plan |
| `DELETE` | `/api/plans/{id}` | Remove a plan that has no active subscribers |
| `POST` | `/api/subscriptions` | Subscribe or switch a plan |
| `POST` | `/api/subscriptions/cancel-change` | Cancel a scheduled plan change |
| `POST` | `/api/customers/{id}/usage` | Add data usage |
| `POST` | `/api/complaints` | Create a support ticket |
| `PATCH` | `/api/complaints/{id}` | Update ticket status |
| `POST` | `/api/bills/{id}/pay` | Record a simulated full payment and payment-history entry |
| `POST` | `/api/reports` | Start an asynchronous admin report |
| `GET` | `/api/reports/{id}` | Read or wait for report progress/result |
| `GET` | `/api/reports/{id}/file` | Read/download the archived report text file |
| `POST` | `/api/reports/{id}/cancel` | Interrupt a running report |

## Syllabus mapping

For a detailed concept-by-concept map, presentation wording, and honest coverage notes, see [JAVA_CONCEPTS_FOR_PRESENTATION.md](JAVA_CONCEPTS_FOR_PRESENTATION.md).

- **OOP:** encapsulated `Customer`, `Subscription`, `Complaint`, `Invoice`, and plan classes, with associations between customers and their records.
- **Inheritance and polymorphism:** abstract `InternetPlan` with single, multilevel, and hierarchical plan inheritance; `HomePlan` also implements two capability interfaces to demonstrate Java's interface-based multiple inheritance and a hybrid class/interface design.
- **Collections:** arrays for allowed status values, `List` for customers and records, `Set` for unique complaint types, `Map` for ID lookups and usage, and `Vector` for the plan catalog.
- **Strings and regex:** validated email/phone patterns, status/search strings, `StringBuilder` report generation, and `StringBuffer` invoice summaries.
- **Exceptions:** checked domain exceptions for missing customers/plans and invalid customer, usage, or complaint data; unchecked validation errors are translated to HTTP error responses.
- **Multithreading:** fixed thread pool for reports, synchronized service and job state, `wait`/`notifyAll` for report progress communication, and interruption when a report is cancelled.
- **Java I/O and file handling:** `IspHttpServer.handleStatic()` demonstrates byte-stream input and output with `java.io.InputStream` and `java.io.OutputStream`: it opens frontend files with `Files.newInputStream()` and copies their bytes to the HTTP response using `InputStream.transferTo()`. `ReportFileStore.writeReport()` and `readReport()` demonstrate UTF-8 character/text file output and input with `Files.writeString()` and `Files.readString()`; generated reports are stored under `backend/exports/`. PostgreSQL state uses JDBC.

### Demonstrate the Java I/O stream classes

From the project root, run:

```powershell
mvn -q compile exec:java "-Dexec.mainClass=isp.StreamDemo"
```

The demo writes a temporary binary file with `FileOutputStream`, buffered and data output streams, reads it back with `FileInputStream`, buffered and data input streams, and formats the verified values through `PrintStream`. It deletes the temporary file when finished. A successful run prints the three decoded values followed by `PASS`. `InputStream` and `OutputStream` are also used by the running web server when it serves frontend files.

## Team areas

- Frontend and API integration: `frontend/`
- Customer, plan, and subscription domain: `Customer.java`, `InternetPlan.java`, `BasicPlan.java`, `HomePlan.java`, `PremiumPlan.java`, `Subscription.java`, `IspService.java`
- Usage and billing domain: `Usage.java`, `Bill.java`, `Invoice.java`, `PaymentRecord.java`
- Support, exceptions, and concurrency: `Complaint.java`, `ComplaintManager.java`, custom exception classes, `ReportService.java`
- HTTP integration and startup: `IspHttpServer.java`, `Json.java`, `Main.java`
