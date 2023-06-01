package org.github.gestalt.config.path.mapper;

import org.github.gestalt.config.annotations.ConfigPriority;
import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.lexer.SentenceLexer;
import org.github.gestalt.config.node.MapNode;
import org.github.gestalt.config.token.Token;
import org.github.gestalt.config.utils.ValidateOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Splits the sentence by camel case and converts it to Snake Case.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
@ConfigPriority(550)
public class SnakeCasePathMapper implements PathMapper {
    private final Pattern regex = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

    @SuppressWarnings("StringSplitter")
    @Override
    public ValidateOf<List<Token>> map(String path, String sentence, SentenceLexer lexer) {
        if (sentence == null) {
            return ValidateOf.inValid(new ValidationError.MappingValueNull(path, "SnakeCasePathMapper"));
        }

        String[] camelCaseWords = regex.split(sentence);
        String kebebCase =  Arrays.stream(camelCaseWords)
                                  .map(it -> it.toLowerCase(Locale.getDefault()))
                                  .collect(Collectors.joining("_"));

        List<Token> tokens = new ArrayList<>();
        ValidateOf<List<Token>> lexedValidateOf = lexer.scan(kebebCase);

        if (!lexedValidateOf.hasResults()) {
            return ValidateOf.inValid(new ValidationError.NoResultsFoundForNode(path, MapNode.class, "Snake case path mapping"));
        }
        tokens.addAll(lexedValidateOf.results());

        return ValidateOf.validateOf(tokens, lexedValidateOf.getErrors());
    }
}