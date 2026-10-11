let customers = [];
let editingId = null;

document.addEventListener("DOMContentLoaded", async () => {
    renderHeader("/customers.html");
    await loadCustomers();
    resetForm();
});

async function loadCustomers() {
    try {
        customers = await api("GET", "/api/customers");
        renderCustomers();
    } catch (e) {
        showMsg(e.message, true);
    }
}

function renderCustomers() {

    const tbody = document.getElementById("customerTableBody");

    if (customers.length === 0) {

        tbody.innerHTML =
            '<tr><td colspan="8">No customers found.</td></tr>';

        return;
    }

    tbody.innerHTML = customers.map(c => `
        <tr>
            <td>${esc(c.code)}</td>
            <td>${esc(c.name)}</td>
            <td>${esc(c.phone || "-")}</td>
            <td>${esc(c.email || "-")}</td>
            <td>${esc(c.city || "-")}</td>
            <td>${esc(c.state || "-")}</td>
            <td>${esc(c.pincode || "-")}</td>
            <td>
                <button type="button" onclick="editCustomer(${c.id})">
                    Edit
                </button>
                <button type="button" onclick="deleteCustomer(${c.id})">
                    Delete
                </button>
            </td>
        </tr>
    `).join("");
}

function editCustomer(id) {

    const customer = customers.find(c => c.id === id);

    if (!customer) return;

    editingId = id;

    document.getElementById("code").value = customer.code || "";
    document.getElementById("name").value = customer.name || "";
    document.getElementById("phone").value = customer.phone || "";
    document.getElementById("email").value = customer.email || "";
    document.getElementById("address").value = customer.address || "";
    document.getElementById("city").value = customer.city || "";
    document.getElementById("state").value = customer.state || "";
    document.getElementById("pincode").value = customer.pincode || "";

    document.getElementById("formTitle").textContent =
        "Edit Customer";

    document.getElementById("saveButton").textContent =
        "Update Customer";

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}

async function saveCustomer(event) {

    event.preventDefault();

    const data = {
        code: document.getElementById("code").value.trim(),
        name: document.getElementById("name").value.trim(),
        phone: document.getElementById("phone").value.trim(),
        email: document.getElementById("email").value.trim(),
        address: document.getElementById("address").value.trim(),
        city: document.getElementById("city").value.trim(),
        state: document.getElementById("state").value.trim(),
        pincode: document.getElementById("pincode").value.trim()
    };

    if (!data.code || !data.name) {

        showMsg(
            "Customer code and customer name are required.",
            true
        );

        return;
    }

    try {

        if (editingId === null) {

            await api(
                "POST",
                "/api/customers",
                data
            );

            showMsg("Customer added successfully.");

        } else {

            await api(
                "PUT",
                `/api/customers/${editingId}`,
                data
            );

            showMsg("Customer updated successfully.");
        }

        resetForm();

        await loadCustomers();

    } catch (e) {

        showMsg(e.message, true);
    }
}

async function deleteCustomer(id) {

    const customer = customers.find(c => c.id === id);

    if (!customer) return;

    if (!confirm(`Delete customer "${customer.name}"?`)) {
        return;
    }

    try {

        await api(
            "DELETE",
            `/api/customers/${id}`
        );

        showMsg("Customer deleted successfully.");

        await loadCustomers();

    } catch (e) {

        showMsg(e.message, true);
    }
}

function resetForm() {

    editingId = null;

    document
        .getElementById("customerForm")
        .reset();

    document.getElementById("formTitle").textContent =
        "Add Customer";

    document.getElementById("saveButton").textContent =
        "Add Customer";
}