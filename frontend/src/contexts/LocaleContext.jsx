import { createContext, useContext, useState } from 'react';
import { createT } from '../i18n';

const LocaleContext = createContext(null);

export function LocaleProvider({ children }) {
  const [locale, setLocaleState] = useState(() => localStorage.getItem('pim_locale') || 'en');

  function setLocale(l) {
    setLocaleState(l);
    localStorage.setItem('pim_locale', l);
  }

  return (
    <LocaleContext.Provider value={{ locale, setLocale, t: createT(locale) }}>
      {children}
    </LocaleContext.Provider>
  );
}

export function useLocale() {
  return useContext(LocaleContext);
}
