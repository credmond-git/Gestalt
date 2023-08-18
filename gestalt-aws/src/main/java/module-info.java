// Module info definition for gestalt s3 integration
module org.github.gestalt.aws {
    requires org.github.gestalt.core;
    requires transitive software.amazon.awssdk.services.s3;
    requires transitive software.amazon.awssdk.core;
    requires transitive software.amazon.awssdk.auth;
    requires transitive software.amazon.awssdk.regions;
    requires transitive software.amazon.awssdk.services.secretsmanager;
    requires transitive software.amazon.awssdk.http.urlconnection;
    requires transitive com.fasterxml.jackson.databind;

    exports org.github.gestalt.config.aws.config;
    exports org.github.gestalt.config.aws.errors;
    exports org.github.gestalt.config.aws.s3;
    exports org.github.gestalt.config.aws.transformer;
}

