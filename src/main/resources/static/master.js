// Generic "list + add + edit + delete" page for simple master data.
//
// initMaster({
//   endpoint: '/api/categories', singular: 'category', plural: 'categories',
//   fields: [ { key: 'name', label: 'Name', required: true, max: 100 }, ... ]
// });
function initMaster(cfg) {
  var form = document.getElementById('form');
  var thead = document.getElementById('thead');
  var tbody = document.getElementById('tbody');
  var items = [];
  var editingId = null;

  form.innerHTML = cfg.fields.map(function (f) {
    return '<div class="field"><label for="f_' + f.key + '">' + esc(f.label) +
           (f.required ? ' *' : '') + '</label>' +
           '<input type="text" id="f_' + f.key + '" maxlength="' + (f.max || 200) + '"' +
           (f.required ? ' required' : '') + '></div>';
  }).join('') +
  '<div class="actions">' +
  '<button type="submit" id="saveBtn"></button>' +
  '<button type="button" id="cancelBtn" class="secondary" style="display:none">Cancel</button>' +
  '</div>';

  thead.innerHTML = '<tr>' + cfg.fields.map(function (f) {
    return '<th>' + esc(f.label) + '</th>';
  }).join('') + '<th></th></tr>';

  var saveBtn = document.getElementById('saveBtn');
  var cancelBtn = document.getElementById('cancelBtn');

  function resetForm() {
    editingId = null;
    cfg.fields.forEach(function (f) { document.getElementById('f_' + f.key).value = ''; });
    saveBtn.textContent = 'Add ' + cfg.singular;
    cancelBtn.style.display = 'none';
  }

  function render() {
    if (!items.length) {
      tbody.innerHTML = '<tr><td class="empty" colspan="' + (cfg.fields.length + 1) +
                        '">No ' + esc(cfg.plural) + ' yet.</td></tr>';
      return;
    }
    tbody.innerHTML = items.map(function (it) {
      return '<tr>' + cfg.fields.map(function (f) {
        return '<td>' + esc(it[f.key]) + '</td>';
      }).join('') +
      '<td class="row-actions">' +
      '<button class="link" data-act="edit" data-id="' + it.id + '">Edit</button>' +
      '<button class="link danger" data-act="del" data-id="' + it.id + '">Delete</button>' +
      '</td></tr>';
    }).join('');
  }

  async function load() {
    items = await api('GET', cfg.endpoint);
    render();
  }

  tbody.addEventListener('click', async function (e) {
    var btn = e.target.closest('button[data-act]');
    if (!btn) return;
    var id = Number(btn.dataset.id);
    var item = items.find(function (x) { return x.id === id; });
    if (!item) return;

    if (btn.dataset.act === 'edit') {
      editingId = id;
      cfg.fields.forEach(function (f) {
        document.getElementById('f_' + f.key).value = item[f.key] == null ? '' : item[f.key];
      });
      saveBtn.textContent = 'Save changes';
      cancelBtn.style.display = '';
      window.scrollTo(0, 0);
    } else {
      if (!confirm('Delete "' + item[cfg.fields[0].key] + '"?')) return;
      try {
        await api('DELETE', cfg.endpoint + '/' + id);
        showMsg('Deleted.');
        if (editingId === id) resetForm();
        await load();
      } catch (err) {
        showMsg(err.message, true);
      }
    }
  });

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    var body = {};
    cfg.fields.forEach(function (f) {
      body[f.key] = document.getElementById('f_' + f.key).value.trim();
    });
    try {
      if (editingId) {
        await api('PUT', cfg.endpoint + '/' + editingId, body);
        showMsg('Changes saved.');
      } else {
        await api('POST', cfg.endpoint, body);
        showMsg(cfg.singular.charAt(0).toUpperCase() + cfg.singular.slice(1) + ' added.');
      }
      resetForm();
      await load();
    } catch (err) {
      showMsg(err.message, true);
    }
  });

  cancelBtn.addEventListener('click', resetForm);

  resetForm();
  load().catch(function (err) { showMsg(err.message, true); });
}
