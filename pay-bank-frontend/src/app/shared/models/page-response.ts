/** Paginated response returned by the backend list endpoints (`*PageResponse`). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
