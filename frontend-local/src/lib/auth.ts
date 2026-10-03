// Day-scoped local session storage (KTD5).
//
// Sessions are stored in IndexedDB rather than localStorage: an XSS payload that can read
// localStorage does so synchronously and can exfiltrate a token in one line. IndexedDB is
// asynchronous and its cross-origin isolation properties make casual exfiltration meaningfully
// harder. This module is deliberately small: one database, one object store, at most one
// record, wrapped in Promises over the native IDBRequest callback API — no wrapper library.

const DB_NAME = 'localday-auth';
const DB_VERSION = 1;
const STORE_NAME = 'session';
const SESSION_KEY = 'current';

export interface StoredSession {
  sessionToken: string;
  officialName: string;
  credentialId: number;
}

function wrapRequest<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME);
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
    request.onblocked = () => reject(new Error('IndexedDB open request blocked'));
  });
}

async function withStore<T>(
  mode: IDBTransactionMode,
  fn: (store: IDBObjectStore) => IDBRequest<T>,
): Promise<T> {
  const db = await openDb();
  try {
    const tx = db.transaction(STORE_NAME, mode);
    const store = tx.objectStore(STORE_NAME);
    const result = await wrapRequest(fn(store));
    return result;
  } finally {
    db.close();
  }
}

export async function getStoredSession(): Promise<StoredSession | null> {
  const result = await withStore<StoredSession | undefined>('readonly', (store) =>
    store.get(SESSION_KEY),
  );
  return result ?? null;
}

export async function storeSession(session: StoredSession): Promise<void> {
  await withStore<IDBValidKey>('readwrite', (store) => store.put(session, SESSION_KEY));
}

export async function clearSession(): Promise<void> {
  await withStore<undefined>('readwrite', (store) => store.delete(SESSION_KEY));
}
