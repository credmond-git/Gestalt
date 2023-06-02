package org.github.gestalt.config.post.process.transform;

import org.github.gestalt.config.annotations.ConfigPriority;
import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.utils.ValidateOf;

/**
 * Allows you to inject System Properties into leaf values that match ${envVar:key},
 * where the key is used to lookup into the Environment Variables.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
@ConfigPriority(200)
public class SystemPropertiesTransformer implements Transformer {
    @Override
    public String name() {
        return "sys";
    }

    @Override
    public ValidateOf<String> process(String path, String key, String rawValue) {
        if (key == null) {
            return ValidateOf.inValid(new ValidationError.InvalidStringSubstitutionPostProcess(path, rawValue, name()));
        } else if (!System.getProperties().containsKey(key)) {
            return ValidateOf.inValid(new ValidationError.NoSystemPropertyFoundPostProcess(path, key));
        } else {
            return ValidateOf.valid(System.getProperties().get(key).toString());
        }
    }
}
