import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminStats, AuditLogEntry, Page, SafetyFlag, ScoringRule, UserListItem } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = '/api/admin';

  constructor(private http: HttpClient) {}

  /**
   * Get overall system metrics for the admin dashboard.
   */
  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.apiUrl}/stats`);
  }

  /**
   * Get paginated list of users with optional search query.
   */
  getUsers(page: number = 0, size: number = 10, search: string = ''): Observable<Page<UserListItem>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<Page<UserListItem>>(`${this.apiUrl}/users`, { params });
  }

  /**
   * Activate or deactivate a user account.
   */
  setUserActiveStatus(userId: string, active: boolean): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/users/${userId}/status`, { active });
  }

  /**
   * Get all scoring rules.
   */
  getScoringRules(): Observable<ScoringRule[]> {
    return this.http.get<ScoringRule[]>(`${this.apiUrl}/scoring-rules`);
  }

  /**
   * Update an existing scoring rule.
   */
  updateScoringRule(ruleId: string, rule: Partial<ScoringRule>): Observable<ScoringRule> {
    return this.http.put<ScoringRule>(`${this.apiUrl}/scoring-rules/${ruleId}`, rule);
  }

  /**
   * Get paginated audit logs.
   */
  getAuditLogs(page: number = 0, size: number = 20): Observable<Page<AuditLogEntry>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<AuditLogEntry>>(`${this.apiUrl}/audit-logs`, { params });
  }

  /**
   * Get paginated safety flags.
   */
  getSafetyFlags(page: number = 0, size: number = 20, unreviewedOnly: boolean = false): Observable<Page<SafetyFlag>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('unreviewedOnly', unreviewedOnly.toString());
    return this.http.get<Page<SafetyFlag>>(`${this.apiUrl}/safety-flags`, { params });
  }

  /**
   * Mark a safety flag as reviewed.
   */
  reviewSafetyFlag(flagId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/safety-flags/${flagId}/review`, {});
  }
}
