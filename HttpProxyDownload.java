/* BeginGroupMembers */
/* f20170177@hyderabad.bits-pilani.ac.in Saurav Gupta */
/* f20170222@hyderabad.bits-pilani.ac.in Sarthak Gupta */
/* f20170868@hyderabad.bits-pilani.ac.in Snehit Gorantla Reddy */
/* f20170073@hyderabad.bits-pilani.ac.in Shubhang Srivastava */
/* EndGroupMembers */

/* Welcome. :) */
/* This program uses socket library in java to establish connection with proxy server provided by user. */
/* Sockets provides interface to interact with Tranport layer protocols and utilize them to exchange data between applicatios accordingly. */
/* HTTP is a application layer protocol which utilizes tcp protocol for information exchange. */
/* In this program 2 types of HTTP requests are used CONNECT and GET. Both can be found in ConnectionViaProxy Class, connect() and getRequest() methods .*/
/* CONNECT request helps to authorize the proxy server whereas GET request is used to fetch data from the desired website. */
/* The data fetched using GET request is parsed and saved to index.html and logo.png using methods downloadWebpage() and downloadWebpageLogo() in HttpProxyDownload Class. */
/* For details description of various methods please go through the comments of the correspoding method. */
/* For debugging, see the output in the terminal. It contains response header of each request made and steps taken to make those request. */
/* Thank You! :) */

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;

/**
 * Class to download content from website using proxy server. Uses Class
 * ConnectionViaProxy to connect to the proxy server and download content.
 * Contains following functions 1) public static void main(String args[]) -
 * driver method of the program. 2) public static void
 * downloadWebpage(InputStream in, String filename) - method to parse html
 * content from the response returned by proxy server 3) public static void
 * downloadWebpageLogo(InputStream in, String filename) - method to parse image
 * object from the response returned by proxy server)
 */
class HttpProxyDownload {

    // Variable to store url of the webpage
    private static String webpageURL = null;
    // Variable to store url of the webpage logo
    private static String imageRelativeUrl = null;
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

    // Driver method of the program.
    public static void main(String[] args) {

        // Setting input parameters to download html and logo of the website via proxy
        webpageURL = args[0];
        if(webpageURL.indexOf("www")==-1){
            webpageURL = "www." + webpageURL;
        }

        imageRelativeUrl = null;
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
            System.err.println("\nIO problem in Class ConnectionViaProxy\n");
            e.printStackTrace();
            System.exit(1);
        }

        // Trying to establish connection with proxy server.
        try {
            cViaProxy.connect();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // GET request to download html of the website given and save as index.html.
        cViaProxy.setRequestMethod("GET");
        try (InputStream in = cViaProxy.getInputStream()) {
            System.out.println("\nHere is the response for your request .....\n");
            downloadWebpage(in);

        } catch (IOException e) {
            System.err.println("\nUnable to fetch the response from the website!! :(\n");
            e.printStackTrace();
        }

        // Disconnecting socket connection to proxy server.
        cViaProxy.disconnect();

        // Trying to establish connection with proxy server.
        try {
            cViaProxy.connect();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        imageRelativeUrl = extractLogoUrl();

        if (imageRelativeUrl != null) {

            // GET request to download logo of the website given and save as logo.png.
            cViaProxy.setRequestMethod("GET");
            cViaProxy.updateUrl(imageRelativeUrl);
            try (InputStream in = cViaProxy.getInputStream()){
                System.out.println("\nHere is the response for your request .....\n");
                downloadWebpageLogo(in);

            }catch(IOException e){
                System.err.println("\nUnable to fetch the response from the website!! :(\n");
                e.printStackTrace();
            }
        }else{
            System.out.println("\nUnable to fetch logo of the website !! :(");
        }

        // Disconnecting socket connection to proxy server.
        cViaProxy.disconnect();

    }

    // Method to parse html content from the response returned by proxy server.
    public static void downloadWebpage(InputStream in) {

        // Buffered reader to read data from the webpage.
        BufferedReader reader = null;
        // Buffered writer to write the html data and save as {filename}.html.
        BufferedWriter writer = null;

        try {

            reader = new BufferedReader(new InputStreamReader(in));
            writer = new BufferedWriter(new FileWriter(htmlFilename));

            // Read each line from response till EOF and separate header lines and html.
            String webpageLine;
            boolean start = false;
            StringBuilder response = new StringBuilder();
            while ((webpageLine = reader.readLine()) != null) {

                // Read each header line from the response till <HTML> tag appears.
                if (webpageLine.indexOf("doctype") == -1 && webpageLine.indexOf("DOCTYPE") == -1) {
                    response.append(webpageLine).append("\n");
                } else {
                    System.out.println(response.toString().trim());
                    start = true;
                }

                // Read each line from webpage till EOF and write to the file.
                if (start) {
                    writer.write(webpageLine);
                    writer.write("\n");
                }
            }

            // Webpage HTML download successful
            System.out.println("\nWepage HTML downloaded successfully. !!");

        } catch (SocketTimeoutException e) {
            System.err.println("\nTimeout while reading from Socket input buffer.\n");
            e.printStackTrace();

        } catch (IOException e) {
            System.err.println("\nIOException raised while downloading webpage. !!\n");
            e.printStackTrace();

        } catch (Exception e) {
            System.err.println("\nFileNotFoundException Raised\n");
            e.printStackTrace();

        } finally {

            try {
                // Closing reader and writer buffer
                reader.close();
                writer.close();

            } catch (IOException e) {
                System.err.println("\nIOException Raised in finally\n");
                e.printStackTrace();
            }
        }
    }

    // Method to extract image url from the index.html downloaded.
    // Brute force approach is applied to find the appropiate img tag. The img tag which whose alt attribute value contains the required website name is considered as logo (including some exception cases).
    public static String extractLogoUrl() {

        String url = null;

        boolean google= false;
        if(webpageURL.equals("www.google.com"))
            google = true;

        // regex to find img tag in the html.
        String IMAGE_TAG_PATTERN = "<img(\"[^\"]*\"|'[^']*'|[^'\">])*>";
        // regex to find alt attribute in the img tag.
        String ALT_ATTR_PATTERN = "alt=(\"[^\"]*\"|'[^']*'|[^'\">])";
        // regex to find src attribute in the img tag.
        String SRC_ATTR_PATTERN = "src=(\"[^\"]*\"|'[^']*'|[^'\">])";
        
        Pattern imgTagPattern = Pattern.compile(IMAGE_TAG_PATTERN);
        Pattern altAttrPattern = Pattern.compile(ALT_ATTR_PATTERN);
        Pattern srcAttrPattern = Pattern.compile(SRC_ATTR_PATTERN);
        
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(htmlFilename));
            
        } catch (FileNotFoundException e) {
            System.out.println("\nError while opening index.html file.\n");
            e.printStackTrace();
        }
        
