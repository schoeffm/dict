package de.bender.dict.boundary;

import de.bender.dict.control.Dict;
import de.bender.dict.control.OutputFormatter;
import de.bender.dict.control.OutputFormatter.OutputFormat;
import de.bender.dict.model.Translation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "dict", mixinStandardHelpOptions = true,
        version = "1.0.1",
        description = """
            A little CLI helper to make calls to 'dict.cc' in order to have a quick CLI translator - it can also be used/integrated with Alfred launcher (see supported output-formats).
            The CLI picks up HTTPS_PROXY settings and also supports proxy-authentication (via basic auth) - you can overwrite that default-behavior by explicitly setting proper proxy-options.
            You can use the tool by just passing in the phase/words you'd like to have translated (where it translates english <-> german by default). You can also specify the languages to be used for translation.""")
public class DictCommand implements Callable<Integer> {

    private static final Integer EXIT_CODE_OK = 0;
    private static final Integer EXIT_CODE_EMPTY_BODY = 123;
    private static final Integer EXIT_CODE_NO_INPUT = 120;

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

    /**
     * The main {@code call}-method which gets executed whenever the CLI command is called
     *
     * @return an integer that represts the return code
     * @throws Exception in case we stumble upon sth.
     */
    @Override
    public Integer call() throws Exception {
        if (Objects.isNull(queryTerms)) { return EXIT_CODE_NO_INPUT; }

        // required in >= JDK8 to make basic-auth for proxies work
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        var fromTo = determineLanguageCombination(queryTerms);      // extract the language instructions (if present)
        var queryTerm = String.join(" ", queryTerms);      // now concatenate the words to form a phrase

        // create a Dict-instance and trigger the translation
        Translation translation = Dict
                .translate(queryTerm)
                .from(fromTo.getKey())
                .to(fromTo.getValue())
                .withProxyHost(proxyHost)
                .withProxyPort(proxyPort)
                .withProxyUser(proxyUser)
                .withProxyPass(Optional.ofNullable(proxyPassword).map(String::toCharArray).orElse(null))
                .build();

        outputFormatter.stream()
                .filter(f -> f.canHandle(outputFormat))
                .map(f -> f.format(translation))
                .forEach(System.out::println);

        return EXIT_CODE_OK;
    }

    /*
     * depending on whether the user put in some dedicated source/destination language pairs we either return
     * the default (which is de<>en) or we create a combination if the passed in parameters are supported.
     */
    private SimpleEntry<String, String> determineLanguageCombination(List<String> queryTerms) {
        var supportedLanguages = List.of("de", "en", "es", "fr", "it");
        if (queryTerms.size() > 2 && supportedLanguages.contains(queryTerms.get(0)) && supportedLanguages.contains(queryTerms.get(1))) {
            var result = new SimpleEntry<>(queryTerms.get(0),  queryTerms.get(1));
            queryTerms.remove(0);
            queryTerms.remove(0);
            return result;
        } else {
            return new SimpleEntry<>("de", "en");
        }
    }

}
