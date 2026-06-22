package it.polimi.ingsw.network.protocol.clientbound;


public interface LoginResultVisitor<R> {

    R visitLoginOk(LoginResult.LoginOk loginOk);

    R visitReconnectOk(LoginResult.ReconnectOk reconnectOk);

    R visitRejected(LoginResult.Rejected rejected);
}
