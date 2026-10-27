package isp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

/** Synchronized in-memory domain service shared by every HTTP request. */
public class IspService {
    private static final double TAX_RATE = 0.18;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.!#$%&'*+/=?^`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?=(?:.*\\d){8,16}$)[0-9+() -]{8,24}$");

    private final List<Customer> customers = new ArrayList<>();
    private final Map<Integer, Customer> customerIndex = new LinkedHashMap<>();
    private final Vector<InternetPlan> plans = new Vector<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final Map<Integer, Usage> usageByCustomer = new LinkedHashMap<>();
    private final List<Invoice> invoices = new ArrayList<>();
    private final ComplaintManager complaintManager = new ComplaintManager();
    private int nextCustomerId = 103;
    private int nextPlanId = 4;
    private int nextSubscriptionId = 3;
    private int nextInvoiceNumber = 1044;
    private int nextComplaintNumber = 103;

    public IspService() {
        seed();
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

        invoices.add(new Invoice("INV-2026-1042", 101, "October 2026", 699, TAX_RATE, "2026-10-25", "UNPAID"));
        invoices.add(new Invoice("INV-2026-1031", 101, "September 2026", 699, TAX_RATE, "2026-09-25", "PAID"));
        invoices.add(new Invoice("INV-2026-1043", 102, "October 2026", 799, TAX_RATE, "2026-10-25", "UNPAID"));
        try {
            complaintManager.addComplaint(new Complaint("CMP101", rahul, "Slow Internet", "Internet speed is very slow."));
            complaintManager.updateStatus("CMP101", "IN_PROGRESS");
            complaintManager.addComplaint(new Complaint("CMP102", priya, "Connection Issue", "Internet connection is intermittent."));
        } catch (InvalidComplaintException impossible) {
            throw new IllegalStateException("Invalid built-in sample data.", impossible);
        }
    }

    public synchronized Map<String, Object> snapshot() {
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
        return root;
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
        return customerJson(customer);
    }

    public synchronized void removeCustomer(int id) throws CustomerNotFoundException {
        Customer customer = customerIndex.remove(id);
        if (customer == null) throw new CustomerNotFoundException("Customer #" + id + " was not found.");
        customers.remove(customer);
        subscriptions.removeIf(subscription -> subscription.getCustomer().getCustomerId() == id);
        usageByCustomer.remove(id);
        invoices.removeIf(invoice -> invoice.getCustomerId() == id);
        complaintManager.removeForCustomer(id);
    }

    public synchronized Map<String, Object> addPlan(Map<String, Object> input) throws InvalidPlanException {
        InternetPlan plan = planFromInput(nextPlanId++, input);
        plans.add(plan);
        return planJson(plan);
    }

    public synchronized Map<String, Object> updatePlan(int id, Map<String, Object> input)
            throws PlanNotFoundException, InvalidPlanException {
        InternetPlan existing = findPlan(id);
        if (existing == null) throw new PlanNotFoundException("Internet plan #" + id + " was not found.");
        InternetPlan updated = planFromInput(id, input);
        plans.set(plans.indexOf(existing), updated);
        for (Subscription subscription : subscriptions) {
            if (subscription.getPlan().getPlanId() == id && "ACTIVE".equals(subscription.getStatus())) {
                int customerId = subscription.getCustomer().getCustomerId();
                subscription.refreshPlanDetails(updated);
                usageByCustomer.get(customerId).updateDataLimit(updated.getDataLimit());
                createCurrentInvoice(customerId, updated);
            }
        }
        return planJson(updated);
    }

