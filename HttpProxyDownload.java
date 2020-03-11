import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.net.*;

/** Class to download content from website using proxy server.
 * Uses Class ConnectionViaProxy to connect to the proxy server and download content.
 * Contains following functions
 * 1) public static void main(String args[]) - driver method of the program.
 * 2) public static void downloadWebpage(InputStream in, String filename) - method to 
 *                                 parse html content from the response returned by proxy server
 * 3) public static void downloadWebpageLogo(InputStream in, String filename) - method to 
 *                                 parse image object from the response returned by proxy server)
 */
class HttpProxyDownload {


    // Variable to store url of the webpage
    private static String webpageURL = null;
    // Variable to store the proxy server IP address
    private static String proxyServerIP = null;
    // Variable to store the port on the proxy server
    private static int proxyPort;
    // Variable to store the proxy server IP username
    private static String proxyServerUsername = null;
    // Variable to store the proxy server IP password
    private static String proxyServerPassword = null;
    // Variable to store filename used to save the html of the website
    private static String htmlFilename = null;
    // Variable to store filename used to save the logo of the website
    private static String logoFilename = null;

    //Driver method of the program.
    public static void main(String[] args) {

        // Setting input parameters to download html and logo of the website via proxy
        webpageURL = args[0];
        proxyServerIP = args[1];
        proxyPort = Integer.parseInt(args[2]);
        proxyServerUsername = args[3];
        proxyServerPassword = args[4];
        htmlFilename = args[5];
        logoFilename = args[6];

        // Instance of class ConnectionViaProxy to handle connection to website through
        // squid proxy.
        ConnectionViaProxy cViaProxy = null;
        try {
            cViaProxy = new ConnectionViaProxy(webpageURL, proxyServerIP, proxyPort, proxyServerUsername,
                    proxyServerPassword);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // GET request to download html of the website given and save as index.html.
        cViaProxy.setRequestMethod("GET");
        try (InputStream in = cViaProxy.getInputStream()){
            System.out.println("\nHere is the response for your request .....\n");
            downloadWebpage(in,htmlFilename);
        
        }catch(IOException e){
            System.err.println("\nUnable to fetch the response from the website!! :(\n");
            e.printStackTrace();
        }

        // GET request to download logo of the website given and save as logo.png.
        // cViaProxy.setRequestMethod("GET");
        // try (InputStream in = cViaProxy.getInputStream()){
        //     System.out.println("\nHere is the response for your request .....\n");
        //     downloadWebpage(in,logoFilename);
        
        // }catch(IOException e){
        //     System.err.println("\nUnable to fetch the response from the website!! :(\n");
        //     e.printStackTrace();
        // }

        //Disconnecting socket connection to proxy server.
        System.out.println("\n\nClosing the socket to the proxy !!\n");
        cViaProxy.disconnect();
    }



    // Method to parse html content from the response returned by proxy server.
    public static void downloadWebpage(InputStream in,String filename) {

        // Buffered reader to read data from the webpage.
        BufferedReader reader = null;
        // Buffered writer to write the html data and save as {filename}.html.
        BufferedWriter writer = null;

        try {

            reader = new BufferedReader(new InputStreamReader(in));
            writer = new BufferedWriter(new FileWriter(filename));

            // Read each line from response till EOF and separate header lines and html.
            String webpageLine;
            boolean start = false;
            StringBuilder response = new StringBuilder();
            while ((webpageLine = reader.readLine()) != null) {
                
                // Read each header line from the response till <HTML> tag appears.
                if(webpageLine.indexOf("<HTML>")==-1){
                    response.append(webpageLine).append("\n");
                }
                else{
                    System.out.println(response.toString());
                    start = true;
                }

                // Read each line from webpage till EOF and write to the file.
                if(start){
                    writer.write(webpageLine);
                    writer.write("\n");
                }
            }

            // Webpage HTML download successful
            System.out.println("Wepage HTML downloaded successfully. !!");

        } catch (IOException e) {
            System.err.println("IOException raised while downloading webpage. !!");

        } catch (Exception e) {
            System.err.println("FileNotFoundException Raised");

        } finally {

            try {
                // Closing reader and writer buffer
                reader.close();
                writer.close();

            } catch (IOException e) {
                System.err.println("IOException Raised in finally");
            }
        }
    }


    // Method to parse image object from the response returned by proxy server.
    public void downloadWebpageLogo(InputStream in, String filename) {

        // Input stream to read image byte data
        InputStream inputStream = null;
        // Output stream to write image byte data
        OutputStream outputStream = null;

        try {

            inputStream = in;
            outputStream = new FileOutputStream(filename);

            byte[] buffer = new byte[2048];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            // Webpage logo download successful
            System.out.println("Wepage logo downloaded successfully. !!");

        } catch (MalformedURLException e) {
            System.err.println("Malformed URL Exception raised. !!");

        } catch (IOException e) {
            System.err.println("IOException raised while downloading logo. !!");

        } catch (Exception e) {
            System.err.println("FileNotFoundException Raised");

        } finally {

            try {
                // Closing input and output stream
                inputStream.close();
                outputStream.close();

            } catch (IOException e) {
                System.err.println("IOException Raised in finally");
            }
        }
    }

};


/* Class to handle connection through proxy to the website
*    1) Proxy Authentication
*    2) Downloading requested content from the website
*/
class ConnectionViaProxy {

