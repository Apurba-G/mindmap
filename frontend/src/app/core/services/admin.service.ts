import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminStats, AuditLogEntry, Page, SafetyFlag, ScoringRule, UserListItem } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  // Auth-service admin endpoints (users, audit logs)
  private readonly authAdminUrl = '/api/auth/admin';
  // Assessment-service admin endpoints (stats, safety flags)
  private readonly assessmentAdminUrl = '/api/assessments/admin';
  // Profiling-service admin endpoints (scoring rules)
  private readonly profilingAdminUrl = '/api/profiles/admin';

  constructor(private http: HttpClient) {}

  /**
   * Get overall system metrics for the admin dashboard.
   */
  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.assessmentAdminUrl}/stats`);
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
    return this.http.get<Page<UserListItem>>(`${this.authAdminUrl}/users`, { params });
  }

  /**
   * Activate or deactivate a user account.
   */
  setUserActiveStatus(userId: string, active: boolean): Observable<void> {
    const params = new HttpParams().set('active', active.toString());
    return this.http.patch<void>(`${this.authAdminUrl}/users/${userId}/status`, null, { params });
  }

  /**
   * Get all scoring rules.
   */
  getScoringRules(): Observable<ScoringRule[]> {
    return this.http.get<ScoringRule[]>(`${this.profilingAdminUrl}/scoring-rules`);
  }

  /**
   * Update an existing scoring rule.
   */
  updateScoringRule(ruleId: string, rule: Partial<ScoringRule>): Observable<ScoringRule> {
    return this.http.put<ScoringRule>(`${this.profilingAdminUrl}/scoring-rules/${ruleId}`, rule);
  }

  /**
   * Get paginated audit logs.
   */
  getAuditLogs(page: number = 0, size: number = 20): Observable<Page<AuditLogEntry>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<AuditLogEntry>>(`${this.authAdminUrl}/audit-logs`, { params });
  }

  /**
   * Get paginated safety flags.
   */
  getSafetyFlags(page: number = 0, size: number = 20, unreviewedOnly: boolean = false): Observable<Page<SafetyFlag>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('unreviewedOnly', unreviewedOnly.toString());
    return this.http.get<Page<SafetyFlag>>(`${this.assessmentAdminUrl}/safety-flags`, { params });
  }

  /**
   * Mark a safety flag as reviewed.
   */
  reviewSafetyFlag(flagId: string): Observable<void> {
    return this.http.post<void>(`${this.assessmentAdminUrl}/safety-flags/${flagId}/review`, {});
  }
}
