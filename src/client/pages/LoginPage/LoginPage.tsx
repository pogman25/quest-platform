import { useIntl } from 'react-intl';
import type { SubmitEvent } from 'react';
import { useSession } from '../../hooks/useSession/useSession';
import './LoginPage.css';

export function LoginPage() {
    const { formatMessage } = useIntl();
    const { emailInput, signIn, signInError, signingIn } = useSession();

    const handleSubmit = (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (signingIn) {
            return;
        }
        const form = new FormData(event.currentTarget);
        signIn({ email: String(form.get('email')), password: String(form.get('password')) });
    };

    return (
        <section className="panel login-panel">
            <div className="eyebrow">{formatMessage({ id: 'common.brand' })}</div>
            <h1>{formatMessage({ id: 'login.title' })}</h1>
            <form onSubmit={handleSubmit} className="stack">
                <label className="field">
                    <span>{formatMessage({ id: 'login.email' })}</span>
                    <input ref={emailInput} name="email" type="email" autoComplete="username" required />
                </label>
                <label className="field">
                    <span>{formatMessage({ id: 'login.password' })}</span>
                    <input name="password" type="password" autoComplete="current-password" required />
                </label>
                <button className="button primary" type="submit" disabled={signingIn}>
                    {formatMessage({ id: 'login.signIn' })}
                </button>
                {signInError && (
                    <p className="notice error" role="alert">
                        {signInError.message}
                    </p>
                )}
            </form>
        </section>
    );
}
