export interface ContactInfo {
  email?: string;
  phoneNumber?: string;
  alternativePhone?: string;
}

export interface Address {
  street?: string;
  city?: string;
  state?: string;
  zipCode?: string;
}

export interface Representative {
  fullName?: string;
  idDocument?: string;
  relationship?: string;
  phoneNumber?: string;
}

export type Sex = 'MALE' | 'FEMALE' | 'INTERSEX' | 'NOT_SPECIFIED';
export type Gender = 'MALE' | 'FEMALE' | 'NON_BINARY' | 'OTHER' | 'NOT_SPECIFIED';

export interface Patient {
  id: string;
  firstName: string;
  lastName: string;
  idDocument: string;
  birthDate: string;
  sex: Sex;
  gender: Gender;
  socialName?: string;
  contactInfo: ContactInfo;
  address?: Address;
  representative?: Representative;
  active: boolean;
}

export interface CreatePatientRequest {
  firstName: string;
  lastName: string;
  idDocument: string;
  birthDate: string;
  sex: Sex;
  gender: Gender;
  socialName?: string;
  contactInfo: ContactInfo;
  address?: Address;
  representative?: Representative;
}

export interface UpdatePatientRequest {
  firstName?: string;
  lastName?: string;
  birthDate?: string;
  sex?: Sex;
  gender?: Gender;
  socialName?: string;
  contactInfo?: ContactInfo;
  address?: Address;
  representative?: Representative;
}

