// Small helpers shared by all pages.

function esc(s) {
  return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
    return {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }[c];
  });
}

async function api(method, url, body) {
  var opts = {
    method: method,
    headers: {}
  };

  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }

  var res = await fetch(url, opts);

  // If the session has expired the server redirects to the login page.
  if (res.redirected && res.url.indexOf('login.html') !== -1) {
    window.location.href = '/login.html';
    throw new Error('Session expired. Please log in again.');
  }

  var text = await res.text();
  var data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch (e) {
      data = null;
    }
  }

  if (!res.ok) {
    throw new Error(
      (data && data.error) ||
      ('Request failed (' + res.status + ')')
    );
  }

  return data;
}

function showMsg(text, isError) {
  var el = document.getElementById('msg');

  if (!el) return;

  el.textContent = text;
  el.className = isError ? 'err' : 'ok';
  el.style.display = 'block';

  clearTimeout(showMsg._t);

  if (!isError) {
    showMsg._t = setTimeout(function () {
      el.style.display = 'none';
    }, 3000);
  }
}

// Top bar used on every page.
function renderHeader(active) {
  var links = [
    ['/', 'Home'],
    ['/categories.html', 'Categories'],
    ['/units.html', 'Units'],
    ['/products.html', 'Products'],
    ['/suppliers.html', 'Suppliers'],
    ['/customers.html', 'Customers'],
    ['/purchases.html', 'Purchases']
  ];

  var nav = links.map(function (l) {
    return '<a href="' + l[0] + '"' +
      (l[0] === active ? ' class="active"' : '') +
      '>' + l[1] + '</a>';
  }).join('');

  document.getElementById('header').innerHTML =
    '<nav><strong style="margin-right:12px">LADO</strong>' + nav + '</nav>' +
    '<form method="post" action="/logout"><button type="submit">Log out</button></form>';
}