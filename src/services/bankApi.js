const BANK_API_BASE = process.env.BANK_API_URL || 'https://api.banco.laplaceta.org';

async function bankFetch(endpoint, token) {
  const url = `${BANK_API_BASE}${endpoint}`;
  const res = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      'X-Placeta-App-ID': 'gdlp-crm'
    }
  });
  const txt = await res.text();
  try { return { status: res.status, data: JSON.parse(txt) }; }
  catch { return { status: res.status, data: { raw: txt.substring(0,200) } }; }
}

export async function getBankState(token) {
  return bankFetch('/api/state', token);
}

export async function getBankCollection(collection, token) {
  return bankFetch(`/api/entity?collection=${collection}`, token);
}

export async function getBankHealth(token) {
  return bankFetch('/api/health', token);
}
