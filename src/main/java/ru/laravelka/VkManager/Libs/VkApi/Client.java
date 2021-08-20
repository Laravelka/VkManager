package ru.laravelka.VkManager.Libs.VkApi;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

public class Client {
    static Logger log = Logger.getLogger(Client.class.getName());

    String token;
    String version;

    public Client(String token, String version) {
        this.token = token;
        this.version = version;
    }

    public String message(int peerId, String message) {
        final Map<String, String> parameters = new HashMap<>();

        parameters.put("message", message);
        parameters.put("peer_id", Integer.toString(peerId));

        return this.query("messages.send", parameters);
    }

    public String query(String method, final Map<String, String> parameters) {
        parameters.put("v", this.version);
        parameters.put("access_token", this.token);
        parameters.put("random_id", Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));

        for(Map.Entry<String, String> parameter : parameters.entrySet()) {
            String value = parameter.getValue();

            value.replace(" ", "%20");

            parameters.put(parameter.getKey(), value);
        }

        try {
            String length = String
                    .valueOf(("https://api.vk.com/method/" + method + getParametersString(parameters))
                            .getBytes(StandardCharsets.UTF_8).length);

            URL url = new URL("https://api.vk.com/method/" + method);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestProperty("Content-Length", length);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            final DataOutputStream output = new DataOutputStream(connection.getOutputStream());

            output.writeBytes(getParametersString(parameters));
            output.flush();

            Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder data = new StringBuilder();
            for (int c; (c = in.read()) >= 0;) {
                data.append((char) c);
            }
            return data.toString();
        } catch (final IOException e) {
            log.info("Client.query:" + e.toString());

            e.printStackTrace();

            return "";
        }
    }

    public String getParametersString(final Map<String, String> params) {
        final StringBuilder result = new StringBuilder();

        params.forEach((name, value) -> {
            try {
                result.append(URLEncoder.encode(name, "UTF-8"));
                result.append('=');
                result.append(URLEncoder.encode(value, "UTF-8"));
                result.append('&');
            } catch (final UnsupportedEncodingException e) {
                log.logp(Level.FINE, "Client", "getParametersString", e.getMessage());

                e.printStackTrace();
            }
        });

        final String resultString = result.toString();
        return !resultString.isEmpty()
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }

}
