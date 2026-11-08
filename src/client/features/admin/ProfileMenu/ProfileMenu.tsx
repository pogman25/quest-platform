import { useIntl } from 'react-intl';
import { ChevronDown, LogIn, LogOut, UserRound } from 'lucide-react';
import { ActionMenu } from '../../../components/ActionMenu/ActionMenu';

export interface ProfileMenuProps {
    signedIn: boolean;
    onSignIn: () => void;
    onSignOut: () => void;
}

export function ProfileMenu({ signedIn, onSignIn, onSignOut }: ProfileMenuProps) {
    const { formatMessage } = useIntl();
    const renderActions = (closeMenu: (restoreFocus?: boolean) => void) => {
        const handleAction = () => {
            closeMenu(signedIn);
            if (signedIn) {
                onSignOut();
            } else {
                onSignIn();
            }
        };

        return (
            <>
                <p className="profile-caption">
                    {signedIn
                        ? formatMessage({ id: 'common.organizer' })
                        : formatMessage({ id: 'common.guest' })}
                </p>
                <button className="action-menu-item" type="button" onClick={handleAction}>
                    {signedIn ? (
                        <LogOut size={16} aria-hidden="true" />
                    ) : (
                        <LogIn size={16} aria-hidden="true" />
                    )}
                    {signedIn
                        ? formatMessage({ id: 'login.signOut' })
                        : formatMessage({ id: 'login.signIn' })}
                </button>
            </>
        );
    };

    return (
        <ActionMenu
            label={formatMessage({ id: 'app.profileMenu' })}
            trigger={
                <>
                    <span className="profile-avatar" aria-hidden="true">
                        {signedIn ? 'О' : <UserRound size={17} />}
                    </span>
                    <ChevronDown size={14} aria-hidden="true" />
                </>
            }
        >
            {renderActions}
        </ActionMenu>
    );
}
