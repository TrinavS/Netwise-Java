package isp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import java.sql.SQLException;

/** Synchronized domain service shared by HTTP requests and persisted through JDBC. */
public class IspService {
    private static final double TAX_RATE = 0.18;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.!#$%&'*+/=?^`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?=(?:.*\\d){8,16}$)[0-9+() -]{8,24}$");
    private final JdbcStore store;

    private final List<Customer> customers = new ArrayList<>();
    private final Map<Integer, Customer> customerIndex = new LinkedHashMap<>();
    private final Vector<InternetPlan> plans = new Vector<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final Map<Integer, Usage> usageByCustomer = new LinkedHashMap<>();
    private final List<Invoice> invoices = new ArrayList<>();
    private final List<ServiceNotice> serviceNotices = new ArrayList<>();
    private final ComplaintManager complaintManager = new ComplaintManager();
    private int nextCustomerId = 103;
    private int nextPlanId = 4;
    private int nextSubscriptionId = 3;
    private int nextInvoiceNumber = 1044;
    private int nextPaymentNumber = 1002;
    private int nextComplaintNumber = 103;
    private int nextNoticeNumber = 1;

    public IspService() {
        this(null);
    }

    public IspService(JdbcStore store) {
        this.store = store;
        if (store != null) {
            try {
                store.initialize();
                String savedState = store.load();
                if (savedState != null) {
                    restore(Json.object(Json.parse(savedState)));
                    return;
                }
            } catch (SQLException | RuntimeException exception) {
                throw new IllegalStateException("Could not initialize PostgreSQL persistence: " + exception.getMessage(), exception);
            }
        }
        seed();
        persistState();
    }

