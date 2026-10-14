/*
 * Netwise ISP frontend.
 * Current mode: standalone demo using localStorage.
 * The Java classes currently run as a console app, not an HTTP API.
 * Replace the data methods in this file with fetch() calls after an API layer is added.
 */
const STORAGE_KEY = "netwise-isp-demo-v1";
const seed = {
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
};
let state = loadState();
let currentPage = "dashboard";
let role = "customer";
let activeCustomerId = 101;
let searchQuery = "";

function loadState() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : structuredClone(seed);
  } catch (e) {
    return JSON.parse(JSON.stringify(seed));
  }
}
function saveState() { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); }
function customer() { return state.customers.find(c => c.id === activeCustomerId) || state.customers[0]; }
function planById(id) { return state.plans.find(p => p.id === Number(id)); }
function subscriptionFor(id) { return state.subscriptions.find(s => s.customerId === Number(id) && s.status === "ACTIVE"); }
function currentPlan(id = activeCustomerId) { const s = subscriptionFor(id); return s ? planById(s.planId) : null; }
function currentUsage(id = activeCustomerId) { return state.usage[id] || { used: 0, history: [0,0,0,0] }; }
function money(n) { return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(n); }
function shortMoney(n) { return "₹" + Number(n).toLocaleString("en-IN", { maximumFractionDigits: 0 }); }
function initials(name) { return String(name || "N").split(/\s+/).map(x => x[0]).slice(0,2).join("").toUpperCase(); }
function esc(value) { return String(value ?? "").replace(/[&<>"']/g, c => ({ "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;" }[c])); }
function statusBadge(status) {
  const s = String(status || "").toUpperCase();
  const cls = ["ACTIVE","PAID","RESOLVED","CLOSED"].includes(s) ? "green" :
    ["PENDING","UNPAID","IN_PROGRESS"].includes(s) ? "amber" : "red";
  const label = s === "IN_PROGRESS" ? "In progress" : s.charAt(0) + s.slice(1).toLowerCase();
  return `<span class="badge badge-${cls}">${esc(label)}</span>`;
}
function pageHeading(eyebrow, title, subtitle, actions = "") {
  return `<div class="page-heading"><div><p class="eyebrow">${eyebrow}</p><h1>${title}</h1><p class="subtitle">${subtitle}</p></div><div class="heading-actions">${actions}</div></div>`;
}
function stat(label, value, foot, icon) {
  return `<article class="stat-card"><div class="stat-top"><span class="stat-label">${label}</span><span class="stat-icon">${icon}</span></div><div class="stat-value">${value}</div><div class="stat-foot">${foot}</div></article>`;
}
function panelTitle(title, sub, action = "") {
  return `<div class="panel-heading"><div><h2>${title}</h2><p>${sub}</p></div>${action}</div>`;
}
function table(headers, rows, emptyText = "Nothing to show yet.") {
  return `<div class="table-wrap"><table><thead><tr>${headers.map(h=>`<th>${h}</th>`).join("")}</tr></thead><tbody>${rows || `<tr><td colspan="${headers.length}"><div class="empty-state">${emptyText}</div></td></tr>`}</tbody></table></div>`;
}
function customerOptions(selected = activeCustomerId) {
  return state.customers.map(c => `<option value="${c.id}" ${c.id === Number(selected) ? "selected" : ""}>${esc(c.name)} · #${c.id}</option>`).join("");
}
function planOptions(selected) {
  return state.plans.map(p => `<option value="${p.id}" ${p.id === Number(selected) ? "selected" : ""}>${esc(p.name)} · ${money(p.price)}/mo</option>`).join("");
}
function getBills(id = activeCustomerId) { return state.bills.filter(b => role === "admin" || b.customerId === Number(id)); }
function getComplaints(id = activeCustomerId) { return state.complaints.filter(c => role === "admin" || c.customerId === Number(id)); }

function render() {
  const root = document.getElementById("page-root");
  const c = customer();
  document.getElementById("breadcrumb-current").textContent = ({
    dashboard: "Dashboard", plans: "Internet plans", usage: "Data usage", billing: "Billing", complaints: "Support & complaints"
  })[currentPage] || "Dashboard";
  document.getElementById("sidebar-name").textContent = role === "admin" ? "Portal Administrator" : c.name;
  document.getElementById("sidebar-role").textContent = role === "admin" ? "Administrator demo" : `Customer ID #${c.id}`;
  document.getElementById("top-avatar").textContent = role === "admin" ? "AD" : initials(c.name);
  document.getElementById("complaint-count").textContent = getComplaints().filter(x => x.status !== "RESOLVED" && x.status !== "CLOSED").length;
  document.getElementById("role-select").value = role;
  document.querySelectorAll(".nav-item").forEach(btn => btn.classList.toggle("active", btn.dataset.page === currentPage));
  const renderers = { dashboard: renderDashboard, plans: renderPlans, usage: renderUsage, billing: renderBilling, complaints: renderComplaints };
  root.innerHTML = renderers[currentPage]();
  if (currentPage === "dashboard") updateDonut();
}
function updateDonut() {
  const p = currentPlan();
  const u = currentUsage();
  const percent = p ? Math.min(100, (u.used / p.data) * 100) : 0;
  const donut = document.querySelector(".donut");
  if (donut) donut.style.background = `conic-gradient(var(--blue) 0 ${percent}%, #edf0ff ${percent}% 100%)`;
  const val = document.getElementById("donut-percent");
  if (val) val.textContent = `${Math.round(percent)}%`;
}
function renderDashboard() {
  const c = customer(), p = currentPlan(), u = currentUsage(), b = getBills(c.id).find(x => x.status === "UNPAID");
  const remaining = p ? Math.max(0, p.data - u.used) : 0;
  const percent = p ? Math.min(100, u.used / p.data * 100) : 0;
  const activeComplaints = getComplaints(c.id).filter(x => !["RESOLVED","CLOSED"].includes(x.status)).length;
  const actions = role === "admin" ? `<button class="button button-quiet" data-action="export">Export report ↓</button><button class="button button-primary" data-action="add-customer">+ Add customer</button>` : `<button class="button button-quiet" data-page="plans">Browse plans</button><button class="button button-primary" data-action="raise-complaint">+ Raise complaint</button>`;
  let html = pageHeading(role === "admin" ? "ADMIN OVERVIEW" : "YOUR ACCOUNT", role === "admin" ? "ISP overview" : `Good evening, ${esc(c.name.split(" ")[0])} 👋`, role === "admin" ? "Monitor customers, subscriptions, billing, and support activity." : "Here’s what’s happening with your internet connection today.", actions);
  if (role === "admin") {
    html += `<div class="grid stats-grid">${stat("Total customers", state.customers.length, `<span class="positive">●</span> Registered accounts`, "♙")}${stat("Active subscriptions", state.subscriptions.filter(s=>s.status==="ACTIVE").length, "Across all customers", "◈")}${stat("Outstanding bills", money(state.bills.filter(x=>x.status==="UNPAID").reduce((a,x)=>a+x.total,0)), "Awaiting payment", "▤")}${stat("Open complaints", state.complaints.filter(x=>!["RESOLVED","CLOSED"].includes(x.status)).length, "Need attention", "◎")}</div>`;
    html += `<section class="panel table-panel">${panelTitle("Customer directory", "All registered customer accounts", `<button class="link-button" data-action="add-customer">+ Add customer</button>`)}${table(["Customer","Email","Phone","Active plan","Status"], state.customers.map(cu=>{const s=subscriptionFor(cu.id),pl=s&&planById(s.planId);return `<tr><td><div class="table-primary"><div class="avatar">${initials(cu.name)}</div><div><strong>${esc(cu.name)}</strong><small>ID #${cu.id}</small></div></div></td><td>${esc(cu.email)}</td><td>${esc(cu.phone)}</td><td>${pl?esc(pl.name):"No active plan"}</td><td>${statusBadge(s?s.status:"CANCELLED")}</td></tr>`}).join(""))}</section>`;
    html += `<section class="panel table-panel">${panelTitle("Latest support tickets", "Recent customer complaints", `<button class="link-button" data-page="complaints">View all →</button>`)}${table(["Ticket","Customer","Issue","Status"],state.complaints.slice().reverse().slice(0,4).map(x=>{const cu=state.customers.find(cu=>cu.id===Number(x.customerId));return `<tr><td><strong>${esc(x.id)}</strong></td><td>${esc(cu?cu.name:"Unknown")}</td><td>${esc(x.type)}</td><td>${statusBadge(x.status)}</td></tr>`}).join(""))}</section>`;
    return html;
  }
  html += `<div class="grid stats-grid">${stat("Current plan", p ? esc(p.name) : "No plan", p ? `${p.data} GB data allowance` : "Choose a plan to get started", "◈")}${stat("Data remaining", `${remaining.toFixed(1)} <span style="font-size:13px;font-weight:500">GB</span>`, `<span class="${percent > 85 ? "" : "positive"}">${percent.toFixed(0)}% used</span> this billing cycle`, "◔")}${stat("Next bill", b ? money(b.total) : "₹0", b ? `Due ${new Date(b.dueDate+"T00:00:00").toLocaleDateString("en-IN",{day:"numeric",month:"short"})}` : "No pending payment", "▤")}${stat("Open tickets", activeComplaints, activeComplaints ? "Support team is tracking them" : "No unresolved complaints", "◎")}</div>`;
  html += `<div class="grid dashboard-grid"><section class="panel">${panelTitle("Data usage", "Your current billing cycle", `<button class="link-button" data-page="usage">View details →</button>`)}<div class="usage-summary"><div class="donut"><div class="donut-content"><strong id="donut-percent">${Math.round(percent)}%</strong><small>data used</small></div></div><div class="usage-details"><div class="usage-number"><span>Used</span><strong>${u.used.toFixed(1)} GB</strong></div><div class="progress"><span style="width:${percent}%"></span></div><div class="usage-number" style="margin-top:17px"><span>Available</span><strong>${remaining.toFixed(1)} GB</strong></div><div class="progress green"><span style="width:${p?remaining/p.data*100:0}%"></span></div><div class="legend"><span><i></i> Data used</span><span><i class="light"></i> Available</span></div></div></div></section>`;
  html += `<section class="panel plan-highlight"><p class="eyebrow">YOUR SUBSCRIPTION</p><h2>${p ? esc(p.name) : "No active plan"}</h2><p class="plan-subtitle">${p ? esc(p.description) : "Pick a plan to get connected."}</p><div class="plan-price">${p ? money(p.price) : "—"}<small> / month</small></div><div class="plan-features">${p ? `<span class="plan-feature">↗ ${esc(p.speed)}</span><span class="plan-feature">◔ ${p.data} GB</span>` : ""}<span class="plan-feature">✓ ${p ? "Active" : "Not subscribed"}</span></div><button class="button" data-page="plans">${p ? "Manage plan →" : "Explore plans →"}</button></section></div>`;
  const recent = getBills(c.id).slice().sort((a,b)=>b.id.localeCompare(a.id)).slice(0,3);
  html += `<section class="panel table-panel">${panelTitle("Recent invoices", "Your latest billing activity", `<button class="link-button" data-page="billing">View all invoices →</button>`)}${table(["Invoice","Billing period","Amount","Due date","Status"],recent.map(x=>`<tr><td><div class="table-primary"><span class="mini-icon">▤</span><div><strong>${esc(x.id)}</strong><small>Internet service</small></div></div></td><td>${esc(x.month)}</td><td><strong>${money(x.total)}</strong></td><td>${new Date(x.dueDate+"T00:00:00").toLocaleDateString("en-IN",{day:"numeric",month:"short",year:"numeric"})}</td><td>${statusBadge(x.status)}</td></tr>`).join(""))}</section>`;
  return html;
}
function renderPlans() {
  const c = customer(), active = currentPlan();
  let html = pageHeading("CONNECTIVITY", "Plans built for your home", "Choose a package that fits how you work, stream, and play.");
  html += `<div class="grid section-grid">${state.plans.map(p=>`<article class="plan-card ${p.featured?"featured":""}">${p.featured?`<span class="featured-label">MOST POPULAR</span>`:""}<div class="plan-symbol">${p.tier==="Premium"?"✦":p.tier==="Basic"?"⌂":"◈"}</div><h3>${esc(p.name)}</h3><p class="muted">${esc(p.description)}</p><div class="big-price">${money(p.price)}<small> / month</small></div><p class="muted">Billed monthly · 18% GST applies</p><ul><li>${esc(p.speed)} download speed</li><li>${p.data} GB monthly data</li><li>24/7 customer support</li><li>Secure home connection</li></ul>${active&&active.id===p.id?`<button class="button button-quiet" disabled>✓ Current plan</button>`:`<button class="button ${p.featured?"button-primary":"button-quiet"}" data-action="subscribe" data-plan="${p.id}">${active?"Switch to this plan":"Choose this plan"}</button>`}</article>`).join("")}</div>`;
  if (active) html += `<p class="subtitle" style="margin-top:18px">Your current subscription is <strong>${esc(active.name)}</strong>. Changing plans updates the demo subscription immediately.</p>`;
  return html;
}
function renderUsage() {
  const p = currentPlan(), u = currentUsage(), used = u.used, limit = p ? p.data : 0, remaining = Math.max(0,limit-used), percent = limit ? Math.min(100,used/limit*100) : 0;
  const months = ["Jul","Aug","Sep","Oct"], hist = u.history || [0,0,0,0], max = Math.max(1,...hist);
  let html = pageHeading("USAGE MONITOR", "Your data, at a glance", "Track your consumption and keep your connection running smoothly.");
  html += `<section class="panel usage-big-card"><div class="usage-overview"><div class="usage-total"><p>DATA USED THIS CYCLE</p><strong>${used.toFixed(1)} <small>GB / ${limit} GB</small></strong><p>${remaining.toFixed(1)} GB remaining in your current plan</p></div><div style="min-width:220px;flex:1;max-width:430px"><div class="usage-number"><span>Monthly allowance used</span><strong>${percent.toFixed(0)}%</strong></div><div class="progress"><span style="width:${percent}%"></span></div><p class="subtitle" style="margin-top:10px">${percent>85?"You are close to your data allowance.":"Your usage is within your monthly allowance."}</p></div></div></section>`;
  html += `<section class="panel" style="margin-top:18px">${panelTitle("Monthly usage history", "Illustrative usage totals by month")}<div class="usage-bars">${hist.map((v,i)=>`<div class="usage-bar-row"><span class="bar-label">${months[i]||"Month "+(i+1)}</span><div class="progress"><span style="width:${v/max*100}%"></span></div><span class="bar-value">${Number(v).toFixed(1)} GB</span></div>`).join("")}</div><p class="subtitle" style="margin-top:20px">Usage figures are sample data for the mini project. A production system would read usage records from the backend.</p></section>`;
  html += `<section class="panel" style="margin-top:18px">${panelTitle("Test usage entry", "Simulate new data usage for this demo account")}<form id="usage-form" class="form-grid"><div class="form-field"><label for="usage-gb">Add usage (GB)</label><input id="usage-gb" name="gb" type="number" min="0.1" step="0.1" required placeholder="e.g. 2.5"></div><div class="form-field" style="align-self:end"><button class="button button-primary" type="submit">Add usage</button></div></form></section>`;
  return html;
}
function renderBilling() {
  const bills = getBills();
  const outstanding = bills.filter(b=>b.status==="UNPAID").reduce((a,b)=>a+b.total,0);
  let html = pageHeading("BILLING CENTRE", "Bills & payments", "Review invoices and keep your account up to date.", `<button class="button button-quiet" data-action="download-bills">Download statement ↓</button>`);
  html += `<div class="bill-summary"><div class="bill-amount-card"><p>OUTSTANDING BALANCE</p><strong>${money(outstanding)}</strong><small>${bills.filter(b=>b.status==="UNPAID").length} unpaid invoice(s)</small></div><section class="panel bill-details"><div class="panel-heading"><div><h2>Current invoice</h2><p>Monthly internet subscription</p></div>${bills.some(b=>b.status==="UNPAID")?statusBadge("UNPAID"):statusBadge("PAID")}</div>${(()=>{const b=bills.find(x=>x.status==="UNPAID")||bills[0];return b?`<div class="bill-line"><span>Plan charge</span><strong>${money(b.amount)}</strong></div><div class="bill-line"><span>GST (18%)</span><strong>${money(b.gst)}</strong></div><div class="bill-line"><span>Invoice</span><strong>${esc(b.id)}</strong></div><div class="bill-line total"><span>Total due</span><strong>${money(b.total)}</strong></div>`:`<div class="empty-state">No invoices yet.</div>`})()}</section></div>`;
  html += `<section class="panel table-panel">${panelTitle("Invoice history", "Invoices associated with your account")}${table(["Invoice","Period","Plan charge","GST","Total","Due date","Status","Action"],bills.map(b=>`<tr><td><strong>${esc(b.id)}</strong></td><td>${esc(b.month)}</td><td>${money(b.amount)}</td><td>${money(b.gst)}</td><td><strong>${money(b.total)}</strong></td><td>${new Date(b.dueDate+"T00:00:00").toLocaleDateString("en-IN",{day:"numeric",month:"short"})}</td><td>${statusBadge(b.status)}</td><td>${b.status==="UNPAID"?`<button class="button button-primary button-small" data-action="pay" data-bill="${esc(b.id)}">Mark paid</button>`:`<button class="button button-quiet button-small" data-action="invoice" data-bill="${esc(b.id)}">Receipt</button>`}</td></tr>`).join(""))}</section>`;
  html += `<p class="subtitle" style="margin-top:13px">Demo only: “Mark paid” changes the local invoice status. It does not process an actual payment.</p>`;
  return html;
}
function renderComplaints() {
  const complaints = getComplaints();
  const rows = complaints.filter(x=>`${x.id} ${x.type} ${x.description} ${x.status}`.toLowerCase().includes(searchQuery.toLowerCase())).map(x=>{
    const cu = state.customers.find(c=>c.id===Number(x.customerId));
    return `<tr><td><strong>${esc(x.id)}</strong><small style="display:block;color:#9aa3b4;margin-top:4px">${esc(x.created)}</small></td>${role==="admin"?`<td>${esc(cu?cu.name:"Unknown")}</td>`:""}<td><div class="table-primary"><span class="mini-icon">◎</span><div><strong>${esc(x.type)}</strong><small>${esc(x.description)}</small></div></div></td><td>${statusBadge(x.status)}</td><td>${role==="admin"?`<select class="filter-select" data-action="update-complaint" data-complaint="${esc(x.id)}"><option ${x.status==="PENDING"?"selected":""}>PENDING</option><option ${x.status==="IN_PROGRESS"?"selected":""}>IN_PROGRESS</option><option ${x.status==="RESOLVED"?"selected":""}>RESOLVED</option><option ${x.status==="CLOSED"?"selected":""}>CLOSED</option></select>`:`<button class="button button-quiet button-small" data-action="complaint-details" data-complaint="${esc(x.id)}">View details</button>`}</td></tr>`;
  }).join("");
  let html = pageHeading("CUSTOMER SUPPORT", "Support & complaints", "Raise a ticket and follow its progress until it is resolved.", `<button class="button button-primary" data-action="raise-complaint">+ New complaint</button>`);
  html += `<div class="toolbar"><label class="search-box"><span>⌕</span><input id="complaint-search" type="search" placeholder="Search complaints..." value="${esc(searchQuery)}"></label><span class="subtitle">${complaints.length} ticket(s)</span></div>`;
  html += `<section class="panel table-panel" style="margin-top:0">${panelTitle("Your support tickets", role==="admin"?"Review and update all customer complaints":"Track the status of your requests")}${table(role==="admin"?["Ticket","Customer","Issue","Status","Update status"]:["Ticket","Issue","Status","Action"],rows,"No complaints match your search.")}</section>`;
  return html;
}

function openModal(title, eyebrow, body, onSubmit, submitLabel = "Save") {
  const modal = document.getElementById("modal");
  document.getElementById("modal-title").textContent = title;
  document.getElementById("modal-eyebrow").textContent = eyebrow;
  document.getElementById("modal-body").innerHTML = body;
  document.getElementById("modal-submit").textContent = submitLabel;
  const form = document.getElementById("modal-form");
  form.onsubmit = (event) => {
    if (event.submitter && event.submitter.value === "cancel") return;
    event.preventDefault();
    const result = onSubmit(new FormData(form));
    if (result !== false) modal.close();
  };
  modal.showModal();
}
function raiseComplaint() {
  const body = `<div class="form-field"><label for="complaint-type">Issue type</label><select id="complaint-type" name="type" required><option value="">Choose an issue</option><option>Slow Internet</option><option>Connection Issue</option><option>Billing Query</option><option>Plan Change</option><option>Other</option></select></div><div class="form-field"><label for="complaint-description">Describe the issue</label><textarea id="complaint-description" name="description" required minlength="8" placeholder="Tell us what is happening..."></textarea></div>${role==="admin"?`<div class="form-field"><label for="complaint-customer">Customer</label><select id="complaint-customer" name="customerId" required>${customerOptions()}</select></div>`:""}`;
  openModal("Raise a complaint", "CUSTOMER SUPPORT", body, fd => {
    const type = String(fd.get("type")||"").trim(), description = String(fd.get("description")||"").trim();
    const customerId = Number(role==="admin" ? fd.get("customerId") : activeCustomerId);
    if (!type || description.length < 8) { toast("Choose an issue and enter at least 8 characters.", true); return false; }
    const id = "CMP" + String(Math.max(100, ...state.complaints.map(x=>Number(String(x.id).replace(/\D/g,""))||0)) + 1);
    state.complaints.push({ id, customerId, type, description, status: "PENDING", created: new Date().toISOString().slice(0,10) });
    saveState(); render(); toast(`Complaint ${id} registered successfully.`); return true;
  }, "Submit complaint");
}
function addCustomer() {
  const body = `<div class="form-grid"><div class="form-field"><label for="cust-name">Full name</label><input id="cust-name" name="name" required></div><div class="form-field"><label for="cust-phone">Phone number</label><input id="cust-phone" name="phone" required pattern="[0-9+() -]{8,16}"></div><div class="form-field full"><label for="cust-email">Email address</label><input id="cust-email" name="email" type="email" required></div></div>`;
  openModal("Add customer", "ADMIN · CUSTOMER MANAGEMENT", body, fd => {
    const name = String(fd.get("name")).trim(), email = String(fd.get("email")).trim(), phone = String(fd.get("phone")).trim();
    if (!name || !email || !phone) { toast("Complete all fields.", true); return false; }
    const id = Math.max(100, ...state.customers.map(x=>x.id)) + 1;
    state.customers.push({id,name,email,phone}); state.usage[id] = {used:0,history:[0,0,0,0]};
    saveState(); render(); toast(`Customer #${id} added.`); return true;
  });
}
function subscribe(planId) {
  const p = planById(planId);
  if (!p) return;
  const body = `<p class="subtitle" style="margin-bottom:16px">You selected <strong>${esc(p.name)}</strong> at <strong>${money(p.price)}/month</strong> before GST.</p><div class="bill-line"><span>Monthly plan fee</span><strong>${money(p.price)}</strong></div><div class="bill-line"><span>GST (18%)</span><strong>${money(p.price*.18)}</strong></div><div class="bill-line total"><span>Monthly total</span><strong>${money(p.price*1.18)}</strong></div><p class="subtitle" style="margin-top:14px">This updates only the local demo account. No real service will be activated.</p>`;
  openModal("Confirm plan", "SUBSCRIPTION", body, () => {
    const existing = state.subscriptions.find(s=>s.customerId===activeCustomerId && s.status==="ACTIVE");
    if (existing) { existing.planId = p.id; existing.daysRemaining = 30; }
    else state.subscriptions.push({id:"SUB-"+String(state.subscriptions.length+1).padStart(3,"0"),customerId:activeCustomerId,planId:p.id,status:"ACTIVE",startDate:new Date().toISOString().slice(0,10),daysRemaining:30});
    if (!state.bills.some(b=>b.customerId===activeCustomerId && b.month==="October 2026" && b.status==="UNPAID")) {
      const amount = p.price;
      state.bills.unshift({id:"INV-"+Date.now().toString().slice(-6),customerId:activeCustomerId,month:"October 2026",amount,gst:amount*.18,total:amount*1.18,status:"UNPAID",dueDate:"2026-10-25"});
    }
    saveState(); render(); toast(`${p.name} is now your active demo plan.`); return true;
  }, "Confirm plan");
}
function markPaid(id) {
  const bill = state.bills.find(b=>b.id===id);
  if (!bill) return;
  bill.status = "PAID"; saveState(); render(); toast(`${id} marked as paid in the demo.`);
}
function downloadText(filename, content, type="text/plain") {
  const url = URL.createObjectURL(new Blob([content], {type}));
  const a = document.createElement("a"); a.href=url; a.download=filename; a.click(); URL.revokeObjectURL(url);
}
function downloadBills() {
  const lines = [["Invoice","Customer ID","Period","Plan Charge","GST","Total","Due Date","Status"], ...getBills().map(b=>[b.id,b.customerId,b.month,b.amount,b.gst,b.total,b.dueDate,b.status])];
  const csv = lines.map(row=>row.map(v=>`"${String(v).replace(/"/g,'""')}"`).join(",")).join("\r\n");
  downloadText("netwise-invoices.csv",csv,"text/csv;charset=utf-8"); toast("Invoice statement exported.");
}
function toast(message, error=false) {
  const el = document.createElement("div"); el.className = "toast" + (error?" error":""); el.textContent = message;
  document.getElementById("toast-region").appendChild(el); setTimeout(()=>el.remove(),3200);
}
function exportReport() {
  const report = { generatedAt: new Date().toISOString(), customers: state.customers, plans: state.plans, subscriptions: state.subscriptions, bills: state.bills, complaints: state.complaints };
  downloadText("netwise-admin-report.json", JSON.stringify(report,null,2), "application/json"); toast("Admin report exported.");
}
function handleAction(action, el) {
  switch(action) {
    case "raise-complaint": raiseComplaint(); break;
    case "add-customer": addCustomer(); break;
    case "subscribe": subscribe(Number(el.dataset.plan)); break;
    case "pay": markPaid(el.dataset.bill); break;
    case "download-bills": downloadBills(); break;
    case "export": exportReport(); break;
    case "invoice": {
      const b=state.bills.find(x=>x.id===el.dataset.bill);
      if(b) downloadText(`${b.id}-receipt.txt`, `NETWISE INTERNET SERVICES\nReceipt: ${b.id}\nPeriod: ${b.month}\nPlan charge: ${money(b.amount)}\nGST: ${money(b.gst)}\nTotal: ${money(b.total)}\nStatus: ${b.status}\nDue date: ${b.dueDate}\n\nDemo receipt; not a tax invoice.`);
      toast("Receipt downloaded."); break;
    }
    case "complaint-details": {
      const c=state.complaints.find(x=>x.id===el.dataset.complaint);
      if(c) openModal(c.id,"SUPPORT TICKET",`<div class="bill-line"><span>Issue type</span><strong>${esc(c.type)}</strong></div><div class="bill-line"><span>Status</span><strong>${esc(c.status)}</strong></div><div class="form-field" style="margin-top:16px"><label>Description</label><p class="subtitle">${esc(c.description)}</p></div><p class="subtitle">Created ${esc(c.created)}. This is a demo ticket.</p>`,()=>true,"Close");
      break;
    }
  }
}
document.addEventListener("click", event => {
  const pageBtn = event.target.closest("[data-page]");
  if (pageBtn) { currentPage = pageBtn.dataset.page; searchQuery=""; render(); document.getElementById("sidebar").classList.remove("open"); return; }
  const actionBtn = event.target.closest("[data-action]");
  if (actionBtn && actionBtn.tagName !== "SELECT") handleAction(actionBtn.dataset.action, actionBtn);
});
document.addEventListener("change", event => {
  if (event.target.id === "role-select") {
    role = event.target.value;
    if (role === "admin") activeCustomerId = state.customers[0]?.id || 101;
    render(); return;
  }
  if (event.target.matches('[data-action="update-complaint"]')) {
    const c = state.complaints.find(x=>x.id===event.target.dataset.complaint);
    if (c) { c.status=event.target.value; saveState(); render(); toast(`${c.id} status updated.`); }
  }
});
document.addEventListener("input", event => {
  if (event.target.id === "complaint-search") {
    const pos = event.target.selectionStart; searchQuery = event.target.value;
    const root = document.getElementById("page-root"); root.innerHTML = renderComplaints();
    const input = document.getElementById("complaint-search"); input.focus(); input.setSelectionRange(pos,pos);
  }
});
document.addEventListener("submit", event => {
  if (event.target.id === "usage-form") {
    event.preventDefault();
    const gb = Number(new FormData(event.target).get("gb"));
    if (!Number.isFinite(gb) || gb <= 0) { toast("Enter a usage value greater than zero.", true); return; }
    const u = currentUsage(); u.used += gb;
    u.history[u.history.length-1] += gb; state.usage[activeCustomerId] = u;
    const p=currentPlan();
    if (p && u.used > p.data) toast("Usage added. You have exceeded your plan allowance.", true);
    else toast(`${gb.toFixed(1)} GB added to demo usage.`);
    saveState(); render();
  }
});
document.getElementById("mobile-menu").addEventListener("click",()=>document.getElementById("sidebar").classList.toggle("open"));
document.getElementById("notifications-button").addEventListener("click",()=>toast("You are all caught up."));
document.getElementById("profile-menu").addEventListener("click",()=>toast("Use the View as selector to switch between customer and admin demo."));
document.addEventListener("keydown",event=>{if(event.key==="Escape")document.getElementById("sidebar").classList.remove("open");});
render();
