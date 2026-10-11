let suppliers = [];
let editingId = null;

document.addEventListener("DOMContentLoaded", async () => {
    renderHeader("/suppliers.html");
    await loadSuppliers();
    resetForm();
});

async function loadSuppliers() {
    try {
        suppliers = await api("GET", "/api/suppliers");
        renderSuppliers();
    } catch (e) {
        showMsg(e.message, true);
    }
}

function renderSuppliers() {

    const tbody = document.getElementById("supplierTableBody");

    if (suppliers.length === 0) {

        tbody.innerHTML =
            '<tr><td colspan="8">No suppliers found.</td></tr>';

        return;
    }

    tbody.innerHTML = suppliers.map(s => `
        <tr>
            <td>${esc(s.code)}</td>

            <td>${esc(s.name)}</td>

            <td>${esc(s.phone || "-")}</td>

            <td>${esc(s.email || "-")}</td>

            <td>${esc(s.city || "-")}</td>

            <td>${esc(s.state || "-")}</td>

            <td>${esc(s.pincode || "-")}</td>

            <td>

                <button
                    type="button"
                    onclick="editSupplier(${s.id})">
                    Edit
                </button>

                <button
                    type="button"
                    onclick="deleteSupplier(${s.id})">
                    Delete
                </button>

            </td>
        </tr>
    `).join("");
}

function editSupplier(id) {

    const supplier = suppliers.find(s => s.id === id);

    if (!supplier) {
        return;
    }

    editingId = id;

    document.getElementById("code").value =
        supplier.code || "";

    document.getElementById("name").value =
        supplier.name || "";

    document.getElementById("phone").value =
        supplier.phone || "";

    document.getElementById("email").value =
        supplier.email || "";

    document.getElementById("address").value =
        supplier.address || "";

    document.getElementById("city").value =
        supplier.city || "";

    document.getElementById("state").value =
        supplier.state || "";

    document.getElementById("pincode").value =
        supplier.pincode || "";

    document.getElementById("formTitle").textContent =
        "Edit Supplier";

    document.getElementById("saveButton").textContent =
        "Update Supplier";

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}

async function saveSupplier(event) {

    event.preventDefault();

    const data = {

        code:
            document.getElementById("code").value.trim(),

        name:
            document.getElementById("name").value.trim(),

        phone:
            document.getElementById("phone").value.trim(),

        email:
            document.getElementById("email").value.trim(),

        address:
            document.getElementById("address").value.trim(),

        city:
            document.getElementById("city").value.trim(),

        state:
            document.getElementById("state").value.trim(),

        pincode:
            document.getElementById("pincode").value.trim()
    };

    if (!data.code || !data.name) {

        showMsg(
            "Supplier code and supplier name are required.",
            true
        );

        return;
    }

    try {

        if (editingId === null) {

            await api(
                "POST",
                "/api/suppliers",
                data
            );

            showMsg(
                "Supplier added successfully."
            );

        } else {

            await api(
                "PUT",
                `/api/suppliers/${editingId}`,
                data
            );

            showMsg(
                "Supplier updated successfully."
            );
        }

        resetForm();

        await loadSuppliers();

    } catch (e) {

        showMsg(
            e.message,
            true
        );
    }
}

async function deleteSupplier(id) {

    const supplier =
        suppliers.find(s => s.id === id);

    if (!supplier) {
        return;
    }

    if (!confirm(
        `Delete supplier "${supplier.name}"?`
    )) {
        return;
    }

    try {

        await api(
            "DELETE",
            `/api/suppliers/${id}`
        );

        showMsg(
            "Supplier deleted successfully."
        );

        await loadSuppliers();

    } catch (e) {

        showMsg(
            e.message,
            true
        );
    }
}

function resetForm() {

    editingId = null;

    document
        .getElementById("supplierForm")
        .reset();

    document.getElementById("formTitle")
        .textContent = "Add Supplier";

    document.getElementById("saveButton")
        .textContent = "Add Supplier";
}