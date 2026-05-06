# dentis-clinical

Bounded context de historia clínica digital. Contiene el odontograma interactivo, evoluciones clínicas, diagnósticos y planes de tratamiento vinculados a presupuestos.

## Modelo de Dominio

```
ClinicalRecord (1 por paciente)
├── patientId: UUID
├── odontogram: List<OdontogramTooth>
├── evolutions: List<ClinicalEvolution>
├── diagnoses: List<Diagnosis>
└── treatmentPlans: List<TreatmentPlan>

OdontogramTooth (numeración FDI — Federación Dental Internacional)
├── toothNumber: int           (11-48 permanentes, 51-85 deciduos)
├── condition: ToothCondition
├── affectedSurfaces: Set<ToothSurface>
└── notes: String

ToothCondition:
  HEALTHY | ABSENT | CARIES | RESTORED | CROWNED | IMPLANT
  ROOT_CANAL | FRACTURE | MALFORMATION | EXTRACTED | ERUPTING | IMPACTED

ToothSurface: MESIAL | DISTAL | BUCCAL | LINGUAL | OCCLUSAL | INCISAL

TreatmentPlan
├── title / description: String
├── status: TreatmentPlanStatus  (PROPOSED → APPROVED → IN_PROGRESS → COMPLETED)
├── budgetId: UUID               (vinculado a dentis-billing)
└── procedures: List<TreatmentProcedure>

TreatmentProcedure
├── description: String
├── toothNumber: int
├── performed: boolean           (¿fue realizado?)
├── performedAt: LocalDateTime
└── budgetItemId: UUID           (para vincular con pago)
```

## Numeración Dental (FDI)

```
Cuadrante superior derecho: 11-18
Cuadrante superior izquierdo: 21-28
Cuadrante inferior izquierdo: 31-38
Cuadrante inferior derecho: 41-48

Deciduos: 51-55 / 61-65 / 71-75 / 81-85
```

## Reglas de Negocio

- Solo puede existir **una** `ClinicalRecord` por paciente.
- El odontograma se actualiza completo en cada modificación.
- Los procedimientos marcados como `performed` pero sin pago asociado quedan en estado "ejecutado pendiente de pago" — trazabilidad financiero-clínica.
- Los planes de tratamiento se vinculan opcionalmente a un `budgetId` del módulo de facturación.

## API (expuesta por dentis-api)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/clinical/patients/{id}` | Crear historia clínica |
| `GET` | `/api/v1/clinical/patients/{id}` | Obtener historia clínica |
| `PUT` | `/api/v1/clinical/patients/{id}/odontogram` | Actualizar odontograma |
| `POST` | `/api/v1/clinical/patients/{id}/evolutions` | Agregar evolución |
| `POST` | `/api/v1/clinical/patients/{id}/treatment-plans` | Crear plan de tratamiento |
| `PATCH` | `/api/v1/clinical/procedures/{id}/perform` | Marcar procedimiento como realizado |
