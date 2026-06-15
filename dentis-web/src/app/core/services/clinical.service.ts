import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '@environments/environment';
import {
  AddDiagnosisRequest, AddEvolutionRequest, AddTreatmentPlanRequest,
  ClinicalRecord, ClinicalAttachment, DentitionType, OdontogramTooth,
  PresignedUploadResponse, RegisterAttachmentRequest
} from '../models/clinical.model';
import { ApiResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class ClinicalService {
  private readonly base = `${environment.apiUrl}/api/v1/clinical-records`;

  constructor(private readonly http: HttpClient) {}

  getOrCreate(patientId: string): Observable<ClinicalRecord> {
    return this.http
      .get<ApiResponse<ClinicalRecord>>(`${this.base}/patient/${patientId}`)
      .pipe(map((r) => r.data));
  }

  updateOdontogram(patientId: string, teeth: OdontogramTooth[]): Observable<ClinicalRecord> {
    return this.http
      .put<ApiResponse<ClinicalRecord>>(`${this.base}/patient/${patientId}/odontogram`, { teeth })
      .pipe(map((r) => r.data));
  }

  updateDentitionType(patientId: string, dentitionType: DentitionType): Observable<ClinicalRecord> {
    return this.http
      .patch<ApiResponse<ClinicalRecord>>(
        `${this.base}/patient/${patientId}/dentition-type?dentitionType=${dentitionType}`, {})
      .pipe(map((r) => r.data));
  }

  addEvolution(patientId: string, request: AddEvolutionRequest): Observable<ClinicalRecord> {
    return this.http
      .post<ApiResponse<ClinicalRecord>>(`${this.base}/patient/${patientId}/evolutions`, request)
      .pipe(map((r) => r.data));
  }

  addDiagnosis(patientId: string, request: AddDiagnosisRequest): Observable<ClinicalRecord> {
    return this.http
      .post<ApiResponse<ClinicalRecord>>(`${this.base}/patient/${patientId}/diagnoses`, request)
      .pipe(map((r) => r.data));
  }

  addTreatmentPlan(patientId: string, request: AddTreatmentPlanRequest): Observable<ClinicalRecord> {
    return this.http
      .post<ApiResponse<ClinicalRecord>>(`${this.base}/patient/${patientId}/treatment-plans`, request)
      .pipe(map((r) => r.data));
  }

  presignAttachmentUpload(patientId: string, fileName: string, contentType: string): Observable<PresignedUploadResponse> {
    return this.http
      .post<ApiResponse<PresignedUploadResponse>>(`${this.base}/patient/${patientId}/attachments/presign`, { fileName, contentType })
      .pipe(map((r) => r.data));
  }

  registerAttachment(patientId: string, req: RegisterAttachmentRequest): Observable<ClinicalAttachment> {
    return this.http
      .post<ApiResponse<ClinicalAttachment>>(`${this.base}/patient/${patientId}/attachments`, req)
      .pipe(map((r) => r.data));
  }

  listAttachments(patientId: string, toothNumber?: number): Observable<ClinicalAttachment[]> {
    const params = toothNumber != null ? `?toothNumber=${toothNumber}` : '';
    return this.http
      .get<ApiResponse<ClinicalAttachment[]>>(`${this.base}/patient/${patientId}/attachments${params}`)
      .pipe(map((r) => r.data));
  }

  deleteAttachment(patientId: string, attachmentId: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.base}/patient/${patientId}/attachments/${attachmentId}`)
      .pipe(map(() => undefined));
  }

  uploadToS3(uploadUrl: string, file: File): Observable<void> {
    return this.http.put<void>(uploadUrl, file, {
      headers: { 'Content-Type': file.type }
    });
  }
}
