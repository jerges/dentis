import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '@environments/environment';
import { SKIP_GLOBAL_ERROR_HANDLER } from '../interceptors/http-error.interceptor';

interface HealthResponse {
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class BackendHealthService {
  private readonly healthUrl = `${environment.apiUrl}/actuator/health`;

  constructor(private readonly http: HttpClient) {}

  check(): Observable<boolean> {
	const context = new HttpContext().set(SKIP_GLOBAL_ERROR_HANDLER, true);

	return this.http.get(this.healthUrl, {
	  context,
	  observe: 'response',
	  responseType: 'text'
	}).pipe(
	  map((response) => this.isHealthyResponse(response)),
	  catchError(() => of(false))
	);
  }

  private isHealthyResponse(response: HttpResponse<string>): boolean {
	if (!response.ok) {
	  return false;
	}

	const healthStatus = this.extractHealthStatus(response.body);

	if (healthStatus !== null) {
	  return healthStatus === 'UP';
	}

	return true;
  }

  private extractHealthStatus(body: string | null): string | null {
	const normalizedBody = body?.trim();

	if (!normalizedBody) {
	  return null;
	}

	try {
	  const parsedBody = JSON.parse(normalizedBody) as HealthResponse | string;

	  if (typeof parsedBody === 'string') {
		return parsedBody.toUpperCase();
	  }

	  return parsedBody.status?.toUpperCase() ?? null;
	} catch {
	  return normalizedBody.toUpperCase();
	}
  }
}

