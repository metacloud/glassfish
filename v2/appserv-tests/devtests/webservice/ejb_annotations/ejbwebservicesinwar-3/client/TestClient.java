package client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.sun.ejte.ccl.reporter.SimpleReporterAdapter;

public class TestClient {

    private static SimpleReporterAdapter stat =
        new SimpleReporterAdapter("appserv-tests");

    public boolean found1 = false;
    public boolean found2 = false;

    public static void main (String[] args) {
        stat.addDescription("ejbwebservices-war-3");
        TestClient client = new TestClient();
        client.doTest(args);
        stat.printSummary("ejbwebservices-war-3");
    }

    public void doTest(String[] args) {

        String url = args[0];
        try {
            int code = invokeServlet(url);
            report(code);
	} catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private int invokeServlet(String url) throws Exception {
        log("Invoking url = " + url);
        URL u = new URL(url);
        HttpURLConnection c1 = (HttpURLConnection)u.openConnection();
        int code = c1.getResponseCode();
        InputStream is = c1.getInputStream();
        BufferedReader input = new BufferedReader (new InputStreamReader(is));
        String line = null;
        while ((line = input.readLine()) != null) {
            log(line);
            if(line.indexOf("So the RESULT OF HELLO SERVICE IS") != -1)
		found1 = true;
            if(line.indexOf("[Hello All]") != -1)
		found2 = true;
        }
        return code;
    }

    private void report(int code) {
        if(code != 200) {
            log("Incorrect return code: " + code);
            fail();
        }
        if(!found1) {
            fail();
        }
        if(!found2) {
            fail();
        }
        pass();
    }

    private void log(String message) {
        System.out.println("[client.TestClient]:: " + message);
    }

    private void pass() {
        stat.addStatus("ejbwebservices-war-3", stat.PASS);
    }

    private void fail() {
        stat.addStatus("ejbwebservices-war-3", stat.FAIL);
    }
}