import React, { createContext, useContext, useEffect, useState } from 'react';

interface HelpContextValue {
  helpContent: React.ReactNode | null;
  setHelpContent: (content: React.ReactNode | null) => void;
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
}

// eslint-disable-next-line react-refresh/only-export-components
export const HelpContext = createContext<HelpContextValue | null>(null);

export function HelpProvider({ children }: { children: React.ReactNode }) {
  const [helpContent, setHelpContent] = useState<React.ReactNode | null>(null);
  const [isOpen, setIsOpen] = useState(false);

  return (
    <HelpContext.Provider value={{ helpContent, setHelpContent, isOpen, setIsOpen }}>
      {children}
    </HelpContext.Provider>
  );
}

export function useHelp(): HelpContextValue {
  const ctx = useContext(HelpContext);
  if (!ctx) throw new Error('useHelp must be used within HelpProvider');
  return ctx;
}

/** Shows `Help` in the help side bar while the calling page is open. */
export function useHelpContent(Help: React.ComponentType) {
  const { setHelpContent } = useHelp();
  useEffect(() => {
    setHelpContent(<Help />);
    return () => setHelpContent(null);
  }, [Help, setHelpContent]);
}
