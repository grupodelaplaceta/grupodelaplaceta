import { Router } from 'express';
import { sbFindJuniorByDip, sbCreateJuniorLog } from '../config/db-supabase.js';
import { supabase } from '../config/supabase.js';

const router = Router();

/**
 * POST /api/junior/amigos/solicitar
 * Body: { dip, dip_amigo }
 * Crea solicitud de amistad y notifica al tutor vía PlacetaID
 */
router.post('/solicitar', async (req, res) => {
  try {
    const { dip, dip_amigo } = req.body;
    if (!dip || !dip_amigo) return res.status(400).json({ error: 'DIP y dip_amigo requeridos' });
    if (dip === dip_amigo) return res.status(400).json({ error: 'No puedes ser amigo de ti mismo' });

    const junior = await sbFindJuniorByDip(dip);
    const amigo = await sbFindJuniorByDip(dip_amigo);

    if (!junior || !amigo) return res.status(404).json({ error: 'Uno de los menores no fue encontrado' });

    // Verificar que no sean ya amigos
    const { data: existing } = await supabase.from('junior_amistades')
      .select('id')
      .or(`(junior_id.eq.${junior.id},amigo_id.eq.${amigo.id}),(junior_id.eq.${amigo.id},amigo_id.eq.${junior.id})`)
      .eq('estado', 'activo')
      .limit(1);

    if (existing && existing.length > 0) {
      return res.status(409).json({ error: 'Ya sois amigos' });
    }

    // Crear solicitud pendiente
    const { data: solicitud, error: insertErr } = await supabase.from('junior_amistades')
      .insert({
        junior_id: junior.id,
        amigo_id: amigo.id,
        estado: 'pendiente',
        creado_en: new Date().toISOString()
      }).select().single();

    if (insertErr) throw new Error(insertErr.message);

    // Notificar al tutor del junior
    if (junior.tutor_dip) {
      try {
        const { solicitarAutorizacionTutor } = await import('../services/placetaidService.js');
        await solicitarAutorizacionTutor({
          dipTutor: junior.tutor_dip,
          concepto: `Solicitud de amistad: ${junior.nombre} quiere ser amigo de ${amigo.nombre}`,
          monto: 0,
          dipMenor: junior.dip,
          detalles: `Amistad solicitada con ${amigo.nombre} (DIP: ${amigo.dip})`
        });
      } catch (_) { /* PlacetaID offline, seguir */ }
    }

    // Notificar al tutor del amigo
    if (amigo.tutor_dip && amigo.tutor_dip !== junior.tutor_dip) {
      try {
        const { solicitarAutorizacionTutor } = await import('../services/placetaidService.js');
        await solicitarAutorizacionTutor({
          dipTutor: amigo.tutor_dip,
          concepto: `Solicitud de amistad para ${amigo.nombre}`,
          monto: 0,
          dipMenor: amigo.dip,
          detalles: `${junior.nombre} (DIP: ${junior.dip}) quiere ser amigo de ${amigo.nombre}`
        });
      } catch (_) { /* PlacetaID offline */ }
    }

    await sbCreateJuniorLog({
      junior_id: junior.id,
      accion: 'solicitud_amistad',
      detalle: `Solicitó amistad con ${amigo.nombre} (DIP: ${amigo.dip})`,
      ip: req.ip
    });

    res.json({
      success: true,
      message: 'Solicitud enviada. Los tutores serán notificados.',
      solicitud_id: solicitud.id
    });
  } catch (err) {
    console.error('[Amigos] Error:', err);
    res.status(500).json({ error: err.message });
  }
});

export default router;