    @SuppressWarnings("unchecked")
    private void restore(Map<String, Object> state) {
        Map<Integer, InternetPlan> planById = new LinkedHashMap<>();
        for (Object value : list(state, "plans")) {
            Map<String, Object> row = (Map<String, Object>) value;
            try {
                InternetPlan plan = planFromInput(intValue(row, "id"), row);
                plans.add(plan);
                planById.put(plan.getPlanId(), plan);
                nextPlanId = Math.max(nextPlanId, plan.getPlanId() + 1);
            } catch (InvalidPlanException exception) {
                throw new IllegalStateException("Invalid saved plan: " + exception.getMessage(), exception);
            }
        }
        for (Object value : list(state, "customers")) {
            Map<String, Object> row = (Map<String, Object>) value;
            Customer customer = new Customer(intValue(row, "id"), text(row, "name"), text(row, "email"),
                    text(row, "phone"), optionalText(row, "address"));
            customers.add(customer);
            customerIndex.put(customer.getCustomerId(), customer);
            nextCustomerId = Math.max(nextCustomerId, customer.getCustomerId() + 1);
        }
        Map<Integer, Map<String, Object>> usageRows = new LinkedHashMap<>();
        Object rawUsage = state.get("usage");
        if (rawUsage instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                usageRows.put(Integer.parseInt(String.valueOf(entry.getKey())), (Map<String, Object>) entry.getValue());
            }
        }
        for (Object value : list(state, "subscriptions")) {
            Map<String, Object> row = (Map<String, Object>) value;
            int customerId = intValue(row, "customerId"), planId = intValue(row, "planId");
            Customer customer = customerIndex.get(customerId);
            InternetPlan plan = planById.get(planId);
            if (customer == null || plan == null) continue;
            Subscription subscription = new Subscription(text(row, "id"), customer, plan,
                    LocalDate.parse(text(row, "startDate")), intValue(row, "daysRemaining"));
            if ("CANCELLED".equals(text(row, "status"))) subscription.cancelSubscription();
            Object pendingId = row.get("pendingPlanId");
            if (pendingId instanceof Number number && planById.containsKey(number.intValue())) {
                subscription.schedulePlanChange(planById.get(number.intValue()), LocalDate.parse(text(row, "changeEffectiveDate")));
            }
            subscriptions.add(subscription);
            nextSubscriptionId = Math.max(nextSubscriptionId, parseSuffix(subscription.getSubscriptionId()) + 1);
        }
        for (Map.Entry<Integer, Map<String, Object>> entry : usageRows.entrySet()) {
            double limit = 0;
            Subscription subscription = activeSubscription(entry.getKey());
            if (subscription != null) limit = subscription.getPlan().getDataLimit();
            Map<String, Object> row = entry.getValue();
            List<Double> history = new ArrayList<>();
            for (Object item : list(row, "history")) history.add(((Number) item).doubleValue());
            usageByCustomer.put(entry.getKey(), new Usage(limit, doubleValue(row, "used"), history,
                    optionalInt(row, "alertThresholdPercent", 80)));
        }
        for (Object value : list(state, "bills")) {
            Map<String, Object> row = (Map<String, Object>) value;
            List<PaymentRecord> payments = new ArrayList<>();
            for (Object paymentValue : list(row, "payments")) {
                Map<String, Object> payment = (Map<String, Object>) paymentValue;
                PaymentRecord record = new PaymentRecord(text(payment, "paymentId"), doubleValue(payment, "amount"),
                        LocalDate.parse(text(payment, "paidOn")), text(payment, "method"));
                payments.add(record);
                nextPaymentNumber = Math.max(nextPaymentNumber, parseSuffix(record.getPaymentId()) + 1);
            }
            String period = text(row, "billingPeriod");
            Invoice invoice = new Invoice(text(row, "id"), intValue(row, "customerId"), text(row, "customerName"),
                    YearMonth.parse(period), intValue(row, "planId"), text(row, "planName"), doubleValue(row, "planPrice"),
                    doubleValue(row, "taxRate"), LocalDate.parse(text(row, "dueDate")), text(row, "paymentStatus"), payments);
            invoices.add(invoice);
            nextInvoiceNumber = Math.max(nextInvoiceNumber, parseSuffix(invoice.getId()) + 1);
        }
        for (Object value : list(state, "complaints")) {
            Map<String, Object> row = (Map<String, Object>) value;
            Customer customer = customerIndex.get(intValue(row, "customerId"));
            if (customer == null) continue;
            String createdAt = optionalText(row, "createdAt");
            if (createdAt.isBlank()) {
                String created = optionalText(row, "created");
                createdAt = (created.isBlank() ? LocalDate.now().toString() : created) + "T09:00:00";
            }
            List<ComplaintEvent> events = new ArrayList<>();
            for (Object eventValue : list(row, "timeline")) {
                Map<String, Object> event = (Map<String, Object>) eventValue;
                events.add(new ComplaintEvent(text(event, "action"), text(event, "detail"),
                        text(event, "actor"), text(event, "timestamp")));
            }
            Complaint complaint = new Complaint(text(row, "id"), customer, text(row, "type"),
                    text(row, "description"), text(row, "status"), optionalText(row, "priority"),
                    optionalText(row, "assignedTo"), createdAt, events);
            try {
                complaintManager.addComplaint(complaint);
            } catch (InvalidComplaintException exception) {
                throw new IllegalStateException("Invalid saved complaint: " + exception.getMessage(), exception);
            }
            nextComplaintNumber = Math.max(nextComplaintNumber, parseSuffix(complaint.getComplaintId()) + 1);
        }
        for (Object value : list(state, "notices")) {
            Map<String, Object> row = (Map<String, Object>) value;
            ServiceNotice notice = new ServiceNotice(text(row, "id"), text(row, "title"), text(row, "message"),
                    text(row, "area"), optionalText(row, "startsAt"), text(row, "status"),
                    optionalText(row, "createdAt"));
            serviceNotices.add(notice);
            nextNoticeNumber = Math.max(nextNoticeNumber, parseSuffix(notice.getId()) + 1);
        }
        if (!state.containsKey("notices")) addDemoNotice();
    }

    private void persistState() {
        if (store == null) return;
        try { store.save(Json.stringify(snapshotWithoutPersist())); }
        catch (SQLException exception) { throw new IllegalStateException("Could not save data to PostgreSQL: " + exception.getMessage(), exception); }
    }

    private Map<String, Object> snapshotWithoutPersist() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("customers", customers.stream().map(this::customerJson).toList());
        root.put("plans", plans.stream().map(this::planJson).toList());
        root.put("subscriptions", subscriptions.stream().map(this::subscriptionJson).toList());
        Map<String, Object> usage = new LinkedHashMap<>();
        usageByCustomer.forEach((id, entry) -> usage.put(String.valueOf(id), usageJson(entry)));
        root.put("usage", usage);
        root.put("bills", invoices.stream().map(this::invoiceJson).toList());
        root.put("complaints", complaintManager.getComplaints().stream().map(this::complaintJson).toList());
        root.put("complaintTypes", complaintManager.getComplaintTypes());
        root.put("notices", serviceNotices.stream().map(this::noticeJson).toList());
        return root;
    }

    private static List<?> list(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof List<?> list ? list : List.of();
    }
    private static int intValue(Map<String, Object> object, String key) { return ((Number) object.get(key)).intValue(); }
    private static int optionalInt(Map<String, Object> object, String key, int fallback) {
        Object value = object.get(key); return value instanceof Number number ? number.intValue() : fallback;
    }
    private static double doubleValue(Map<String, Object> object, String key) { return ((Number) object.get(key)).doubleValue(); }
    private static int parseSuffix(String value) {
        int dash = value.lastIndexOf('-');
        try { return Integer.parseInt(dash >= 0 ? value.substring(dash + 1) : value.replaceAll("^.*?(\\d+)$", "$1")); }
        catch (RuntimeException ignored) { return 1; }
    }

    private void seed() {
        Customer rahul = new Customer(101, "Rahul Sharma", "rahul@gmail.com", "9876543210", "Bengaluru, Karnataka");
        Customer priya = new Customer(102, "Priya Mehta", "priya@gmail.com", "9876501234", "Pune, Maharashtra");
        customers.add(rahul); customers.add(priya);
        customerIndex.put(rahul.getCustomerId(), rahul); customerIndex.put(priya.getCustomerId(), priya);

        InternetPlan basic = new BasicPlan(1, "Basic 100", 499, 100, "50 Mbps", "Reliable everyday browsing", false);
        InternetPlan home = new HomePlan(2, "Home Plus", 699, 300, "100 Mbps", "Streaming and work from home", true);
        InternetPlan premium = new PremiumPlan(3, "Premium 500", 799, 500, "200 Mbps", "Built for busy households", false);
        plans.add(basic); plans.add(home); plans.add(premium);

        subscriptions.add(new Subscription("SUB-001", rahul, home, LocalDate.of(2026, 9, 1), 17));
        subscriptions.add(new Subscription("SUB-002", priya, premium, LocalDate.of(2026, 9, 12), 4));
        usageByCustomer.put(101, new Usage(300, 132.4, List.of(24.0, 38.0, 29.0, 41.4)));
        usageByCustomer.put(102, new Usage(500, 291.7, List.of(64.0, 81.0, 70.0, 76.7)));

        invoices.add(new Invoice("INV-2026-1042", 101, rahul.getName(), YearMonth.of(2026, 10), 2, "Home Plus", 699,
                TAX_RATE, LocalDate.of(2026, 10, 25), "UNPAID", List.of()));
        invoices.add(new Invoice("INV-2026-1031", 101, rahul.getName(), YearMonth.of(2026, 9), 2, "Home Plus", 699,
                TAX_RATE, LocalDate.of(2026, 9, 25), "PAID",
                List.of(new PaymentRecord("PAY-2026-1001", 824.82, LocalDate.of(2026, 9, 24), "SIMULATED"))));
        invoices.add(new Invoice("INV-2026-1043", 102, priya.getName(), YearMonth.of(2026, 10), 3, "Premium 500", 799,
                TAX_RATE, LocalDate.of(2026, 10, 25), "UNPAID", List.of()));
        try {
            complaintManager.addComplaint(new Complaint("CMP101", rahul, "Slow Internet", "Internet speed is very slow."));
            complaintManager.updateStatus("CMP101", "IN_PROGRESS");
            complaintManager.addComplaint(new Complaint("CMP102", priya, "Connection Issue", "Internet connection is intermittent."));
        } catch (InvalidComplaintException impossible) {
            throw new IllegalStateException("Invalid built-in sample data.", impossible);
        }
        addDemoNotice();
    }

    private void addDemoNotice() {
        LocalDateTime startsAt = LocalDate.now().plusDays(7).atTime(1, 0);
        serviceNotices.add(new ServiceNotice("NTC-" + String.format("%04d", nextNoticeNumber++),
                "Demo: scheduled network maintenance",
                "Sample notice for the project demo. Administrators can update its status or publish a project notice.",
                "All service areas", startsAt.toString(), "SCHEDULED", LocalDateTime.now().withNano(0).toString()));
    }

    public synchronized Map<String, Object> snapshot() {
        applyCurrentBillingCycle();
        persistState();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("customers", customers.stream().map(this::customerJson).toList());
        root.put("plans", plans.stream().map(this::planJson).toList());
        root.put("subscriptions", subscriptions.stream().map(this::subscriptionJson).toList());
        Map<String, Object> usage = new LinkedHashMap<>();
        usageByCustomer.forEach((id, entry) -> usage.put(String.valueOf(id), usageJson(entry)));
        root.put("usage", usage);
        root.put("bills", invoices.stream().map(this::invoiceJson).toList());
        root.put("complaints", complaintManager.getComplaints().stream().map(this::complaintJson).toList());
        root.put("complaintTypes", complaintManager.getComplaintTypes());
        root.put("notices", serviceNotices.stream().map(this::noticeJson).toList());
        return root;
    }

    /** Look up a customer by numeric account ID. */
    public synchronized Map<String, Object> findCustomer(int customerId) throws CustomerNotFoundException {
        Customer customer = customerIndex.get(customerId);
        if (customer == null) throw new CustomerNotFoundException("Customer #" + customerId + " was not found.");
        return customerJson(customer);
    }

    /** Overloaded customer lookup by email address. */
    public synchronized Map<String, Object> findCustomer(String email) throws CustomerNotFoundException {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required for customer lookup.");
        for (Customer customer : customers) {
            if (customer.getEmail().equalsIgnoreCase(email.trim())) return customerJson(customer);
        }
        throw new CustomerNotFoundException("No customer with email " + email.trim() + " was found.");
    }

    public synchronized Map<String, Object> addCustomer(Map<String, Object> input) throws InvalidCustomerException {
        String name = text(input, "name").trim();
        String email = text(input, "email").trim();
        String phone = text(input, "phone").trim();
        String address = optionalText(input, "address").trim();
        if (name.isEmpty()) throw new InvalidCustomerException("Customer name is required.");
        if (!EMAIL_PATTERN.matcher(email).matches()) throw new InvalidCustomerException("Enter a valid email address.");
        if (!PHONE_PATTERN.matcher(phone).matches()) throw new InvalidCustomerException("Enter a valid phone number (8 to 16 characters).");
        int id = nextCustomerId++;
        Customer customer = new Customer(id, name, email, phone, address);
        customers.add(customer); customerIndex.put(id, customer);
        usageByCustomer.put(id, new Usage(0));
        persistState();
        return customerJson(customer);
    }

    public synchronized Map<String, Object> updateCustomer(int id, Map<String, Object> input)
            throws CustomerNotFoundException, InvalidCustomerException {
        Customer customer = customerIndex.get(id);
        if (customer == null) throw new CustomerNotFoundException("Customer #" + id + " was not found.");
        String name = text(input, "name").trim(), email = text(input, "email").trim();
        String phone = text(input, "phone").trim(), address = optionalText(input, "address").trim();
        if (name.isEmpty()) throw new InvalidCustomerException("Customer name is required.");
        if (!EMAIL_PATTERN.matcher(email).matches()) throw new InvalidCustomerException("Enter a valid email address.");
        if (!PHONE_PATTERN.matcher(phone).matches()) throw new InvalidCustomerException("Enter a valid phone number (8 to 16 characters).");
        customer.updateProfile(name, email, phone, address);
        persistState();
        return customerJson(customer);
    }

    public synchronized void removeCustomer(int id) throws CustomerNotFoundException {
        Customer customer = customerIndex.remove(id);
        if (customer == null) throw new CustomerNotFoundException("Customer #" + id + " was not found.");
        customers.remove(customer);
        subscriptions.removeIf(subscription -> subscription.getCustomer().getCustomerId() == id);
        usageByCustomer.remove(id);
        complaintManager.removeForCustomer(id);
        persistState();
    }

    public synchronized Map<String, Object> addPlan(Map<String, Object> input) throws InvalidPlanException {
        InternetPlan plan = planFromInput(nextPlanId++, input);
        plans.add(plan);
        persistState();
        return planJson(plan);
    }

    public synchronized Map<String, Object> updatePlan(int id, Map<String, Object> input)
            throws PlanNotFoundException, InvalidPlanException {
        InternetPlan existing = findPlan(id);
        if (existing == null) throw new PlanNotFoundException("Internet plan #" + id + " was not found.");
        InternetPlan updated = planFromInput(id, input);
        plans.set(plans.indexOf(existing), updated);
        for (Subscription subscription : subscriptions) {
            boolean currentPlan = subscription.getPlan().getPlanId() == id;
            boolean scheduledPlan = subscription.getPendingPlan() != null && subscription.getPendingPlan().getPlanId() == id;
            if ((currentPlan || scheduledPlan) && "ACTIVE".equals(subscription.getStatus())) {
                int customerId = subscription.getCustomer().getCustomerId();
                subscription.refreshPlanDetails(updated);
                if (currentPlan) usageByCustomer.get(customerId).updateDataLimit(updated.getDataLimit());
            }
        }
        persistState();
        return planJson(updated);
    }

    public synchronized void removePlan(int id) throws PlanNotFoundException {
        InternetPlan plan = findPlan(id);
        if (plan == null) throw new PlanNotFoundException("Internet plan #" + id + " was not found.");
        boolean inUse = subscriptions.stream().anyMatch(s -> "ACTIVE".equals(s.getStatus()) &&
                (s.getPlan().getPlanId() == id || (s.getPendingPlan() != null && s.getPendingPlan().getPlanId() == id)));
        if (inUse) throw new IllegalArgumentException("Switch active customers to another plan before removing this one.");
        plans.remove(plan);
        persistState();
    }

    public synchronized Map<String, Object> subscribe(int customerId, int planId)
            throws CustomerNotFoundException, PlanNotFoundException {
        Customer customer = customerIndex.get(customerId);
        if (customer == null) throw new CustomerNotFoundException("Customer #" + customerId + " was not found.");
        InternetPlan plan = findPlan(planId);
        if (plan == null) throw new PlanNotFoundException("Internet plan #" + planId + " was not found.");
        Subscription active = activeSubscription(customerId);
        if (active == null) {
            active = new Subscription("SUB-" + String.format("%03d", nextSubscriptionId++), customer, plan, LocalDate.now(), 30);
            subscriptions.add(active);
            usageByCustomer.computeIfAbsent(customerId, ignored -> new Usage(plan.getDataLimit()))
                    .updateDataLimit(plan.getDataLimit());
            createInvoice(customerId, plan, YearMonth.now());
        } else {
            if (active.getPlan().getPlanId() == planId) {
                active.cancelScheduledPlanChange();
            } else {
                LocalDate effectiveDate = YearMonth.now().plusMonths(1).atDay(1);
                active.schedulePlanChange(plan, effectiveDate);
            }
        }
        persistState();
        return subscriptionJson(active);
    }

    public synchronized Map<String, Object> cancelScheduledPlanChange(int customerId) throws CustomerNotFoundException {
        if (!customerIndex.containsKey(customerId)) throw new CustomerNotFoundException("Customer #" + customerId + " was not found.");
        Subscription active = activeSubscription(customerId);
        if (active == null) throw new IllegalArgumentException("Customer has no active subscription.");
        active.cancelScheduledPlanChange();
        persistState();
        return subscriptionJson(active);
    }

    public synchronized Map<String, Object> addUsage(int customerId, double gb)
            throws CustomerNotFoundException, InvalidUsageException {
        if (!customerIndex.containsKey(customerId)) throw new CustomerNotFoundException("Customer #" + customerId + " was not found.");
        Usage usage = usageByCustomer.get(customerId);
        if (usage == null) throw new InvalidUsageException("This account has no active usage allowance.");
        if (activeSubscription(customerId) == null) throw new InvalidUsageException("Choose an internet plan before recording data usage.");
        usage.addUsage(gb);
        persistState();
        return usageJson(usage);
    }

    public synchronized Map<String, Object> updateUsageAlert(int customerId, int thresholdPercent)
            throws CustomerNotFoundException {
        if (!customerIndex.containsKey(customerId)) throw new CustomerNotFoundException("Customer #" + customerId + " was not found.");
        Usage usage = usageByCustomer.get(customerId);
        if (usage == null) throw new IllegalArgumentException("This account has no usage settings.");
        usage.setAlertThresholdPercent(thresholdPercent);
        persistState();
        return usageJson(usage);
    }

    public synchronized Map<String, Object> addComplaint(Map<String, Object> input)
            throws CustomerNotFoundException, InvalidComplaintException {
        int customerId = integer(input, "customerId");
        Customer customer = customerIndex.get(customerId);
        if (customer == null) throw new CustomerNotFoundException("Customer #" + customerId + " was not found.");
        String type = text(input, "type").trim();
        String description = text(input, "description").trim();
        String id = "CMP" + nextComplaintNumber++;
        Complaint complaint = new Complaint(id, customer, type, description);
        complaintManager.addComplaint(complaint);
        persistState();
        return complaintJson(complaint);
    }

    public synchronized Map<String, Object> updateComplaint(String id, String status)
            throws InvalidComplaintException {
        Map<String, Object> update = new LinkedHashMap<>(); update.put("status", status);
        return updateComplaint(id, update);
    }

    public synchronized Map<String, Object> updateComplaint(String id, Map<String, Object> input)
            throws InvalidComplaintException {
        String status = input.containsKey("status") ? optionalText(input, "status") : null;
        String priority = input.containsKey("priority") ? optionalText(input, "priority") : null;
        String assignedTo = input.containsKey("assignedTo") ? optionalText(input, "assignedTo") : null;
        String note = input.containsKey("note") ? optionalText(input, "note") : null;
        complaintManager.updateComplaint(id, status, priority, assignedTo, note);
        persistState();
        return complaintJson(complaintManager.findComplaint(id));
    }

    public synchronized Map<String, Object> addServiceNotice(Map<String, Object> input) {
        String title = text(input, "title").trim();
        String message = text(input, "message").trim();
        String area = text(input, "area").trim();
        String startsAt = optionalText(input, "startsAt").trim();
        String status = text(input, "status").trim().toUpperCase();
        if (title.isEmpty() || title.length() > 90) throw new IllegalArgumentException("Notice title is required and must be 90 characters or fewer.");
        if (message.length() < 8 || message.length() > 500) throw new IllegalArgumentException("Notice message must be between 8 and 500 characters.");
        if (area.isEmpty() || area.length() > 100) throw new IllegalArgumentException("Service area is required and must be 100 characters or fewer.");
        validateNoticeStatus(status);
        if (!startsAt.isEmpty()) {
            try { LocalDateTime.parse(startsAt); }
            catch (RuntimeException exception) { throw new IllegalArgumentException("Scheduled start must be a valid local date and time."); }
        }
        String id = "NTC-" + String.format("%04d", nextNoticeNumber++);
        ServiceNotice notice = new ServiceNotice(id, title, message, area, startsAt, status, LocalDateTime.now().withNano(0).toString());
        serviceNotices.add(0, notice);
        persistState();
        return noticeJson(notice);
    }

    public synchronized Map<String, Object> updateServiceNotice(String id, String status) {
        ServiceNotice notice = serviceNotices.stream().filter(n -> n.getId().equalsIgnoreCase(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Service notice " + id + " was not found."));
        validateNoticeStatus(status == null ? "" : status.trim().toUpperCase());
        notice.setStatus(status.trim().toUpperCase());
        persistState();
        return noticeJson(notice);
    }

    private void validateNoticeStatus(String status) {
        if (!List.of("SCHEDULED", "ACTIVE", "RESOLVED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Notice status must be Scheduled, Active, Resolved, or Cancelled.");
        }
    }

    public synchronized Map<String, Object> markInvoicePaid(String id) {
        for (Invoice invoice : invoices) {
            if (invoice.getId().equalsIgnoreCase(id)) {
                PaymentRecord payment = new PaymentRecord("PAY-" + LocalDate.now().getYear() + "-" + nextPaymentNumber++,
                        invoice.getTotal(), LocalDate.now(), "SIMULATED");
                invoice.markPaid(payment);
                persistState();
                return invoiceJson(invoice);
            }
        }
        throw new IllegalArgumentException("Invoice " + id + " was not found.");
    }

    public synchronized String buildReport() throws InterruptedException {
        StringBuilder report = new StringBuilder();
        report.append("NETWISE ISP ADMIN REPORT\nGenerated: ").append(LocalDate.now()).append("\n\nCUSTOMERS\n");
        for (Customer customer : customers) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Report generation was interrupted.");
            Subscription sub = activeSubscription(customer.getCustomerId());
            report.append("#").append(customer.getCustomerId()).append(" | ").append(customer.getName())
                    .append(" | ").append(customer.getEmail()).append(" | ")
                    .append(sub == null ? "No active plan" : sub.getPlan().getPlanName()).append('\n');
        }
        report.append("\nBILLING\n");
        for (Invoice invoice : invoices) {
            report.append(invoice.getId()).append(" | Customer #").append(invoice.getCustomerId())
                    .append(" ").append(invoice.getCustomerName()).append(" | ").append(invoice.getMonth())
                    .append(" | ").append(invoice.getPlanName()).append(" | Plan ₹")
                    .append(String.format("%.2f", invoice.getPlanPrice())).append(" | Tax ")
                    .append(String.format("%.0f%%", invoice.getTaxRate() * 100)).append(" ₹")
                    .append(String.format("%.2f", invoice.getTax())).append(" | Total ₹")
                    .append(String.format("%.2f", invoice.getTotal())).append(" | Due ")
                    .append(invoice.getDueDate()).append(" | ").append(invoice.getStatus()).append('\n');
            for (PaymentRecord payment : invoice.getPaymentHistory()) {
                report.append("  Payment ").append(payment.getPaymentId()).append(" | ")
                        .append(payment.getPaidOn()).append(" | ₹")
                        .append(String.format("%.2f", payment.getAmount())).append(" | ")
                        .append(payment.getMethod()).append('\n');
            }
        }
        report.append("\nSUPPORT TICKETS\n");
        for (Complaint complaint : complaintManager.getComplaints()) report.append(complaint.getComplaintId())
                .append(" | Customer #").append(complaint.getCustomerId()).append(" | ")
                .append(complaint.getComplaintType()).append(" | ").append(complaint.getStatus()).append('\n');
        return report.toString();
    }

    public synchronized int getCustomerCount() { return customers.size(); }

    private void applyCurrentBillingCycle() {
        YearMonth currentPeriod = YearMonth.now();
        LocalDate effectiveDate = currentPeriod.atDay(1);
        for (Subscription subscription : subscriptions) {
            if (!"ACTIVE".equals(subscription.getStatus())) continue;
            int customerId = subscription.getCustomer().getCustomerId();
            if (subscription.applyScheduledPlanChange(effectiveDate)) {
                usageByCustomer.get(customerId).updateDataLimit(subscription.getPlan().getDataLimit());
            }
            createInvoice(customerId, subscription.getPlan(), currentPeriod);
        }
    }

    private void createInvoice(int customerId, InternetPlan plan, YearMonth period) {
        boolean alreadyIssued = invoices.stream().anyMatch(invoice -> invoice.getCustomerId() == customerId &&
                invoice.getBillingPeriod().equals(period));
        if (alreadyIssued) return;
        LocalDate dueDate = period.atDay(Math.min(25, period.lengthOfMonth()));
        if (dueDate.isBefore(LocalDate.now())) {
            YearMonth nextPeriod = period.plusMonths(1);
            dueDate = nextPeriod.atDay(Math.min(25, nextPeriod.lengthOfMonth()));
        }
        String invoiceId;
        do {
            invoiceId = "INV-" + period.getYear() + "-" + String.format("%04d", nextInvoiceNumber++);
        } while (invoiceIdExists(invoiceId));
        Customer customer = customerIndex.get(customerId);
        invoices.add(0, new Invoice(invoiceId, customerId, customer == null ? null : customer.getName(), period, plan.getPlanId(), plan.getPlanName(),
                plan.getPrice(), TAX_RATE, dueDate, "UNPAID", List.of()));
    }

    private boolean invoiceIdExists(String id) {
        for (Invoice invoice : invoices) if (invoice.getId().equals(id)) return true;
        return false;
    }

    private Subscription activeSubscription(int customerId) {
        for (Subscription subscription : subscriptions) {
            if (subscription.getCustomer().getCustomerId() == customerId && "ACTIVE".equals(subscription.getStatus())) return subscription;
        }
        return null;
    }

    private InternetPlan findPlan(int id) {
        for (InternetPlan plan : plans) if (plan.getPlanId() == id) return plan;
        return null;
    }

    private InternetPlan planFromInput(int id, Map<String, Object> input) throws InvalidPlanException {
        String name = text(input, "name").trim();
        String tier = text(input, "tier").trim();
        String speed = text(input, "speed").trim();
        String description = text(input, "description").trim();
        Object priceValue = input.get("price"), dataValue = input.get("data");
        if (name.isEmpty() || speed.isEmpty() || description.isEmpty()) throw new InvalidPlanException("Name, speed, and description are required.");
        if (!List.of("basic", "standard", "premium").contains(tier.toLowerCase())) {
            throw new InvalidPlanException("Choose Basic, Standard, or Premium as the plan type.");
        }
        if (!(priceValue instanceof Number priceNumber) || !Double.isFinite(priceNumber.doubleValue()) || priceNumber.doubleValue() <= 0) {
            throw new InvalidPlanException("Plan price must be a finite positive number.");
        }
        if (!(dataValue instanceof Number dataNumber) || dataNumber.intValue() <= 0 || dataNumber.doubleValue() != dataNumber.intValue()) {
            throw new InvalidPlanException("Monthly data allowance must be a positive whole number of GB.");
        }
        boolean featured = Boolean.TRUE.equals(input.get("featured"));
        return switch (tier.toLowerCase()) {
            case "basic" -> new BasicPlan(id, name, priceNumber.doubleValue(), dataNumber.intValue(), speed, description, featured);
            case "premium" -> new PremiumPlan(id, name, priceNumber.doubleValue(), dataNumber.intValue(), speed, description, featured);
            case "standard" -> new HomePlan(id, name, priceNumber.doubleValue(), dataNumber.intValue(), speed, description, featured);
            default -> new CustomPlan(id, name, priceNumber.doubleValue(), dataNumber.intValue(), speed, description, featured);
        };
    }

    private Map<String, Object> customerJson(Customer customer) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", customer.getCustomerId()); item.put("name", customer.getName());
        item.put("email", customer.getEmail()); item.put("phone", customer.getPhone()); item.put("address", customer.getAddress());
        return item;
    }
    private Map<String, Object> planJson(InternetPlan plan) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", plan.getPlanId()); item.put("name", plan.getPlanName()); item.put("tier", plan.getTier());
        item.put("price", plan.getPrice()); item.put("data", plan.getDataLimit()); item.put("speed", plan.getSpeed());
        item.put("description", plan.getDescription()); item.put("featured", plan.isFeatured()); item.put("summary", plan.getPlanSummary());
        return item;
    }
    private Map<String, Object> subscriptionJson(Subscription subscription) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", subscription.getSubscriptionId());
        item.put("customerId", subscription.getCustomer().getCustomerId()); item.put("planId", subscription.getPlan().getPlanId());
        item.put("status", subscription.getStatus()); item.put("startDate", subscription.getStartDate().toString());
        item.put("daysRemaining", subscription.getDaysRemaining());
        item.put("pendingPlanId", subscription.getPendingPlan() == null ? null : subscription.getPendingPlan().getPlanId());
        item.put("pendingPlanName", subscription.getPendingPlan() == null ? null : subscription.getPendingPlan().getPlanName());
        item.put("changeEffectiveDate", subscription.getPlanChangeEffectiveDate() == null ? null : subscription.getPlanChangeEffectiveDate().toString());
        return item;
    }
    private Map<String, Object> usageJson(Usage usage) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("used", usage.getDataUsed()); item.put("history", usage.getHistory());
        item.put("alertThresholdPercent", usage.getAlertThresholdPercent());
        return item;
    }
    private Map<String, Object> invoiceJson(Invoice invoice) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", invoice.getId()); item.put("customerId", invoice.getCustomerId()); item.put("customerName", invoice.getCustomerName()); item.put("month", invoice.getMonth());
        item.put("billingPeriod", invoice.getPeriodKey()); item.put("planId", invoice.getPlanId()); item.put("planName", invoice.getPlanName());
        item.put("planPrice", invoice.getPlanPrice()); item.put("amount", invoice.getAmount()); item.put("taxRate", invoice.getTaxRate());
        item.put("gst", invoice.getTax()); item.put("total", invoice.getTotal());
        item.put("status", invoice.getStatus()); item.put("paymentStatus", invoice.getPaymentStatus());
        item.put("dueDate", invoice.getDueDate()); item.put("summary", invoice.getSummary());
        item.put("payments", invoice.getPaymentHistory().stream().map(this::paymentJson).toList());
        return item;
    }
    private Map<String, Object> paymentJson(PaymentRecord payment) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("paymentId", payment.getPaymentId()); item.put("amount", payment.getAmount());
        item.put("paidOn", payment.getPaidOn().toString()); item.put("method", payment.getMethod());
        return item;
    }
    private Map<String, Object> complaintJson(Complaint complaint) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", complaint.getComplaintId()); item.put("customerId", Integer.parseInt(complaint.getCustomerId()));
        item.put("type", complaint.getComplaintType()); item.put("description", complaint.getDescription());
        item.put("status", complaint.getStatus()); item.put("priority", complaint.getPriority());
        item.put("assignedTo", complaint.getAssignedTo()); item.put("createdAt", complaint.getCreatedAt());
        item.put("created", complaint.getCreatedAt().substring(0, Math.min(10, complaint.getCreatedAt().length())));
        item.put("timeline", complaint.getTimeline().stream().map(this::complaintEventJson).toList());
        return item;
    }
    private Map<String, Object> complaintEventJson(ComplaintEvent event) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("action", event.getAction()); item.put("detail", event.getDetail());
        item.put("actor", event.getActor()); item.put("timestamp", event.getTimestamp());
        return item;
    }
    private Map<String, Object> noticeJson(ServiceNotice notice) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", notice.getId()); item.put("title", notice.getTitle()); item.put("message", notice.getMessage());
        item.put("area", notice.getArea()); item.put("startsAt", notice.getStartsAt());
        item.put("status", notice.getStatus()); item.put("createdAt", notice.getCreatedAt());
        return item;
    }

    private static String text(Map<String, Object> object, String key) {
        Object value = object.get(key); return value == null ? "" : String.valueOf(value);
    }
    private static String optionalText(Map<String, Object> object, String key) { return object.get(key) == null ? "" : String.valueOf(object.get(key)); }
    private static int integer(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (!(value instanceof Number number)) throw new IllegalArgumentException(key + " must be a number.");
        return number.intValue();
    }
}
