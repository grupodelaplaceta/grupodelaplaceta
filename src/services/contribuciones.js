/**
 * Sistema de Contribuciones GDLP
 * Basado en Normativa — Art. 4.8 a 4.16
 * IGF + IRM + Contribuciones especiales
 */

// ── CONSTANTES ──────────────────────────────────────────────────────────────
const EXENTO_IGF = 5000; // Primeros 5.000 Pz exentos de IGF
const CAPITALIA_ACCOUNT = 'CAPITALIA'; // Cuenta que paga las contribuciones exentas junior
const TGLP_ACCOUNT = 'TGLP'; // Tesoro de La Placeta / Tributos

// ── TIPOS DE CONTRIBUCIÓN ──────────────────────────────────────────────────
const TIPOS_CONTRIBUCION = {
  estandar: {
    id: 'estandar',
    label: 'Contribución Estándar Art. 4.10 y 4.13',
    descripcion: 'IRM + IGF para personas físicas. Paga el contribuyente.',
    incluye_IRM: true,
    incluye_IGF: true,
    paga_capitalia: false,
    exenta_igf: false
  },
  empresa: {
    id: 'empresa',
    label: 'Contribución Empresa Art. 4.14',
    descripcion: 'IGF para empresas y entidades con tarifas Art. 4.14.',
    incluye_IRM: true,
    incluye_IGF: true,
    paga_capitalia: false,
    exenta_igf: false
  },
  exenta_junior: {
    id: 'exenta_junior',
    label: 'Contribución Exenta Junior DEMO',
    descripcion: 'Mismo cálculo que Estándar pero lo paga CAPITALIA a Tributos.',
    incluye_IRM: true,
    incluye_IGF: true,
    paga_capitalia: true,
    exenta_igf: false
  },
  empresa_exenta_igf: {
    id: 'empresa_exenta_igf',
    label: 'Contribución Empresa Exenta de IGF Art. 4.15',
    descripcion: 'Empresas < 20.000 Pz que pagan IVA. Solo IRM.',
    incluye_IRM: true,
    incluye_IGF: false,
    paga_capitalia: false,
    exenta_igf: true
  }
};

// ── CÁLCULO IGF (INDIVIDUAL) - Art. 4.13 ──────────────────────────────────
function calcularIGFIndividual(patrimonioMedio) {
  if (patrimonioMedio <= EXENTO_IGF) return 0;
  let total = 0;
  let restante = patrimonioMedio;

  // Tramo 1: 5.001 a 20.000 → 10%
  if (restante > EXENTO_IGF) {
    const baseTramo = Math.min(restante - EXENTO_IGF, 15000); // Máximo 15.000 en este tramo
    total += baseTramo * 0.10;
    restante = EXENTO_IGF + 15000; // Avanzamos a 20.000
  }

  // Tramo 2: 20.001 a 500.000 → 30%
  if (restante > 20000) {
    const baseTramo = Math.min(restante - 20000, 480000); // Máximo hasta 500.000
    total += baseTramo * 0.30;
    restante = 20000 + 480000; // Avanzamos a 500.000
  }

  // Tramo 3: > 500.000 → 85% (solo empresas)
  // No aplica para individuales

  return Math.round(total);
}

// ── CÁLCULO IGF (EMPRESA) - Art. 4.14 ─────────────────────────────────────
function calcularIGFEmpresa(patrimonioMedio) {
  if (patrimonioMedio <= EXENTO_IGF) return 0;
  let total = 0;

  // Tramo 1: 5.001 a 20.000 → 5%
  if (patrimonioMedio > EXENTO_IGF) {
    const baseTramo = Math.min(patrimonioMedio - EXENTO_IGF, 15000);
    total += baseTramo * 0.05;
  }

  // Tramo 2: 20.001 a 500.000 → 35%
  if (patrimonioMedio > 20000) {
    const baseTramo = Math.min(patrimonioMedio - 20000, 480000);
    total += baseTramo * 0.35;
  }

  // Tramo 3: > 500.000 → 85%
  if (patrimonioMedio > 500000) {
    const baseTramo = patrimonioMedio - 500000;
    total += baseTramo * 0.85;
  }

  return Math.round(total);
}

