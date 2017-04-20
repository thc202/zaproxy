package org.zaproxy.zap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class DnsLogger {

    private static Logger logger = Logger.getLogger(DnsLogger.class);

    private Process process;
    private boolean started;

    public void start() {
        stop();

        List<String> cmd = new ArrayList<>();
        cmd.add("tcpdump");
        cmd.add("-vvv");
        cmd.add("-s 0");
        cmd.add("-l");
        cmd.add("-n");
        cmd.add("port 53");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        try {
            process = pb.start();
            started = true;
        } catch (Exception e) {
            logger.error("Failed to start the DNS logger:", e);
            process = null;
            return;
        }

        try {
            int exitValue = process.exitValue();
            logger.error("Failed to start the DNS logger, exit value: " + exitValue);
        } catch (IllegalThreadStateException ignore) {
            // Started successfully...
        }

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            logger.info(line);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to obtain output:", e);
                }
                stop();
            }
        }, "ZAP-DNS-Logger");
        thread.start();
    }

    public void stop() {
        if (!started) {
            return;
        }

        started = false;
        process.destroy();
        process = null;
    }
}
