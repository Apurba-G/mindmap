import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { assessmentGuard } from './core/guards/assessment.guard';

import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { RecoverComponent } from './pages/recover/recover.component';
import { ChangePasswordComponent } from './pages/change-password/change-password.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AssessmentLevelComponent } from './pages/assessment-level/assessment-level.component';
import { UserProfileComponent } from './pages/user-profile/user-profile.component';
import { AdminDashboardComponent } from './pages/admin/admin-dashboard/admin-dashboard.component';
import { UserListComponent } from './pages/admin/user-list/user-list.component';
import { UserProfileViewerComponent } from './pages/admin/user-profile-viewer/user-profile-viewer.component';
import { ScoringRulesComponent } from './pages/admin/scoring-rules/scoring-rules.component';
import { AuditLogComponent } from './pages/admin/audit-log/audit-log.component';
import { SafetyFlagsComponent } from './pages/admin/safety-flags/safety-flags.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard'
  },
  {
    path: 'login',
    component: LoginComponent,
    title: 'MindMap — Sign In'
  },
  {
    path: 'register',
    component: RegisterComponent,
    title: 'MindMap — Create Account'
  },
  {
    path: 'recover',
    component: RecoverComponent,
    title: 'MindMap — Account Recovery'
  },
  {
    path: 'change-password',
    component: ChangePasswordComponent,
    canActivate: [authGuard],
    title: 'MindMap — Change Password'
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
    title: 'MindMap — Dashboard'
  },
  {
    path: 'assessments/level-1',
    component: AssessmentLevelComponent,
    canActivate: [authGuard, assessmentGuard],
    data: { level: 1 },
    title: 'MindMap — Level 1 Assessment'
  },
  {
    path: 'assessments/level-2',
    component: AssessmentLevelComponent,
    canActivate: [authGuard, assessmentGuard],
    data: { level: 2 },
    title: 'MindMap — Level 2 Assessment'
  },
  {
    path: 'assessments/level-3',
    component: AssessmentLevelComponent,
    canActivate: [authGuard, assessmentGuard],
    data: { level: 3 },
    title: 'MindMap — Level 3 Assessment'
  },
  {
    path: 'profile',
    component: UserProfileComponent,
    canActivate: [authGuard],
    title: 'MindMap — My Profile'
  },

  // Admin Routes
  {
    path: 'admin',
    component: AdminDashboardComponent,
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    title: 'MindMap Admin — Console'
  },
  {
    path: 'admin/users',
    component: UserListComponent,
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    title: 'MindMap Admin — User Directory'
  },
  {
    path: 'admin/users/:userId',
    component: UserProfileViewerComponent,
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    title: 'MindMap Admin — Psychological Profile'
  },
  {
    path: 'admin/scoring-rules',
    component: ScoringRulesComponent,
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    title: 'MindMap Admin — Scoring Rules'
  },
  {
    path: 'admin/audit-log',
    component: AuditLogComponent,
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    title: 'MindMap Admin — Audit Ledger'
  },
  {
    path: 'admin/safety-flags',
    component: SafetyFlagsComponent,
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    title: 'MindMap Admin — Safety Moderation'
  },

  // Catch-all wildcard redirect
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