    public synchronized void removePlan(int id) throws PlanNotFoundException {
        InternetPlan plan = findPlan(id);
        if (plan == null) throw new PlanNotFoundException("Internet plan #" + id + " was not found.");
        boolean inUse = subscriptions.stream().anyMatch(s -> s.getPlan().getPlanId() == id && "ACTIVE".equals(s.getStatus()));
        if (inUse) throw new IllegalArgumentException("Switch active customers to another plan before removing this one.");
        plans.remove(plan);
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
        } else {
            active.changePlan(plan);
        }
        usageByCustomer.computeIfAbsent(customerId, ignored -> new Usage(plan.getDataLimit()))
                .updateDataLimit(plan.getDataLimit());
        createCurrentInvoice(customerId, plan);
        return subscriptionJson(active);
    }

    public synchronized Map<String, Object> addUsage(int customerId, double gb)
            throws CustomerNotFoundException, InvalidUsageException {
        if (!customerIndex.containsKey(customerId)) throw new CustomerNotFoundException("Customer #" + customerId + " was not found.");
        Usage usage = usageByCustomer.get(customerId);
        if (usage == null) throw new InvalidUsageException("This account has no active usage allowance.");
        if (activeSubscription(customerId) == null) throw new InvalidUsageException("Choose an internet plan before recording data usage.");
        usage.addUsage(gb);
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
        return complaintJson(complaint);
    }

    public synchronized Map<String, Object> updateComplaint(String id, String status)
            throws InvalidComplaintException {
        complaintManager.updateStatus(id, status);
        return complaintJson(complaintManager.findComplaint(id));
    }

    public synchronized Map<String, Object> markInvoicePaid(String id) {
        for (Invoice invoice : invoices) {
            if (invoice.getId().equalsIgnoreCase(id)) {
                invoice.markPaid();
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
        for (Invoice invoice : invoices) report.append(invoice.getId()).append(" | Customer #")
                .append(invoice.getCustomerId()).append(" | ").append(invoice.getMonth())
                .append(" | ₹").append(String.format("%.2f", invoice.getTotal()))
                .append(" | ").append(invoice.getStatus()).append('\n');
        report.append("\nSUPPORT TICKETS\n");
        for (Complaint complaint : complaintManager.getComplaints()) report.append(complaint.getComplaintId())
                .append(" | Customer #").append(complaint.getCustomerId()).append(" | ")
                .append(complaint.getComplaintType()).append(" | ").append(complaint.getStatus()).append('\n');
        return report.toString();
    }

    public synchronized int getCustomerCount() { return customers.size(); }

    private void createCurrentInvoice(int customerId, InternetPlan plan) {
        YearMonth now = YearMonth.now();
        String month = now.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        for (int i = 0; i < invoices.size(); i++) {
            Invoice invoice = invoices.get(i);
            if (invoice.getCustomerId() == customerId && invoice.getMonth().equals(month) && "UNPAID".equals(invoice.getStatus())) {
                if (Math.abs(invoice.getAmount() - plan.getPrice()) < 0.001) return;
                invoices.remove(i);
                break;
            }
        }
        int dueDay = Math.min(25, now.lengthOfMonth());
        String dueDate = now.atDay(dueDay).toString();
        invoices.add(0, new Invoice("INV-" + now.getYear() + "-" + nextInvoiceNumber++, customerId,
                month, plan.getPrice(), TAX_RATE, dueDate, "UNPAID"));
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
        if (!(priceValue instanceof Number priceNumber) || !Double.isFinite(priceNumber.doubleValue()) || priceNumber.doubleValue() < 0) {
            throw new InvalidPlanException("Plan price must be a finite non-negative number.");
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
        return item;
    }
    private Map<String, Object> usageJson(Usage usage) {
        Map<String, Object> item = new LinkedHashMap<>(); item.put("used", usage.getDataUsed()); item.put("history", usage.getHistory()); return item;
    }
    private Map<String, Object> invoiceJson(Invoice invoice) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", invoice.getId()); item.put("customerId", invoice.getCustomerId()); item.put("month", invoice.getMonth());
        item.put("amount", invoice.getAmount()); item.put("gst", invoice.getTax()); item.put("total", invoice.getTotal());
        item.put("status", invoice.getStatus()); item.put("dueDate", invoice.getDueDate()); item.put("summary", invoice.getSummary());
        return item;
    }
    private Map<String, Object> complaintJson(Complaint complaint) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", complaint.getComplaintId()); item.put("customerId", Integer.parseInt(complaint.getCustomerId()));
        item.put("type", complaint.getComplaintType()); item.put("description", complaint.getDescription());
        item.put("status", complaint.getStatus()); item.put("created", LocalDate.now().toString());
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
