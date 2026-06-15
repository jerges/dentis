import { Injectable } from '@angular/core';
import { ClinicalRecord } from '../models/clinical.model';
import { Patient } from '../models/patient.model';

@Injectable({ providedIn: 'root' })
export class PdfService {

  exportClinicalRecord(patient: Patient, record: ClinicalRecord): void {
    const win = window.open('', '_blank', 'width=900,height=700');
    if (!win) return;

    const patientName = `${patient.firstName} ${patient.lastName}`;
    const today = new Date().toLocaleDateString('es-ES');

    const evolutionsHtml = record.evolutions.length
      ? record.evolutions.map(e => `
          <div class="entry">
            <div class="entry-date">${e.recordedAt ? new Date(e.recordedAt).toLocaleString('es-ES') : ''}</div>
            <p>${e.description}</p>
            ${e.findings ? `<p><strong>Hallazgos:</strong> ${e.findings}</p>` : ''}
            ${e.treatment ? `<p><strong>Tratamiento:</strong> ${e.treatment}</p>` : ''}
          </div>`).join('')
      : '<p class="empty">Sin evoluciones registradas.</p>';

    const diagnosesHtml = record.diagnoses.length
      ? `<table><thead><tr><th>Código</th><th>Descripción</th><th>Diente</th><th>Fecha</th></tr></thead><tbody>
          ${record.diagnoses.map(d => `<tr>
            <td><strong>${d.code}</strong></td>
            <td>${d.description}</td>
            <td>${d.toothNumber ?? '—'}</td>
            <td>${d.diagnosedAt ? new Date(d.diagnosedAt).toLocaleDateString('es-ES') : '—'}</td>
          </tr>`).join('')}
        </tbody></table>`
      : '<p class="empty">Sin diagnósticos registrados.</p>';

    const plansHtml = record.treatmentPlans.length
      ? record.treatmentPlans.map(p => `
          <div class="entry">
            <strong>${p.title}</strong> — <em>${p.status}</em>
            ${p.description ? `<p>${p.description}</p>` : ''}
          </div>`).join('')
      : '<p class="empty">Sin planes de tratamiento.</p>';

    win.document.write(`<!DOCTYPE html><html lang="es"><head>
      <meta charset="UTF-8">
      <title>Historia Clínica — ${patientName}</title>
      <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: Arial, sans-serif; font-size: 12px; color: #1a1a2e; padding: 24px; }
        h1 { font-size: 18px; color: #5b5bd6; margin-bottom: 4px; }
        .meta { color: #666; font-size: 11px; margin-bottom: 24px; }
        h2 { font-size: 13px; font-weight: 700; border-bottom: 2px solid #5b5bd6;
             padding-bottom: 4px; margin: 20px 0 12px; text-transform: uppercase; letter-spacing: 0.5px; }
        .entry { border-left: 3px solid #e2e8f0; padding: 8px 12px; margin-bottom: 10px; }
        .entry-date { font-size: 10px; color: #5b5bd6; font-weight: 600; margin-bottom: 4px; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #e2e8f0; padding: 6px 8px; text-align: left; font-size: 11px; }
        th { background: #f8f9fa; font-weight: 600; }
        .empty { color: #999; font-style: italic; font-size: 11px; }
        @media print { body { padding: 0; } }
      </style>
    </head><body>
      <h1>Historia Clínica</h1>
      <div class="meta">
        Paciente: <strong>${patientName}</strong> · Doc: ${patient.idDocument} · Generado: ${today}
      </div>
      <h2>Evoluciones Clínicas</h2>
      ${evolutionsHtml}
      <h2>Diagnósticos</h2>
      ${diagnosesHtml}
      <h2>Planes de Tratamiento</h2>
      ${plansHtml}
    </body></html>`);
    win.document.close();
    win.focus();
    setTimeout(() => win.print(), 500);
  }
}
