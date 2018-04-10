package socket;

import java.net.Socket;

public interface ISocket {
	
	public void initSocketConnection(Socket serverSocket);
	public Object readSocketData();
	public Socket getServerSocket();
	public void closeSocket();
}
