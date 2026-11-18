/** Body of `POST /api/auth/login` (see `LoginRequest` in the backend OpenAPI contract). */
export interface LoginRequest {
  alias: string;
  password: string;
}

/** Response of `POST /api/auth/login` and `POST /api/auth/refresh` (`AuthResponse`). */
export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  /** Access token lifetime in seconds. */
  expiresIn: number;
  alias: string;
}
