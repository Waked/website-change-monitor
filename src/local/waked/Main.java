package local.waked;

import org.apache.commons.codec.binary.Base64;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("Not enough parameters!");
            System.console().readLine();
            throw new Exception("Not enough parameters");
        }

        Properties properties = ReadConfig.getProperties("config.properties");

        System.out.println("\nInitialising...");

        String url = properties.getProperty("url");
        Integer intervalSeconds = Integer.parseInt(properties.getProperty("intervalSeconds", "300"));
        String user = args[0];
        String pass = args[1];
        String login = user + ":" + pass;
        String base64login = new String(Base64.encodeBase64(login.getBytes()));

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        final Connection conn = Jsoup.connect(url).header("Authorization", "Basic " + base64login);

        final Document document = conn.get();

        System.out.println("Downloaded page at: " + url);

        // Zapisuje stronę odniesienia do pliku ref.html
        try (PrintWriter writer = new PrintWriter("ref.html", "UTF-8")) {

            writer.write(document.html());
            writer.close();
            System.out.println("Reference webpage saved to file \"ref.html\", feel free to check it out.");

        } catch (IOException e) {

            e.printStackTrace();
            System.out.println("Failed to write reference webpage to file, will continue nonetheless");

        }

        System.out.println("\nThe program will now check for changes in the webpage every " + intervalSeconds + " seconds.");

        final Runnable checker = () -> {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            System.out.print("[" + sdf.format(timestamp) + "]Checking... ");
            try {
                Document newdoc = conn.get();
                if (newdoc.body().child(1).html().equals(document.body().child(1).html())) {
                    System.out.print("No change.\n");
                } else {
                    System.out.print("Page update detected!\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        final ScheduledFuture<?> checkerHandle  = scheduler.scheduleAtFixedRate(checker, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

        System.console().readLine();

        System.out.print("Zatrzymywanie... ");
        checkerHandle.cancel(true);
        System.out.print("gotowe!\n");

    }
}
