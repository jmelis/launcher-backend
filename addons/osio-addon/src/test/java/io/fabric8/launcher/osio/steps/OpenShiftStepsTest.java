package io.fabric8.launcher.osio.steps;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import io.fabric8.launcher.base.identity.IdentityFactory;
import io.fabric8.launcher.base.identity.TokenIdentity;
import io.fabric8.launcher.booster.catalog.rhoar.Mission;
import io.fabric8.launcher.booster.catalog.rhoar.Runtime;
import io.fabric8.launcher.osio.OsioConfigs;
import io.fabric8.launcher.osio.client.ImmutableNamespace;
import io.fabric8.launcher.osio.client.ImmutableSpace;
import io.fabric8.launcher.osio.client.ImmutableTenant;
import io.fabric8.launcher.osio.client.Tenant;
import io.fabric8.launcher.osio.projectiles.ImmutableOsioLaunchProjectile;
import io.fabric8.launcher.osio.projectiles.OsioProjectile;
import io.fabric8.launcher.service.git.api.GitRepository;
import io.fabric8.launcher.service.git.api.ImmutableGitRepository;
import io.fabric8.launcher.service.git.github.KohsukeGitHubServiceFactory;
import io.fabric8.launcher.service.openshift.impl.fabric8.openshift.client.Fabric8OpenShiftServiceFactory;
import io.fabric8.launcher.service.openshift.impl.fabric8.openshift.client.Fabric8OpenShiftServiceImpl;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static io.fabric8.launcher.base.test.hoverfly.LauncherHoverflyEnvironment.createDefaultHoverflyEnvironment;
import static io.fabric8.launcher.base.test.hoverfly.LauncherHoverflyRuleConfigurer.createHoverflyProxy;
import static io.fabric8.launcher.service.openshift.api.OpenShiftEnvVarSysPropNames.OPENSHIFT_API_URL;

public class OpenShiftStepsTest {

    private static final HoverflyRule HOVERFLY_RULE = createHoverflyProxy("openshiftsteps-simulation.json",
                                                                          "github.com|githubusercontent.com|api.openshift.io|api.prod-preview.openshift.io|openshiftapps.com");

    @ClassRule
    public static RuleChain ruleChain = RuleChain
            // After recording on a real environment against a real service,
            // You should adapt the Hoverfly descriptors (.json) to make them work in simulation mode with the mock environment.
            .outerRule(createDefaultHoverflyEnvironment(HOVERFLY_RULE)
                               .andForSimulationOnly(OPENSHIFT_API_URL, "https://f8osoproxy-test-dsaas-preview.b6ff.rh-idev.openshiftapps.com"))
            .around(HOVERFLY_RULE);

    @Test
    public void shouldCreateBuildConfig() throws IOException, URISyntaxException {
        //given
        OpenShiftSteps steps = new OpenShiftSteps();
        steps.gitService = new KohsukeGitHubServiceFactory().create(IdentityFactory.createFromUserPassword("edewit", "123"));

        final String expectedName = "my-space";
        File tempDir = Files.createTempDirectory("mc").toFile();

        final OsioProjectile projectile = ImmutableOsioLaunchProjectile.builder()
                .mission(new Mission("crud"))
                .runtime(new Runtime("vert.x"))
                .gitRepositoryName("foo")
                .gitOrganization("edewit")
                .openShiftProjectName(expectedName)
                .projectLocation(tempDir.toPath())
                .space(ImmutableSpace.builder().id("some-crazy-id").name("my-space").build())
                .pipelineId("id")
                .build();

        URI uri = new URI("https://github.com/fabric8-launcher/launcher-backend.git");
        GitRepository repository = ImmutableGitRepository.builder().gitCloneUri(uri)
                .fullName("edewit/foo")
                .homepage(new URI("https://dontcare.ch"))
                .build();


        ImmutableNamespace namespace = ImmutableNamespace.builder().name("edewit-osio-jenkins")
                .type("user")
                .clusterUrl("clusterUrl")
                .clusterConsoleUrl("http://conso")
                .build();
        TokenIdentity identity = IdentityFactory.createFromToken("123");
        List<ImmutableNamespace> elements = Collections.singletonList(namespace);
        Tenant tenant = ImmutableTenant.builder().identity(identity)
                .username("edewit").email("me@nerdin.ch").namespaces(elements).build();

        Fabric8OpenShiftServiceImpl openShiftService = new Fabric8OpenShiftServiceFactory(null)
                .create(OsioConfigs.getOpenShiftCluster(), IdentityFactory.createFromToken("123"));
        steps.openShiftService = openShiftService;

        steps.tenant = tenant;

        //when
        steps.createBuildConfig(projectile, repository);
    }

}