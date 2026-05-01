import { createContext, useContext, useState } from 'react';

const EMPTY_FILTERS = {
  toolMaterial: '', workpieceMaterial: '', machineType: '', machineBrand: '',
  cuttingType: '', dMmMin: '', dMmMax: '', shankMm: '', hasBallBearing: '',
  productType: '', inStock: '', priceMin: '', priceMax: '',
};

const SessionContext = createContext(null);

export function SessionProvider({ children }) {
  const [catalog, setCatalog] = useState({
    selectedIds: [],
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

  function setSelected(updater) {
    if (typeof updater === 'function') {
      setCatalog(prev => {
        const next = updater(new Set(prev.selectedIds));
        return { ...prev, selectedIds: [...next] };
      });
    } else {
      setCatalog(prev => ({ ...prev, selectedIds: [...updater] }));
    }
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
    selected: new Set(catalog.selectedIds),
    setSelected,

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
