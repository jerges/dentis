export interface ContactInfo {
  email: string;
  phoneNumber: string;
  alternativePhone?: string;
}

export interface Address {
  street?: string;
  city?: string;
  state?: string;
  zipCode?: string;
}

export interface Representative {
  fullName: string;
  relationship: string;
  phoneNumber: string;
  email?: string;
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
  email: string;
  phoneNumber: string;
  alternativePhone?: string;
  street?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  representativeName?: string;
  representativeRelationship?: string;
  representativePhone?: string;
  representativeEmail?: string;
}

export type UpdatePatientRequest = Partial<CreatePatientRequest>;

