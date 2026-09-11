export class Users {
    userId: number;
    username: string;
    name: string;
    password: string;
    /** Initialisé avec un rôle Adhérent par défaut pour que le ngModel du
     * formulaire (user.role[0].roleName) ne plante jamais. */
    role: any[] = [{ roleName: 'User' }];
}
