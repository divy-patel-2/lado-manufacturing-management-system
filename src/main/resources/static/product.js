let products = [];
let categories = [];
let units = [];
let editingId = null;

document.addEventListener("DOMContentLoaded", async () => {
    renderHeader("/products.html");
    await loadCategories();
    await loadUnits();
    await loadProducts();
    resetForm();
});

async function loadCategories() {
    try {
        categories = await api("GET", "/api/categories");
        const select = document.getElementById("categoryId");

        select.innerHTML =
            '<option value="">Select Category</option>' +
            categories.map(c =>
                `<option value="${c.id}">${esc(c.name)}</option>`
            ).join("");
    } catch (e) {
        showMsg(e.message, true);
    }
}

async function loadUnits() {
    try {
        units = await api("GET", "/api/units");
        const select = document.getElementById("unitId");

        select.innerHTML =
            '<option value="">Select Unit</option>' +
            units.map(u =>
                `<option value="${u.id}">${esc(u.name)} (${esc(u.symbol || "")})</option>`
            ).join("");
    } catch (e) {
        showMsg(e.message, true);
    }
}

async function loadProducts() {
    try {
        products = await api("GET", "/api/products");
        renderProducts();
    } catch (e) {
        showMsg(e.message, true);
    }
}

function renderProducts() {
    const tbody = document.getElementById("productTableBody");

    if (products.length === 0) {
        tbody.innerHTML =
            '<tr><td colspan="8">No products found.</td></tr>';
        return;
    }

    tbody.innerHTML = products.map(p => `
        <tr>
            <td>${esc(p.code)}</td>
            <td>${esc(p.name)}</td>
            <td>${esc(categoryName(p.categoryId))}</td>
            <td>${esc(unitName(p.unitId))}</td>
            <td>${formatNumber(p.costPrice)}</td>
            <td>${formatNumber(p.sellingPrice)}</td>
            <td>${formatNumber(p.reorderLevel)}</td>
            <td>
                <button type="button" onclick="editProduct(${p.id})">
                    Edit
                </button>
                <button type="button" onclick="deleteProduct(${p.id})">
                    Delete
                </button>
            </td>
        </tr>
    `).join("");
}

function categoryName(id) {
    const category = categories.find(c => c.id === id);
    return category ? category.name : "-";
}

function unitName(id) {
    const unit = units.find(u => u.id === id);

    if (!unit) {
        return "-";
    }

    return unit.symbol
        ? `${unit.name} (${unit.symbol})`
        : unit.name;
}

function formatNumber(value) {
    if (value === null || value === undefined || value === "") {
        return "-";
    }

    return Number(value).toFixed(2);
}

function editProduct(id) {
    const product = products.find(p => p.id === id);

    if (!product) {
        return;
    }

    editingId = id;

    document.getElementById("code").value = product.code || "";
    document.getElementById("name").value = product.name || "";
    document.getElementById("categoryId").value =
        product.categoryId || "";
    document.getElementById("unitId").value =
        product.unitId || "";
    document.getElementById("description").value =
        product.description || "";
    document.getElementById("costPrice").value =
        product.costPrice ?? "";
    document.getElementById("sellingPrice").value =
        product.sellingPrice ?? "";
    document.getElementById("reorderLevel").value =
        product.reorderLevel ?? "";

    document.getElementById("formTitle").textContent = "Edit Product";
    document.getElementById("saveButton").textContent = "Update Product";

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}

async function saveProduct(event) {
    event.preventDefault();

    const data = {
        code: document.getElementById("code").value.trim(),
        name: document.getElementById("name").value.trim(),
        categoryId: Number(document.getElementById("categoryId").value),
        unitId: Number(document.getElementById("unitId").value),
        description: document.getElementById("description").value.trim(),
        costPrice: decimalOrNull("costPrice"),
        sellingPrice: decimalOrNull("sellingPrice"),
        reorderLevel: decimalOrNull("reorderLevel")
    };

    if (!data.code || !data.name) {
        showMsg("Product code and product name are required.", true);
        return;
    }

    if (!data.categoryId) {
        showMsg("Please select a category.", true);
        return;
    }

    if (!data.unitId) {
        showMsg("Please select a unit.", true);
        return;
    }

    try {
        if (editingId === null) {
            await api("POST", "/api/products", data);
            showMsg("Product added successfully.");
        } else {
            await api("PUT", `/api/products/${editingId}`, data);
            showMsg("Product updated successfully.");
        }

        resetForm();
        await loadProducts();

    } catch (e) {
        showMsg(e.message, true);
    }
}

async function deleteProduct(id) {
    const product = products.find(p => p.id === id);

    if (!product) {
        return;
    }

    if (!confirm(`Delete product "${product.name}"?`)) {
        return;
    }

    try {
        await api("DELETE", `/api/products/${id}`);
        showMsg("Product deleted successfully.");
        await loadProducts();
    } catch (e) {
        showMsg(e.message, true);
    }
}

function resetForm() {
    editingId = null;

    document.getElementById("productForm").reset();

    document.getElementById("formTitle").textContent = "Add Product";
    document.getElementById("saveButton").textContent = "Add Product";
}

function decimalOrNull(id) {
    const value = document.getElementById(id).value.trim();

    if (value === "") {
        return null;
    }

    return Number(value);
}