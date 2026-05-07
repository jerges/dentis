export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: ApiError;
  timestamp: string;
}

export interface ApiFieldError {
  field?: string;
  message?: string;
}

export interface ApiError {
  code?: string;
  message?: string;
  title?: string;
  fieldErrors?: ApiFieldError[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

