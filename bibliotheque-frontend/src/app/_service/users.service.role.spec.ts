import { UsersService } from './users.service';

describe('UsersService role matching', () => {
  it('cherche la correspondance dans tous les rôles autorisés', () => {
    const authService = {
      getRoles: () => [{ roleName: 'User' }]
    };
    const service = new UsersService({} as any, authService as any);

    expect(service.roleMatch(['Admin', 'User'])).toBeTrue();
  });

  it('refuse un rôle qui ne correspond pas', () => {
    const authService = {
      getRoles: () => [{ roleName: 'User' }]
    };
    const service = new UsersService({} as any, authService as any);

    expect(service.roleMatch(['Admin'])).toBeFalse();
  });
});