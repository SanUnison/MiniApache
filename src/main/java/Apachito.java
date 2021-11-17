import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class Apachito {

    /*
     *   Programa que implementa Document Root como parametro y muestra listado de archivos de directorios en servidor apache.
     *
     *   @version 1.0   16/11/2021
     *   @author Santiago Contreras
     */

    public static final String CLASS_NAME = Apachito.class.getSimpleName();

    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public static final int HTTP_PORT = 8080;

    public static void main(String[] args) {
        // http://localhost:8080/ejemplo.jpg

        // http://localhost:8080
        // http://localhost:8080/uno.html
        // https://mdn.mozillademos.org/files/13677/Fetching_a_page.png
        try {
            ServerSocket serverSocket
                    = new ServerSocket( HTTP_PORT  );
            LOG.info("SERVER UP");

            //
            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOG.info("CONNECT:" + clientSocket.toString() );
                Thread serviceProcess;
                if (args.length > 0) {
                    serviceProcess = new Thread(new HTTPService(clientSocket, args[0]));
                } else {
                    serviceProcess = new Thread(new HTTPService(clientSocket));

                }

                serviceProcess.start();
            }

        } catch (IOException e) {
            LOG.warning("Exception caught when trying to listen on port "
                    + HTTP_PORT + " or listening for a connection");
            LOG.warning(e.getMessage());
        }
    }


}
