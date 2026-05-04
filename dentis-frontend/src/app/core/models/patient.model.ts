export interface Patient {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  documentId: string;
  birthDate: string;
  age: number;
  gender: Gender;
  socialName?: string;
  phone?: string;
  altPhone?: string;
  email?: string;
  address?: string;
  city?: string;
  state?: string;
  representativeName?: string;
  representativePhone?: string;
  representativeRelationship?: string;
  notes?: string;
  active: boolean;
  createdAt: string;
}

export interface PatientSummary {
  id: string;
  fullName: string;
  documentId: string;
  birthDate: string;
  age: number;
  gender: Gender;
  phone?: string;
  email?: string;
  active: boolean;
}

export type Gender = 'MALE' | 'FEMALE' | 'NON_BINARY' | 'OTHER' | 'PREFER_NOT_TO_SAY';

export interface CreatePatientRequest {
  firstName: string;
  lastName: string;
  documentId?: string;
  birthDate: string;
  gender: Gender;
  socialName?: string;
  phone?: string;
  altPhone?: string;
  email?: string;
  address?: string;
  city?: string;
  state?: string;
  representativeName?: string;
  representativePhone?: string;
  representativeRelationship?: string;
  notes?: string;
}
