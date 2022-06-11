import org.apache.commons.cli.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author karottc@gmail.com
 * @date 2022-06-10 18:01
 */
public class MainApplication {

    private static final String OUTPUT_PATH = "kindle_download";

    public static void main(String[] args) {
        HelpFormatter helpFormatter = new HelpFormatter();
        Options options = new Options();
        addOptions(options);

        CommandLine cmd = null;
        CommandLineParser parser = new DefaultParser();
        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            e.printStackTrace();
            helpFormatter.printHelp("kindle-download-helper", options);
            System.exit(-1);
        }

        if (cmd.hasOption("h")) {
            helpFormatter.printHelp("kindle-download-helper", options);
            System.exit(0);
        }

        String cookieFile = cmd.getOptionValue("cf");
        String csrfToken = cmd.getOptionValue("ct");
        String outPath = cmd.getOptionValue("o");

        if (cookieFile == null || cookieFile.length() == 0) {
            System.out.println("cookie file is not exist");
            helpFormatter.printHelp("kindle-download-helper", options);
            System.exit(-1);
        }
        File cf = new File(cookieFile);
        if (!cf.exists()) {
            System.out.println("cookie file is not exist");
            System.exit(-1);
        }

        if (csrfToken == null || csrfToken.length() == 0) {
            helpFormatter.printHelp("kindle-download-helper", options);
            System.out.println("csrf token is not exist");
            System.exit(-1);
        }

        outPath = createDownloadPath(outPath);

        Kindle kindle = new Kindle(csrfToken, cookieFile, outPath);
        kindle.downloadBooks();
    }

    private static void addOptions(Options options) {
        options.addOption("h", "help", false, "Print this usage information");
        options.addOption("cf", "cookie-file", true, "cookie file path");
        options.addOption("ct", "csrf-token", true, "enter csrf token");
        options.addOption("o", "output", true, "download file path");
    }

    private static String createDownloadPath(String outPath) {
        if (outPath == null || outPath.length() == 0) {
            outPath = OUTPUT_PATH;
        } else {
            if (outPath.endsWith("/")) {
                outPath += OUTPUT_PATH;
            } else {
                outPath += "/" + OUTPUT_PATH;
            }
        }

        try {
            Files.createDirectories(Paths.get(outPath));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return outPath;
    }
}
