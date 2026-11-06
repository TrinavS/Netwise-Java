# Java Concepts and Code Locations

| Concept | Where used |
|---|---|
| Classes | `Customer.java` — `Customer`; `InternetPlan.java` — `InternetPlan`; `Invoice.java` — `Invoice`; `Subscription.java` — `Subscription`; `IspService.java` — `IspService` |
| Objects | `IspService.java` — `seed()`, `addCustomer()`, `subscribe()`, `createInvoice()` |
| `static` and `private` variables | `IspService.java` — `TAX_RATE`, `EMAIL_PATTERN`, `PHONE_PATTERN`; `Customer.java` — private profile fields; `Invoice.java` — private invoice fields; `Subscription.java` — `GENERATED_IDS` |
| Inheritance | `BasicPlan.java`, `PremiumPlan.java`, `CustomPlan.java` extend `InternetPlan.java`; `HomePlan.java` extends `ResidentialPlan.java` |
| Single inheritance | `BasicPlan.java` — `BasicPlan extends InternetPlan` |
| Multilevel inheritance | `HomePlan.java` → `ResidentialPlan.java` → `InternetPlan.java` |
| Hierarchical inheritance | `BasicPlan.java`, `PremiumPlan.java`, `CustomPlan.java`, `ResidentialPlan.java` extend `InternetPlan.java` |
| Multiple inheritance through interfaces | `HomePlan.java` — implements `StreamingPlan` and `PrioritySupportPlan` |
| Hybrid inheritance | `HomePlan.java` — extends `ResidentialPlan` and implements `StreamingPlan`, `PrioritySupportPlan` |
| Method overloading | `IspService.java` — `findCustomer(int)` and `findCustomer(String)`; `IspHttpServer.java` — `GET /api/customers/{id}` and `GET /api/customers?email=...` |
| Method overriding | `BasicPlan.java`, `HomePlan.java`, `PremiumPlan.java`, `CustomPlan.java` — `displayPlan()` and `getTier()`; `NotificationThread.java` — `run()` |
| Abstract methods | `InternetPlan.java` — `displayPlan()`, `getTier()` |
| Interfaces | `PlanDetails.java`; `Billable.java`; `StreamingPlan.java`; `PrioritySupportPlan.java`; implementations in `InternetPlan.java`, `Bill.java`, `HomePlan.java` |
| Java collections | `IspService.java` — `List`, `ArrayList`, `Map`, `LinkedHashMap`, `Vector`; `ComplaintManager.java` — `ArrayList`, `Set`, `HashSet`; `ReportService.java` — `ConcurrentHashMap` |
| Strings and string functions | `IspService.java` — input trimming, validation, `buildReport()`; `ComplaintManager.java` — string/status comparisons; `Invoice.java` — `getSummary()`; `Json.java` — `stringify()` |
| Exception handling | `InvalidCustomerException.java`, `InvalidPlanException.java`, `InvalidUsageException.java`, `InvalidComplaintException.java`, `CustomerNotFoundException.java`, `PlanNotFoundException.java`; `IspService.java` — `throws`; `IspHttpServer.java` — `handleApi()` `try`/`catch` |
| Multithreading | `ReportService.java` — `start()`, fixed thread pool, `ReportJob.update()`, `awaitSnapshot()`, `cancel()`; `NotificationThread.java` — `run()`; `IspHttpServer.java` — request executor |
| File handling | `IspHttpServer.java` — `handleStatic()`, `Files.readAllBytes()`; `ReportFileStore.java` — `writeReport()`, `readReport()`; `IspHttpServer.java` — `GET /api/reports/{id}/file` |
| JDBC | `JdbcStore.java` — `connect()`, `initialize()`, `load()`, `save()`; `Main.java` — `main()` |
| REST API | `IspHttpServer.java` — `handleApi()`, `/api/health`, `/api/state`, customer, plan, subscription, billing, complaint, and report routes |
| `final` keyword | `Customer.java` — `customerId`; `Invoice.java` — issued invoice fields; `PaymentRecord.java` — payment fields; `IspService.java` — constants; `JdbcStore.java` — connection settings |
| `super` keyword | `BasicPlan.java`, `HomePlan.java`, `PremiumPlan.java`, `CustomPlan.java`, `ResidentialPlan.java` — superclass constructors; custom exception classes — `super(message)` |
