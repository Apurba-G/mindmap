import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, RegisterRequest, RegisterResponse, UserInfo } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = '/api/auth';
  private readonly TOKEN_KEY = 'mindmap_token';
  private readonly USER_KEY = 'mindmap_user';

  private currentUserSubject = new BehaviorSubject<UserInfo | null>(this.loadUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /**
   * Log in user with credentials.
   */
  login(userId: string, password: string): Observable<LoginResponse> {
    const payload: LoginRequest = { userId, password };
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, payload).pipe(
      tap((response: LoginResponse) => {
        if (response.token) {
          this.setToken(response.token);
          const userInfo: UserInfo = {
            userId: response.userId,
            displayName: response.displayName,
            roles: response.roles || [],
            mustChangePassword: response.mustChangePassword
          };
          this.setCurrentUser(userInfo);
        }
      })
    );
  }

  /**
   * Register a new user.
   */
  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.apiUrl}/register`, request).pipe(
      tap((response: RegisterResponse) => {
        if (response.token) {
          this.setToken(response.token);
          const userInfo: UserInfo = {
            userId: response.userId,
            displayName: response.displayName,
            roles: ['ROLE_USER'],
            mustChangePassword: false
          };
          this.setCurrentUser(userInfo);
        }
      })
    );
  }

  /**
   * Log out current user and clear stored credentials.
   */
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  /**
   * Checks if user is authenticated and token is not expired.
   */
  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    return !this.isTokenExpired(token);
  }

  /**
   * Check if current user has the specified role.
   */
  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    if (!user || !user.roles) {
      return false;
    }
    const cleanRole = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
    return user.roles.includes(cleanRole) || user.roles.includes(role);
  }

  /**
   * Get JWT token from storage.
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Check if user must change password.
   */
  mustChangePassword(): boolean {
    const user = this.getCurrentUser();
    return !!(user && user.mustChangePassword);
  }

  /**
   * Set mustChangePassword status after password update.
   */
  setMustChangePassword(mustChange: boolean): void {
    const user = this.getCurrentUser();
    if (user) {
      user.mustChangePassword = mustChange;
      this.setCurrentUser(user);
    }
  }

  /**
   * Get currently logged-in user info.
   */
  getCurrentUser(): UserInfo | null {
    return this.currentUserSubject.value;
  }

  private setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  private setCurrentUser(user: UserInfo): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  private loadUserFromStorage(): UserInfo | null {
    const raw = localStorage.getItem(this.USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserInfo;
    } catch {
      return null;
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return true;
      }
      const payload = JSON.parse(atob(parts[1]));
      if (!payload.exp) {
        return false;
      }
      const expirationDate = new Date(payload.exp * 1000);
      return expirationDate <= new Date();
    } catch {
      return true;
    }
  }
}
