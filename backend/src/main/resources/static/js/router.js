/**
 * WPW PIM — Hash Router
 * Routes are #/page or #/page/param
 * Exported as window.Router
 */

const Router = (() => {
  // Route definitions: { pattern: RegExp, handler: fn(match) }
  const routes = [];

  function on(pattern, handler) {
    // Convert '/product/:id' string patterns to regex
    const re = typeof pattern === 'string'
      ? new RegExp('^' + pattern.replace(/:([^/]+)/g, '([^/]+)') + '/?$')
      : pattern;
    routes.push({ re, handler });
  }

  function resolve() {
    const hash = window.location.hash || '#/';
    // Strip leading #
    const path = hash.replace(/^#/, '') || '/';

    for (const { re, handler } of routes) {
      const match = path.match(re);
      if (match) {
        // match[1..n] are captured groups (params)
        handler(...match.slice(1));
        return;
      }
    }

    // Fallback — no route matched, go to catalog
    navigate('/catalog');
  }

  function navigate(path) {
    window.location.hash = path;
  }

  function init() {
    window.addEventListener('hashchange', resolve);
    resolve(); // Run immediately on load
  }

  // Update active nav link
  function setActiveNav(pageId) {
    document.querySelectorAll('.nav-link').forEach(el => {
      el.classList.toggle('active', el.dataset.page === pageId);
    });
  }

  return { on, navigate, init, setActiveNav };
})();

window.Router = Router;
