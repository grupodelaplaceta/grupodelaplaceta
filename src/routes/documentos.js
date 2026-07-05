import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import PDFGenerator from '../services/pdfGenerator.js';

const router = Router();

// GET /admin/documentos - Lista de documentos/manuales
router.get('/admin/documentos', verificarSesion, verificarRol('administrador', 'junta', 'secretario'), (req, res) => {
  res.render('admin/documentos', {
    titulo: 'Gestión de Documentos',
    pathActual: '/admin/documentos',
    usuario: req.session.usuario
  });
});

// GET /admin/documentos/editor - Editor online tipo Word
router.get('/admin/documentos/editor', verificarSesion, verificarRol('administrador', 'junta', 'secretario'), (req, res) => {
  res.render('admin/editor', {
    titulo: 'Editor de Documentos',
    pathActual: '/admin/documentos',
    usuario: req.session.usuario,
    documento: null
  });
});

// GET /admin/documentos/editor/:id - Cargar documento existente
router.get('/admin/documentos/editor/:id', verificarSesion, verificarRol('administrador', 'junta', 'secretario'), (req, res) => {
  try {
    const db = getDb();
    const doc = db.prepare('SELECT * FROM documentos WHERE id = ?').get(req.params.id);
    if (!doc) return res.status(404).json({ error: 'Documento no encontrado' });
    res.render('admin/editor', {
      titulo: `Editando: ${doc.titulo}`,
      pathActual: '/admin/documentos',
      usuario: req.session.usuario,
      documento: doc
    });
  } catch (err) {
    console.error('Error cargando documento:', err);
    res.status(500).json({ error: 'Error al cargar el documento' });
  }
});

// API: Listar documentos
router.get('/api/documentos', verificarSesion, (req, res) => {
  try {
    const db = getDb();
    const docs = db.prepare('SELECT * FROM documentos ORDER BY updated_at DESC').all();
    res.json(docs);
  } catch (err) {
    console.error('Error listando documentos:', err);
    res.json([]);
  }
});

// API: Guardar documento (crear o actualizar)
router.post('/api/documentos/guardar', verificarSesion, verificarRol('administrador', 'junta', 'secretario'), (req, res) => {
  try {
    const db = getDb();
    const { id, titulo, subtitulo, contenido, tipo, autor, version } = req.body;

    if (!titulo || !contenido) {
      return res.status(400).json({ error: 'Título y contenido son obligatorios' });
    }

    const now = new Date().toISOString();
    const autorFinal = autor || req.session.usuario.alias;

    if (id) {
      // Actualizar existente
      db.prepare(`
        UPDATE documentos SET titulo=?, subtitulo=?, contenido=?, tipo=?, autor=?, version=?, updated_at=?
        WHERE id=?
      `).run(titulo, subtitulo || '', contenido, tipo || 'manual', autorFinal, version || '1.0', now, id);
    } else {
      // Crear nuevo
      db.prepare(`
        INSERT INTO documentos (titulo, subtitulo, contenido, tipo, autor, version, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `).run(titulo, subtitulo || '', contenido, tipo || 'manual', autorFinal, version || '1.0', now, now);
    }

    const result = db.prepare('SELECT * FROM documentos ORDER BY updated_at DESC LIMIT 1').get();
    res.json({ success: true, documento: result });
  } catch (err) {
    console.error('Error guardando documento:', err);
    res.status(500).json({ error: 'Error al guardar el documento' });
  }
});

// API: Eliminar documento
router.delete('/api/documentos/:id', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    db.prepare('DELETE FROM documentos WHERE id = ?').run(req.params.id);
    res.json({ success: true });
  } catch (err) {
    console.error('Error eliminando documento:', err);
    res.status(500).json({ error: 'Error al eliminar el documento' });
  }
});

// API: Exportar documento existente como PDF
router.post('/api/documentos/exportar-pdf/:id', verificarSesion, async (req, res) => {
  try {
    const generator = new PDFGenerator();
    const db = getDb();
    const docData = db.prepare('SELECT * FROM documentos WHERE id = ?').get(req.params.id);
    if (!docData) return res.status(404).json({ error: 'Documento no encontrado' });

    const meta = {
      subtitulo: docData.subtitulo,
      autor: docData.autor,
      version: docData.version,
      fecha: docData.created_at ? new Date(docData.created_at).toLocaleDateString('es-ES') : undefined
    };

    const pdfDoc = generator.generarManual(docData.titulo, docData.contenido, meta);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="${docData.titulo.replace(/[^a-zA-Z0-9]/g,'_')}.pdf"`);
    pdfDoc.pipe(res);
    pdfDoc.end();
  } catch (err) {
    console.error('Error exportando PDF:', err);
    res.status(500).json({ error: 'Error al exportar el documento PDF' });
  }
});

// API: Exportar contenido directo como PDF (sin guardar)
router.post('/api/documentos/exportar-pdf', verificarSesion, async (req, res) => {
  try {
    const generator = new PDFGenerator();
    const titulo = req.body.titulo || 'Documento';
    const contenido = req.body.contenido || '';
    const meta = {
      subtitulo: req.body.subtitulo,
      autor: req.body.autor || req.session.usuario.alias,
      version: req.body.version || '1.0',
      items: req.body.metaItems
    };

    const pdfDoc = generator.generarManual(titulo, contenido, meta);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="${titulo.replace(/[^a-zA-Z0-9]/g,'_')}.pdf"`);
    pdfDoc.pipe(res);
    pdfDoc.end();
  } catch (err) {
    console.error('Error exportando PDF:', err);
    res.status(500).json({ error: 'Error al exportar el documento PDF' });
  }
});

// API: Obtener modelo de documento
router.get('/api/documentos/modelos', verificarSesion, (req, res) => {
  const modelos = [
    {
      id: 'manual-convivencia',
      titulo: 'Manual de Convivencia',
      subtitulo: 'Normas de conducta y régimen interno',
      contenido: '<h1>Manual de Convivencia</h1>\n<p>Este manual establece las normas básicas de convivencia dentro del Grupo de La Placeta.</p>\n<h2>Capítulo 1: Principios generales</h2>\n<p>Artículo 1. Todos los integrantes deben tratarse con respeto mutuo.</p>',
      tipo: 'manual'
    },
    {
      id: 'manual-bienvenida',
      titulo: 'Manual de Bienvenida',
      subtitulo: 'Guía para nuevos integrantes',
      contenido: '<h1>Bienvenido a La Placeta</h1>\n<p>Gracias por formar parte del Grupo de La Placeta. Este manual te guiará en tus primeros pasos.</p>',
      tipo: 'manual'
    },
    {
      id: 'procedimiento-sancionador',
      titulo: 'Procedimiento Sancionador',
      subtitulo: 'Guía del Departamento de Justicia',
      contenido: '<h1>Procedimiento Sancionador</h1>\n<p>El presente documento detalla el procedimiento a seguir para la imposición de sanciones.</p>',
      tipo: 'procedimiento'
    }
  ];
  res.json(modelos);
});

export default router;
