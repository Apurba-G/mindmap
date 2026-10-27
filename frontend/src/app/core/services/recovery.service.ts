import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
  private readonly apiUrl = '/api/recovery';

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
   */
  resetPassword(token: string, newPassword: string, confirm: string): Observable<PasswordResetResponse> {
    const payload: PasswordResetRequest = {
      token,
      newPassword,
      confirmPassword: confirm
    };
    return this.http.post<PasswordResetResponse>(`${this.apiUrl}/reset-password`, payload);
  }
}