// ── CÁLCULO IRM - Art. 4.10 ────────────────────────────────────────────────
function calcularIRM(patrimonioMedio, ingresos, pagos, tipoCuenta = 'Personal') {
  // IA = (Media ingresos - Media pagos) / Patrimonio medio
  const ia = patrimonioMedio > 0 ? (ingresos - pagos) / patrimonioMedio : 0;

  let porcentaje = 0;
  if (ia <= 0) {
    porcentaje = 0; // Consumo neto / equilibrio
  } else if (ia <= 0.05) {
    porcentaje = tipoCuenta === 'Empresa' ? 1 : tipoCuenta === 'Compartida' ? 0.75 : 0.5;
  } else if (ia <= 0.15) {
    porcentaje = tipoCuenta === 'Empresa' ? 3 : tipoCuenta === 'Compartida' ? 2 : 1.5;
  } else if (ia <= 0.30) {
    porcentaje = tipoCuenta === 'Empresa' ? 6 : tipoCuenta === 'Compartida' ? 4 : 3;
  } else {
    porcentaje = tipoCuenta === 'Empresa' ? 9 : tipoCuenta === 'Compartida' ? 6 : 5;
  }

  return {
    ia: Math.round(ia * 10000) / 10000,
    porcentaje,
    importe: Math.round(patrimonioMedio * porcentaje / 100),
    tipoCuenta
  };
}

// ── CÁLCULO COMPLETO ──────────────────────────────────────────────────────
function calcularContribucion(contribuyente, patrimonioMedio, ingresos = 0, pagos = 0) {
  const tipo = contribuyente.tipo_contribucion || (contribuyente.tipo_sujeto === 'Empresa' ? 'empresa' : 'estandar');
  const config = TIPOS_CONTRIBUCION[tipo] || TIPOS_CONTRIBUCION.estandar;
  const esEmpresa = contribuyente.tipo_sujeto === 'Empresa' || (contribuyente.roles_json || []).includes('empresa');

  // Calcular IGF
  let igf = 0;
  if (config.incluye_IGF && !config.exenta_igf) {
    igf = esEmpresa ? calcularIGFEmpresa(patrimonioMedio) : calcularIGFIndividual(patrimonioMedio);
  }

  // Calcular IRM (siempre, aunque esté exento, para mostrar IA)
  const tipoCuenta = esEmpresa ? 'Empresa' : 'Personal';
  const irmResult = calcularIRM(patrimonioMedio, ingresos, pagos, tipoCuenta);
  const importeIRM = config.incluye_IRM ? irmResult.importe : 0;

  const total = igf + importeIRM;

  return {
    contribuyente: contribuyente.nombre,
    dip: contribuyente.dip,
    placeta_id: contribuyente.placeta_id,
    tipo_contribucion: tipo,
    tipo_sujeto: contribuyente.tipo_sujeto || 'Fisico',
    patrimonio_medio: patrimonioMedio,
    igf: {
      importe: igf,
      exento: config.exenta_igf ? 'Exento Art. 4.15' : (patrimonioMedio <= EXENTO_IGF ? 'Primeros 5.000 Pz exentos' : ''),
      base_calculo: Math.max(0, patrimonioMedio - EXENTO_IGF),
      tarifa: esEmpresa ? 'Art. 4.14 (empresa)' : 'Art. 4.13 (individual)'
    },
    irm: irmResult,
    total_contribucion: total,
    paga_capitalia: config.paga_capitalia,
    pagador_efectivo: config.paga_capitalia ? CAPITALIA_ACCOUNT : contribuyente.iban || contribuyente.placeta_id,
    receptor: TGLP_ACCOUNT,
    config
  };
}

export {
  TIPOS_CONTRIBUCION,
  calcularContribucion,
  calcularIGFIndividual,
  calcularIGFEmpresa,
  calcularIRM,
  EXENTO_IGF,
  CAPITALIA_ACCOUNT,
  TGLP_ACCOUNT
};
