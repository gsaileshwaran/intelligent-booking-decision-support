export interface AuditLog {
  auditLogId: number;
  actorUserId: number;
  action: string;
  actionType?: string;
  entityType: string;
  entityId: number;
  occurredAt: string;
  createdAt?: string;
  oldValue?: string;
  newValue?: string;
  details?: string;
}

export interface AdminUser {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  accountStatus: string;
  createdAt: string;
  roles: string[];
}

export interface RoleInfo {
  roleId: number;
  roleName: string;
  description?: string;
  permissions?: string[];
}
