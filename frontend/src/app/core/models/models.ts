/**
 * Core Data Models & DTOs for the MindMap application.
 */

export interface RegisterRequest {
  displayName: string;
  userId: string;
  password: string;
  confirmPassword?: string;
}

export interface RegisterResponse {
  userId: string;
  displayName: string;
  recoveryParagraph: string;
  token?: string;
  message?: string;
}

export interface LoginRequest {
  userId: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: string;
  displayName: string;
  roles: string[];
  mustChangePassword: boolean;
  lockedUntil?: string;
  message?: string;
}

export interface UserInfo {
  userId: string;
  displayName: string;
  roles: string[];
  mustChangePassword: boolean;
}

export interface UserIdAvailabilityResponse {
  available: boolean;
  checkedVia: string;
  message?: string;
}

export interface RecoveryInitiateRequest {
  userId: string;
  paragraph: string;
}

export interface RecoveryInitiateResponse {
  recoveryToken: string;
  expiresIn: number;
  remainingAttempts?: number;
}

export interface PasswordResetRequest {
  token: string;
  newPassword: string;
  confirmPassword?: string;
}

export interface PasswordResetResponse {
  newRecoveryParagraph: string;
  message: string;
}

export interface OptionDto {
  id: string;
  text: string;
  value?: number;
  score?: number;
}

export interface QuestionDto {
  id: string;
  text: string;
  scenarioText?: string;
  contentWarning?: string;
  options: OptionDto[];
  isSkippable: boolean;
  timeLimitSeconds?: number;
  part?: string;
}

export interface SessionResponse {
  sessionId: string;
  level: number;
  currentPart?: string;
  currentQuestionIndex: number;
  totalQuestions: number;
  currentQuestion: QuestionDto | null;
  completed: boolean;
  warningRequired?: boolean;
  warningAcknowledged?: boolean;
}

export interface SubmitAnswerRequest {
  questionId: string;
  selectedOptionId?: string;
  responseTimeMs: number;
  skipped?: boolean;
}

export interface SubmitAnswerResponse {
  nextQuestion: QuestionDto | null;
  questionIndex: number;
  totalQuestions: number;
  completed: boolean;
  safetyFlagTriggered?: boolean;
  safetyAlertMessage?: string;
}

export type LevelStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'LOCKED';

export interface LevelStatusResponse {
  level1Status: LevelStatus;
  level2Status: LevelStatus;
  level3Status: LevelStatus;
  activeSessionId?: string;
  activeLevel?: number;
}

export interface AcknowledgmentRequest {
  level: number;
  part: string;
  accepted: boolean;
}

export interface UserProfileSummary {
  userId: string;
  displayName: string;
  completedLevels: number[];
  summaryText?: string;
  visibility: 'NONE' | 'SHORT' | 'DETAILED';
  generatedAt?: string;
}

export interface TraitScore {
  trait: string;
  score: number;
  confidence: number;
  label?: string;
}

export interface Temperament {
  primaryType: string;
  breakdown: { [key: string]: number };
  explanation: string;
  confidence: number;
}

export interface FullProfile {
  userId: string;
  displayName: string;
  level1Scores: TraitScore[];
  level2Scores: {
    ambiguity: number;
    darkTheme: number;
    moralFlexibility: number;
    consistency: number;
  };
  temperament: Temperament;
  fastAnswerCount: number;
  contradictionCount: number;
  safetyFlagCount: number;
  overallConfidence: number;
  generatedAt: string;
}

export interface AdminStats {
  totalUsers: number;
  completedL1: number;
  completedL2: number;
  completedL3: number;
  safetyFlagsCount: number;
}

export interface UserListItem {
  userId: string;
  displayName: string;
  roles: string[];
  active: boolean;
  level1Status: LevelStatus;
  level2Status: LevelStatus;
  level3Status: LevelStatus;
  createdAt: string;
}

export interface ScoringRule {
  id: string;
  level: number;
  trait: string;
  ruleName: string;
  ruleType: string;
  parameters: string;
  active: boolean;
}

export interface AuditLogEntry {
  id: string;
  timestamp: string;
  adminHandle: string;
  targetUser: string;
  action: string;
  ipAddress: string;
  details?: string;
}

export interface SafetyFlag {
  id: string;
  userId: string;
  timestamp: string;
  level: number;
  questionId: string;
  triggerReason: string;
  reviewed: boolean;
  reviewerHandle?: string;
  reviewedAt?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
