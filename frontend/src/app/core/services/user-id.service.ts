import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserIdAvailabilityResponse } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class UserIdService {
  private readonly apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  /**
   * Check if a given userId is available.
   * Backend uses Bloom filter + DB check.
   */
  checkAvailability(userId: string): Observable<UserIdAvailabilityResponse> {
    const params = new HttpParams().set('userId', userId.trim().toLowerCase());
    return this.http.get<UserIdAvailabilityResponse>(`${this.apiUrl}/check-availability`, { params });
  }

  /**
   * Get handle/userId suggestions based on display name and optional interests.
   */
  getSuggestions(name: string, interests: string[] = []): Observable<string[]> {
    return this.http.post<string[]>(`${this.apiUrl}/suggestions`, {
      name,
      interests
    });
  }
}
