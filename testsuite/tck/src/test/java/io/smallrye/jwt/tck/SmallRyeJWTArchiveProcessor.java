package io.smallrye.jwt.tck;

import java.io.File;
import java.util.Map;

import javax.enterprise.inject.spi.Extension;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

public class SmallRyeJWTArchiveProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            WebArchive war = (WebArchive) applicationArchive;
            war.addClass(OptionalAwareSmallRyeJWTAuthCDIExtension.class);
            war.addClass(SmallRyeJWTAuthJaxRsFeature.class);
            war.addAsServiceProvider(Extension.class, OptionalAwareSmallRyeJWTAuthCDIExtension.class);

            // MP Config in wrong place - See https://github.com/eclipse/microprofile/issues/46.
            Map<ArchivePath, Node> content = war.getContent(object -> object.get().matches(".*META-INF/.*"));
            content.forEach((archivePath, node) -> {
                if (node.getAsset() != null) {
                    war.addAsResource(node.getAsset(), node.getPath());
                }
            });

            if (!war.contains("META-INF/microprofile-config.properties")) {
                war.addAsWebInfResource("microprofile-config-local.properties", "microprofile-config.properties");
            }

            // A few tests require the apps to be deployed in the root. Check PublicKeyAsJWKLocationURLTest and PublicKeyAsPEMLocationURLTest
            // Both tests set the public key location url to be in root.
            war.addAsWebInfResource("jboss-web.xml");
            war.addAsWebInfResource("jetty-web.xml");

            String[] deps = {
                    "org.jboss.resteasy:resteasy-servlet-initializer",
                    "org.jboss.resteasy:resteasy-cdi",
                    "org.jboss.resteasy:resteasy-json-binding-provider",
                    "org.jboss.weld.servlet:weld-servlet-core",
                    "commons-io:commons-io:2.11.0",
                    "io.smallrye:smallrye-jwt",
                    "io.smallrye:smallrye-jwt-jaxrs",
                    "io.smallrye:smallrye-jwt-cdi-extension"
            };
            File[] dependencies = Maven.resolver()
                    .loadPomFromFile(new File("pom.xml"), "jetty", "wildfly")
                    .resolve(deps)
                    .withTransitivity()
                    .asFile();

            war.addAsLibraries(dependencies);
        }
    }
}
