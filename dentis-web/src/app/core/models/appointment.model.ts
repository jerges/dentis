export type AppointmentStatus =
  | 'SCHEDULED'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW';

export interface Appointment {
  id: string;
  patientId: string;
  patientName: string;
  dentistId: string;
  dentistName: string;
  startDateTime: string;
  endDateTime: string;
  status: AppointmentStatus;
  consultationReason: string;
  notes?: string;
}

export interface CreateAppointmentRequest {
  patientId: string;
  dentistId: string;
  startDateTime: string;
  endDateTime: string;
  consultationReason: string;
  notes?: string;
}

