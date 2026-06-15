export type ToothCondition =
  | 'HEALTHY' | 'ABSENT' | 'CARIES' | 'RESTORED' | 'CROWNED'
  | 'IMPLANT' | 'ROOT_CANAL' | 'FRACTURE' | 'MALFORMATION'
  | 'EXTRACTED' | 'ERUPTING' | 'IMPACTED' | 'ROOT_REMNANT' | 'DEFECTIVE_FILLING';

export type ToothSurface = 'MESIAL' | 'DISTAL' | 'BUCCAL' | 'LINGUAL' | 'OCCLUSAL' | 'INCISAL';

export type SpaceStatus = 'OPEN' | 'PARTIALLY_CLOSED' | 'CLOSED';

export type DentitionType = 'PERMANENT' | 'PRIMARY' | 'MIXED';

export type TreatmentPlanStatus = 'PROPOSED' | 'APPROVED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export type RootFinding =
  | 'ENDODONTIC_TREATMENT' | 'PERIAPICAL_LESION' | 'PERIAPICAL_ABSCESS'
  | 'ROOT_FRACTURE' | 'INTERNAL_RESORPTION' | 'EXTERNAL_RESORPTION'
  | 'POST_CORE' | 'APICOECTOMY' | 'HYPERCEMENTOSIS';

export interface OdontogramTooth {
  id?: string;
  toothNumber: number;
  condition: ToothCondition;
  affectedSurfaces?: ToothSurface[];
  surfaceConditions?: Partial<Record<ToothSurface, ToothCondition>>;
  spaceStatus?: SpaceStatus;
  notes?: string;
  rootFindings?: RootFinding[];
  rootNotes?: string;
}

export interface ClinicalEvolution {
  id?: string;
  clinicalRecordId?: string;
  dentistId: string;
  description: string;
  findings?: string;
  treatment?: string;
  recordedAt?: string;
}

export interface Diagnosis {
  id?: string;
  clinicalRecordId?: string;
  code: string;
  description: string;
  diagnosedAt?: string;
  dentistId?: string;
  toothNumber?: number;
}

export interface TreatmentProcedure {
  id?: string;
  treatmentPlanId?: string;
  description: string;
  toothNumber?: number;
  performed: boolean;
  performedAt?: string;
  performedByDentistId?: string;
  budgetItemId?: string;
}

export interface TreatmentPlan {
  id?: string;
  clinicalRecordId?: string;
  dentistId: string;
  title: string;
  description?: string;
  status: TreatmentPlanStatus;
  procedures?: TreatmentProcedure[];
  budgetId?: string;
  startDate?: string;
  estimatedEndDate?: string;
}

export interface ClinicalRecord {
  id: string;
  patientId: string;
  dentitionType: DentitionType;
  odontogram: OdontogramTooth[];
  evolutions: ClinicalEvolution[];
  diagnoses: Diagnosis[];
  treatmentPlans: TreatmentPlan[];
  createdAt?: string;
  updatedAt?: string;
}

export interface AddEvolutionRequest {
  dentistId: string;
  description: string;
  findings?: string;
  treatment?: string;
}

export interface AddDiagnosisRequest {
  code: string;
  description: string;
  diagnosedAt?: string;
  dentistId?: string;
  toothNumber?: number;
}

export interface AddTreatmentPlanRequest {
  dentistId: string;
  title: string;
  description?: string;
  startDate?: string;
  estimatedEndDate?: string;
}

export interface UpdateOdontogramRequest {
  teeth: OdontogramTooth[];
}

export interface ClinicalAttachment {
  id: string;
  fileName: string;
  contentType: string;
  fileSize?: number;
  description?: string;
  toothNumber?: number;
  uploadedAt?: string;
  downloadUrl?: string;
}

export interface PresignedUploadResponse {
  s3Key: string;
  uploadUrl: string;
}

export interface RegisterAttachmentRequest {
  s3Key: string;
  fileName: string;
  contentType: string;
  fileSize?: number;
  description?: string;
  toothNumber?: number;
  uploadedByDentistId?: string;
}
