import { createContext, useContext, useState } from 'react';

const EMPTY_FILTERS = {
  toolMaterial: '', workpieceMaterial: '', machineType: '', machineBrand: '',
  cuttingType: '', dMmMin: '', dMmMax: '', shankMm: '', hasBallBearing: '',
  productType: '', inStock: '', priceMin: '', priceMax: '',
};

const SessionContext = createContext(null);

export function SessionProvider({ children }) {
  const [catalog, setCatalog] = useState({
    qtys: {},          // { [productId]: number } — единственный источник "выбранности"
    filters: EMPTY_FILTERS,
    selectedNode: null,
    selectedOperation: null,
    page: 1,
    searchQuery: '',
    searchPage: 1,
    searchResults: null,
    searchTotal: 0,
  });

  const [importPage, setImportPage] = useState({
    tab: 'mapping',
    file: null,
    report: null,
  });

  return (
    <SessionContext.Provider value={{ catalog, setCatalog, importPage, setImportPage }}>
      {children}
    </SessionContext.Provider>
  );
}

function useSession() {
  return useContext(SessionContext);
}

// ── Catalog session hook ───────────────────────────────────────────────────────

export function useCatalogSession() {
  const { catalog, setCatalog } = useSession();

  function patch(updates) {
    setCatalog(prev => ({ ...prev, ...updates }));
  }

  function setProductQty(id, qty) {
    setCatalog(prev => {
      const next = { ...prev.qtys };
      if (qty <= 0) delete next[id]; else next[id] = qty;
      return { ...prev, qtys: next };
    });
  }

  function setManyQtys(updater) {
    setCatalog(prev => ({
      ...prev,
      qtys: typeof updater === 'function' ? updater({ ...prev.qtys }) : updater,
    }));
  }

  function clearQtys() {
    setCatalog(prev => ({ ...prev, qtys: {} }));
  }

  function setPage(updater) {
    setCatalog(prev => ({
      ...prev,
      page: typeof updater === 'function' ? updater(prev.page) : updater,
    }));
  }

  function setSearchPage(updater) {
    setCatalog(prev => ({
      ...prev,
      searchPage: typeof updater === 'function' ? updater(prev.searchPage) : updater,
    }));
  }

  return {
    qtys: new Map(Object.entries(catalog.qtys || {})),
    setProductQty,
    setManyQtys,
    clearQtys,

    filters: catalog.filters,
    setFilters: (f) => patch({ filters: f }),

    selectedNode: catalog.selectedNode,
    setSelectedNode: (n) => patch({ selectedNode: n }),

    selectedOperation: catalog.selectedOperation,
    setSelectedOperation: (o) => patch({ selectedOperation: o }),

    page: catalog.page,
    setPage,

    searchQuery: catalog.searchQuery,
    setSearchQuery: (q) => patch({ searchQuery: q }),

    searchPage: catalog.searchPage,
    setSearchPage,

    searchResults: catalog.searchResults,
    setSearchResults: (r) => patch({ searchResults: r }),

    searchTotal: catalog.searchTotal,
    setSearchTotal: (t) => patch({ searchTotal: t }),
  };
}

// ── Import session hook ────────────────────────────────────────────────────────

export function useImportSession() {
  const { importPage, setImportPage } = useSession();

  function patch(updates) {
    setImportPage(prev => ({ ...prev, ...updates }));
  }

  return {
    tab: importPage.tab,
    setTab: (t) => patch({ tab: t }),

    file: importPage.file,
    setFile: (f) => patch({ file: f, report: f ? importPage.report : null }),

    report: importPage.report,
    setReport: (r) => patch({ report: r }),

    clearImport: () => patch({ file: null, report: null }),
  };
}
