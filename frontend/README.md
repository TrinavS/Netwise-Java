# Netwise ISP Frontend

Responsive HTML, CSS, and JavaScript portal served by the Java application. The UI reads and writes all operational data through the same-origin `/api` endpoints; it does not keep a separate browser copy of customers, plans, subscriptions, usage, bills, or complaints.

## Run it

Start the Java server from the project root using the commands in the root `README.md`, then open `http://127.0.0.1:8080`. Opening `index.html` directly will not connect to the backend API.
Start the Postgre server before running the required

## Main screens

- Customer dashboard with active plan, usage, outstanding invoice, and support ticket summary
- Plan catalog and subscription changes
- Admin plan catalog create, edit, and remove actions
- Usage tracking and monthly usage history
- Immutable invoice history with overdue status, simulated payment history, receipts, and CSV statement export
- Plan changes scheduled for the next billing period so issued invoices retain their original amounts
- Support ticket creation, search, detail view, and admin status updates
- Admin customer search, add/edit/remove operations, and asynchronous report generation/cancellation

Portal brand, display locale, and support copy are browser preferences. Project records are persisted by the Java backend in PostgreSQL and survive server restarts.
