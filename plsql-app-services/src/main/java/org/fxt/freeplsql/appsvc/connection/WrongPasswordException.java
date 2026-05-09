package org.fxt.freeplsql.appsvc.connection;

/**
 * Thrown when {@link CryptoStore#unlock} fails the GCM auth-tag check, which
 * almost always means the master password was wrong (could also be a corrupt
 * file — same observable failure).
 */
public final class WrongPasswordException extends Exception {

    public WrongPasswordException() {
        super("Wrong master password");
    }
}
