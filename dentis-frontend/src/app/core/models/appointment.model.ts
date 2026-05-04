export interface Appointment {
  id: string;
  patientId: string;
  patientName: string;
  dentistId: string;
  dentistName: string;
  dentistColorCode?: string;
  startDateTime: string;
  endDateTime: string;
  durationMinutes: number;
  status: AppointmentStatus;
  reason?: string;
  notes?: string;
  treatmentPlanId?: string;
  reminderSent: boolean;
  createdAt: string;
}

export type AppointmentStatus =
  | 'SCHEDULED' | 'CONFIRMED' | 'IN_PROGRESS'
  | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface CreateAppointmentRequest {
  patientId: string;
  dentistId: string;
  startDateTime: string;
  endDateTime: string;
  reason?: string;
  notes?: string;
  treatmentPlanId?: string;
}
