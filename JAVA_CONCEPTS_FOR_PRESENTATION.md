# Java Concepts in the Netwise ISP Project

This guide maps the requested syllabus topics to the current project code and gives a short explanation you can use in a presentation. The status labels describe what the code actually demonstrates:

- **Implemented**: there is a clear working example in the project.
- **Partly implemented**: an example exists, but it does not cover every interpretation of the topic.

## Quick status

| Topic | Status | Main example |
|---|---|---|
| Classes and objects | Implemented | `Customer`, `InternetPlan`, `Invoice`, and objects created by `IspService` |
| `static` and `private` fields | Implemented | `IspService`, `Customer`, `Invoice`, `Subscription` |
| Inheritance | Implemented | Plan classes inherit common behavior from `InternetPlan` |
| Types of inheritance | Implemented with Java's interface limitation | Single, multilevel, hierarchical, interface-based multiple, and hybrid forms |
| Method overloading and overriding | Implemented | `IspService.findCustomer(int)` and `findCustomer(String)` overload; plan methods override |
| Abstract methods | Implemented | `InternetPlan.displayPlan()` and `getTier()` |
| Interfaces | Implemented | `PlanDetails`, `Billable`, `AutoCloseable` |
| Java collections | Implemented | `List`, `ArrayList`, `Map`, `Set`, `HashSet`, `Vector`, `ConcurrentHashMap` |
| Strings and string functions | Implemented | Validation, search/status comparisons, `StringBuilder`, and `StringBuffer` |
| Exception handling | Implemented | Checked domain exceptions, unchecked validation exceptions, `try`/`catch`/`throws`, and JDBC try-with-resources |
| Multithreading | Implemented | Report thread pool, synchronized report jobs, waiting/notification, interruption |
| File handling | Implemented | Java writes and reads generated report text files; HTTP route serves the archived report |
| JDBC | Implemented | `JdbcStore` connects to PostgreSQL and stores/restores app state |
| REST API | Implemented | `IspHttpServer` exposes HTTP routes used by the frontend and Postman |
| `final` and `super` | Implemented | Immutable identifiers/constants and superclass constructor calls |

---

## 1. Classes and objects — Implemented

**Where:** `Customer.java`, `InternetPlan.java`, `Subscription.java`, `Invoice.java`, `Complaint.java`, `IspService.java`.

**How it appears:** A class defines the data and behavior of one concept. For example, `Customer` stores a customer's ID and profile. `IspService.seed()` and methods such as `addCustomer()` create actual `Customer` objects, and subscriptions refer to customer and plan objects.

**Why it is useful:** It keeps related data and operations together. The billing code can work with an `Invoice` object instead of passing unrelated invoice fields around.

**Presentation line:** “The project models ISP entities as Java classes. At runtime, the service creates objects of those classes and connects them—for example, a subscription has a customer and an internet plan.”

## 2. `static` and `private` fields — Implemented

**Where:** `IspService.java` has `private static final` tax and validation-pattern fields; `Customer.java` has private profile fields; `Invoice.java` has private invoice fields and a static month formatter; `Subscription.java` has a static ID generator.

**How it appears:** `private` fields hide internal state so other classes use public methods. `static` values belong to the class rather than an individual object. For example, the tax rate and email-validation pattern are shared by the service instead of copied into every customer.

**Why it is useful:** Encapsulation protects data from uncontrolled changes. Static constants and helpers provide one shared definition for class-wide rules.

**Presentation line:** “Customer details are private and changed through methods such as `updateProfile()`. Shared rules such as the tax rate are static because they apply to all service operations.”

## 3. Inheritance — Implemented

**Where:** `InternetPlan.java` is the abstract parent. `BasicPlan.java`, `HomePlan.java`, `PremiumPlan.java`, and `CustomPlan.java` extend it.

**How it appears:** Each plan subclass inherits common fields such as plan name, price, speed, and data allowance. Each subclass can provide its own plan behavior.

**Why it is useful:** Common plan data and validation live in one parent class, while each plan type can specialize display or tier behavior.

**Presentation line:** “All internet plans share the `InternetPlan` base class. The concrete plan subclasses inherit the common plan properties and override behavior where needed.”

## 4. Types of inheritance — Partly implemented

**Where:** `BasicPlan`, `PremiumPlan`, and `CustomPlan` extend `InternetPlan` directly. `ResidentialPlan` also extends `InternetPlan`, and `HomePlan` extends `ResidentialPlan`. `HomePlan` implements both `StreamingPlan` and `PrioritySupportPlan`.

**Forms demonstrated:**

