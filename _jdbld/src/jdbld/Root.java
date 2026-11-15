/*
 * JDrupes HTTP Codec
 * Copyright (C) 2016, 2022 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jdbld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.InvalidPatternException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.MergedTestProject;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import static org.jdrupes.builder.api.CoreProperties.Version;
import org.jdrupes.builder.ext.bnd.BndAnalyzer;
import org.jdrupes.builder.ext.bnd.BndBaselineEvaluation;
import org.jdrupes.builder.ext.bnd.BndBaseliner;
import static org.jdrupes.builder.ext.git.GitProperties.*;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.core.VersionReporter;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.LibraryBuilder;
import org.jdrupes.builder.junit.JUnitTestRunner;
import org.jdrupes.builder.mvnrepo.JavadocJarBuilder;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;
import static org.jdrupes.builder.mvnrepo.MvnProperties.GroupId;
import org.jdrupes.builder.mvnrepo.MvnPublisher;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarBuilder;
import org.jdrupes.gitversioning.api.VersionEvaluator;
import org.jdrupes.gitversioning.core.DefaultTagFilter;
import org.jdrupes.gitversioning.core.MavenStyleTagProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Root extends AbstractRootProject {

    @Override
    public void prepareProject(Project project) throws Exception {
        setupVersion();
        setupCommonGenerators();
        if (project instanceof JavaProject) {
            setupJavaProjectGenerators(project);
        }
        if (project instanceof JavaLibraryProject) {
            setupJavaLibraryGenerators(project);
        }
        setupEclipseConfigurator(project);
    }

    public Root() throws IOException {
        super(name("JDrupes-HttpCodec"));
        set(GroupId, "org.jdrupes.httpcodec");

        dependency(Expose, project(HttpCodec.class));

        // Commands
        commandAlias("build").projects("**")
            .resources(of(LibraryJarFileType).using(Supply));
        commandAlias("javadoc").resources(of(JavadocDirectoryType));
        commandAlias("pomFile").resources(of(PomFileType));
        commandAlias("mavenInstallation")
            .description("Install artifacts in local maven repository")
            .projects("**").resources(of(MvnInstallationType).using(Supply));
        commandAlias("mavenPublication").resources(of(MvnPublicationType));
        commandAlias("test").resources(of(TestResultType));
        commandAlias("eclipse")
            .resources(of(new ResourceType<EclipseConfiguration>() {}));
        commandAlias("baseline")
            .resources(of(new ResourceType<BndBaselineEvaluation>() {}));
    }

    private void setupVersion()
            throws IOException, GitAPIException, InvalidPatternException {
        set(GitApi, Git.open(directory().toFile()));

        var evaluator = VersionEvaluator
            .forRepository(get(GitApi).getRepository())
            .tagFilter(new DefaultTagFilter())
            .tagProcessor(new MavenStyleTagProcessor()
                .ignoreBranches("testing/.*", "release/.*", "develop/.*"))
            .subDirectory(directory());
        set(Version, evaluator.version());
    }

    private void setupCommonGenerators() {
        generator(VersionReporter::new);
    }

    private static void setupJavaProjectGenerators(Project project) {
        Path srcDir;
        Path resourceDir;
        if (!(project instanceof MergedTestProject)) {
            srcDir = Path.of("src");
            resourceDir = Path.of("resources");
        } else {
            srcDir = Path.of("test");
            resourceDir = Path.of("test-resources");
            project.dependency(Consume, new MvnRepoLookup()
                .bom("org.junit:junit-bom:5.14.2")
                .resolve("org.junit.jupiter:junit-jupiter-api")
                .resolve("org.junit.jupiter:junit-jupiter-params")
                .resolve("org.junit.jupiter:junit-jupiter-engine",
                    "org.junit.vintage:junit-vintage-engine",
                    "net.jodah:concurrentunit:0.4.2"));
            project.dependency(Supply, JUnitTestRunner::new);
        }
        project.generator(JavaCompiler::new).addSources(srcDir, "**/*.java")
            .options("--release", "17");
        project.generator(JavaResourceCollector::new)
            .add(resourceDir, "**/*");
    }

    private static void setupJavaLibraryGenerators(Project project) {
        // Generate POM
        project.generator(PomFileGenerator::new).adaptPom(model -> {
            model.setDescription(
                "HTTP coders and decoders for non-blocking I/O");
            model.setUrl("https://github.com/mnlipp/jdrupes-httpcodec");
            var scm = new Scm();
            scm.setUrl("scm:git@github.com:mnlipp/jdrupes-httpcodec.git");
            scm.setConnection(
                "scm:git@github.com:mnlipp/jdrupes-httpcodec.git");
            scm.setDeveloperConnection(
                "git@github.com:mnlipp/jdrupes-httpcodec.git");
            model.setScm(scm);
            var license = new License();
            license.setName("LGPL 3.0");
            license.setUrl("https://www.gnu.org/licenses/lgpl-3.0.en.html");
            license.setDistribution("repo");
            model.setLicenses(List.of(license));
            var developer = new Developer();
            developer.setId("mnlipp");
            developer.setName("Michael N. Lipp");
            model.setDevelopers(List.of(developer));
        });

        // Build the library / bundle
        var gen = project.generator(LibraryBuilder::new).addFrom(project)
            .addAttributeValues(Map.of(
                IMPLEMENTATION_TITLE, project.name(),
                IMPLEMENTATION_VERSION, project.get(Version),
                IMPLEMENTATION_VENDOR, "Michael N. Lipp (mnl@mnl.de)")
                .entrySet().stream())
            .addManifestAttributes(project.resources(
                project.of(ManifestAttributesType).using(Consume)))
            .addEntries(project.resources(project.of(PomFileType).using(Supply))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven").resolve(
                    (String) project.get(GroupId), project.name(), "pom.xml"),
                    pomFile)));
        var git = project.get(GitApi);
        try {
            gen.attributes(
                Map.entry(new Attributes.Name("Git-Descriptor"),
                    git.describe().setAll(true).call()
                        + (git.status().call().getUncommittedChanges()
                            .isEmpty() ? "" : "-dirty")),
                Map.entry(new Attributes.Name("Git-SHA"),
                    git.getRepository().resolve("HEAD").getName()));
        } catch (GitAPIException | RevisionSyntaxException
                | IOException e) {
            throw new BuildException().from(project).cause(e);
        }

        // Provide OSGi data
        if (project.directory().resolve("bnd.bnd").toFile().exists()) {
            String bundleVersion = project.get(Version);
            if (bundleVersion.endsWith("-SNAPSHOT")) {
                bundleVersion = bundleVersion.replaceFirst("-", ".-")
                    .replaceAll("-SNAPSHOT$", "-\\${tstamp}-SNAPSHOT");
            } else {
                bundleVersion += ".ga";
            }
            project.dependency(Consume, BndAnalyzer::new)
                .instruction("Bundle-SymbolicName", project.name())
                .instructions(project.directory().resolve("bnd.bnd"))
                .instruction("Bundle-Version", bundleVersion)
                .instructions(Map.of("-diffignore", "Git-Descriptor, Git-SHA",
                    "Bundle-Version", bundleVersion));
            project.dependency(Supply, BndBaseliner::new)
                .instruction("-diffignore", "Git-Descriptor, Git-SHA");
        }

        // Supply sources jar
        project.generator(SourcesJarBuilder::new).addTrees(
            project.resources(
                project.of(JavaSourceTreeType).using(Supply, Expose)));

        // Build javadoc
        project.generator(JDrupesJavadoc::new);

        // Supply javadoc jar
        project.generator(JavadocJarBuilder::new);

        // Publish (deploy). Credentials and signing information is
        // obtained through properties.
        project.generator(MvnPublisher::new);
    }

    private static void setupEclipseConfigurator(Project project) {
        project.generator(new EclipseConfigurator(project)
            .adaptProjectConfiguration((Document doc,
                    Node buildSpec, Node natures) -> {
                var cmd = buildSpec
                    .appendChild(doc.createElement("buildCommand"));
                cmd.appendChild(doc.createElement("name"))
                    .appendChild(doc.createTextNode(
                        "net.sf.eclipsecs.core.CheckstyleBuilder"));
                cmd.appendChild(doc.createElement("arguments"));
                natures.appendChild(doc.createElement("nature"))
                    .appendChild(doc.createTextNode(
                        "net.sf.eclipsecs.core.CheckstyleNature"));
            }).adaptJdtCorePrefs(props -> {
                var formatterPrefs = new java.util.Properties();
                try {
                    formatterPrefs.load(HttpCodec.class.getResourceAsStream(
                        "org.eclipse.jdt.core.formatter.prefs"));
                    formatterPrefs.entrySet().stream()
                        .forEach(e -> props.put(e.getKey(), e.getValue()));
                } catch (IOException e) {
                    throw new BuildException().from(project).cause(e);
                }
            }).adaptConfiguration(() -> {
                try {
                    Files.copy(HttpCodec.class
                        .getResourceAsStream("net.sf.jautodoc.prefs"),
                        project.directory()
                            .resolve(".settings/net.sf.jautodoc.prefs"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(
                        HttpCodec.class.getResourceAsStream("checkstyle"),
                        project.directory().resolve(".checkstyle"),
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BuildException().from(project).cause(e);
                }
            }));
    }
}
