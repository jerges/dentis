import { HttpErrorResponse } from '@angular/common/http';
import { ApiError, ApiFieldError } from '../models/api.model';

export function getHttpErrorMessage(error: unknown): string {
  if (isHttpErrorResponse(error) && error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifica la red e intenta nuevamente.';
  }

  const payload = isHttpErrorResponse(error) ? error.error : error;

  if (typeof payload === 'string' && payload.trim()) {
    return payload;
  }

  const apiError = extractApiError(payload);
  const fieldErrorMessage = formatFieldErrors(apiError?.fieldErrors);

  if (fieldErrorMessage) {
    return fieldErrorMessage;
  }

  const apiMessage = apiError?.message ?? apiError?.title;
  if (typeof apiMessage === 'string' && apiMessage.trim()) {
    return apiMessage;
  }

  if (isHttpErrorResponse(error)) {
    switch (error.status) {
      case 400:
        return 'La solicitud contiene datos inválidos.';
      case 401:
        return 'Tu sesión no es válida o ha expirado. Inicia sesión nuevamente.';
      case 403:
        return 'No tienes permisos para realizar esta acción.';
      case 404:
        return 'El recurso solicitado no existe o fue eliminado.';
      case 409:
        return 'No se pudo completar la operación por un conflicto de datos.';
      case 500:
        return 'El servidor encontró un error inesperado. Intenta más tarde.';
      default:
        return 'Ocurrió un error inesperado al procesar la operación.';
    }
  }

  return 'Ocurrió un error inesperado al procesar la operación.';
}

function formatFieldErrors(fieldErrors?: ApiFieldError[]): string {
  if (!Array.isArray(fieldErrors) || !fieldErrors.length) {
    return '';
  }

  return fieldErrors
    .map((fieldError) => formatFieldError(fieldError))
    .filter((message): message is string => !!message)
    .join(' · ');
}

function formatFieldError(fieldError: ApiFieldError): string | null {
  const field = typeof fieldError.field === 'string' ? fieldError.field.trim() : '';
  const message = typeof fieldError.message === 'string' ? fieldError.message.trim() : '';

  if (field && message) {
    return `${field}: ${message}`;
  }

  return field || message || null;
}

function extractApiError(payload: unknown): ApiError | null {
  let candidate = payload;

  for (let depth = 0; depth < 3; depth++) {
    if (!candidate || typeof candidate !== 'object') {
      return null;
    }

    if (hasApiErrorShape(candidate)) {
      return candidate as ApiError;
    }

    if (!hasErrorEnvelope(candidate)) {
      return candidate as ApiError;
    }

    candidate = candidate.error;
  }

  return null;
}

function hasErrorEnvelope(payload: object): payload is { error: ApiError } {
  return 'error' in payload;
}

function hasApiErrorShape(payload: object): boolean {
  return 'message' in payload || 'title' in payload || 'fieldErrors' in payload || 'code' in payload;
}

function isHttpErrorResponse(error: unknown): error is HttpErrorResponse {
  return error instanceof HttpErrorResponse;
}

