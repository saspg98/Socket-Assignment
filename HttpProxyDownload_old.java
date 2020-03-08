/* BeginGroupMembers */
/* f20170177@hyderabad.bits-pilani.ac.in Saurav Gupta */
/* f20170073@hyderabad.bits-pilani.ac.in Shubhang Srivastava */
/* f20120868@hyderabad.bits-pilani.ac.in Snehit Gorantla Reddy */
/* f20170222@hyderabad.bits-pilani.ac.in Sarthak Gupta */
/* EndGroupMembers *

/* Brief description of prgram.....*/
/*
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.net.*;

public class HttpProxyDownload_old {

    // Variable to store url of the webpage
    private static String webpageURL = null;
    // Variable to store the proxy server IP address
    private static String proxyServerIP = null;
    // Variable to store the port on the proxy server
    private static String proxyPort = null;
    // Variable to store the proxy server IP username
    private static String proxyServerUsername = null;
    // Variable to store the proxy server IP password
    private static String proxyServerPassword = null;
    // Variable to store filename used to save the html of the website
    private static String htmlFilename = null;
    // Variable to store filename used to save the logo of the website
    private static String logoFilename = null;
    // For Https request to server
    private final static String PORT = "443";

    public void downloadWebpageLogo(final String imageURL, final String filename) {

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

        } catch (final MalformedURLException e) {
            System.err.println("Malformed URL Exception raised. !!");

        } catch (final IOException e) {
            System.err.println("IOException raised while downloading logo. !!");

        } catch (final Exception e) {
            System.err.println("FileNotFoundException Raised");

        } finally {

            try {
                // Closing input and output stream
                inputStream.close();
                outputStream.close();

            } catch (final IOException e) {
                System.err.println("IOException Raised in finally");
            }
        }
    }

    public void downloadWebpage(final String webpageURL, final String filename) {

        // Buffered reader to read data from the webpage
        BufferedReader reader = null;
        // Buffered writer to write the html data and save as {filename}.html
        BufferedWriter writer = null;

        try {
            // Creating url object of the webpage
            final URL url = new URL(webpageURL);

            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            writer = new BufferedWriter(new FileWriter(filename));

            // Read each line from webpage till EOF and write to the file
            String webpageLine;
            while ((webpageLine = reader.readLine()) != null) {

                writer.write(webpageLine);
            }

            // Webpage HTML download successful
            System.out.println("Wepage HTML downloaded successfully. !!");

        } catch (final MalformedURLException e) {
            System.err.println("Malformed URL Exception raised. !!");

        } catch (final IOException e) {
            System.err.println("IOException raised while downloading webpage. !!");

        } catch (final Exception e) {
            System.err.println("FileNotFoundException Raised");

        } finally {

            try {
                // Closing reader and writer buffer
                reader.close();
                writer.close();

            } catch (final IOException e) {
                System.err.println("IOException Raised in finally");
            }
        }
    }

    public static void getSocketForProxy(final String proxyServerIP, final String proxyServerUsername,
            final String proxyServerPassword, final String proxyPort, final String webPageURL, final String port) {

        // Socket object connecting to proxy
        Socket socket = null;
        try {
            // System.setProperty("http.proxyHost", proxyServerIP);
            // System.setProperty("http.proxyPort", proxyPort);

            SocketAddress addr = new InetSocketAddress(proxyServerIP, Integer.parseInt(proxyPort));
            Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
            socket = new Socket(proxy);
            socket.connect(new InetSocketAddress(webPageURL, Integer.parseInt(port)));

            socket.setKeepAlive(true);
        
            System.err.println(socket.getLocalAddress());
            System.err.println(socket.getLocalPort());
            System.err.println(socket.getPort());
           
        } catch (Exception e) {
            System.err.println("Unable to create socket :( !!");
        }

        /**************************************
         **** Authorizing the proxy server *****
         **************************************/
        if (socket != null) {

            System.out.println("\nSocket created. !! :)\n");

            // String formatting based on HTTP1.1 CONNECT protocol RFC 7231
            String proxyConnect = "CONNECT " + webpageURL + ":" + port;

            try {
                String proxyUserPass = String.format("%s:%s", proxyServerUsername, proxyServerPassword);
                proxyConnect = proxyConnect.concat(" HTTP/1.1\r\n")
                .concat("Host: " + webPageURL + ":" + port + "\r\n")
                        .concat("Proxy-Authorization: basic "
                                + Base64.getUrlEncoder().encodeToString(proxyUserPass.getBytes()));

            } catch (Exception e) {
                System.err.println("String formatting Error. !!");

            } finally {
                proxyConnect.concat("\r\n\r\n");
            }

            System.out.println("Http request to server :\n\n" + proxyConnect);

            try {
                OutputStream out = socket.getOutputStream();
                out.write(proxyConnect.getBytes());
                out.flush();

            } catch (IOException e) {
                System.err.println("IOException raised in getSocketFactory. !!");

            }

            System.out.println("\nValidating HTTP response..........\n");
            /***************************************************
             * validate HTTP response for authorization success.
             ***************************************************/

            try {
                // InputStream socketInput = socket.getInputStream();
                // // int size = socket.getReceiveBufferSize();
                // byte[] tmpBuffer = new byte[1024];

                String page=null,line;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while((line = in.readLine()) != null){
                    page.concat(line+"\n");
                }

                System.out.println(page);
                // System.out.println(" Checking the response code..........\n");
                
                // int len = socketInput.read(tmpBuffer);
                // socket.close();

                // if(len == 0){
                //     System.err.println("Invalid response from proxy. :( !!");
                // }

                // String proxyResponse = new String(tmpBuffer, 0, len, "UTF-8");

                // // Expecting HTTP/1.1 200 OK
                // if (proxyResponse.indexOf("200") != -1) {

                //     // Proxy Connect Successful, return the socket for IO
                //     System.out.println("Proxy authorization successful. !!");

                //     // // Flush any outstanding message in buffer
                //     // if (socketInput.available() > 0)
                //     //     socketInput.skip(socketInput.available());

                // }else{
                //     System.err.println("Unable to authorize the client. :( !!\n"+proxyResponse);
                // }

            }catch(IOException e){
                System.err.println("IOException raised in getSocketFactory. !!");

            }
        }
    }    
    
    public static void main(final String[] args) {
        
        // Setting input parameters to download html and logo of the website through proxy
        webpageURL = args[0];
        proxyServerIP = args[1];
        proxyPort = args[2];
        proxyServerUsername = args[3];
        proxyServerPassword = args[4];
        htmlFilename = args[5];
        logoFilename = args[6];

        // Creation of socket to connect with webpage through given proxy server
        getSocketForProxy(proxyServerIP, proxyServerUsername, proxyServerPassword, proxyPort, webpageURL, PORT);

    }
}  