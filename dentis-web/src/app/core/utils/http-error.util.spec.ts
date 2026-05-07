import { HttpErrorResponse } from '@angular/common/http';
import { getHttpErrorMessage } from './http-error.util';

describe('getHttpErrorMessage', () => {
  it('should format backend validation field errors', () => {
    const error = new HttpErrorResponse({
      status: 400,
      statusText: 'Bad Request',
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Request validation failed',
        fieldErrors: [
          {
            field: 'birthDate',
            message: 'debe ser una fecha pasada'
          }
        ]
      }
    });

    expect(getHttpErrorMessage(error)).toBe('birthDate: debe ser una fecha pasada');
  });

  it('should resolve nested backend error envelopes', () => {
    expect(
      getHttpErrorMessage({
        error: {
          error: {
            message: 'Dentista no disponible'
          }
        }
      })
    ).toBe('Dentista no disponible');
  });
});

