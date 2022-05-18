package de.bender.dict.boundary;

import de.bender.dict.control.OutputFormatter;
import de.bender.dict.control.OutputFormatter.OutputFormat;
import de.bender.dict.model.Translation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * TODOs
 * - make proxy optional/configurable
 * - make XML-output possible and also configureble
 * - also implement a plain output mode
 * - refactoring into several classes
 */

@Command(name = "dict", mixinStandardHelpOptions = true,
        description = "A little CLI helper to make calls to 'www.dict.cc' in order to have a " +
                "quick translator - the CLI can also be used/integrated with Alfred launcher (see supported " +
                "output-formats).\nThe CLI picks up HTTPS_PROXY settings and also supports proxy-authentication (via " +
                "basic auth) - you can overwrite that default-behavior by explicitly setting proper proxy-options.")
public class DictCommand implements Callable<Integer> {

    private static final Integer EXIT_CODE_OK = 0;
    private static final Integer EXIT_CODE_EMPTY_BODY = 123;
    private static final Integer EXIT_CODE_NO_INPUT = 120;

    private static final String EN_MARKER = "var c1Arr";
    private static final String DE_MARKER = "var c2Arr";

    @Inject
    @Any
    Instance<OutputFormatter> outputFormatter;

    @Parameters(description = "Query term to be translated")
    private List<String> queryTerms;

    @Option(names = {"-o", "--output"}, defaultValue = "raw",
            required = true,
            description = "Determines the output-format (currently supported: raw, json, alfred)")
    private OutputFormat outputFormat;

    @Option(names = {"--proxy-host"},
            description = "The Proxy-Host to be used (i.e. proxy.muc)")
    private String proxyHost;

    @Option(names = {"--proxy-port"},
            description = "The Proxy-Port to be used (i.e. 8080)")
    private Integer proxyPort;

    @Option(names = {"--proxy-user"},
            description = "The username to be used for proxy-authentication")
    private String proxyUser;

    @Option(names = {"--proxy-pass"},
            description = "The password to be used for proxy-authentication")
    private String proxyPassword;

    @Override
    public Integer call() throws Exception {
        if (Objects.isNull(queryTerms)) { return EXIT_CODE_NO_INPUT; }

        // required in >= JDK8 to make basic-auth for proxies work
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        var queryTerm = String.join(" ", queryTerms);
        var request = HttpRequest
                .newBuilder(URI.create("https://www.dict.cc/?s=" + URLEncoder.encode(queryTerm, UTF_8.toString())))
                .header("User-agent", "Mozilla/6.0")
                .build();
        var response = createHttpClient().send(request, BodyHandlers.ofString());

        Translation translation = convert(queryTerm, response)
                .orElseThrow(() -> new RuntimeException("Dict returned " + response.statusCode() + "!!!"));

        outputFormatter.stream()
                .filter(f -> f.canHandle(outputFormat))
                .map(f -> f.format(translation))
                .forEach(System.out::println);

        return EXIT_CODE_OK;
    }

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
                        translation.setEnglish(processResults(l));
                    } else {
                        translation.setGerman(processResults(l));
                    }
                });
        return Optional.of(translation);
    }

    HttpClient createHttpClient() {
        var clientBuilder = HttpClient.newBuilder()
                .followRedirects(NORMAL)
                .version(HTTP_1_1);

        if (Objects.nonNull(proxyHost) && Objects.nonNull(proxyPort)) {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
            if (Objects.nonNull(proxyUser) && Objects.nonNull(proxyPassword)) {
                clientBuilder.authenticator(basicAuthAuthenticator(proxyUser, proxyPassword.toCharArray()));
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

    Authenticator basicAuthAuthenticator(String username, char[] password) {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
    }

    List<String> processResults(String line) {
        var values = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')'));
        return Stream.of(values.split(","))                     // split the string up
                .map(l -> l.replaceAll("\"", ""))   // remove the javascript "-marks
                .filter(l -> !l.equals(""))                           // remove empty entries
                .collect(Collectors.toList());
    }


}
