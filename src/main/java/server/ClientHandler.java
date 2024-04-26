package server;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.fileupload.FileUploadException;
import server.actors.ServerActor;
import server.messages.tasks.ImageCompressionTask;
import server.beans.User;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import static akka.pattern.Patterns.ask;
import scala.concurrent.duration.Duration;
import akka.util.Timeout;
import server.messages.data.AskUserTasksMessage;
import server.messages.data.ResponseUserTasksMessage;
import server.messages.tasks.NumberSumTask;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import server.messages.tasks.TextFormattingTask;


/**
 * Main client handler
 * Receives HTTPRequests parse them and forwards the data to the server for processing
 */
public class ClientHandler extends Thread {

    //server port
    private final static int httpPort = 8600;
    //path to access offered services
    private final static String httpCompressServicePath = "/compressService";
    private final static String httpSumServicePath = "/sumService";
    private final static String httpFormatServicePath = "/formatService";
    private final static String httpLoginPath = "/userLogin";

    //simulate a database with user credentials
    private static Map<String, String> db_users;

    //Actor system for server
    private static ActorSystem sys;
    private static ActorRef server;

    public ClientHandler(Map<String, String> db_users) {
        ClientHandler.db_users = db_users;
    }

    public void run() {

        sys = ActorSystem.create("System");
        server = sys.actorOf(ServerActor.props(), "CentralServer");

        HttpServer server;
        try {
            //create an HTTPServer
            server = HttpServer.create(new InetSocketAddress(StringUtils.httpServers, httpPort), 0);
            //bind service handlers to each path
            HttpContext contextCompress = server.createContext(httpCompressServicePath);
            contextCompress.setHandler(ClientHandler::handleRequestCompress);
            HttpContext contextFormat = server.createContext(httpFormatServicePath);
            contextFormat.setHandler(ClientHandler::handleRequestFormat);
            HttpContext contextSum = server.createContext(httpSumServicePath);
            contextSum.setHandler(ClientHandler::handleRequestSum);
            HttpContext contextLogin = server.createContext(httpLoginPath);
            contextLogin.setHandler(ClientHandler::handleRequestLogin);
            server.start();
            System.out.println("CompressService is up at " + StringUtils.httpServers + ":" + httpPort + httpCompressServicePath);
            System.out.println("FormatService is up at " + StringUtils.httpServers + ":" + httpPort + httpFormatServicePath);
            System.out.println("SumService is up at " + StringUtils.httpServers + ":" + httpPort + httpSumServicePath);
            System.out.println("LoginService is up at " + StringUtils.httpServers + ":" + httpPort + httpLoginPath);
        } catch (IOException e) {
            System.out.println("ERROR - unable to start user service on port " + httpPort + ". closing\n");
        }
    }

    /**
     * Handle an HTTPRequest for formatting a test
     * The data received is in multipart/form-data and requires parsing to obtain its part
     * (every part is a form field sent by the client: Sender,payload,resultDirectory)
     * @param exchange HTTPExchange with the client
     * @throws IOException if data contains errors
     */
    private static void handleRequestFormat(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if ("POST".equals(exchange.getRequestMethod())) {
            try {
                Map<String,FileItem> parts = getFormParts(exchange);
                TextFormattingTask task = new TextFormattingTask(parts);
                server.tell(task,null);
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                exchange.sendResponseHeaders(400, 0);
                byte[] response = "ERROR WHILE PARSING FORMAT DATA".getBytes();
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
                exchange.getResponseBody().close();
            }
        }
    }

    /**
     * Handle an HTTPRequest for compressing an image
     * The data received is in multipart/form-data and requires parsing to obtain its part
     * (every part is a form field sent by the client: Sender,compression ratio,payload,resultDirectory)
     *
     * Also handles the request for task updating
     *
     * @param exchange HTTPExchange with the client
     * @throws IOException if data contains errors
     */
    private static void handleRequestCompress(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if ("POST".equals(exchange.getRequestMethod())) {
            try {
                Map<String,FileItem> parts = getFormParts(exchange);
                ImageCompressionTask task = new ImageCompressionTask(parts);
                server.tell(task, null);
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                exchange.sendResponseHeaders(400, 0);
                byte[] response = "ERROR WHILE PARSING COMPRESS DATA".getBytes();
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
                exchange.getResponseBody().close();
            }
        } else if ("GET".equals(exchange.getRequestMethod())) {
            try {
                String data = exchange.getRequestURI().getQuery();
                String username = data.split("=")[1];
                String response = askTaskData(username);
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (Exception e) {
                exchange.sendResponseHeaders(401, 0);
                byte[] response = "ERROR WHILE GETTING TASK DATA".getBytes();
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
                exchange.getResponseBody().close();
            }
            exchange.getResponseBody().close();
        }
    }

    /**
     * Handle an HTTPRequest for sum task
     * The data received is NOT from a multipart/form-data so parsing happens line by line of HTTPBody
     * (every line is a form field sent by the client: Sender,num1,num2,resultDirectory)
     * @param exchange HTTPExchange with the client
     * @throws IOException if data contains errors
     */
    private static void handleRequestSum(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if ("POST".equals(exchange.getRequestMethod())) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            try {
                Gson gson = new Gson();
                NumberSumTask task = gson.fromJson(stringBuilder.toString(), NumberSumTask.class);
                //todo change null to nosender!
                server.tell(task, ActorRef.noSender());
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                exchange.sendResponseHeaders(400, 0);
                byte[] response = "ERROR WHILE PARSING SUM DATA".getBytes();
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
                exchange.getResponseBody().close();
            }
        }
    }

