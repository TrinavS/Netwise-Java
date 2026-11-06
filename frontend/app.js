
/* Netwise ISP portal. All operational data is served by the Java API. */
const API = "/api";
const config = window.NETWISE_CONFIG || {};
let preferences = loadPreferences();
let state = { customers: [], plans: [], subscriptions: [], usage: {}, bills: [], complaints: [], complaintTypes: [] };
let currentPage = "dashboard";
let role = "customer";
let activeCustomerId = null;
let searchQuery = "";
let customerSearch = "";
let backendConnected = false;
let reportStatus = null;
function loadPreferences() {
  try { return { ...config, ...(JSON.parse(localStorage.getItem("netwise-portal-preferences-v1") || "{}")) }; }
  catch { return { ...config }; }
}
function savePreferences() { localStorage.setItem("netwise-portal-preferences-v1", JSON.stringify(preferences)); }
function taxRate() { return 0.18; }
async function api(path, method = "GET", body = undefined) {
  const options = { method, headers: {} };
  if (body !== undefined) { options.headers["Content-Type"] = "application/json"; options.body = JSON.stringify(body); }
  const response = await fetch(`${API}${path}`, options);
  let payload = {};
  try { payload = await response.json(); } catch { /* No content response. */ }
  if (!response.ok) throw new Error(payload.error || `Request failed (${response.status}).`);
  return payload;
}
async function refreshState() {
  state = await api("/state"); backendConnected = true;
  if (!state.customers.some(c => c.id === Number(activeCustomerId))) activeCustomerId = state.customers[0]?.id ?? null;
  render();
}
function customer() { return state.customers.find(c => c.id === Number(activeCustomerId)) || state.customers[0] || { id: "—", name: "ISP Customer", email: "", phone: "", address: "" }; }
function planById(id) { return state.plans.find(p => p.id === Number(id)); }
function subscriptionFor(id) { return state.subscriptions.find(s => s.customerId === Number(id) && s.status === "ACTIVE"); }
function currentPlan(id = activeCustomerId) { const s = subscriptionFor(id); return s ? planById(s.planId) : null; }
function currentUsage(id = activeCustomerId) { return state.usage?.[id] || state.usage?.[String(id)] || { used: 0, history: [0, 0, 0, 0] }; }
function money(n) { return new Intl.NumberFormat(preferences.locale || "en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(Number(n) || 0); }
function formatDate(value, options = { day: "numeric", month: "short" }) { return value ? new Intl.DateTimeFormat(preferences.locale || "en-IN", options).format(new Date(`${value}T00:00:00`)) : "—"; }
function initials(name) { return String(name || "N").split(/\s+/).map(x => x[0]).slice(0, 2).join("").toUpperCase(); }
function esc(value) { return String(value ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); }
function statusBadge(status) {
  const s = String(status || "").toUpperCase();
  const cls = ["ACTIVE", "PAID", "RESOLVED", "CLOSED", "COMPLETED"].includes(s) ? "green" : ["PENDING", "UNPAID", "IN_PROGRESS", "RUNNING", "QUEUED"].includes(s) ? "amber" : "red";
  const label = s === "IN_PROGRESS" ? "In progress" : s.charAt(0) + s.slice(1).toLowerCase();
  return `<span class="badge badge-${cls}">${esc(label)}</span>`;
}
function pageHeading(eyebrow, title, subtitle, actions = "") { return `<div class="page-heading"><div><p class="eyebrow">${eyebrow}</p><h1>${title}</h1><p class="subtitle">${subtitle}</p></div><div class="heading-actions">${actions}</div></div>`; }
function stat(label, value, foot, icon) { return `<article class="stat-card"><div class="stat-top"><span class="stat-label">${label}</span><span class="stat-icon">${icon}</span></div><div class="stat-value">${value}</div><div class="stat-foot">${foot}</div></article>`; }
function panelTitle(title, sub, action = "") { return `<div class="panel-heading"><div><h2>${title}</h2><p>${sub}</p></div>${action}</div>`; }
function table(headers, rows, emptyText = "Nothing to show yet.") { return `<div class="table-wrap"><table><thead><tr>${headers.map(h => `<th>${h}</th>`).join("")}</tr></thead><tbody>${rows || `<tr><td colspan="${headers.length}"><div class="empty-state">${emptyText}</div></td></tr>`}</tbody></table></div>`; }
function customerOptions(selected = activeCustomerId) { return state.customers.map(c => `<option value="${c.id}" ${c.id === Number(selected) ? "selected" : ""}>${esc(c.name)} · #${c.id}</option>`).join(""); }
function getBills(id = activeCustomerId) { return state.bills.filter(b => role === "admin" || b.customerId === Number(id)); }
function getComplaints(id = activeCustomerId) { return state.complaints.filter(c => role === "admin" || c.customerId === Number(id)); }
function render() {
  const root = document.getElementById("page-root"), c = customer();
  document.getElementById("breadcrumb-current").textContent = ({ dashboard: "Dashboard", plans: "Internet plans", usage: "Data usage", billing: "Billing", complaints: "Support & complaints" })[currentPage] || "Dashboard";
  document.getElementById("sidebar-name").textContent = role === "admin" ? "Portal Administrator" : c.name;
  document.getElementById("sidebar-role").textContent = role === "admin" ? "Administrator view" : `Customer ID #${c.id}`;
  document.getElementById("top-avatar").textContent = role === "admin" ? "AD" : initials(c.name);
  document.getElementById("complaint-count").textContent = getComplaints().filter(x => !["RESOLVED", "CLOSED"].includes(x.status)).length;
  document.getElementById("role-select").value = role;
  document.title = `${preferences.brand?.name || "Netwise"} ISP | Customer Portal`;
  document.getElementById("brand-name").textContent = (preferences.brand?.name || "Netwise").toLowerCase();
  document.getElementById("brand-tagline").textContent = preferences.brand?.tagline || "INTERNET SERVICES";
  document.getElementById("breadcrumb-brand").textContent = preferences.brand?.name || "Netwise";
  document.getElementById("support-copy").textContent = preferences.support?.message || "Our support team is here to help.";
  document.getElementById("service-status").textContent = backendConnected ? "Java backend connected" : "Connecting to Java backend";
  document.getElementById("portal-mode").textContent = backendConnected ? "Live project data" : "Waiting for backend";
  document.getElementById("footer-brand").textContent = `© ${new Date().getFullYear()} ${preferences.brand?.name || "Netwise"} Internet Services`;
  const select = document.getElementById("customer-select"); select.innerHTML = customerOptions(); select.value = String(activeCustomerId ?? "");
  document.getElementById("customer-switch-wrap").hidden = role === "admin" || state.customers.length < 2;
  document.querySelectorAll(".nav-item").forEach(btn => btn.classList.toggle("active", btn.dataset.page === currentPage));
  const renderers = { dashboard: renderDashboard, plans: renderPlans, usage: renderUsage, billing: renderBilling, complaints: renderComplaints };
  root.dataset.page = currentPage;
  root.innerHTML = renderers[currentPage](); if (currentPage === "dashboard") updateDonut();
}
function updateDonut() {
  const p = currentPlan(), u = currentUsage(), percent = p ? Math.min(100, u.used / p.data * 100) : 0;
  const donut = document.querySelector(".donut"); if (donut) donut.style.background = `conic-gradient(#356c4f 0 ${percent}%, #edf1e9 ${percent}% 100%)`;
  const val = document.getElementById("donut-percent"); if (val) val.textContent = `${Math.round(percent)}%`;
}
function renderDashboard() {
  const c = customer(), p = currentPlan(), u = currentUsage();
  const b = getBills(c.id).find(x => ["UNPAID", "OVERDUE"].includes(x.status));
  const activeSubscription = subscriptionFor(c.id);
  const pendingPlan = activeSubscription?.pendingPlanId ? planById(activeSubscription.pendingPlanId) : null;
  const remaining = p ? Math.max(0, p.data - u.used) : 0, percent = p ? Math.min(100, u.used / p.data * 100) : 0;
  const activeComplaints = getComplaints(c.id).filter(x => !["RESOLVED", "CLOSED"].includes(x.status)).length;
  const actions = role === "admin" ? `<button class="button button-quiet" data-action="export">Generate report</button><button class="button button-primary" data-action="add-customer">+ Add customer</button>` : `<button class="button button-quiet" data-page="plans">Browse plans</button><button class="button button-primary" data-action="raise-complaint">+ Raise complaint</button>`;
  let html = pageHeading(role === "admin" ? "ADMIN OVERVIEW" : "YOUR ACCOUNT", role === "admin" ? "ISP overview" : `Welcome back, ${esc(c.name.split(" ")[0])}`, role === "admin" ? "Manage customers, plans, billing, and support activity." : "Here’s what’s happening with your internet connection today.", actions);
  if (role === "admin" && reportStatus) html += `<section class="panel report-job"><div class="report-job-head"><div><strong>Admin report</strong><p>${esc(reportStatus.message || "Generating report…")}</p></div>${statusBadge(reportStatus.status)}${["QUEUED", "RUNNING"].includes(reportStatus.status) ? `<button class="button button-danger button-small" data-action="cancel-report">Cancel</button>` : ""}</div>${["QUEUED", "RUNNING"].includes(reportStatus.status) ? `<div class="progress"><span style="width:${Number(reportStatus.progress) || 0}%"></span></div>` : ""}</section>`;
  if (role === "admin") {
    html += `<div class="grid stats-grid">${stat("Total customers", state.customers.length, "Registered accounts", "♙")}${stat("Active subscriptions", state.subscriptions.filter(s => s.status === "ACTIVE").length, "Across all customers", "◈")}${stat("Outstanding bills", money(state.bills.filter(x => ["UNPAID", "OVERDUE"].includes(x.status)).reduce((a, x) => a + x.total, 0)), "Unpaid or overdue", "▤")}${stat("Open complaints", state.complaints.filter(x => !["RESOLVED", "CLOSED"].includes(x.status)).length, "Need attention", "◎")}</div>`;
    const matching = state.customers.filter(x => `${x.name} ${x.email} ${x.phone} ${x.id}`.toLowerCase().includes(customerSearch.toLowerCase()));
    const customerRows = matching.map(cu => {
      const s = subscriptionFor(cu.id), pl = s && planById(s.planId);
      return `<tr><td><div class="table-primary"><div class="avatar">${initials(cu.name)}</div><div><strong>${esc(cu.name)}</strong><small>ID #${cu.id}</small></div></div></td><td>${esc(cu.email)}<small class="cell-sub">${esc(cu.phone)}</small></td><td>${esc(cu.address || "—")}</td><td>${pl ? esc(pl.name) : "No active plan"}</td><td>${statusBadge(s ? s.status : "CANCELLED")}</td><td><div class="row-actions"><button class="button button-quiet button-small" data-action="edit-customer" data-customer="${cu.id}">Edit</button><button class="button button-danger button-small" data-action="remove-customer" data-customer="${cu.id}">Remove</button></div></td></tr>`;
    }).join("");
    html += `<section class="panel table-panel">${panelTitle("Customer directory", "Search, update, or remove a customer account", `<button class="link-button" data-action="add-customer">+ Add customer</button>`)}<div class="toolbar admin-search"><label class="search-box"><span>⌕</span><input id="customer-search" type="search" placeholder="Search name, email, phone or ID" value="${esc(customerSearch)}"></label></div>${table(["Customer", "Contact", "Address", "Active plan", "Status", "Actions"], customerRows, "No customers match your search.")}</section>`;
    const latest = state.complaints.slice().reverse().slice(0, 4).map(x => {
      const cu = state.customers.find(y => y.id === Number(x.customerId));
      return `<tr><td><strong>${esc(x.id)}</strong></td><td>${esc(cu ? cu.name : "Unknown")}</td><td>${esc(x.type)}</td><td>${statusBadge(x.status)}</td></tr>`;
    }).join("");
    html += `<section class="panel table-panel">${panelTitle("Latest support tickets", "Recent customer complaints", `<button class="link-button" data-page="complaints">View all →</button>`)}${table(["Ticket", "Customer", "Issue", "Status"], latest)}</section>`;
    return html;
  }
  html += `<div class="grid stats-grid">${stat("Current plan", p ? esc(p.name) : "No plan", p ? `${p.data} GB data allowance` : "Choose a plan to get started", "◈")}${stat("Data remaining", `${remaining.toFixed(1)} <span class="unit">GB</span>`, `<span class="positive">${percent.toFixed(0)}% used</span> this billing cycle`, "◔")}${stat("Next bill", b ? money(b.total) : money(0), b ? `Due ${formatDate(b.dueDate)}` : "No pending payment", "▤")}${stat("Open tickets", activeComplaints, activeComplaints ? "Support team is tracking them" : "No unresolved complaints", "◎")}</div>`;
  html += `<div class="grid dashboard-grid"><section class="panel">${panelTitle("Data usage", "Your current billing cycle", `<button class="link-button" data-page="usage">View details →</button>`)}<div class="usage-summary"><div class="donut"><div class="donut-content"><strong id="donut-percent">${Math.round(percent)}%</strong><small>data used</small></div></div><div class="usage-details"><div class="usage-number"><span>Used</span><strong>${u.used.toFixed(1)} GB</strong></div><div class="progress"><span style="width:${percent}%"></span></div><div class="usage-number usage-gap"><span>Available</span><strong>${remaining.toFixed(1)} GB</strong></div><div class="progress green"><span style="width:${p ? remaining / p.data * 100 : 0}%"></span></div><div class="legend"><span><i></i> Data used</span><span><i class="light"></i> Available</span></div></div></div></section>`;
  html += `<section class="panel plan-highlight"><p class="eyebrow">YOUR SUBSCRIPTION</p><h2>${p ? esc(p.name) : "No active plan"}</h2><p class="plan-subtitle">${p ? esc(p.description) : "Pick a plan to get connected."}</p>${pendingPlan ? `<p class="plan-change-notice">${esc(pendingPlan.name)} is scheduled to start ${formatDate(activeSubscription.changeEffectiveDate, { day: "numeric", month: "short", year: "numeric" })}. Your current invoice will remain unchanged.</p>` : ""}<div class="plan-price">${p ? money(p.price) : "—"}<small> / month</small></div><div class="plan-features">${p ? `<span class="plan-feature">↗ ${esc(p.speed)}</span><span class="plan-feature">◔ ${p.data} GB</span>` : ""}<span class="plan-feature">✓ ${p ? "Active" : "Not subscribed"}</span></div><button class="button" data-page="plans">${p ? "Manage plan →" : "Explore plans →"}</button></section></div>`;
  const recent = getBills(c.id).slice().sort((a, b) => b.id.localeCompare(a.id)).slice(0, 3);
  html += `<section class="panel table-panel">${panelTitle("Recent invoices", "Your latest billing activity", `<button class="link-button" data-page="billing">View all invoices →</button>`)}${table(["Invoice", "Billing period", "Amount", "Due date", "Status"], recent.map(x => `<tr><td><strong>${esc(x.id)}</strong></td><td>${esc(x.month)}</td><td><strong>${money(x.total)}</strong></td><td>${formatDate(x.dueDate, { day: "numeric", month: "short", year: "numeric" })}</td><td>${statusBadge(x.status)}</td></tr>`).join(""))}</section>`;
  return html;
}
function renderPlans() {
  const active = currentPlan(), activeSubscription = subscriptionFor(activeCustomerId);
  const pendingPlanId = activeSubscription?.pendingPlanId ? Number(activeSubscription.pendingPlanId) : null;
  const headingAction = role === "admin" ? `<button class="button button-primary" data-action="add-plan">+ Add internet plan</button>` : "";
  let html = pageHeading(role === "admin" ? "ADMIN · CATALOG" : "CONNECTIVITY", "Plans built for your home", role === "admin" ? "Create and maintain the available ISP packages." : "Choose a package that fits how you work, stream, and play.", headingAction);
  html += `<div class="grid section-grid">${state.plans.map(p => `<article class="plan-card ${p.featured ? "featured" : ""}">${p.featured ? `<span class="featured-label">MOST POPULAR</span>` : ""}<div class="plan-symbol">${p.tier === "Premium" ? "✦" : p.tier === "Basic" ? "⌂" : "◈"}</div><h3>${esc(p.name)}</h3><p class="muted">${esc(p.description)}</p><div class="big-price">${money(p.price)}<small> / month</small></div><p class="muted">Billed monthly · 18% GST applies</p><ul><li>${esc(p.speed)} download speed</li><li>${p.data} GB monthly data</li><li>24/7 customer support</li><li>Secure home connection</li></ul>${role === "admin" ? `<div class="row-actions"><button class="button button-quiet" data-action="edit-plan" data-plan="${p.id}">Edit plan</button><button class="button button-danger" data-action="remove-plan" data-plan="${p.id}">Remove</button></div>` : pendingPlanId === p.id ? `<button class="button button-quiet" disabled>Scheduled for next cycle</button>` : active && active.id === p.id ? pendingPlanId ? `<button class="button button-quiet" data-action="cancel-plan-change">Keep current plan</button>` : `<button class="button button-quiet" disabled>✓ Current plan</button>` : `<button class="button ${p.featured ? "button-primary" : "button-quiet"}" data-action="subscribe" data-plan="${p.id}">${active ? pendingPlanId ? "Change scheduled plan" : "Switch next cycle" : "Choose this plan"}</button>`}</article>`).join("")}</div>`;
  if (active) html += `<p class="subtitle section-note">Your current plan stays active through this billing period. A plan change takes effect on the first day of the next billing period. No partial-month charge is calculated.</p>`;
  return html;
}
function renderUsage() {
  const p = currentPlan(), u = currentUsage(), used = Number(u.used) || 0, limit = p ? p.data : 0;
  const remaining = Math.max(0, limit - used), percent = limit ? Math.min(100, used / limit * 100) : 0;
  const hist = u.history || [0, 0, 0, 0], monthCount = hist.length, currentMonth = new Date().getMonth();
  const months = hist.map((_, i) => new Intl.DateTimeFormat(preferences.locale || "en-IN", { month: "short" }).format(new Date(new Date().getFullYear(), currentMonth - monthCount + 1 + i, 1)));
  const max = Math.max(1, ...hist.map(Number));
  let html = pageHeading("USAGE MONITOR", "Your data, at a glance", "Track consumption and keep your connection running smoothly.");
  html += `<section class="panel usage-big-card"><div class="usage-overview"><div class="usage-total"><p>DATA USED THIS CYCLE</p><strong>${used.toFixed(1)} <small>GB / ${limit} GB</small></strong><p>${remaining.toFixed(1)} GB remaining in your current plan</p></div><div class="usage-meter"><div class="usage-number"><span>Monthly allowance used</span><strong>${percent.toFixed(0)}%</strong></div><div class="progress"><span style="width:${percent}%"></span></div><p class="subtitle usage-hint">${percent > 85 ? "You are close to your data allowance." : "Your usage is within your monthly allowance."}</p></div></div></section>`;
  html += `<section class="panel section-note">${panelTitle("Monthly usage history", "Usage records stored by the Java backend")}<div class="usage-bars">${hist.map((v, i) => `<div class="usage-bar-row"><span class="bar-label">${months[i] || `Month ${i + 1}`}</span><div class="progress"><span style="width:${Number(v) / max * 100}%"></span></div><span class="bar-value">${Number(v).toFixed(1)} GB</span></div>`).join("")}</div></section>`;
  html += `<section class="panel section-note">${panelTitle("Add usage record", p ? "Simulate usage for this customer" : "An active plan is required")}${p ? `<form id="usage-form" class="form-grid"><div class="form-field"><label for="usage-gb">Add usage (GB)</label><input id="usage-gb" name="gb" type="number" min="0.1" step="0.1" required placeholder="e.g. 2.5"></div><div class="form-field form-end"><button class="button button-primary" type="submit">Add usage</button></div></form>` : `<p class="subtitle">Choose an internet plan before adding data usage.</p><button class="button button-quiet" data-page="plans">Browse plans</button>`}</section>`;
  return html;
}
function renderBilling() {
  const bills = getBills();
  const openBills = bills.filter(b => ["UNPAID", "OVERDUE"].includes(b.status));
  const outstanding = openBills.reduce((a, b) => a + b.total, 0);
  const current = openBills[0] || bills[0];
  let html = pageHeading("BILLING CENTRE", "Bills & payments", "Issued invoices are preserved with their original plan and tax details.", `<button class="button button-quiet" data-action="download-bills">Download statement ↓</button>`);
  html += `<div class="bill-summary"><div class="bill-amount-card"><p>OUTSTANDING BALANCE</p><strong>${money(outstanding)}</strong><small>${openBills.length} unpaid or overdue invoice(s)</small></div><section class="panel bill-details"><div class="panel-heading"><div><h2>Current invoice</h2><p>${current ? esc(current.month) : "No issued invoice"}</p></div>${current ? statusBadge(current.status) : ""}</div>${current ? `<div class="bill-line"><span>${esc(current.planName || "Plan charge")}</span><strong>${money(current.planPrice ?? current.amount)}</strong></div><div class="bill-line"><span>Tax (${((current.taxRate ?? 0.18) * 100).toFixed(0)}%)</span><strong>${money(current.gst)}</strong></div><div class="bill-line"><span>Invoice</span><strong>${esc(current.id)}</strong></div><div class="bill-line"><span>Due date</span><strong>${formatDate(current.dueDate, { day: "numeric", month: "short", year: "numeric" })}</strong></div><div class="bill-line total"><span>Total due</span><strong>${money(current.total)}</strong></div>` : `<div class="empty-state">No invoices yet.</div>`}</section></div>`;
  const rows = bills.map(b => {
    const payments = (b.payments || []).map(payment => `<div class="payment-entry"><strong>${esc(payment.paymentId)}</strong><br>${formatDate(payment.paidOn, { day: "numeric", month: "short", year: "numeric" })} · ${money(payment.amount)} · ${esc(payment.method)}</div>`).join("") || `<span class="payment-empty">No payments recorded</span>`;
    const action = ["UNPAID", "OVERDUE"].includes(b.status)
      ? `<button class="button button-primary button-small" data-action="pay" data-bill="${esc(b.id)}">Simulate payment</button>`
      : `<button class="button button-quiet button-small" data-action="invoice" data-bill="${esc(b.id)}">Receipt</button>`;
    return `<tr><td><strong>${esc(b.id)}</strong></td>${role === "admin" ? `<td>${esc(b.customerName || `Customer #${b.customerId}`)}<small class="cell-sub">#${b.customerId}</small></td>` : ""}<td>${esc(b.month)}</td><td>${esc(b.planName || "Internet service")}<small class="cell-sub">Plan #${esc(b.planId ?? "—")}</small></td><td>${money(b.planPrice ?? b.amount)}</td><td>${money(b.gst)}<small class="cell-sub">${((b.taxRate ?? 0.18) * 100).toFixed(0)}% tax</small></td><td><strong>${money(b.total)}</strong></td><td>${formatDate(b.dueDate)}</td><td>${statusBadge(b.status)}</td><td>${payments}</td><td>${action}</td></tr>`;
  }).join("");
  const headers = role === "admin" ? ["Invoice", "Customer", "Billing period", "Plan", "Plan price", "Tax", "Total", "Due date", "Status", "Payment history", "Action"] : ["Invoice", "Billing period", "Plan", "Plan price", "Tax", "Total", "Due date", "Status", "Payment history", "Action"];
  html += `<section class="panel table-panel">${panelTitle("Invoice history", role === "admin" ? "All customer invoices and payment records" : "Invoices and payment records for your account")}${table(headers, rows)}</section>`;
  html += `<p class="subtitle section-note">Plan switches take effect next billing period; issued invoices are never recalculated. “Simulate payment” records a project payment only and does not contact a payment gateway.</p>`;
  return html;
}
function renderComplaints() {
  const complaints = getComplaints();
  const rows = complaints.filter(x => `${x.id} ${x.type} ${x.description} ${x.status}`.toLowerCase().includes(searchQuery.toLowerCase())).map(x => {
    const cu = state.customers.find(c => c.id === Number(x.customerId));
    return `<tr><td><strong>${esc(x.id)}</strong><small class="cell-sub">${esc(x.created)}</small></td>${role === "admin" ? `<td>${esc(cu ? cu.name : "Unknown")}</td>` : ""}<td><div class="table-primary"><span class="mini-icon">◎</span><div><strong>${esc(x.type)}</strong><small>${esc(x.description)}</small></div></div></td><td>${statusBadge(x.status)}</td><td>${role === "admin" ? `<select class="filter-select" data-action="update-complaint" data-complaint="${esc(x.id)}"><option ${x.status === "PENDING" ? "selected" : ""}>PENDING</option><option ${x.status === "IN_PROGRESS" ? "selected" : ""}>IN_PROGRESS</option><option ${x.status === "RESOLVED" ? "selected" : ""}>RESOLVED</option><option ${x.status === "CLOSED" ? "selected" : ""}>CLOSED</option></select>` : `<button class="button button-quiet button-small" data-action="complaint-details" data-complaint="${esc(x.id)}">View details</button>`}</td></tr>`;
  }).join("");
  let html = pageHeading("CUSTOMER SUPPORT", "Support & complaints", "Raise a ticket and follow its progress until it is resolved.", `<button class="button button-primary" data-action="raise-complaint">+ New complaint</button>`);
  html += `<div class="toolbar"><label class="search-box"><span>⌕</span><input id="complaint-search" type="search" placeholder="Search complaints..." value="${esc(searchQuery)}"></label><span class="subtitle">${complaints.length} ticket(s)</span></div>`;
  html += `<section class="panel table-panel section-no-top">${panelTitle("Support tickets", role === "admin" ? "Review and update all customer complaints" : "Track the status of your requests")}${table(role === "admin" ? ["Ticket", "Customer", "Issue", "Status", "Update status"] : ["Ticket", "Issue", "Status", "Action"], rows, "No complaints match your search.")}</section>`;
  return html;
}
function openModal(title, eyebrow, body, onSubmit, submitLabel = "Save") {
  const modal = document.getElementById("modal");
  document.getElementById("modal-title").textContent = title;
  document.getElementById("modal-eyebrow").textContent = eyebrow;
  document.getElementById("modal-body").innerHTML = body;
  document.getElementById("modal-submit").textContent = submitLabel;
  const form = document.getElementById("modal-form");
  form.onsubmit = async event => {
    if (event.submitter && event.submitter.value === "cancel") return;
    event.preventDefault();
    try { const result = await onSubmit(new FormData(form)); if (result !== false) modal.close(); }
    catch (error) { toast(error.message, true); }
  };
  modal.showModal();
}
function raiseComplaint() {
  const types = state.complaintTypes || ["Slow Internet", "Connection Issue", "Billing Query", "Plan Change", "Other"];
  const body = `<div class="form-field"><label for="complaint-type">Issue type</label><select id="complaint-type" name="type" required><option value="">Choose an issue</option>${types.map(type => `<option>${esc(type)}</option>`).join("")}</select></div><div class="form-field"><label for="complaint-description">Describe the issue</label><textarea id="complaint-description" name="description" required minlength="8" placeholder="Tell us what is happening..."></textarea></div>${role === "admin" ? `<div class="form-field"><label for="complaint-customer">Customer</label><select id="complaint-customer" name="customerId" required>${customerOptions()}</select></div>` : ""}`;
  openModal("Raise a complaint", "CUSTOMER SUPPORT", body, async fd => {
    const payload = { type: String(fd.get("type") || "").trim(), description: String(fd.get("description") || "").trim(), customerId: Number(role === "admin" ? fd.get("customerId") : activeCustomerId) };
    const complaint = await api("/complaints", "POST", payload);
    await refreshState(); toast(`Complaint ${complaint.id} registered successfully.`);
  }, "Submit complaint");
}
function addCustomer(existing = null) {
  const body = `<div class="form-grid"><div class="form-field"><label for="cust-name">Full name</label><input id="cust-name" name="name" required value="${esc(existing?.name || "")}"></div><div class="form-field"><label for="cust-phone">Phone number</label><input id="cust-phone" name="phone" required pattern="[0-9+() -]{8,16}" value="${esc(existing?.phone || "")}"></div><div class="form-field full"><label for="cust-email">Email address</label><input id="cust-email" name="email" type="email" required value="${esc(existing?.email || "")}"></div><div class="form-field full"><label for="cust-address">Service address</label><input id="cust-address" name="address" value="${esc(existing?.address || "")}"></div></div>`;
  openModal(existing ? "Edit customer" : "Add customer", "ADMIN · CUSTOMER MANAGEMENT", body, async fd => {
    const payload = { name: String(fd.get("name")).trim(), email: String(fd.get("email")).trim(), phone: String(fd.get("phone")).trim(), address: String(fd.get("address") || "").trim() };
    const result = existing ? await api(`/customers/${existing.id}`, "PUT", payload) : await api("/customers", "POST", payload);
    await refreshState(); if (!existing) activeCustomerId = result.id;
    render(); toast(existing ? `Customer #${result.id} updated.` : `Customer #${result.id} added.`);
  });
}
async function removeCustomer(id) {
  const cu = state.customers.find(x => x.id === Number(id));
  if (!cu || !confirm(`Remove ${cu.name} and their project records?`)) return;
  await api(`/customers/${id}`, "DELETE"); await refreshState(); toast(`Customer #${id} removed.`);
}
function customizePortal() {
  const body = `<div class="form-grid"><div class="form-field"><label for="pref-brand">Brand name</label><input id="pref-brand" name="brand" value="${esc(preferences.brand?.name || "Netwise")}" required></div><div class="form-field"><label for="pref-tagline">Brand tagline</label><input id="pref-tagline" name="tagline" value="${esc(preferences.brand?.tagline || "INTERNET SERVICES")}"></div><div class="form-field full"><label for="pref-locale">Display locale</label><input id="pref-locale" name="locale" value="${esc(preferences.locale || "en-IN")}" required></div><div class="form-field full"><label for="pref-support">Support message</label><input id="pref-support" name="support" value="${esc(preferences.support?.message || "Our support team is here to help.")}"></div></div><p class="subtitle">Portal appearance settings are saved in this browser. Customer and service data is managed by the Java backend.</p>`;
  openModal("Customize portal", "PORTAL SETTINGS", body, fd => {
    const locale = String(fd.get("locale")).trim(); if (!locale) { toast("Enter a display locale.", true); return false; }
    try { new Intl.DateTimeFormat(locale); } catch { toast("Enter a valid display locale.", true); return false; }
    preferences = { ...preferences, brand: { name: String(fd.get("brand")).trim(), tagline: String(fd.get("tagline")).trim() }, locale, support: { ...preferences.support, message: String(fd.get("support")).trim() } };
    savePreferences(); render(); toast("Portal preferences saved in this browser.");
  });
}
function addPlan(existing = null) {
  const body = `<div class="form-grid"><div class="form-field"><label for="plan-name">Plan name</label><input id="plan-name" name="name" required maxlength="50" value="${esc(existing?.name || "")}"></div><div class="form-field"><label for="plan-tier">Plan type</label><select id="plan-tier" name="tier"><option ${existing?.tier === "Basic" ? "selected" : ""}>Basic</option><option ${!existing || existing.tier === "Standard" ? "selected" : ""}>Standard</option><option ${existing?.tier === "Premium" ? "selected" : ""}>Premium</option></select></div><div class="form-field"><label for="plan-price">Monthly price (₹)</label><input id="plan-price" name="price" type="number" min="0.01" step="0.01" required value="${existing?.price ?? ""}"></div><div class="form-field"><label for="plan-data">Data allowance (GB)</label><input id="plan-data" name="data" type="number" min="1" step="1" required value="${existing?.data ?? ""}"></div><div class="form-field"><label for="plan-speed">Speed</label><input id="plan-speed" name="speed" required placeholder="e.g. 100 Mbps" value="${esc(existing?.speed || "")}"></div><div class="form-field"><label class="checkbox-field"><input name="featured" type="checkbox" ${existing?.featured ? "checked" : ""}> Featured plan</label></div><div class="form-field full"><label for="plan-description">Description</label><input id="plan-description" name="description" required value="${esc(existing?.description || "")}"></div></div>`;
  openModal(existing ? "Edit internet plan" : "Add internet plan", "ADMIN · PLAN CATALOG", body, async fd => {
    const payload = { name: String(fd.get("name")).trim(), tier: String(fd.get("tier")), price: Number(fd.get("price")), data: Number(fd.get("data")), speed: String(fd.get("speed")).trim(), description: String(fd.get("description")).trim(), featured: Boolean(fd.get("featured")) };
    const result = existing ? await api(`/plans/${existing.id}`, "PUT", payload) : await api("/plans", "POST", payload);
    await refreshState(); render(); toast(existing ? `${result.name} updated.` : `${result.name} added to the catalog.`);
  });
}
async function removePlan(id) {
  const plan = planById(id);
  if (!plan || !confirm(`Remove the ${plan.name} plan from the catalog?`)) return;
  await api(`/plans/${id}`, "DELETE"); await refreshState(); toast(`${plan.name} removed.`);
}
function subscribe(planId) {
  const p = planById(planId); if (!p) return;
  const existing = subscriptionFor(activeCustomerId);
  const timing = existing
    ? "The change is scheduled for the first day of the next billing period. Your current plan and all issued invoices stay unchanged; no partial-month charge is calculated."
    : "A new subscription is billed at the full monthly plan price for the current billing period. The due date is shown on the issued invoice.";
  const body = `<p class="subtitle modal-copy">You selected <strong>${esc(p.name)}</strong> at <strong>${money(p.price)}/month</strong> before tax.</p><div class="bill-line"><span>Monthly plan fee</span><strong>${money(p.price)}</strong></div><div class="bill-line"><span>GST (18%)</span><strong>${money(p.price * taxRate())}</strong></div><div class="bill-line total"><span>Monthly total</span><strong>${money(p.price * (1 + taxRate()))}</strong></div><p class="subtitle modal-copy">${timing}</p>`;
  openModal("Confirm plan", "SUBSCRIPTION", body, async () => {
    const result = await api("/subscriptions", "POST", { customerId: Number(activeCustomerId), planId: p.id });
    await refreshState();
    toast(result.pendingPlanId ? `${p.name} scheduled for ${formatDate(result.changeEffectiveDate, { day: "numeric", month: "short", year: "numeric" })}.` : `${p.name} is now the active plan.`);
  }, "Confirm plan");
}
async function cancelScheduledPlanChange() {
  await api("/subscriptions/cancel-change", "POST", { customerId: Number(activeCustomerId) });
  await refreshState(); toast("Scheduled plan change cancelled; current plan remains active.");
}
async function markPaid(id) { await api(`/bills/${encodeURIComponent(id)}/pay`, "POST", { method: "SIMULATED" }); await refreshState(); toast(`Simulated payment recorded for ${id}.`); }
function downloadText(filename, content, type = "text/plain") {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const a = document.createElement("a"); a.href = url; a.download = filename; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
}
function downloadBills() {
  const lines = [["Invoice", "Customer ID", "Customer Name", "Billing Period", "Plan", "Plan Price", "Tax Rate", "Tax", "Total", "Due Date", "Status", "Payment History"], ...getBills().map(b => [b.id, b.customerId, b.customerName || "", b.billingPeriod || b.month, b.planName || "Internet service", b.planPrice ?? b.amount, `${((b.taxRate ?? 0.18) * 100).toFixed(0)}%`, b.gst, b.total, b.dueDate, b.status, (b.payments || []).map(p => `${p.paymentId} ${p.paidOn} ${p.amount} ${p.method}`).join("; ")])];
  const csv = lines.map(row => row.map(v => `"${String(v).replace(/"/g, '""')}"`).join(",")).join("\r\n");
  downloadText("netwise-invoices.csv", csv, "text/csv;charset=utf-8"); toast("Invoice statement exported.");
}
function toast(message, error = false) {
  const el = document.createElement("div"); el.className = "toast" + (error ? " error" : ""); el.textContent = message;
  document.getElementById("toast-region").appendChild(el); setTimeout(() => el.remove(), 3600);
}
async function generateReport() {
  reportStatus = await api("/reports", "POST", {}); render();
  while (["QUEUED", "RUNNING"].includes(reportStatus.status)) {
    const version = reportStatus.version || 0;
    reportStatus = await api(`/reports/${encodeURIComponent(reportStatus.id)}?version=${version}&wait=3500`);
    if (currentPage === "dashboard") render();
  }
  if (reportStatus.status === "COMPLETED") {
    downloadText("netwise-admin-report.txt", reportStatus.result, "text/plain;charset=utf-8"); toast("Background report completed and downloaded.");
  } else if (reportStatus.status === "FAILED") toast(reportStatus.message || "Report generation failed.", true);
  if (currentPage === "dashboard") render();
}
async function cancelReport() {
  if (!reportStatus?.id) return;
  reportStatus = await api(`/reports/${encodeURIComponent(reportStatus.id)}/cancel`, "POST", {}); render();
  toast("Report generation cancelled.");
}
function handleAction(action, el) {
  switch (action) {
    case "raise-complaint": raiseComplaint(); break;
    case "add-customer": addCustomer(); break;
    case "edit-customer": addCustomer(state.customers.find(c => c.id === Number(el.dataset.customer))); break;
    case "remove-customer": removeCustomer(el.dataset.customer).catch(e => toast(e.message, true)); break;
    case "add-plan": addPlan(); break;
    case "edit-plan": addPlan(planById(el.dataset.plan)); break;
    case "remove-plan": removePlan(el.dataset.plan).catch(e => toast(e.message, true)); break;
    case "subscribe": subscribe(Number(el.dataset.plan)); break;
    case "cancel-plan-change": cancelScheduledPlanChange().catch(e => toast(e.message, true)); break;
    case "pay": markPaid(el.dataset.bill).catch(e => toast(e.message, true)); break;
    case "download-bills": downloadBills(); break;
    case "export": generateReport().catch(e => toast(e.message, true)); break;
    case "cancel-report": cancelReport().catch(e => toast(e.message, true)); break;
    case "invoice": {
      const b = state.bills.find(x => x.id === el.dataset.bill);
      if (b) {
        const payments = (b.payments || []).map(p => `${p.paymentId} | ${p.paidOn} | ${money(p.amount)} | ${p.method}`).join("\n") || "No payments recorded";
        downloadText(`${b.id}-receipt.txt`, `${preferences.brand?.name || "NETWISE"} INTERNET SERVICES\nInvoice: ${b.id}\nCustomer: ${b.customerName || `Customer #${b.customerId}`}\nBilling period: ${b.month}\nPlan: ${b.planName || "Internet service"}\nPlan price: ${money(b.planPrice ?? b.amount)}\nTax rate: ${((b.taxRate ?? 0.18) * 100).toFixed(0)}%\nTax: ${money(b.gst)}\nTotal: ${money(b.total)}\nStatus: ${b.status}\nDue date: ${b.dueDate}\n\nPAYMENT HISTORY\n${payments}\n\nDemo receipt; not a tax invoice.`);
      }
      toast("Receipt downloaded."); break;
    }
    case "complaint-details": {
      const c = state.complaints.find(x => x.id === el.dataset.complaint);
      if (c) openModal(c.id, "SUPPORT TICKET", `<div class="bill-line"><span>Issue type</span><strong>${esc(c.type)}</strong></div><div class="bill-line"><span>Status</span><strong>${esc(c.status)}</strong></div><div class="form-field detail-copy"><label>Description</label><p class="subtitle">${esc(c.description)}</p></div><p class="subtitle">Created ${esc(c.created)}.</p>`, () => true, "Close");
      break;
    }
  }
}
document.addEventListener("click", event => {
  const pageBtn = event.target.closest("[data-page]");
  if (pageBtn) { currentPage = pageBtn.dataset.page; searchQuery = ""; render(); document.getElementById("sidebar").classList.remove("open"); return; }
  const actionBtn = event.target.closest("[data-action]");
  if (actionBtn && actionBtn.tagName !== "SELECT") handleAction(actionBtn.dataset.action, actionBtn);
});
document.addEventListener("change", async event => {
  if (event.target.id === "role-select") { role = event.target.value; if (role === "admin") activeCustomerId = state.customers[0]?.id ?? null; render(); return; }
  if (event.target.matches('[data-action="update-complaint"]')) {
    try { const id = event.target.dataset.complaint; await api(`/complaints/${encodeURIComponent(id)}`, "PATCH", { status: event.target.value }); await refreshState(); toast(`${id} status updated.`); }
    catch (error) { toast(error.message, true); }
  }
});
document.addEventListener("input", event => {
  if (event.target.id === "complaint-search" || event.target.id === "customer-search") {
    const id = event.target.id, pos = event.target.selectionStart;
    if (id === "complaint-search") searchQuery = event.target.value; else customerSearch = event.target.value;
    render(); const input = document.getElementById(id); if (input) { input.focus(); input.setSelectionRange(pos, pos); }
  }
});
document.addEventListener("submit", async event => {
  if (event.target.id === "usage-form") {
    event.preventDefault();
    try {
      const gb = Number(new FormData(event.target).get("gb"));
      const usage = await api(`/customers/${activeCustomerId}/usage`, "POST", { gb });
      await refreshState(); const p = currentPlan();
      if (p && usage.used > p.data) toast("Usage added. This account is over its plan allowance.", true);
      else toast(`${gb.toFixed(1)} GB added to the account.`);
    } catch (error) { toast(error.message, true); }
  }
});
document.getElementById("mobile-menu").addEventListener("click", () => document.getElementById("sidebar").classList.toggle("open"));
document.getElementById("notifications-button").addEventListener("click", () => toast("You are all caught up."));
document.getElementById("profile-menu").addEventListener("click", () => toast("Use the View as selector to switch between customer and admin."));
document.getElementById("settings-button").addEventListener("click", customizePortal);
document.getElementById("customer-select").addEventListener("change", event => { activeCustomerId = Number(event.target.value); render(); });
document.addEventListener("keydown", event => { if (event.key === "Escape") document.getElementById("sidebar").classList.remove("open"); });
render();
api("/state").then(data => { state = data; backendConnected = true; activeCustomerId = state.customers[0]?.id ?? null; render(); })
  .catch(error => { backendConnected = false; render(); toast(`Java backend unavailable: ${error.message}. Start the app using the project README.`, true); });
