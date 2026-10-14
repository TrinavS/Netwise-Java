# ISP Mini Project (Java)

A modular Java console project for an Internet Service Provider (ISP). The frontend directory is intentionally blank until Person 1 shares their HTML, CSS, and JavaScript.

## Project structure

```text
ISP-Mini-Project/
├── frontend/
│   └── .gitkeep
├── backend/
│   └── src/
│       └── isp/
│           ├── Main.java
│           ├── InternetPlan.java
│           ├── BasicPlan.java
│           ├── PremiumPlan.java
│           ├── Customer.java
│           ├── Subscription.java
│           ├── CustomerManager.java
│           ├── Billable.java
│           ├── Bill.java
│           ├── Usage.java
│           ├── Complaint.java
│           ├── ComplaintManager.java
│           ├── InvalidComplaintException.java
│           ├── CustomerNotFoundException.java
│           ├── PlanNotFoundException.java
│           ├── InvalidUsageException.java
│           └── NotificationThread.java
└── .gitignore
```

## Requirements

- JDK 17 or later recommended
- No external libraries required

## Compile and run

Open a terminal in the project root.

Windows PowerShell:

```powershell
javac -d backend/out backend/src/isp/*.java
java -cp backend/out isp.Main
```

macOS/Linux:

```bash
mkdir -p backend/out
javac -d backend/out backend/src/isp/*.java
java -cp backend/out isp.Main
```

## Team ownership

- Person 1: `frontend/` and later frontend/backend integration
- Person 2: customer, plans, and subscriptions
- Person 3: billing and usage
- Person 4: complaints, custom exceptions, and notification multithreading

All Java files use the `isp` package so they can work together without package mismatches.

## Notes

- `PremiumPlan.java` is corrected to declare `PremiumPlan`, not a second `BasicPlan`.
- There is one integrated `Main.java`, avoiding multiple competing main classes.
- Complaints and other records are stored in memory only. They are lost when the program exits.
- `NotificationThread` simulates subscription-expiry notifications; it is not a real notification service.
