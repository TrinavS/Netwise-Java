# Netwise ISP Frontend

Responsive HTML, CSS, and JavaScript frontend for the ISP mini project.

## Run it

1. Open this `frontend` folder in VS Code.
2. Open `index.html` in a browser, or use the VS Code Live Server extension.
3. Use the sidebar to move between Dashboard, Internet plans, Data usage, Billing, and Support & complaints.
4. Switch **View as** between Customer and Admin demo to see both interfaces.

## Implemented interactions

- Customer dashboard with current plan, usage, open tickets, and invoice overview
- Internet plan cards and plan switching
- Demo data usage entry with allowance warnings
- Billing table, mark-as-paid demo action, CSV statement export, and receipt download
- Complaint creation, searching, viewing, and admin status updates
- Admin customer directory, adding a customer, and exporting a report
- Responsive mobile sidebar
- Local demo state saved in browser `localStorage`

## Important backend integration note

The Java project currently uses console classes (`Main`, `CustomerManager`, `Bill`, `Usage`, and `ComplaintManager`). It does **not** expose HTTP endpoints. For that reason, this frontend currently runs in demo mode with sample data stored in the browser. Its actions do not change Java objects.

To connect the UI to Java using `fetch()`, add an HTTP API layer (for example, Java's built-in `com.sun.net.httpserver.HttpServer` for a simple syllabus project), define endpoints and JSON request/response formats, then replace the demo state operations in `app.js` with calls to those endpoints. Do not point `fetch()` at the console `Main.java`; it is not an HTTP server.

## Demo reset

To reset local demo data, open browser developer tools and run:

```js
localStorage.removeItem("netwise-isp-demo-v1");
location.reload();
```
