import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FullProfile, Temperament, UserProfileSummary } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private readonly apiUrl = '/api/profiles';

  constructor(private http: HttpClient) {}

  /**
   * Get gentle profile summary for currently logged-in user.
   */
  getMyProfile(): Observable<UserProfileSummary> {
    return this.http.get<UserProfileSummary>(`${this.apiUrl}/me`);
  }

  /**
   * Admin: Get full psychological profile for a user.
   */
  getFullProfile(userId: string): Observable<FullProfile> {
    return this.http.get<FullProfile>(`${this.apiUrl}/admin/${userId}`);
  }

  /**
   * Admin: Get temperament profile for a user.
   */
  getTemperament(userId: string): Observable<Temperament> {
    return this.http.get<Temperament>(`${this.apiUrl}/admin/${userId}/temperament`);
  }

  /**
   * Export user's own data as JSON/text download.
   */
  exportData(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/me/export`, { responseType: 'blob' });
  }

  /**
   * Request account deletion (GDPR compliance).
   */
  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me`);
  }
}
