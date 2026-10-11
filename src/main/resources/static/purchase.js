let purchases = [];
let suppliers = [];
let products = [];
let editingId = null;

document.addEventListener("DOMContentLoaded", async () => {
    renderHeader("/purchases.html");
    setDefaultDate();

    try {
        await loadMasterData();
        await loadPurchases();
        resetForm();
    } catch (e) {
        showMsg(e.message, true);
    }
});

async function loadMasterData() {
    suppliers = await api("GET", "/api/suppliers");
    products = await api("GET", "/api/products");

    renderSupplierOptions();
}

function renderSupplierOptions(selectedId) {
    const select = document.getElementById("supplierId");

    select.innerHTML =
        '<option value="">Select supplier</option>' +
        suppliers.map(s =>
            `<option value="${s.id}">
                ${esc(s.code)} - ${esc(s.name)}
            </option>`
        ).join("");

    if (selectedId != null) {
        select.value = String(selectedId);
    }
}

async function loadPurchases() {
    purchases = await api("GET", "/api/purchases");
    renderPurchases();
}

function renderPurchases() {
    const tbody = document.getElementById("purchaseTableBody");

    if (purchases.length === 0) {
        tbody.innerHTML =
            '<tr><td colspan="6">No purchases found.</td></tr>';
        return;
    }

    tbody.innerHTML = purchases.map(p => `
        <tr>
            <td>${esc(p.purchaseNo)}</td>

            <td>
                ${esc(p.purchaseDate || "-")}
            </td>

            <td>
                ${esc(getSupplierName(p.supplierId))}
            </td>

            <td>
                ${esc(formatAmount(p.totalAmount))}
            </td>

            <td>
                ${esc(p.paymentStatus || "-")}
            </td>

            <td>
                <button
                    type="button"
                    onclick="editPurchase(${p.id})">
                    Edit
                </button>

                <button
                    type="button"
                    onclick="deletePurchase(${p.id})">
                    Delete
                </button>
            </td>
        </tr>
    `).join("");
}

function getSupplierName(id) {
    const supplier = suppliers.find(
        s => s.id === id
    );

    return supplier
        ? `${supplier.code} - ${supplier.name}`
        : `Supplier #${id}`;
}

function addItemRow(item) {
    const tbody =
        document.getElementById("purchaseItemsBody");

    const row = document.createElement("tr");

    row.innerHTML = `
        <td>
            <select
                class="item-product"
                required>

                <option value="">
                    Select product
                </option>

                ${products.map(p =>
                    `<option value="${p.id}">
                        ${esc(p.code)} - ${esc(p.name)}
                    </option>`
                ).join("")}

            </select>
        </td>

        <td>
            <input
                class="item-quantity"
                type="number"
                min="0.000001"
                step="any"
                required>
        </td>

        <td>
            <input
                class="item-price"
                type="number"
                min="0"
                step="0.01"
                required>
        </td>

        <td>
            <input
                class="item-tax"
                type="number"
                min="0"
                step="0.01"
                value="0">
        </td>

        <td>
            <input
                class="item-discount"
                type="number"
                min="0"
                step="0.01"
                value="0">
        </td>

        <td class="item-total">
            0.00
        </td>

        <td>
            <button
                type="button"
                onclick="removeItemRow(this)">
                Remove
            </button>
        </td>
    `;

    tbody.appendChild(row);

    if (item) {
        row.querySelector(".item-product").value =
            String(item.productId);

        row.querySelector(".item-quantity").value =
            item.quantity ?? "";

        row.querySelector(".item-price").value =
            item.unitPrice ?? "";

        row.querySelector(".item-tax").value =
            item.taxAmount ?? 0;

        row.querySelector(".item-discount").value =
            item.discountAmount ?? 0;
    }

    row.querySelectorAll("input").forEach(input => {
        input.addEventListener(
            "input",
            calculateTotals
        );
    });

    calculateTotals();
}

function removeItemRow(button) {
    const rows =
        document.querySelectorAll(
            "#purchaseItemsBody tr"
        );

    if (rows.length <= 1) {
        showMsg(
            "A purchase must contain at least one item.",
            true
        );
        return;
    }

    button.closest("tr").remove();

    calculateTotals();
}

function calculateTotals() {
    let grandTotal = 0;

    document
        .querySelectorAll("#purchaseItemsBody tr")
        .forEach(row => {

            const quantity =
                Number(
                    row.querySelector(
                        ".item-quantity"
                    ).value
                ) || 0;

            const price =
                Number(
                    row.querySelector(
                        ".item-price"
                    ).value
                ) || 0;

            const tax =
                Number(
                    row.querySelector(
                        ".item-tax"
                    ).value
                ) || 0;

            const discount =
                Number(
                    row.querySelector(
                        ".item-discount"
                    ).value
                ) || 0;

            const total =
                quantity * price +
                tax -
                discount;

            const safeTotal =
                Math.max(total, 0);

            row.querySelector(
                ".item-total"
            ).textContent =
                formatAmount(safeTotal);

            grandTotal += safeTotal;
        });

    document.getElementById(
        "grandTotal"
    ).textContent =
        formatAmount(grandTotal);
}

