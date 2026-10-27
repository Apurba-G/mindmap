import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AcknowledgmentRequest,
  LevelStatusResponse,
  SessionResponse,
  SubmitAnswerRequest,
  SubmitAnswerResponse
} from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AssessmentService {
  private readonly apiUrl = '/api/assessments';

  constructor(private http: HttpClient) {}

  /**
   * Start a new assessment session for a given level (1, 2, or 3).
   */
  createSession(level: number): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(`${this.apiUrl}/sessions`, { level });
  }

  /**
   * Get the current active session if one is in progress.
   */
  getCurrentSession(): Observable<SessionResponse> {
    return this.http.get<SessionResponse>(`${this.apiUrl}/sessions/current`);
  }

  /**
   * Submit an answer to the current question in the session.
   */
  submitAnswer(sessionId: string, answer: SubmitAnswerRequest): Observable<SubmitAnswerResponse> {
    return this.http.post<SubmitAnswerResponse>(`${this.apiUrl}/sessions/${sessionId}/answers`, answer);
  }

  /**
   * Skip a skippable question in the session.
   */
  skipQuestion(sessionId: string, questionId: string): Observable<SubmitAnswerResponse> {
    const payload: SubmitAnswerRequest = {
      questionId,
      responseTimeMs: 0,
      skipped: true
    };
    return this.http.post<SubmitAnswerResponse>(`${this.apiUrl}/sessions/${sessionId}/answers`, payload);
  }

  /**
   * Complete the session and finalize scores.
   */
  completeLevel(sessionId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/sessions/${sessionId}/complete`, {});
  }

  /**
   * Get the completion and lock status for all levels for the current user.
   */
  getLevelStatus(): Observable<LevelStatusResponse> {
    return this.http.get<LevelStatusResponse>(`${this.apiUrl}/status`);
  }

  /**
   * Acknowledge sensitive content warning for a level part (e.g. L2B).
   */
  acknowledgeWarning(level: number, part: string, accepted: boolean): Observable<void> {
    const payload: AcknowledgmentRequest = { level, part, accepted };
    return this.http.post<void>(`${this.apiUrl}/warning-acknowledgment`, payload);
  }
}
