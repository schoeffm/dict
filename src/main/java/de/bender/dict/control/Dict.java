package de.bender.dict.control;

import de.bender.dict.model.Translation;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Encapsulates the actual call as well as the result processing of `dict.cc`
 */
public class Dict {
    private static final String EN_MARKER = "var c1Arr";
    private static final String DE_MARKER = "var c2Arr";

    private final DictBuilder dictBuilder;

    public Dict(DictBuilder dictBuilder) {
        this.dictBuilder = dictBuilder;
    }

    public static DictBuilder translate(String toBeTranslated) {
        return new DictBuilder(toBeTranslated);
    }

    private Translation execute() throws IOException, InterruptedException {
        var request = HttpRequest
                .newBuilder(URI.create(String.format("https://%s.dict.cc/?s=%s", dictBuilder.from + dictBuilder.to, URLEncoder.encode(dictBuilder.toBeTranslated, UTF_8.toString()))))
                .header("User-agent", "Mozilla/6.0")
                .build();

        var response = createHttpClient(this.dictBuilder)
                .send(request, HttpResponse.BodyHandlers.ofString());

        return convert(dictBuilder.toBeTranslated, response)
                .orElseThrow(() -> new RuntimeException("Dict returned " + response.statusCode() + "!!!"));
    }

    HttpClient createHttpClient(DictBuilder builder) {
        var clientBuilder = HttpClient.newBuilder()
                .followRedirects(NORMAL)
                .version(HTTP_1_1);

        if (Objects.nonNull(builder.proxyHost) && Objects.nonNull(builder.proxyPort)) {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(builder.proxyHost, builder.proxyPort)));
            if (Objects.nonNull(builder.proxyUser) && Objects.nonNull(builder.proxyPass)) {
                clientBuilder.authenticator(basicAuthAuthenticator(builder.proxyUser, builder.proxyPass));
            }
        } else if (Objects.nonNull(System.getenv("HTTPS_PROXY"))) {
            URI proxyUri = URI.create(System.getenv("HTTPS_PROXY"));
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
            if (Objects.nonNull(proxyUri.getUserInfo())) {
                var proxyAuth = proxyUri.getUserInfo().split(":");
                clientBuilder.authenticator(basicAuthAuthenticator(proxyAuth[0], proxyAuth[1].toCharArray()));
            }
        }

        return clientBuilder.build();
    }

    /*
     * provides a basic-auth Authenticator for authentication against a web-proxy (in case it was revealed)
     */
    Authenticator basicAuthAuthenticator(String username, char[] password) {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
    }

    /*
     * encapsulates the formatter-determination and the final serialization of the translation results
     */
    Optional<Translation> convert(String queryTerm, HttpResponse<String> response) {
        if (Objects.isNull(response.body())) {
            return Optional.empty();
        }
        /*
         * we basically parse the HTML-output which contains two JS-variable definitions that contain
         * - c1Arr == the list of english words
         * - c2Arr == the list of german words
         */
        Translation translation = new Translation(queryTerm);
        response.body()
                .lines()
                .filter(l -> l.contains(EN_MARKER) || l.contains(DE_MARKER))
                .forEach(l -> {
                    if (l.contains(EN_MARKER)) {
                        translation.setDestination(processResults(l));
                    } else {
                        translation.setSource(processResults(l));
                    }
                });
        return Optional.of(translation);
    }

    /*
     * simple string-based processor of the result-lines from dict.cc
     */
    List<String> processResults(String line) {
        var values = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')'));
        return Stream.of(values.split("\",\""))                 // split the string up
                .map(l -> l.replaceAll("\"", ""))   // remove the javascript "-marks
                .filter(l -> !l.equals(""))                           // remove empty entries
                .collect(Collectors.toList());
    }

    /**
     * To make the creation of a proper configured Dict-Service a bit easier we provide a builder pattern for it
     */
    public static class DictBuilder {
        private final String toBeTranslated;
        private String from = "de";
        private String to = "en";
        private String proxyHost;
        private Integer proxyPort;
        private String proxyUser;
        private char[] proxyPass;

        DictBuilder(String toBeTranslated) {
            this.toBeTranslated = toBeTranslated;
        }

        public DictBuilder from(String sourceLanguageCode) {
            this.from = sourceLanguageCode;
            return this;
        }

        public DictBuilder to(String destinationLanguageCode) {
            this.to = destinationLanguageCode;
            return this;
        }

        public DictBuilder withProxyHost(String host) {
            this.proxyHost = host;
            return this;
        }

        public DictBuilder withProxyPort(Integer port) {
            this.proxyPort = port;
            return this;
        }

        public DictBuilder withProxyUser(String username) {
            this.proxyUser = username;
            return this;
        }

        public DictBuilder withProxyPass(char[] password) {
            this.proxyPass = password;
            return this;
        }

        public Translation build() throws IOException, InterruptedException {
            if (Objects.nonNull(this.proxyHost) && Objects.isNull(this.proxyPort) || Objects.isNull(this.proxyHost) && Objects.nonNull(this.proxyPort)) {
                throw new IllegalStateException("You have to define both - a proxy host along with a proxy port");
            } else {
                if (Objects.nonNull(this.proxyUser) && Objects.isNull(this.proxyPass) || Objects.isNull(this.proxyUser) && Objects.nonNull(this.proxyPass)) {
                    throw new IllegalStateException("When providing authentication information you have to provide both, username as well as password");
                }
            }
            return new Dict(this).execute();
        }
    }
}