    private Socket socket;

    private String proxyHost;
    private int proxyPort;
    private URL url;
    private String method;
    private Map<String, List<String>> sendheaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, List<String>> proxyheaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, List<String>> proxyreturnheaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public ConnectionViaProxy(String url, String proxyHost, int proxyPort, String username, String password)
            throws IOException {

        // Creating object of Class Socket
        socket = new Socket();

        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        String encoded = Base64.getUrlEncoder().encodeToString((username + ":" + password).getBytes()).replace("\r\n",
                "");
        proxyheaders.put("Proxy-Authorization", new ArrayList<>(Arrays.asList("Basic " + encoded)));
        this.url = new URL("https://"+ url + "/");

    }

    public void setRequestMethod(String method) {
        System.out.println("\nSetting method for the request to the proxy......");
        this.method = method;
        System.out.println("\n" + this.method + " method added successfully!");

    }

    public void setRequestProperty(String key, String value) {
        sendheaders.put(key, new ArrayList<>(Arrays.asList(value)));
    }

    public void addRequestProperty(String key, String value) {
        sendheaders.computeIfAbsent(key, l -> new ArrayList<>()).add(value);
    }

    public InputStream getInputStream() throws IOException {
        connect();
        return socket.getInputStream();
    }

    public void connect() throws IOException {

        /** Connecting socket to the proxy server.
         *  Also setting timeout out limit to 60sec, so that if response is not 
         * recieved from server side within 60sec socket can be freed for next 
         * request completion.
         */
        try{
            socket.setSoTimeout(60000);;
            socket.connect(new InetSocketAddress(proxyHost, proxyPort));
        }catch(Exception e){
            e.printStackTrace();
        }


        /** Constructing CONNECT request message to the proxy server before requesting through other methods.
        *   Required because proxy requires authentication before sending other HTTP requests.
        */
        System.out.println("\nConstructing CONNECT request message......\n");

        StringBuilder msg = new StringBuilder();
        msg.append("CONNECT ");
        msg.append(url.getHost());
        msg.append(':');
        msg.append(url.getPort() == -1 ? 443 : url.getPort());
        msg.append(" HTTP/1.1\r\n");
        for (Map.Entry<String, List<String>> header : proxyheaders.entrySet()) {
            for (String l : header.getValue()) {
                msg.append(header.getKey()).append(": ").append(l);
                msg.append("\r\n");
            }
        }
        msg.append("Proxy-Connection: close\r\n");
        msg.append("\r\n");

        System.out.println(msg.toString());
        System.out.println("CONNECT Request message constructed.");

        byte[] bytes;
        try {
            bytes = msg.toString().getBytes("ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            bytes = msg.toString().getBytes();
        }


        /** Sending HTTP CONNECT request message to socket.
        * Required for authentication of our device with the proxy server.
        */
        System.out.println("\nSending HTTP CONNECT request to the proxy. Fetching response .....");
        socket.getOutputStream().write(bytes);
        socket.getOutputStream().flush();



        /** Parsing the response for CONNECT request to the proxy server
         * Following things to be verified or noted down after successfull connection between our device 
         * and proxy server.
         * 1) Status Code from proxy server
         * 2) Version of the HTTP returned (will be required while sending further requests)
         */ 
        byte reply[] = new byte[200];
        byte header[] = new byte[200];
        int replyLen = 0;
        int headerLen = 0;
        int newlinesSeen = 0;
        boolean headerDone = false;

        InputStream in = socket.getInputStream();
        while (newlinesSeen < 2) {
            int i = in.read();
            if (i < 0) {
                throw new IOException("\nUnexpected EOF from the remote server side. !! :(");
            }
            if (i == '\n') {
                if (newlinesSeen != 0) {
                    String h = new String(header, 0, headerLen);
                    String[] split = h.split(": ");
                    if (split.length != 1) {
                        proxyreturnheaders.computeIfAbsent(split[0], l -> new ArrayList<>()).add(split[1]);
                    }
                }
                headerDone = true;
                ++newlinesSeen;
                headerLen = 0;
            } else if (i != '\r') {
                newlinesSeen = 0;
                if (!headerDone && replyLen < reply.length) {
                    reply[replyLen++] = (byte) i;
                } else if (headerLen < reply.length) {
                    header[headerLen++] = (byte) i;
                }
            }
        }

        String replyStr;
        try {
            replyStr = new String(reply, 0, replyLen, "ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            replyStr = new String(reply, 0, replyLen);
        }



        // In case none of HTTP/1.0 200 and HTTP/1.1 200 is returned as a header in response exception is thrown.
        // Some proxies return http/1.1, some http/1.0 even we asked for 1.0
        if (!replyStr.startsWith("HTTP/1.0 200") && !replyStr.startsWith("HTTP/1.1 200")) {

            System.out.println("\nUnable to connect to the website using proxy !! :(");
            throw new IOException("Unable to tunnel. Proxy returns \"" + replyStr + "\"");

        }
            
        System.out.println("\nConnection established successfully !! :)");

        System.out.println("\nConstructing request message......\n");
 


        /** Handshake between proxy server and our device to create a secure channel in between the two.
         * SSLSocketFactory is required for the same. Evidence of SSL handshake can be seen in 
         * traffic_proxy.pcapng and traffic_proxy2.pcapng, collected while connecting to proxy via website.
         * If SSL socket factory is not used, the response from the server in this case is recorded in 
         * response_with_no_sslfactory.html
        */
        SSLSocket s = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                .createSocket(socket, url.getHost(), url.getPort(), true);
        s.startHandshake();
        socket = s;


        
        /** Creating HTTP request message for the appropiate HTTP method according to the content required or
        * need to be send.
        */
        msg.setLength(0);
        msg.append(this.method);
        msg.append(" ");
        msg.append(url.toExternalForm().split(String.valueOf(url.getPort()), -2)[0]);
        msg.append(" HTTP/1.1\r\n");
        for (Map.Entry<String, List<String>> h : sendheaders.entrySet()) {
            for (String l : h.getValue()) {
                msg.append(h.getKey()).append(": ").append(l);
                msg.append("\r\n");
            }
        }
        if (method.equals("POST") || method.equals("PUT")) {
            msg.append("Transfer-Encoding: Chunked\r\n");
        }
        msg.append("Host: ").append(url.getHost()).append("\r\n");
        msg.append("Proxy-Connection: close\r\n");
        msg.append("\r\n");

        System.out.println(msg.toString());
        System.out.println("Request message constructed.");

        try {
            bytes = msg.toString().getBytes("ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            bytes = msg.toString().getBytes();
        }



        //Sending HTTP request message through socket to the website server.
        System.out.println("\nSending "+ this.method + " request to the proxy!!");
        socket.getOutputStream().write(bytes);
        socket.getOutputStream().flush();

    }


    // Method to turn off the socket connecting our device and proxy server.
    public void disconnect() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ConnectionViaProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

};