async function savePurchase(event) {
    event.preventDefault();

    const rows =
        document.querySelectorAll(
            "#purchaseItemsBody tr"
        );

    const items = [];

    for (const row of rows) {

        const productId =
            row.querySelector(
                ".item-product"
            ).value;

        const quantity =
            Number(
                row.querySelector(
                    ".item-quantity"
                ).value
            );

        const unitPrice =
            Number(
                row.querySelector(
                    ".item-price"
                ).value
            );

        const taxAmount =
            Number(
                row.querySelector(
                    ".item-tax"
                ).value
            ) || 0;

        const discountAmount =
            Number(
                row.querySelector(
                    ".item-discount"
                ).value
            ) || 0;

        if (
            !productId ||
            quantity <= 0 ||
            unitPrice < 0
        ) {
            showMsg(
                "Please enter valid product, quantity and unit price for every item.",
                true
            );
            return;
        }

        items.push({
            productId: Number(productId),
            quantity: quantity,
            unitPrice: unitPrice,
            taxAmount: taxAmount,
            discountAmount: discountAmount
        });
    }

    const data = {
        purchaseNo:
            document.getElementById(
                "purchaseNo"
            ).value.trim(),

        supplierId:
            Number(
                document.getElementById(
                    "supplierId"
                ).value
            ),

        purchaseDate:
            document.getElementById(
                "purchaseDate"
            ).value,

        paymentStatus:
            document.getElementById(
                "paymentStatus"
            ).value,

        notes:
            document.getElementById(
                "notes"
            ).value.trim(),

        items: items
    };

    if (
        !data.purchaseNo ||
        !data.supplierId ||
        !data.purchaseDate
    ) {
        showMsg(
            "Purchase number, supplier and date are required.",
            true
        );
        return;
    }

    try {

        if (editingId === null) {

            await api(
                "POST",
                "/api/purchases",
                data
            );

            showMsg(
                "Purchase added successfully."
            );

        } else {

            await api(
                "PUT",
                `/api/purchases/${editingId}`,
                data
            );

            showMsg(
                "Purchase updated successfully."
            );
        }

        resetForm();

        await loadPurchases();

    } catch (e) {

        showMsg(
            e.message,
            true
        );
    }
}

async function editPurchase(id) {
    try {

        const purchase =
            await api(
                "GET",
                `/api/purchases/${id}`
            );

        editingId = id;

        document.getElementById(
            "purchaseNo"
        ).value =
            purchase.purchaseNo || "";

        renderSupplierOptions(
            purchase.supplierId
        );

        document.getElementById(
            "purchaseDate"
        ).value =
            purchase.purchaseDate || "";

        document.getElementById(
            "paymentStatus"
        ).value =
            purchase.paymentStatus ||
            "PENDING";

        document.getElementById(
            "notes"
        ).value =
            purchase.notes || "";

        document.getElementById(
            "purchaseItemsBody"
        ).innerHTML = "";

        purchase.items.forEach(
            item => addItemRow(item)
        );

        document.getElementById(
            "formTitle"
        ).textContent =
            "Edit Purchase";

        document.getElementById(
            "saveButton"
        ).textContent =
            "Update Purchase";

        calculateTotals();

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });

    } catch (e) {

        showMsg(
            e.message,
            true
        );
    }
}

async function deletePurchase(id) {

    const purchase =
        purchases.find(
            p => p.id === id
        );

    if (!purchase) return;

    if (!confirm(
        `Delete purchase "${purchase.purchaseNo}"?`
    )) {
        return;
    }

    try {

        await api(
            "DELETE",
            `/api/purchases/${id}`
        );

        showMsg(
            "Purchase deleted successfully."
        );

        await loadPurchases();

    } catch (e) {

        showMsg(
            e.message,
            true
        );
    }
}

function resetForm() {

    editingId = null;

    document.getElementById(
        "purchaseForm"
    ).reset();

    document.getElementById(
        "formTitle"
    ).textContent =
        "Add Purchase";

    document.getElementById(
        "saveButton"
    ).textContent =
        "Add Purchase";

    setDefaultDate();

    document.getElementById(
        "purchaseItemsBody"
    ).innerHTML = "";

    addItemRow();

    calculateTotals();
}

function setDefaultDate() {

    const input =
        document.getElementById(
            "purchaseDate"
        );

    if (!input.value) {

        const now = new Date();

        const offset =
            now.getTimezoneOffset() * 60000;

        input.value =
            new Date(
                Date.now() - offset
            )
            .toISOString()
            .slice(0, 10);
    }
}

function formatAmount(value) {

    return Number(
        value || 0
    ).toFixed(2);
}