        String line;
        try {
            while ((line = reader.readLine()) != null) {

                Matcher imgTagMatcher = imgTagPattern.matcher(line);
                while (imgTagMatcher.find()) {

                    Matcher altAttrMatcher = altAttrPattern.matcher(imgTagMatcher.group());
                    if(altAttrMatcher.find()){

                        String inGroup = altAttrMatcher.group(1).toLowerCase().replaceAll("\\s", "");
                        if(inGroup.indexOf("home") !=-1 || inGroup.indexOf("logo") !=-1 || google || inGroup.indexOf("bits")!=-1){
                            
                            Matcher srcAttrMatcher = srcAttrPattern.matcher(imgTagMatcher.group());
                            srcAttrMatcher.find();
                            url = srcAttrMatcher.group(1).replaceAll("\"", "");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("\nError while reading index.html file.\n");
            e.printStackTrace();

        }finally{
            try {
                // Closing reader and writer buffer
                reader.close();

            } catch (IOException e) {
                System.err.println("\nIOException Raised in finally\n");
                e.printStackTrace();
            }
        }

        return url;
    }


    // Method to parse image object from the response returned by proxy server.
    public static void downloadWebpageLogo(InputStream in) {

        // Input stream to read image byte data
        InputStream inputStream = null;
        // Output stream to write image byte data
        OutputStream outputStream = null;

        try {

            inputStream = in;
            outputStream = new FileOutputStream(logoFilename);

            boolean headerEnded = false;

            byte[] bytes = new byte[2048];
            String response = "";
            int length,offset;
            while ((length = inputStream.read(bytes)) != -1) {
                
                offset = 0;
                
                /** This locates the end of the header by comparing the current byte as well as the next 3 bytes
                 * with the HTTP header end "\r\n\r\n" (which in integer representation would be 13 10 13 10).
                 * If the end of the header is reached, the flag is set to true and the remaining data in the
                 * currently buffered byte array is written into the file.
                 */
                if(!headerEnded){

                    String header = new String(bytes, 0, length);
                    int indexOfEOH = header.indexOf("\r\n\r\n");

                    if(indexOfEOH != -1) {

                        length = length-indexOfEOH-4;
                        offset = indexOfEOH+4;
                        headerEnded = true;
                        response += header.substring(0, indexOfEOH+4);
                        System.out.println(response.toString().trim());

                    }else{
                        response += header;
                        length = 0;
                    }
                }

                // If the end of the header had already been reached, write the bytes to the file as normal.
                outputStream.write(bytes, offset, length);
                outputStream.flush();
            }

            // Webpage logo download successful
            System.out.println("\nWebpage logo downloaded successfully. !!");

        } catch(SocketTimeoutException e){
            System.err.println("\nTimeout while reading from Socket input buffer.\n");
            e.printStackTrace();
 
        } catch (IOException e) {
            System.err.println("\nIOException raised while downloading logo. !!\n");
            e.printStackTrace();

        } catch (Exception e) {
            System.err.println("\nFileNotFoundException Raised\n");
            e.printStackTrace();

        } finally {

            try {
                // Closing input and output stream
                inputStream.close();
                outputStream.close();  // Forces socket object created to get closed.

            } catch (IOException e) {
                System.err.println("\nIOException Raised in finally.\n");
                e.printStackTrace();
            }
        }
    }

};


/* Class to handle connection through proxy to the website
*    1) Proxy Authentication
*    2) Downloading requested content from the website
*/
class ConnectionViaProxy {

    public static Socket socket;

    int PORT = 443;

    private String proxyHost;
    private int proxyPort;
    private String baseURL;
    private String fetchURL; 
    private String method;
    private Map<String, List<String>> sendheaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, List<String>> proxyheaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, List<String>> proxyreturnheaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public ConnectionViaProxy(String url, String proxyHost, int proxyPort, String username, String password)
            throws IOException {

        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        String encoded = Base64.getUrlEncoder().encodeToString((username + ":" + password).getBytes()).replace("\r\n",
                "");
        proxyheaders.put("Proxy-Authorization", new ArrayList<>(Arrays.asList("Basic " + encoded)));
        this.baseURL = url;
        this.fetchURL = "https://" + url + "/";
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
        getRequest();
        return socket.getInputStream();
    }

    public void connect() throws IOException {

        // Creating object of Class Socket
        socket = new Socket();

        /** Connecting socket to the proxy server.
         *  Also setting timeout out limit to 60sec, so that if response is not 
         * recieved from server side within 60sec socket can be freed for next 
         * request completion.
         */
        try{
            socket.setSoTimeout(6000);
            socket.connect(new InetSocketAddress(proxyHost, proxyPort));
        }catch(SocketTimeoutException e){
            System.err.println("\nTimeout while connecting to proxy server while initial setup.\n");
            e.printStackTrace();
            System.exit(1);
        }
        catch(Exception e){
            System.err.println("\nError while configuring socket.\n");
            e.printStackTrace();
            System.exit(1);
        }


        /** Constructing CONNECT request message to the proxy server before requesting through other methods.
        *   Required because proxy requires authentication before sending other HTTP requests.
        */
        System.out.println("\nConstructing CONNECT request message......\n");

        StringBuilder msg = new StringBuilder();
        msg.append("CONNECT ");
        msg.append(baseURL);
        msg.append(':');
        msg.append(PORT);
        msg.append(" HTTP/1.0\r\n");
        for (Map.Entry<String, List<String>> header : proxyheaders.entrySet()) {
            for (String l : header.getValue()) {
                msg.append(header.getKey()).append(": ").append(l);
                msg.append("\r\n");
            }
        }
        msg.append("Proxy-Connection: keep-alive\r\n");
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
        try{
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();

        }catch(SocketTimeoutException e){
            System.err.println("\nTimeout while writing to Socket output buffer for HTTP CONNECT.\n");
            e.printStackTrace();
            System.exit(1);
        }

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


        /** Handshake between proxy server and our device to create a secure channel in between the two.
         * SSLSocketFactory is required for the same. Evidence of SSL handshake can be seen in 
         * traffic_proxy.pcapng and traffic_proxy2.pcapng, collected while connecting to proxy via website.
         * If SSL socket factory is not used, the response from the server in this case is recorded in 
         * response_with_no_sslfactory.html
        */
        SSLSocket s = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                .createSocket(socket,baseURL,PORT,true);
        s.startHandshake();
        socket = s;

    }

    public void getRequest() throws IOException{

        System.out.println("\nConstructing request message......\n");

        StringBuilder msg = new StringBuilder();
        byte[] bytes;
        
        /** Creating HTTP request message for the appropiate HTTP method according to the content required or
        * need to be send.
        */
        msg.setLength(0);
        msg.append(this.method);
        msg.append(" ");
        msg.append(fetchURL);
        msg.append(" HTTP/1.0\r\n");
        for (Map.Entry<String, List<String>> h : sendheaders.entrySet()) {
            for (String l : h.getValue()) {
                msg.append(h.getKey()).append(": ").append(l);
                msg.append("\r\n");
            }
        }
        msg.append("Host: ").append(baseURL).append("\r\n");
        msg.append("Proxy-Connection: keep-alive\r\n");
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
        try{
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();

        }catch(SocketTimeoutException e){
            System.err.println("\nTimeout while writing to Socket output buffer while HTTP "+this.method+" request.\n");
            e.printStackTrace();
        }
    }


    // Method to turn off the socket connecting our device and proxy server.
    public void disconnect() {

        System.out.println("\n\nClosing the socket to the proxy !!\n");
        try {
            socket.close();
        } catch (IOException ex) {
            System.err.println("\nError while closing socket.\n");
            Logger.getLogger(ConnectionViaProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    //Method to change the url in order to fetch the image
    //Base url remains equal to the url of webpage provided by the user 
    public void updateUrl(String relativeUrl){

        if(relativeUrl.indexOf("https://")==-1){

            if(relativeUrl.indexOf(baseURL)==-1){

                if(relativeUrl.charAt(0)!='/'){

                    relativeUrl = "/" + relativeUrl;
                }
                relativeUrl = baseURL + relativeUrl;
            }
            relativeUrl = "https://" + relativeUrl;
        }
        fetchURL = relativeUrl;
    }
};