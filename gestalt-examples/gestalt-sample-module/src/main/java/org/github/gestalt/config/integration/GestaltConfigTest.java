package org.github.gestalt.config.integration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.annotations.Config;
import org.github.gestalt.config.annotations.ConfigPrefix;
import org.github.gestalt.config.aws.config.AWSBuilder;
import org.github.gestalt.config.aws.s3.S3ConfigSource;
import org.github.gestalt.config.builder.GestaltBuilder;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.git.GitConfigSource;
import org.github.gestalt.config.git.GitConfigSourceBuilder;
import org.github.gestalt.config.google.storage.GCSConfigSource;
import org.github.gestalt.config.guice.GestaltModule;
import org.github.gestalt.config.guice.InjectConfig;
import org.github.gestalt.config.post.process.transform.RandomTransformer;
import org.github.gestalt.config.post.process.transform.SystemPropertiesTransformer;
import org.github.gestalt.config.post.process.transform.TransformerPostProcessor;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.reload.CoreReloadListener;
import org.github.gestalt.config.source.*;
import org.github.gestalt.config.tag.Tags;
import org.github.gestalt.config.vault.config.VaultBuilder;
import org.github.gestalt.config.vault.config.VaultModuleConfig;
import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GestaltConfigTest {

    public void integrationTest() throws GestaltException {
        // Create a map of configurations we wish to inject.
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        // using the builder to layer on the configuration files.
        // The later ones layer on and over write any values in the previous
        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("default.properties"))
                .addSource(new ClassPathConfigSource("dev.properties"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        // Load the configurations, this will thow exceptions if there are any errors.
        gestalt.loadConfigs();

        validateResults(gestalt);
    }

    public void integrationTestNoCache() throws GestaltException {
        // Create a map of configurations we wish to inject.
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        // using the builder to layer on the configuration files.
        // The later ones layer on and over write any values in the previous
        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("default.properties"))
                .addSource(new ClassPathConfigSource("dev.properties"))
                .addSource(new MapConfigSource(configs))
                .useCacheDecorator(false)
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        // Load the configurations, this will thow exceptions if there are any errors.
        gestalt.loadConfigs();

        validateResults(gestalt);
    }

    public void integrationTestTags() throws GestaltException {
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");

        String fileURL = "https://raw.githubusercontent.com/gestalt-config/gestalt/main/gestalt-core/src/test/resources/default.properties";

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new URLConfigSource(fileURL))
                .addSource(new ClassPathConfigSource("dev.properties", Tags.of("toy", "ball")))
                .addSource(new MapConfigSource(configs))
                .addSource(new StringConfigSource("db.idleTimeout=123", "properties"))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        HttpPool pool = gestalt.getConfig("http.pool", HttpPool.class);

        Assertions.assertEquals(100, pool.maxTotal);
        Assertions.assertEquals((short) 100, gestalt.getConfig("http.pool.maxTotal", Short.class));
        Assertions.assertEquals(10L, pool.maxPerRoute);
        Assertions.assertEquals(10L, gestalt.getConfig("http.pool.maxPerRoute", Long.class));
        Assertions.assertEquals(6000, pool.validateAfterInactivity);
        Assertions.assertEquals(60000D, pool.keepAliveTimeoutMs);
        Assertions.assertEquals(25, pool.idleTimeoutSec);
        Assertions.assertEquals(33.0F, pool.defaultWait);

        HttpPool poolTags = gestalt.getConfig("http.pool", HttpPool.class, Tags.of("toy", "ball"));

        Assertions.assertEquals(1000, poolTags.maxTotal);
        Assertions.assertEquals((short) 1000, gestalt.getConfig("http.pool.maxTotal", Short.class, Tags.of("toy", "ball")));
        Assertions.assertEquals(50L, poolTags.maxPerRoute);
        Assertions.assertEquals(50L, gestalt.getConfig("http.pool.maxPerRoute", Long.class, Tags.of("toy", "ball")));
        Assertions.assertEquals(6000, poolTags.validateAfterInactivity);
        Assertions.assertEquals(60000D, poolTags.keepAliveTimeoutMs);
        Assertions.assertEquals(25, poolTags.idleTimeoutSec);
        Assertions.assertEquals(33.0F, poolTags.defaultWait);

        HttpPool poolTags2 = gestalt.getConfig("http.pool", HttpPool.class, Tags.of("toy", "car"));

        Assertions.assertEquals(100, poolTags2.maxTotal);
        Assertions.assertEquals((short) 100, gestalt.getConfig("http.pool.maxTotal", Short.class, Tags.of("toy", "car")));
        Assertions.assertEquals(10L, poolTags2.maxPerRoute);
        Assertions.assertEquals(10L, gestalt.getConfig("http.pool.maxPerRoute", Long.class, Tags.of("toy", "car")));
        Assertions.assertEquals(6000, poolTags2.validateAfterInactivity);
        Assertions.assertEquals(60000D, poolTags2.keepAliveTimeoutMs);
        Assertions.assertEquals(25, poolTags2.idleTimeoutSec);
        Assertions.assertEquals(33.0F, poolTags2.defaultWait);
    }

    public void integrationTestEnvVars() throws GestaltException {

        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");

        String urlFile = "https://raw.githubusercontent.com/gestalt-config/gestalt/main/gestalt-examples/gestalt-sample/src/test/resources/default.json";

        /*
        Expects the following environment variables
            DB_IDLETIMEOUT: 123
            SUBSERVICE_BOOKING_ISENABLED: true
            SUBSERVICE_BOOKING_SERVICE_HOST: https://dev.booking.host.name
            SUBSERVICE_BOOKING_SERVICE_PORT: 443
         */

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new URLConfigSource(urlFile))
                .addSource(new ClassPathConfigSource("dev.properties"))
                .addSource(new MapConfigSource(configs))
                .addSource(new EnvironmentConfigSource())
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);

        SubService booking = gestalt.getConfig("subservice.booking", TypeCapture.of(SubService.class));
        Assertions.assertTrue(booking.isEnabled());
        Assertions.assertEquals("https://dev.booking.host.name", booking.getService().getHost());
        Assertions.assertEquals(443, booking.getService().getPort());
        Assertions.assertEquals("booking", booking.getService().getPath());
    }

    public void integrationTestJson() throws GestaltException {
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("default.json"))
                .addSource(new ClassPathConfigSource("dev.json"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);
    }

    public void integrationTestYaml() throws GestaltException {
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("default.yml"))
                .addSource(new ClassPathConfigSource("dev.yml"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);
    }

    public void integrationTestJsonAndYaml() throws GestaltException {
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("default.json"))
                .addSource(new ClassPathConfigSource("dev.yml"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);
    }

    public void integrationTestHocon() throws GestaltException {
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("default.conf"))
                .addSource(new ClassPathConfigSource("dev.yml"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);
    }

    public void integrationTestToml() throws GestaltException {
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("default.conf"))
                .addSource(new ClassPathConfigSource("dev.toml"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);
    }

    public void integrationGitTest() throws GestaltException, IOException {
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        Path configDirectory = Files.createTempDirectory("gitConfigIntegration");
        configDirectory.toFile().deleteOnExit();


        GitConfigSourceBuilder gitBuilder = new GitConfigSourceBuilder()
                .setRepoURI("https://github.com/gestalt-config/gestalt.git")
                .setConfigFilePath("gestalt-examples/gestalt-sample/src/test/resources/default.properties")
                .setLocalRepoDirectory(configDirectory);
        GitConfigSource source = gitBuilder.build();

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(source)
                .addSource(new ClassPathConfigSource("dev.properties"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);
    }

    public void integrationTestGoogleCloud() throws GestaltException {
        // Create a map of configurations we wish to inject.
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        // Load the default property files from resources.
        // Contents of the dev.properties on GCP Storage, note the use of ${gcpSecret:booking-host}
        // the secrets "bank-host" value is "booking.host.name"
        /*
        db.hosts[0].url=jdbc:postgresql://dev.host.name1:5432/mydb
        db.hosts[1].url=jdbc:postgresql://dev.host.name2:5432/mydb
        db.hosts[2].url=jdbc:postgresql://dev.host.name3:5432/mydb
        db.connectionTimeout=600

        http.pool.maxTotal=1000
        http.pool.maxPerRoute=50

        subservice.booking.service.isEnabled=true
        subservice.booking.service.host=https://dev.${gcpSecret:bank-host}
        subservice.booking.service.port=443
        subservice.booking.service.path=booking

        subservice.search.service.isEnabled=false

        admin.user=Peter, Kim, Steve
        admin.overrideEnabled=true
         */


        // using the builder to layer on the configuration files.
        // The later ones layer on and over write any values in the previous
        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("/default.properties"))
                .addSource(new GCSConfigSource("gestalt-test", "dev.properties"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        // Load the configurations, this will thow exceptions if there are any errors.
        gestalt.loadConfigs();

        validateResults(gestalt);


        SubService booking = gestalt.getConfig("subservice.booking", TypeCapture.of(SubService.class));
        Assertions.assertTrue(booking.isEnabled());
        Assertions.assertEquals("https://dev.booking.host.name", booking.getService().getHost());
        Assertions.assertEquals(443, booking.getService().getPort());
        Assertions.assertEquals("booking", booking.getService().getPath());
    }

    public void integrationTestAws() throws GestaltException {
        // Create a map of configurations we wish to inject.
        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");
        configs.put("db.idleTimeout", "123");

        // Load the default property files from resources.
        // Contents of the dev.properties on aws Storage, note the use of ${awsSecret:booking-host:host}
        // the secrets "booking-host:host" value is "booking.host.name"
        /*
        db.hosts[0].url=jdbc:postgresql://dev.host.name1:5432/mydb
        db.hosts[1].url=jdbc:postgresql://dev.host.name2:5432/mydb
        db.hosts[2].url=jdbc:postgresql://dev.host.name3:5432/mydb
        db.connectionTimeout=600

        http.pool.maxTotal=1000
        http.pool.maxPerRoute=50

        subservice.booking.service.isEnabled=true
        subservice.booking.service.host=https://dev.${awsSecret:booking-host:host}
        subservice.booking.service.port=443
        subservice.booking.service.path=booking

        subservice.search.service.isEnabled=false

        admin.user=Peter, Kim, Steve
        admin.overrideEnabled=true
         */
        S3Client s3Client = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();


        // using the builder to layer on the configuration files.
        // The later ones layer on and over write any values in the previous
        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("/default.properties"))
                .addSource(new S3ConfigSource(s3Client, "gestalt-test", "dev.properties"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .addModuleConfig(AWSBuilder.builder().setRegion("us-east-1").build())
                .build();

        // Load the configurations, this will thow exceptions if there are any errors.
        gestalt.loadConfigs();

        validateResults(gestalt);


        SubService booking = gestalt.getConfig("subservice.booking", TypeCapture.of(SubService.class));
        Assertions.assertTrue(booking.isEnabled());
        Assertions.assertEquals("https://dev.booking.host.name", booking.getService().getHost());
        Assertions.assertEquals(443, booking.getService().getPort());
        Assertions.assertEquals("booking", booking.getService().getPath());
    }

    public void integrationTestPostProcessorVault() throws GestaltException, VaultException {
        String VAULT_TOKEN = "my-root-token-2";


        final VaultConfig config = new VaultConfig()
                .address("http://127.0.0.1:8080")
                .token(VAULT_TOKEN)
                .build();

        Vault vault = Vault.create(config);

        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");

        VaultModuleConfig vaultModuleConfig = VaultBuilder.builder().setVault(vault).build();

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("/defaultPPVault.properties"))
                .addSource(new ClassPathConfigSource("/integration.properties"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .addModuleConfig(vaultModuleConfig)
                .build();

        gestalt.loadConfigs();

        SubService booking = gestalt.getConfig("subservice.booking", TypeCapture.of(SubService.class));
        Assertions.assertTrue(booking.isEnabled());
        Assertions.assertEquals("https://dev.booking.host.name", booking.getService().getHost());
        Assertions.assertEquals(443, booking.getService().getPort());
        Assertions.assertEquals("booking", booking.getService().getPath());

        validateResults(gestalt);

    }


    private void validateResults(Gestalt gestalt) throws GestaltException {
        HttpPool pool = gestalt.getConfig("http.pool", HttpPool.class);

        Assertions.assertEquals(1000, pool.maxTotal);
        Assertions.assertEquals((short) 1000, gestalt.getConfig("http.pool.maxTotal", Short.class));

        //Check with the wrong case
        Assertions.assertEquals((short) 1000, gestalt.getConfig("HTTP.pool.MAXTotal", Short.class));

        Assertions.assertEquals(50L, pool.maxPerRoute);
        Assertions.assertEquals(50L, gestalt.getConfig("http.pool.maxPerRoute", Long.class));
        Assertions.assertEquals(6000, pool.validateAfterInactivity);
        Assertions.assertEquals(60000D, pool.keepAliveTimeoutMs);
        Assertions.assertEquals(25, pool.idleTimeoutSec);
        Assertions.assertEquals(33.0F, pool.defaultWait);

        long startTime = System.nanoTime();
        gestalt.getConfig("db", DataBase.class);
        long timeTaken = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        DataBase db = gestalt.getConfig("db", DataBase.class);
        long cacheTimeTaken = System.nanoTime() - startTime;

        // not really a great test for ensuring we are hitting a cache
        //Assertions.assertTrue(timeTaken > cacheTimeTaken);

        Assertions.assertEquals(600, db.connectionTimeout);
        Assertions.assertEquals(600, gestalt.getConfig("DB.connectionTimeout", Integer.class));
        Assertions.assertEquals(123, db.idleTimeout);
        Assertions.assertEquals(60000.0F, db.maxLifetime);
        Assertions.assertNull(db.isEnabled);
        Assertions.assertTrue(gestalt.getConfig("db.isEnabled", true, Boolean.class));

        Assertions.assertEquals(3, db.hosts.size());
        Assertions.assertEquals("credmond", db.hosts.get(0).getUser());
        // index into the path of an array.
        Assertions.assertEquals("credmond", gestalt.getConfig("db.hosts[0].user", "test", String.class));
        Assertions.assertEquals("1234", db.hosts.get(0).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name1:5432/mydb", db.hosts.get(0).url);
        Assertions.assertEquals("credmond", db.hosts.get(1).getUser());
        Assertions.assertEquals("5678", db.hosts.get(1).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name2:5432/mydb", db.hosts.get(1).url);
        Assertions.assertEquals("credmond", db.hosts.get(2).getUser());
        Assertions.assertEquals("9012", db.hosts.get(2).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name3:5432/mydb", db.hosts.get(2).url);

        Assertions.assertEquals("test", gestalt.getConfig("db.does.not.exist", "test", String.class));

        //  validate prefix annotation
        DataBasePrefix dbPrefix = gestalt.getConfig("", DataBasePrefix.class);
        // not really a great test for ensuring we are hitting a cache
        Assertions.assertEquals(600, dbPrefix.connectionTimeout);
        Assertions.assertEquals(123, dbPrefix.idleTimeout);
        Assertions.assertEquals(60000.0F, dbPrefix.maxLifetime);
        Assertions.assertNull(dbPrefix.isEnabled);

        Assertions.assertEquals(3, dbPrefix.hosts.size());
        Assertions.assertEquals("credmond", dbPrefix.hosts.get(0).getUser());
        // index into the path of an array.
        Assertions.assertEquals("1234", dbPrefix.hosts.get(0).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name1:5432/mydb", dbPrefix.hosts.get(0).url);
        Assertions.assertEquals("credmond", dbPrefix.hosts.get(1).getUser());
        Assertions.assertEquals("5678", dbPrefix.hosts.get(1).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name2:5432/mydb", dbPrefix.hosts.get(1).url);
        Assertions.assertEquals("credmond", dbPrefix.hosts.get(2).getUser());
        Assertions.assertEquals("9012", dbPrefix.hosts.get(2).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name3:5432/mydb", dbPrefix.hosts.get(2).url);

        List<Host> hosts = gestalt.getConfig("db.hosts", Collections.emptyList(), new TypeCapture<List<Host>>() {
        });
        Assertions.assertEquals(3, hosts.size());
        Assertions.assertEquals("credmond", hosts.get(0).getUser());
        Assertions.assertEquals("1234", hosts.get(0).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name1:5432/mydb", hosts.get(0).url);
        Assertions.assertEquals("credmond", hosts.get(1).getUser());
        Assertions.assertEquals("5678", hosts.get(1).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name2:5432/mydb", hosts.get(1).url);
        Assertions.assertEquals("credmond", hosts.get(2).getUser());
        Assertions.assertEquals("9012", hosts.get(2).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name3:5432/mydb", hosts.get(2).url);

        List<IHost> ihosts = gestalt.getConfig("db.hosts", Collections.emptyList(), new TypeCapture<List<IHost>>() {
        });
        Assertions.assertEquals(3, ihosts.size());
        Assertions.assertEquals("credmond", ihosts.get(0).getUser());
        Assertions.assertEquals("1234", ihosts.get(0).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name1:5432/mydb", ihosts.get(0).getUrl());
        Assertions.assertEquals("credmond", ihosts.get(1).getUser());
        Assertions.assertEquals("5678", ihosts.get(1).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name2:5432/mydb", ihosts.get(1).getUrl());
        Assertions.assertEquals("credmond", ihosts.get(2).getUser());
        Assertions.assertEquals("9012", ihosts.get(2).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name3:5432/mydb", ihosts.get(2).getUrl());

        List<IHostDefault> ihostsDefault = gestalt.getConfig("db.hosts", Collections.emptyList(), new TypeCapture<List<IHostDefault>>() {
        });
        Assertions.assertEquals(3, ihostsDefault.size());
        Assertions.assertEquals("credmond", ihostsDefault.get(0).getUser());
        Assertions.assertEquals("1234", ihostsDefault.get(0).getPassword());
        //Assertions.assertEquals(10, ihostsDefault.get(0).getPort());

        List<IHostAnnotations> iHostAnnotations = gestalt.getConfig("db.hosts", Collections.emptyList(), new TypeCapture<>() {
        });
        Assertions.assertEquals(3, iHostAnnotations.size());
        Assertions.assertEquals("credmond", iHostAnnotations.get(0).getUser());
        Assertions.assertEquals("1234", iHostAnnotations.get(0).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name1:5432/mydb", iHostAnnotations.get(0).getUrl());
        Assertions.assertEquals("customers", iHostAnnotations.get(0).getTable());
        Assertions.assertEquals("credmond", iHostAnnotations.get(1).getUser());
        Assertions.assertEquals("5678", iHostAnnotations.get(1).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name2:5432/mydb", iHostAnnotations.get(1).getUrl());
        Assertions.assertEquals("customers", iHostAnnotations.get(1).getTable());
        Assertions.assertEquals("credmond", iHostAnnotations.get(2).getUser());
        Assertions.assertEquals("9012", iHostAnnotations.get(2).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name3:5432/mydb", iHostAnnotations.get(2).getUrl());
        Assertions.assertEquals("customers", iHostAnnotations.get(02).getTable());

        List<HostAnnotations> hostsAnnotations = gestalt.getConfig("db.hosts", Collections.emptyList(), new TypeCapture<List<HostAnnotations>>() {
        });
        Assertions.assertEquals(3, hostsAnnotations.size());
        Assertions.assertEquals("credmond", hostsAnnotations.get(0).getUser());
        Assertions.assertEquals("1234", hostsAnnotations.get(0).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name1:5432/mydb", hostsAnnotations.get(0).getUrl());
        Assertions.assertEquals("customers", hostsAnnotations.get(0).getTable());
        Assertions.assertEquals("credmond", hostsAnnotations.get(1).getUser());
        Assertions.assertEquals("5678", hostsAnnotations.get(1).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name2:5432/mydb", hostsAnnotations.get(1).getUrl());
        Assertions.assertEquals("customers", hostsAnnotations.get(1).getTable());
        Assertions.assertEquals("credmond", hostsAnnotations.get(2).getUser());
        Assertions.assertEquals("9012", hostsAnnotations.get(2).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name3:5432/mydb", hostsAnnotations.get(2).getUrl());
        Assertions.assertEquals("customers", hostsAnnotations.get(2).getTable());

        List<HostMethodAnnotations> hostsMethodAnnotations = gestalt.getConfig("db.hosts", Collections.emptyList(), new TypeCapture<List<HostMethodAnnotations>>() {
        });
        Assertions.assertEquals(3, hostsMethodAnnotations.size());
        Assertions.assertEquals("credmond", hostsMethodAnnotations.get(0).getUser());
        Assertions.assertEquals("1234", hostsMethodAnnotations.get(0).getSecret());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name1:5432/mydb", hostsMethodAnnotations.get(0).getUrl());
        Assertions.assertEquals("customers", hostsMethodAnnotations.get(0).getTable());
        Assertions.assertEquals("credmond", hostsMethodAnnotations.get(1).getUser());
        Assertions.assertEquals("5678", hostsMethodAnnotations.get(1).getSecret());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name2:5432/mydb", hostsMethodAnnotations.get(1).getUrl());
        Assertions.assertEquals("customers", hostsMethodAnnotations.get(1).getTable());
        Assertions.assertEquals("credmond", hostsMethodAnnotations.get(2).getUser());
        Assertions.assertEquals("9012", hostsMethodAnnotations.get(2).getSecret());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name3:5432/mydb", hostsMethodAnnotations.get(2).getUrl());
        Assertions.assertEquals("customers", hostsMethodAnnotations.get(2).getTable());

        List<Host> noHosts = gestalt.getConfig("db.not.hosts", Collections.emptyList(), new TypeCapture<List<Host>>() {
        });
        Assertions.assertEquals(0, noHosts.size());

        User admin = gestalt.getConfig("admin", new TypeCapture<User>() {
        });
        Assertions.assertEquals(3, admin.user.length);
        Assertions.assertEquals("Peter", admin.user[0]);
        Assertions.assertEquals("Kim", admin.user[1]);
        Assertions.assertEquals("Steve", admin.user[2]);
        Assertions.assertEquals(Role.LEVEL0, admin.accessRole);
        Assertions.assertTrue(admin.overrideEnabled);

        User user = gestalt.getConfig("employee", new TypeCapture<User>() {
        });
        Assertions.assertEquals(1, user.user.length);
        Assertions.assertEquals("Janice", user.user[0]);
        Assertions.assertEquals(Role.LEVEL1, user.accessRole);
        Assertions.assertFalse(user.overrideEnabled);

        Assertions.assertEquals("active", gestalt.getConfig("serviceMode", TypeCapture.of(String.class)));
        Assertions.assertEquals('a', gestalt.getConfig("serviceMode", TypeCapture.of(Character.class)));

        // Validate that guice gets the injected config.

        Injector injector = Guice.createInjector(new GestaltModule(gestalt));
        DBQueryService dbService = injector.getInstance(DBQueryService.class);
        db = dbService.getDataBase();
        Assertions.assertEquals(600, db.connectionTimeout);
        Assertions.assertEquals(600, gestalt.getConfig("DB.connectionTimeout", Integer.class));
        Assertions.assertEquals(123, db.idleTimeout);
        Assertions.assertEquals(60000.0F, db.maxLifetime);
        Assertions.assertNull(db.isEnabled);
        Assertions.assertTrue(gestalt.getConfig("db.isEnabled", true, Boolean.class));

        Assertions.assertEquals(3, db.hosts.size());
        Assertions.assertEquals("credmond", db.hosts.get(0).getUser());
        // index into the path of an array.
        Assertions.assertEquals("credmond", gestalt.getConfig("db.hosts[0].user", "test", String.class));
        Assertions.assertEquals("1234", db.hosts.get(0).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name1:5432/mydb", db.hosts.get(0).url);
        Assertions.assertEquals("credmond", db.hosts.get(1).getUser());
        Assertions.assertEquals("5678", db.hosts.get(1).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name2:5432/mydb", db.hosts.get(1).url);
        Assertions.assertEquals("credmond", db.hosts.get(2).getUser());
        Assertions.assertEquals("9012", db.hosts.get(2).getPassword());
        Assertions.assertEquals("jdbc:postgresql://dev.host.name3:5432/mydb", db.hosts.get(2).url);


        Assertions.assertEquals("test", gestalt.getConfig("db.does.not.exist", "test", String.class));

    }

    public void integrationTestPostProcessorEnvironment() throws GestaltException {

        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");

        /*
        Expects the following environment variables
            DB_IDLETIMEOUT: 123
            SUBSERVICE_BOOKING_ISENABLED: true
            SUBSERVICE_BOOKING_SERVICE_HOST: https://dev.booking.host.name
            SUBSERVICE_BOOKING_SERVICE_PORT: 443
         */

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("defaultPPEnv.properties"))
                .addSource(new ClassPathConfigSource("integration.properties"))
                .addSource(new MapConfigSource(configs))
                .addDefaultPostProcessors()
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);

        SubService booking = gestalt.getConfig("subservice.booking", TypeCapture.of(SubService.class));
        Assertions.assertTrue(booking.isEnabled());
        Assertions.assertEquals("https://dev.booking.host.name", booking.getService().getHost());
        Assertions.assertEquals(443, booking.getService().getPort());
        Assertions.assertEquals("booking", booking.getService().getPath());
    }

    public void integrationTestPostProcessorSystem() throws GestaltException {

        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");

        /*
        Expects the following system properties variables
            DB_IDLETIMEOUT: 123
            SUBSERVICE_BOOKING_ISENABLED: true
            SUBSERVICE_BOOKING_SERVICE_HOST: https://dev.booking.host.name
            SUBSERVICE_BOOKING_SERVICE_PORT: 443
         */

        System.getProperties().put("DB_IDLETIMEOUT", "123");
        System.getProperties().put("SUBSERVICE_BOOKING_ISENABLED", "true");
        System.getProperties().put("SUBSERVICE_BOOKING_SERVICE_HOST", "https://dev.booking.host.name");
        System.getProperties().put("SUBSERVICE_BOOKING_SERVICE_PORT", "443");

        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("defaultPPSys.properties"))
                .addSource(new ClassPathConfigSource("integration.properties"))
                .addSource(new MapConfigSource(configs))
                .addPostProcessor(new TransformerPostProcessor(List.of(new SystemPropertiesTransformer(), new RandomTransformer())))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);

        SubService booking = gestalt.getConfig("subservice.booking", TypeCapture.of(SubService.class));
        Assertions.assertTrue(booking.isEnabled());
        Assertions.assertEquals("https://dev.booking.host.name", booking.getService().getHost());
        Assertions.assertEquals(443, booking.getService().getPort());
        Assertions.assertEquals("booking", booking.getService().getPath());
        Assertions.assertNotNull(gestalt.getConfig("appUUID", UUID.class));
        Assertions.assertTrue(gestalt.getConfig("appId", Integer.class) == 20 ||
                gestalt.getConfig("appId", Integer.class) == 21);
    }

    public void integrationTestPostProcessorNode() throws GestaltException {

        Map<String, String> configs = new HashMap<>();
        configs.put("db.hosts[0].password", "1234");
        configs.put("db.hosts[1].password", "5678");
        configs.put("db.hosts[2].password", "9012");


        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new ClassPathConfigSource("defaultPPNode.properties"))
                .addSource(new ClassPathConfigSource("integration.properties"))
                .addSource(new MapConfigSource(configs))
                .setTreatNullValuesInClassAsErrors(false)
                .build();

        gestalt.loadConfigs();

        validateResults(gestalt);

        SubService booking = gestalt.getConfig("subservice.booking", TypeCapture.of(SubService.class));
        Assertions.assertTrue(booking.isEnabled());
        Assertions.assertEquals("https://dev.booking.host.name", booking.getService().getHost());
        Assertions.assertEquals(443, booking.getService().getPort());
        Assertions.assertEquals("booking", booking.getService().getPath());
    }

    public void integrationTestCamelCase() throws GestaltException {

        Map<String, String> configs = new HashMap<>();
        configs.put("users.host", "myHost");
        configs.put("users.uri", "different host");
        configs.put("users.db.port", "1234");
        configs.put("users.db.path", "usersTable");


        GestaltBuilder builder = new GestaltBuilder();
        Gestalt gestalt = builder
                .addSource(new MapConfigSource(configs))
                .build();

        gestalt.loadConfigs();

        DBConnection connection = gestalt.getConfig("users", TypeCapture.of(DBConnection.class));
        Assertions.assertEquals("myHost", connection.getUri());
        Assertions.assertEquals(1234, connection.getDbPort());
        Assertions.assertEquals("usersTable", connection.getDbPath());
    }


    public enum Role {
        LEVEL0, LEVEL1
    }

    public interface IHostDefault {
        String getUser();

        String getUrl();

        String getPassword();

        // disable default interface methods for now.
        //default int getPort() {
        //    return 10;
        //}
    }

    public interface IHost {
        String getUser();

        String getUrl();

        String getPassword();
    }

    public interface IHostAnnotations {
        @Config(path = "user")
        String getUser();

        String getUrl();

        String getPassword();

        @Config(defaultVal = "customers")
        String getTable();
    }

    public static class TestReloadListener implements CoreReloadListener {

        int count = 0;

        @Override
        public void reload() {
            count++;
        }
    }

    public static class HttpPool {
        public short maxTotal;
        public long maxPerRoute;
        public int validateAfterInactivity;
        public double keepAliveTimeoutMs = 6000;
        public int idleTimeoutSec = 10;
        public float defaultWait = 33.0F;

        public HttpPool() {

        }
    }

    public static class HostAnnotations implements IHost {
        private String user;
        private String url;

        @Config(path = "password")
        private String secret;

        @Config(defaultVal = "customers")
        private String table;

        public HostAnnotations() {
        }

        @Override
        public String getUser() {
            return user;
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public String getPassword() {
            return secret;
        }

        public String getTable() {
            return table;
        }
    }

    public static class HostMethodAnnotations {
        private String user;
        private String url;
        private String secret;
        private String table;

        public String getUser() {
            return user;
        }

        public String getUrl() {
            return url;
        }

        @Config(path = "password")
        public String getSecret() {
            return secret;
        }

        @Config(defaultVal = "customers")
        public String getTable() {
            return table;
        }
    }

    public static class Host implements IHost {
        private String user;
        private String url;
        private String password;

        public Host() {
        }

        @Override
        public String getUser() {
            return user;
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public String getPassword() {
            return password;
        }
    }

    public static class DataBase {
        public List<Host> hosts;
        public int connectionTimeout;
        public Integer idleTimeout;
        public float maxLifetime;
        public Boolean isEnabled;


        public DataBase() {
        }
    }

    @ConfigPrefix(prefix = "db")
    public static class DataBasePrefix {
        public List<Host> hosts;
        public int connectionTimeout;
        public Integer idleTimeout;
        public float maxLifetime;
        public Boolean isEnabled;

        public DataBasePrefix() {
        }
    }

    public static class User {
        public String[] user;
        public Boolean overrideEnabled = false;
        public Role accessRole;
    }

    public static class SubService {
        private boolean isEnabled;
        private Connection service;

        public boolean isEnabled() {
            return isEnabled;
        }

        public void setEnabled(boolean enabled) {
            isEnabled = enabled;
        }

        public Connection getService() {
            return service;
        }

        public void setService(Connection service) {
            this.service = service;
        }
    }

    public static class Connection {
        private String host;
        private int port;
        private String path;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class DBConnection {
        @Config(path = "host")
        private String uri;
        private int dbPort;
        private String dbPath;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public int getDbPort() {
            return dbPort;
        }

        public void setDbPort(int dbPort) {
            this.dbPort = dbPort;
        }

        public String getDbPath() {
            return dbPath;
        }

        public void setDbPath(String dbPath) {
            this.dbPath = dbPath;
        }
    }

    public static class DBQueryService {
        private @InjectConfig(path = "db") DataBase dataBase;

        public DataBase getDataBase() {
            return dataBase;
        }

        public void setDataBase(DataBase dataBase) {
            this.dataBase = dataBase;
        }
    }
}
