/*
 * Edit this file to tailor the standalone frontend demo.
 * These browser defaults are separate from and do not replace the Java backend.
 */
window.NETWISE_CONFIG = {
  brand: { name: "Netwise", tagline: "INTERNET SERVICES" },
  locale: "en-IN",
  currency: "INR",
  taxRate: 0.18,
  billingDueDay: 25,
  support: { label: "Support & complaints", message: "Our support team is here to help." },
  serviceStatus: "All systems operational",
  portalLabel: "Demo portal",
  seed: {
    customers: [
      { id: 101, name: "Rahul Sharma", email: "rahul@gmail.com", phone: "9876543210" },
      { id: 102, name: "Priya Mehta", email: "priya@gmail.com", phone: "9876501234" }
    ],
    plans: [
      { id: 1, name: "Basic 100", tier: "Basic", price: 499, data: 100, speed: "50 Mbps", description: "Reliable everyday browsing", featured: false },
      { id: 2, name: "Home Plus", tier: "Standard", price: 699, data: 300, speed: "100 Mbps", description: "Streaming and work from home", featured: true },
      { id: 3, name: "Premium 500", tier: "Premium", price: 799, data: 500, speed: "200 Mbps", description: "Built for busy households", featured: false }
    ],
    subscriptions: [
      { id: "SUB-001", customerId: 101, planId: 2, status: "ACTIVE", startDate: "2026-09-01", daysRemaining: 17 },
      { id: "SUB-002", customerId: 102, planId: 3, status: "ACTIVE", startDate: "2026-09-12", daysRemaining: 4 }
    ],
    usage: { 101: { used: 132.4, history: [24, 38, 29, 41.4] }, 102: { used: 291.7, history: [64, 81, 70, 76.7] } },
    bills: [
      { id: "INV-2026-1042", customerId: 101, month: "October 2026", amount: 699, gst: 125.82, total: 824.82, status: "UNPAID", dueDate: "2026-10-25" },
      { id: "INV-2026-1031", customerId: 101, month: "September 2026", amount: 699, gst: 125.82, total: 824.82, status: "PAID", dueDate: "2026-09-25" },
      { id: "INV-2026-1043", customerId: 102, month: "October 2026", amount: 799, gst: 143.82, total: 942.82, status: "UNPAID", dueDate: "2026-10-25" }
    ],
    complaints: [
      { id: "CMP101", customerId: 101, type: "Slow Internet", description: "Internet speed is very slow.", status: "IN_PROGRESS", created: "2026-10-12" },
      { id: "CMP102", customerId: 102, type: "Connection Issue", description: "Internet connection is intermittent.", status: "PENDING", created: "2026-10-13" }
    ]
  }
};
