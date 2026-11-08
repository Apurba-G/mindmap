import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PasswordResetRequest,
  PasswordResetResponse,
  RecoveryInitiateRequest,
  RecoveryInitiateResponse
} from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class RecoveryService {
  private readonly apiUrl = '/api/auth/recovery';

  constructor(private http: HttpClient) {}

  /**
   * Submit recovery paragraph to initiate account recovery.
   */
  initiateRecovery(userId: string, paragraph: string): Observable<RecoveryInitiateResponse> {
    const payload: RecoveryInitiateRequest = {
      userId: userId.trim().toLowerCase(),
      paragraph: paragraph.trim()
    };
    return this.http.post<RecoveryInitiateResponse>(`${this.apiUrl}/initiate`, payload);
  }

  /**
   * Reset password with the one-time recovery token.
   * Token is passed via X-Recovery-Token header.
   */
  resetPassword(token: string, newPassword: string, confirm: string): Observable<PasswordResetResponse> {
    const payload: PasswordResetRequest = {
      newPassword,
      newPasswordConfirm: confirm
    };
    const headers = new HttpHeaders().set('X-Recovery-Token', token);
    return this.http.post<PasswordResetResponse>(`${this.apiUrl}/reset`, payload, { headers });
  }
}