- **Single:** `BasicPlan extends InternetPlan`.
- **Multilevel:** `HomePlan extends ResidentialPlan extends InternetPlan`.
- **Hierarchical:** several plan classes share `InternetPlan` as a parent.
- **Multiple through interfaces:** `HomePlan` implements two interfaces and gets both capability contracts/default methods.
- **Hybrid:** the hierarchy combines a multilevel class chain with multiple interface implementation.

**Java limitation:** Java does not allow a class to extend two classes. The multiple-inheritance example is through interfaces, which is the Java-supported approach; it does not inherit state from multiple classes.

**Presentation line:** “I demonstrate single, multilevel, and hierarchical class inheritance in the plan model. Java does not support multiple class inheritance, so the multiple and hybrid examples combine the class hierarchy with two interfaces implemented by `HomePlan`.”

## 5. Method overloading and overriding — Partly implemented

**Overloading evidence:** `IspService` declares `findCustomer(int customerId)` and `findCustomer(String email)`. They have the same method name but different parameter types. `IspHttpServer` uses the ID overload for `GET /api/customers/{id}` and the email overload for `GET /api/customers?email=...`. Constructors in `Customer`, `Bill`, the plan classes, and `Subscription` are overloaded too.

**Overriding evidence:** Plan subclasses override `displayPlan()` and `getTier()`. `NotificationThread` overrides `Thread.run()`. These use the same method signature as the inherited method but provide subclass behavior.

**Presentation line:** “`findCustomer` is overloaded: one version accepts an integer account ID and another accepts an email string. The HTTP layer chooses the overload based on the lookup route. Plan-specific methods such as `displayPlan()` demonstrate overriding.”

## 6. Abstract methods — Implemented

**Where:** `InternetPlan.java` declares `displayPlan()` and `getTier()` as abstract. Concrete plan subclasses implement them.

**Why it is useful:** The abstract parent guarantees that every plan type supplies the behavior, while allowing each type to implement it in its own way.

**Presentation line:** “`InternetPlan` defines the operations every plan must provide but leaves their implementation to the concrete plan classes.”

## 7. Interfaces — Implemented

**Where:** `PlanDetails.java` is implemented by `InternetPlan`; `Billable.java` is implemented by `Bill.java`; `ReportService.java` implements Java's `AutoCloseable` interface.

**Why it is useful:** An interface specifies operations that a class promises to provide without requiring unrelated classes to share a parent class.

**Presentation line:** “`Billable` describes billing operations. `Bill` implements that contract, so callers can rely on the billing methods without depending on how the class calculates them.”

## 8. Java collections — Implemented

**Where:** `IspService.java` uses `List`/`ArrayList` for customers, subscriptions, and invoices; `Map`/`LinkedHashMap` for ID indexes and usage; and `Vector` for plans. `ComplaintManager.java` uses an `ArrayList` for complaints and a `Set`/`HashSet` for unique complaint types. `ReportService.java` uses a `ConcurrentHashMap` for report jobs.

**Why it is useful:** Collections store changing groups of domain objects and provide operations for lookup, iteration, and uniqueness. The concurrent map is appropriate for report jobs accessed by request and worker threads.

**Presentation line:** “I selected collection types based on their role: lists preserve records, maps support lookup by ID, sets prevent duplicate complaint types, and a concurrent map stores background report jobs.”

## 9. Strings and string functions — Implemented

**Where:** `IspService.java` and `Customer.java` trim and validate names, emails, and phone numbers; `ComplaintManager.java` compares statuses without case sensitivity; `Invoice.java` uses `StringBuffer` to build an invoice summary; `IspService.buildReport()` and `Json.java` use `StringBuilder` to build report and JSON text. Regular expressions validate email, phone, and API path formats.

**Examples of string operations:** `trim()`, `isBlank()`, `equalsIgnoreCase()`, `substring()`, `String.format()`, and `append()`.

**Why it is useful:** These operations normalize user input, validate fields, compare values consistently, and efficiently assemble generated text.

**Presentation line:** “String processing is used for input cleanup and validation. I use regex patterns for email and phone formats, `StringBuilder` for report/JSON construction, and `StringBuffer` for the invoice summary example.”

## 10. Exception handling — Implemented

**Where:** Checked domain exceptions include `InvalidCustomerException`, `InvalidPlanException`, `InvalidUsageException`, `InvalidComplaintException`, `CustomerNotFoundException`, and `PlanNotFoundException`. Service methods declare exceptions with `throws`; the HTTP layer catches them and returns JSON errors. Unchecked exceptions such as `IllegalArgumentException` handle invalid request values. `JdbcStore` uses try-with-resources around JDBC connections and statements.

