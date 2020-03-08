import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.*;

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

    public static void main(String[] args) {

        // Setting input parameters to download html and logo of the website through
        // proxy
        webpageURL = args[0];
        proxyServerIP = args[1];
        proxyPort = Integer.parseInt(args[2]);
        proxyServerUsername = args[3];
        proxyServerPassword = args[4];
        htmlFilename = args[5];
        logoFilename = args[6];

        ConnectionViaProxy cViaProxy = null;

        try {
            cViaProxy = new ConnectionViaProxy(webpageURL, proxyServerIP, proxyPort, proxyServerUsername,
                    proxyServerPassword);
            cViaProxy.connect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(cViaProxy.getWebpage());
        cViaProxy.disconnect();
    }

    public void downloadWebpage(String webpageURL, String filename) {

        // Buffered reader to read data from the webpage
        BufferedReader reader = null;
        // Buffered writer to write the html data and save as {filename}.html
        BufferedWriter writer = null;

        try {
            // Creating url object of the webpage
            URL url = new URL(webpageURL);

            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            writer = new BufferedWriter(new FileWriter(filename));

            // Read each line from webpage till EOF and write to the file
            String webpageLine;
            while ((webpageLine = reader.readLine()) != null) {

                writer.write(webpageLine);
            }

            // Webpage HTML download successful
            System.out.println("Wepage HTML downloaded successfully. !!");

        } catch (MalformedURLException e) {
            System.err.println("Malformed URL Exception raised. !!");

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

    public void downloadWebpageLogo(String imageURL, String filename) {

        // Input stream to read image byte data
        InputStream inputStream = null;
        // Output stream to write image byte data
        OutputStream outputStream = null;

        try {
            // Creating url object of the image
            URL url = new URL(imageURL);

            inputStream = url.openStream();
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

class ConnectionViaProxy {

    private Socket socket;

    private String proxyHost;
    private int proxyPort;
    private URL url;
    private Map<String, List<String>> proxyheaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, List<String>> proxyreturnheaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private String webpage;

    public ConnectionViaProxy(String url, String proxyHost, int proxyPort, String username, String password)
            throws IOException {

        socket = new Socket();
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        String encoded = Base64.getUrlEncoder().encodeToString((username + ":" + password).getBytes()).replace("\r\n",
                "");
        proxyheaders.put("Proxy-Authorization", new ArrayList<>(Arrays.asList("Basic " + encoded)));
        this.url = new URL("https://" + url);

    }

    public void connect() throws IOException {

        socket.connect(new InetSocketAddress(proxyHost, proxyPort));

        StringBuilder msg = new StringBuilder();
        msg.append("CONNECT ");
        msg.append(url.getHost());
        msg.append(':');
        msg.append(url.getPort() == -1 ? 443 : url.getPort());
        msg.append(" HTTP/1.0\r\n");
        for (Map.Entry<String, List<String>> header : proxyheaders.entrySet()) {
            for (String l : header.getValue()) {
                msg.append(header.getKey()).append(": ").append(l);
                msg.append("\r\n");
            }
        }
        msg.append("Connection: close\r\n");
        msg.append("\r\n");
        byte[] bytes;
        try {
            bytes = msg.toString().getBytes("ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            bytes = msg.toString().getBytes();
        }

        System.out.println("\nSending HTTP request to proxy. Fetching " + url.getHost() + " .....");
        socket.getOutputStream().write(bytes);
        socket.getOutputStream().flush();

        byte reply[] = new byte[200];
        byte header[] = new byte[200];
        int replyLen = 0;
        int headerLen = 0;
        int newlinesSeen = 0;
        boolean headerDone = false;

        /* Done on first newline */
        InputStream in = socket.getInputStream();
        while (newlinesSeen < 2) {
            int i = in.read();
            if (i < 0) {
                throw new IOException("Unexpected EOF from remote server");
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

        // Some proxies return http/1.1, some http/1.0 even we asked for 1.0
        if (!replyStr.startsWith("HTTP/1.0 200") && !replyStr.startsWith("HTTP/1.1 200")) {
            System.out.println("\nUnable to connect to the website using proxy !! :(");
            throw new IOException("Unable to tunnel. Proxy returns \"" + replyStr + "\"");
        } else {
            System.out.println("\nConnection established successfully !! :)");
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ConnectionViaProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void storeWebpage(InputStream in) {
        // try {
        // BufferedReader reader = new BufferedReader(new InputStreamReader(in,
        // "UTF-8"));
        // String c = "";
        // while((c = reader.readLine())!=null)
        // {
        // webpage += c;
        // }
        // } catch (Exception e) {
        // e.printStackTrace();
    }

    public String getWebpage(){
        return webpage;
    }

};