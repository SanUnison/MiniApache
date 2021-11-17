import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HTTPService implements Runnable {
    public static final String CLASS_NAME = HTTPService.class.getSimpleName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public static String DOCUMENT_ROOT;

    private Socket clientSocket;
    private BufferedReader in;
    private PrintStream out;

    public HTTPService(Socket c) {
        super();

        clientSocket = c;
        DOCUMENT_ROOT = System.getProperty("user.dir");

        System.out.println(DOCUMENT_ROOT);

        iniciarStreams();
    }

    public HTTPService(Socket c, String document_root) {
        super();

        clientSocket = c;

        if (Paths.get(document_root).toFile().exists()){
            DOCUMENT_ROOT = document_root;
        } else {
            DOCUMENT_ROOT = System.getProperty("user.dir");
        }
        iniciarStreams();
    }

    private void iniciarStreams() {
        try {
            out = new PrintStream(clientSocket.getOutputStream(), true);
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
        }

        try {
            in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
        }
    }

    @Override
    public void run() {

        try {

            String requestLine;
            String commandLine = null;
            String body = null;
            String contentLength = null;

            boolean hasBody = false;
            // leer la solicitud del cliente
            while ((requestLine = in.readLine()) != null) {

                if (requestLine.startsWith("GET")) {
                    LOG.info(requestLine);
                    commandLine = requestLine;
                }
                if (requestLine.startsWith("POST")) {
                    LOG.info(requestLine);
                    commandLine = requestLine;
                    hasBody = true;
                }

                if (requestLine.startsWith("Content-Length:")) {
                    contentLength = requestLine.substring(requestLine.indexOf(":") + 1);

                    LOG.info(contentLength);
                    //commandLine = requestLine;
                    //hasBody = true;
                }

                //System.out.println(requestLine);
                //Si recibimos linea en blanco, es fin del la solicitud
                if (requestLine.isEmpty()) {

                    if (hasBody) {
                        int len = Integer.parseInt(contentLength.trim());
                        char buffer[] = new char[len];
                        in.read(buffer);
                        body = new String(buffer);
                    }
                    break;
                }
            }

            String fileName = null;
            if (commandLine == null) {
                notImplemented();
            } else {
                // GET / HTTP/1.1
                // GET /uno.html HTTP/1.1
                String tokens[] = commandLine.split("\\s+");
                System.out.println("Arrays.toString(tokens); = " + Arrays.toString(tokens));
                //fileName = tokens[1].substring(tokens[1].lastIndexOf('/') + 1);

                if (tokens[1].length() == 1) {
                    fileName = DOCUMENT_ROOT + "/index.html";
                } else {
                    fileName = DOCUMENT_ROOT + tokens[1];
                    System.out.println("fileName = " + fileName);                }
                File fp = new File(fileName);
                if (fp.isDirectory()){
                    LOG.info("Es un directorio: " + fileName);
                    dirListing(fileName);
                    clientSocket.close();
                    return;
                }
            }

            Path fp = Paths.get(fileName);
            File filePointer = fp.toFile();

            if (fileName.contains(".php")) {
                LOG.info("FORM");
                if (commandLine.startsWith("GET")) {
                    doGet(commandLine);
                } else {
                    doPost(body);
                }
            } else if (!filePointer.exists()) {
                notFound();
            } else if (fileName.endsWith(".ico") || fileName.endsWith(".png")
                    || fileName.endsWith(".jpg") || fileName.endsWith(".gif")) {

                LOG.info("IMAGE");

                sendIMGFile(filePointer);

            } else {
                LOG.info("TEXT");
                sendHTMLFile(filePointer);
            }

            clientSocket.close();

        } catch (IOException ex) {
            LOG.severe(ex.getMessage());

        }
    }

    private void dirListing(String ruta) throws IOException {
        StringBuilder html = new StringBuilder("<html><head>");
        html.append("<title>Variables</title>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("</head><body>");

        Path dir = Paths.get(ruta);
        ExploraRuta er = new ExploraRuta(dir);
        Files.walkFileTree(dir, er);
        LinkedList<String> fileNameList = er.getFileNameList();
        LinkedList<Path> dirNameList = er.getDirNameList();

        String rutaRelativa = dir.toString().replaceAll(DOCUMENT_ROOT + "/", "");
        html.append("<h1>Listado de directorios de " + rutaRelativa + "/</h1>");

        for (Path dirName:
                dirNameList) {
            html.append("<li><a href=/" + rutaRelativa +  dirName + "> " + dirName.toString().replaceAll(DOCUMENT_ROOT, "") + "</a></li>");
        }

        for (String fileName:
             fileNameList) {
            html.append("<li><a href=/" + rutaRelativa + "/" +  fileName + "> " + fileName + "</a></li>");
        }
        html.append("</body></html>");


        out.println("HTTP/1.1 200 OK");
        //out.println(lastModified(filePointer));
        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: " + html.length());
        out.println();
        out.println(html.toString());

    }

    private String getExtension(String f) {

        int p = f.lastIndexOf('.');

        return f.substring(p + 1);
    }

    private void sendIMGFile(File filePointer) {

        LOG.info(filePointer.getName());

        out.println("HTTP/1.1 200 OK");

        out.println(lastModified(filePointer));

        String content = getExtension(filePointer.getName());

        if (content.equals("ico")) {
            out.println("Content-Type: image/ico");
        }
        if (content.equals("png")) {
            out.println("Content-Type: image/png");
        }
        if (content.equals("jpg")) {
            out.println("Content-Type: image/jpeg");
        }
        if (content.equals("gif")) {
            out.println("Content-Type: image/gif");
        }

        //out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: " + filePointer.length());
        out.println();
        out.flush();
        LOG.log(Level.INFO, "Content-Length: {0}", filePointer.length());

        FileInputStream file;
        try {
            file = new FileInputStream(filePointer);
            int data;

            while ((data = file.read()) != -1) {
                out.write(data);

            }
            out.flush();
            file.close();

        } catch (FileNotFoundException ex) {
           LOG.severe(ex.getMessage());
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
        }

    }

    private void sendHTMLFile(File filePointer) {
        System.out.println(filePointer.getName() + ", " + filePointer.length());
        out.println("HTTP/1.1 200 OK");

        out.println(lastModified(filePointer));

        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: " + filePointer.length());
        out.println();

        FileReader file;
        try {
            file = new FileReader(filePointer);
            int data;

            while ((data = file.read()) != -1) {
                out.write(data);

                //System.out.println(".");
            }
            out.flush();
            file.close();
        } catch (FileNotFoundException ex) {
            LOG.severe(ex.getMessage());
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
        }

    }

    private String lastModified(File f) {
        long d = f.lastModified();

        Date lastModified = new Date(d);

        return "Last-Modified: " + lastModified.toString();
    }

    public void notImplemented() {
        File f = new File("501.html");

        out.println("HTTP/1.1 501 Not Implemented");
        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: " + f.length());
        out.println();

        FileReader file = null;
        try {
            file = new FileReader(f);

            int data;

            while ((data = file.read()) != -1) {
                out.write(data);
                out.flush();
                //System.out.println(".");
            }
            file.close();
        } catch (FileNotFoundException ex) {
            LOG.severe(ex.getMessage());
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
        }

    }

    public void notFound() {
        File f = new File("404.html");

        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: " + f.length());
        out.println();

        FileReader file = null;
        try {
            file = new FileReader(f);

            int data;

            while ((data = file.read()) != -1) {
                out.write(data);
                out.flush();
                //System.out.println(".");
            }
            file.close();
        } catch (FileNotFoundException ex) {
            LOG.severe(ex.getMessage());
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
        }

    }

    private void doGet(String commandLine) {
        StringBuilder response = new StringBuilder();
        System.out.println(commandLine);

        String query = commandLine.substring(commandLine.lastIndexOf('?') + 1);

        System.out.println(query);

        String tokens[] = query.split("[&,\\s]");

        StringBuilder html = new StringBuilder("<html><head>");
        html.append("<title>Variables</title>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("</head><body>");
        html.append("<h1>Lista de variables</h1>");
        html.append("<table style=\"width:50%\">");

        String var = null;
        String val = null;

        for (int i = 0; i < tokens.length; i++) {
            System.out.println(tokens[i]);
            if (tokens[i].contains("=")) {
                html.append("<tr><td>");
                var = tokens[i].substring(0, tokens[i].lastIndexOf('='));

                html.append(var);
                html.append("</td><td>");
                //System.out.println(var);

                val = tokens[i].substring(tokens[i].lastIndexOf('=') + 1);
                html.append(val);
                html.append("</td></tr>");
                //System.out.println( val );

            }
        }
        html.append("</table></body></html>");

        out.println("HTTP/1.1 200 OK");

        //out.println(lastModified(filePointer));
        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: " + html.length());
        out.println();
        out.println(html.toString());

    }

    private void doPost(String commandLine) {
        StringBuilder response = new StringBuilder();
        System.out.println(commandLine);

        //String query = commandLine.substring(commandLine.lastIndexOf('?') + 1);
        String tokens[] = commandLine.split("[&,\\s]");

        StringBuilder html = new StringBuilder("<html><head>");
        html.append("<title>Variables</title>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("</head><body>");
        html.append("<h1>Lista de variables</h1>");
        html.append("<table style=\"width:50%\">");

        String var = null;
        String val = null;

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].contains("=")) {
                html.append("<tr><td>");
                var = tokens[i].substring(0, tokens[i].lastIndexOf('='));

                html.append(var);
                html.append("</td><td>");
                //System.out.println(var);

                val = tokens[i].substring(tokens[i].lastIndexOf('=') + 1);
                html.append(val);
                html.append("</td></tr>");
                //System.out.println( val );

            }
        }
        html.append("</table></body></html>");

        out.println("HTTP/1.1 200 OK");

        //out.println(lastModified(filePointer));
        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: " + html.length());
        out.println();
        out.println(html.toString());

    }
}