**Why it is useful:** Errors such as invalid input, missing records, and database failures can be handled explicitly and turned into understandable API responses. Try-with-resources also closes database resources even when an operation fails.

**Presentation line:** “The project separates expected domain errors into checked exceptions, uses unchecked exceptions for invalid arguments or state, and translates request errors into HTTP responses.”

## 11. Multithreading — Implemented

**Where:** `ReportService.java` runs reports in a fixed thread pool. Its `ReportJob` synchronizes shared status, uses `wait()` and `notifyAll()` for progress readers, and interrupts cancelled jobs. `IspHttpServer.java` also handles HTTP requests using an executor. `NotificationThread.java` demonstrates extending `Thread` and handling interruption.

**Why it is useful:** A report can run in the background while the API continues to handle requests. Synchronization protects shared report status, and interruption lets cancellation stop work cleanly.

**Presentation line:** “The admin report is asynchronous: a worker thread generates it, clients can poll for progress, and cancellation interrupts the worker. The shared job state is synchronized.”

## 12. File handling — Partly implemented

**Where:** `IspHttpServer.handleStatic()` uses Java NIO `Path` and `Files.readAllBytes()` to serve frontend files. `ReportFileStore.writeReport()` creates `backend/exports/` and writes generated report text with `Files.writeString()`; `readReport()` reads it back with `Files.readString()`. `ReportService` archives each completed background report, and `GET /api/reports/{id}/file` returns the saved report. In `frontend/app.js`, browser `Blob` downloads are also used for statements and receipts.

**Why it is useful:** Java file I/O leaves a server-side copy of completed reports that can be read later through the API, in addition to the browser's user download.

**Presentation line:** “The report worker writes a UTF-8 text report to the backend exports folder using `Files.writeString`. The report-file endpoint reads it with `Files.readString` and returns the archived text.”

## 13. JDBC — Implemented

**Where:** `JdbcStore.java` connects through `DriverManager`, creates `isp_app_state`, uses `PreparedStatement` to load/save, and closes resources with try-with-resources. `Main.java` checks the connection and constructs `IspService` with the JDBC store.

**Database design detail:** The current project saves the whole portal state as one PostgreSQL `JSONB` document in `isp_app_state.payload`. JDBC connectivity and persistence work, but this is **not a normalized set of SQL tables** for customers, plans, invoices, and other records.

**Why it is useful:** JDBC gives the Java application a standard API to connect to PostgreSQL and run SQL. Persisting the state means records survive application restarts.

**Presentation line:** “The Java backend uses JDBC to connect to PostgreSQL with credentials from environment variables. It persists and reloads the portal state using a prepared statement. The current storage format is one JSONB state document.”

## 14. REST API — Implemented

**Where:** `IspHttpServer.java` starts Java's built-in `HttpServer`, routes `/api` requests, parses JSON, and returns JSON responses. It supports routes including `GET /api/health`, `GET /api/state`, `POST /api/customers`, `POST /api/subscriptions`, `POST /api/complaints`, and `POST /api/bills/{id}/pay`. The frontend calls the API using `fetch()` in `frontend/app.js`; Postman can call the same endpoints.

**Why it is useful:** The API separates the user interface from business logic and provides a consistent way for the browser or Postman to read and change data.

**Presentation line:** “The frontend and Postman communicate with the Java backend over HTTP. The backend validates requests, calls the service layer, persists changes, and returns JSON with appropriate HTTP status codes.”

## 15. `final` and `super` — Implemented

**Where:** `Customer.customerId`, `Invoice`'s issued snapshot fields, `PaymentRecord` fields, constants such as `IspService.TAX_RATE`, and service dependencies are declared `final`. `BasicPlan`, `PremiumPlan`, and other subclasses call parent constructors with `super(...)`; custom exception constructors call `super(message)`.

**How it appears:** `final` prevents reassignment of identifiers and fixed configuration references. In an invoice, final fields preserve the values captured when that invoice was issued. `super(...)` invokes the parent constructor so shared fields are initialized by the parent implementation.

**Presentation line:** “I use `final` for identifiers and invoice snapshot data that should not be reassigned. Subclasses use `super` to reuse the parent constructor's validation and initialization.”

---

## Be ready to state these limitations

1. Java does not support multiple inheritance of classes. This project demonstrates multiple inheritance through interfaces, and hybrid inheritance by combining interfaces with its class hierarchy.
2. JDBC persistence is real, but stores one JSONB document rather than normalized entity tables.
3. `NotificationThread` demonstrates thread creation by subclassing `Thread`; the actively used background feature is the report thread pool.

These distinctions make the explanation accurate if the examiner asks whether every subtopic has a direct code example.
