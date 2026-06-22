package it.polimi.ingsw.network.protocol.clientbound;

import java.io.Serializable;


public sealed interface LoginResult extends Serializable
        permits LoginResult.LoginOk, LoginResult.ReconnectOk, LoginResult.Rejected {

    /** Whether the attempt let the player into the match. */
    boolean isSuccess();

    /** Human-readable reason when the attempt failed, {@code null} otherwise. */
    String errorReason();

    /** Double dispatch entry point: routes to the matching visitor method. */
    <R> R accept(LoginResultVisitor<R> visitor);

    /** A brand-new player was accepted into the lobby. */
    record LoginOk() implements LoginResult {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public String errorReason() {
            return null;
        }

        @Override
        public <R> R accept(LoginResultVisitor<R> visitor) {
            return visitor.visitLoginOk(this);
        }
    }

    /** A previously disconnected player resumed an existing match. */
    record ReconnectOk() implements LoginResult {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public String errorReason() {
            return null;
        }

        @Override
        public <R> R accept(LoginResultVisitor<R> visitor) {
            return visitor.visitReconnectOk(this);
        }
    }

    /** The attempt was refused; {@link #errorReason()} explains why. */
    record Rejected(String errorReason) implements LoginResult {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public <R> R accept(LoginResultVisitor<R> visitor) {
            return visitor.visitRejected(this);
        }
    }
}
