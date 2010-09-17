package org.dcache.auth;

import javax.security.auth.Subject;

import diskCacheV111.util.CacheException;

/**
 * LoginStrategy that caches the last login request. If the following
 * login uses the same Subject, then the cached LoginReply is
 * provided.
 */
public class CachingLoginStrategy implements LoginStrategy
{
    private final LoginStrategy _loginStrategy;
    private LoginReply _lastReply;
    private Subject _lastSubject;

    public CachingLoginStrategy(LoginStrategy loginStrategy)
    {
        _loginStrategy = loginStrategy;
    }

    public synchronized LoginReply login(Subject subject) throws CacheException
    {
        if (!subject.equals(_lastSubject)) {
            _lastReply = _loginStrategy.login(subject);
            _lastSubject = new Subject(true,
                                       subject.getPrincipals(),
                                       subject.getPublicCredentials(),
                                       subject.getPrivateCredentials());
        }
        return _lastReply;
    }
}