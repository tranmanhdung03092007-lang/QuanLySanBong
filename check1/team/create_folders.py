#!/usr/bin/env python3
import re, os, glob

base = "team"
for p in range(1, 5):
    os.makedirs(f"{base}/person{p}", exist_ok=True)

sections = {}
for p in range(1, 5):
    files = glob.glob(f"{base}/person{p}_*.java")
    if not files:
        continue
    with open(files[0], "r", encoding="utf-8") as f:
        content = f.read()
    parts = re.split(r'//={20,}\n// (.+?)\n//={20,}', content)
    for i in range(1, len(parts), 2):
        if i + 1 < len(parts):
            sections[parts[i].strip()] = parts[i+1]

person_map = {
    "BookingService.java": 1, "ScheduleApiServlet.java": 1, "BookingServlet.java": 1,
    "HomeServlet.java": 1, "user/booking.jsp": 1, "user/home.jsp": 1, "booking.js": 1,
    "InvoiceServlet.java": 2, "user/invoice.jsp": 2, "app.js": 2,
    "AdminLoginServlet.java": 3, "AdminLogoutServlet.java": 3, "AdminServlet.java": 3,
    "AdminCrudServlet.java": 3, "admin/login.jsp": 3, "admin/dashboard.jsp": 3,
    "admin/list.jsp": 3, "admin/form.jsp": 3,
    "AdminInvoiceApiServlet.java": 4, "InvoiceManagementServlet.java": 4, "admin/invoice-management.jsp": 4
}

for filename, person in person_map.items():
    dest = f"{base}/person{person}/{filename}"
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    if filename in sections:
        with open(dest, "w", encoding="utf-8") as f:
            f.write(sections[filename].strip() + "\n")
        print(f"Created: {dest}")
    else:
        print(f"WARN: {filename} not found")

print("Done.")