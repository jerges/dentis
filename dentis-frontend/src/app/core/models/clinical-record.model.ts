export interface ClinicalRecord {
  id: string;
  patientId: string;
  patientName: string;
  generalAnamnesis?: string;
  allergies?: string;
  currentMedications?: string;
  medicalHistory?: string;
  dentalHistory?: string;
  odontogram: OdontogramTooth[];
  evolutions: ClinicalEvolution[];
  diagnoses: Diagnosis[];
  treatmentPlans: TreatmentPlanSummary[];
}

export interface OdontogramTooth {
  id: string;
  toothNumber: number;
  status: ToothStatus;
  affectedSurfaces: ToothSurface[];
  notes?: string;
}

export interface ClinicalEvolution {
  id: string;
  clinicalRecordId: string;
  dentistId?: string;
  dentistName?: string;
  evolutionDate: string;
  description: string;
  procedure?: string;
  observations?: string;
  createdAt: string;
}

export interface Diagnosis {
  id: string;
  dentistId?: string;
  dentistName?: string;
  title: string;
  description?: string;
  icdCode?: string;
  diagnosisDate: string;
  active: boolean;
}

export interface TreatmentPlanSummary {
  id: string;
  title: string;
  status: string;
  startDate: string;
  estimatedEndDate?: string;
  dentistName?: string;
}

export type ToothStatus =
  | 'HEALTHY' | 'CARIES' | 'RESTORED' | 'ABSENT' | 'MALFORMATION'
  | 'CROWN' | 'ROOT_CANAL' | 'IMPLANT' | 'FRACTURE' | 'EXTRACTION_INDICATED'
  | 'BRIDGE_ABUTMENT' | 'TEMPORARY_RESTORATION';

export type ToothSurface =
  | 'MESIAL' | 'DISTAL' | 'BUCCAL' | 'LINGUAL' | 'PALATAL'
  | 'OCCLUSAL' | 'INCISAL' | 'VESTIBULAR' | 'CERVICAL';

export interface UpdateOdontogramRequest {
  toothNumber: number;
  status: ToothStatus;
  affectedSurfaces?: ToothSurface[];
  notes?: string;
}