    /**
     * Function used to request data about client's tasks to the server.
     * It uses ask actor method and wait for the Future response to be delivered
     *
     * @param username user requesting the tasks
     * @return a Json string containing the task entities
     */
    private static String askTaskData(String username) {
        final Timeout t = new Timeout(Duration.create(5, TimeUnit.SECONDS));
        scala.concurrent.Future<Object> waitingForData = ask(server, new AskUserTasksMessage(username), 1000); // using 1000ms timeout
        try {
            ResponseUserTasksMessage m = (ResponseUserTasksMessage) waitingForData.result(t.duration(), null);
            return m.getData();
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles HTTPRequest for login (both post if register and get if login)
     * The data is NOT a multipart/form-data and parsing of the body can happen line by line
     * @param exchange HTTPExchange with the client
     * @throws IOException if data is malformed
     */
    private static void handleRequestLogin(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        // Signup
        if ("POST".equals(exchange.getRequestMethod())) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            try {
                String[] params = stringBuilder.toString().split("&");
                if (params.length != 2)
                    throw new Exception();
                User myUser = new User();
                myUser.setUsername(params[0].split("=")[1]);
                myUser.setPassword(params[1].split("=")[1]);
                // if the user doesn't exists, add it
                if (db_users.get(myUser.getUsername()) == null) {
                    createNewUser(myUser.getUsername(), myUser.getPassword());
                    exchange.sendResponseHeaders(200, 0);
                } else
                    exchange.sendResponseHeaders(401, 0);

                exchange.getResponseBody().close();
            } catch (Exception e) {
                exchange.sendResponseHeaders(400, 0);
                exchange.getResponseBody().close();
            }
        }
        // Login
        else if ("GET".equals(exchange.getRequestMethod())) {
            try {
                String[] credentials = exchange.getRequestURI().getQuery().split("&");
                String username = credentials[0].split("=")[1];
                String password = credentials[1].split("=")[1];
                if (checkLogin(username, password))
                    exchange.sendResponseHeaders(200, 0);
                else
                    exchange.sendResponseHeaders(401, 0);

                exchange.getResponseBody().close();
            } catch (Exception e) {
                exchange.sendResponseHeaders(401, 0);
                exchange.getResponseBody().close();
            }
        }
    }

    /**
     * Add a new user to the mock database
     * @param username user chosen username (also used as key)
     * @param password user chosen password
     */
    private static void createNewUser(String username, String password) {
        ClientHandler.db_users.put(username, password);
    }

    /**
     * Checks if credentials for specified username are correct
     * @param username username
     * @param password password to check
     * @return true if credentials match db data
     */
    private static boolean checkLogin(String username, String password) {
        String usrPwd = db_users.get(username);
        if (password != null) {
            return usrPwd.equals(password);
        }
        return false;

    }

    /**
     * Utility function used to parse a multipart/form-data and obtain its parts
     * It does so by creating a ServletFileUpload class and parsing the request through it
     *
     * @param exchange HTTPExchange with client
     * @return A MAP containing part name and part data for easy access
     * @throws FileUploadException if parsing fails at any point
     */
    public static Map<String,FileItem> getFormParts(HttpExchange exchange) throws FileUploadException {
        DiskFileItemFactory d = new DiskFileItemFactory();
        List<FileItem> parts;
        ServletFileUpload up = new ServletFileUpload(d);
        parts = up.parseRequest(new RequestContext() {
            @Override
            public String getCharacterEncoding() {
                return "UTF-8";
            }

            @Override
            public int getContentLength() {
                return 0; //tested to work with 0 as return
            }

            @Override
            public String getContentType() {
                return exchange.getRequestHeaders().getFirst("Content-type");
            }

            @Override
            public InputStream getInputStream() {
                return exchange.getRequestBody();
            }
        });
        return parts.stream()
                .collect(Collectors.toMap(FileItem::getFieldName, Function.identity()));
    }
}
