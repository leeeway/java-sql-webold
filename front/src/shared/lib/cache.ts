interface CacheEntry {
  value: unknown;
  expirse: number;
}

const cacheLocalStorage = {
  set(key: string, value: unknown, ttl: number): void {
    const date = new Date().getTime() + ttl;
    const data: CacheEntry = { value, expirse: new Date(date).getTime() };
    localStorage.setItem(key, JSON.stringify(data));
  },

  get(key: string): any {
    const raw = localStorage.getItem(key);
    if (raw === null) {
      return null;
    }

    const data: CacheEntry = JSON.parse(raw);
    if (data.expirse != null && data.expirse < new Date().getTime()) {
      localStorage.removeItem(key);
      return null;
    }

    return data.value;
  },
};

export default cacheLocalStorage;
