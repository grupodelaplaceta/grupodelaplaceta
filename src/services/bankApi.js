const BANK_API_BASE = process.env.BANK_API_URL || 'https://api.banco.laplaceta.org';
const CRM_KEY = process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026';

async function bankFetch(endpoint, token) {
  const url = `${BANK_API_BASE}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    'X-Placeta-App-ID': 'gdlp-crm'
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  // Intentar con Bearer primero, fallback a CRM key
  let res = await fetch(url, { headers });
  if (!res.ok && token) {
    // Fallback: usar CRM key (endpoint /api/crm-state)
    const crmUrl = `${BANK_API_BASE}/api/crm-state`;
    res = await fetch(crmUrl, { headers: { 'Content-Type': 'application/json', 'X-CRM-Key': CRM_KEY } });
  }
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
