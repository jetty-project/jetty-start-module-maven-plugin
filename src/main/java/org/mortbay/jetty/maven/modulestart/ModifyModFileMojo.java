package org.mortbay.jetty.maven.modulestart;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

/**
 * Modify Jetty module files
 */
@Mojo(name = "modify-mod-file", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ModifyModFileMojo
    extends AbstractMojo
{

    // TODO extra resources directory??
//    @Parameter
//    private List<Resource> resources;

    @Parameter(defaultValue = "${basedir}/src/main/config")
    private File configDirectory;

    @Parameter(defaultValue = "${basedir}/src/main/config/modules")
    private File modulesDirectory;

    /**
     * Directory of the modified files.
     */
    @Parameter(defaultValue = "${project.build.directory}/mod-files/modules", property = "jetty.startmodule.outputdir", required = true)
    private File modFilesOutputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Directory containing the generated -config.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    /**
     * Name of the generated JAR.
     */
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
    private String finalName;

    @Parameter(defaultValue = "false", property = "jetty.startmodule.skip")
    private boolean skip;

    @Parameter
    private List<String> excludeGroupIds = new ArrayList<>();

    /**
     * exclude some default groupIds such: org.eclipse.jetty, javax.servlet, jakarta.servlet
     */
    @Parameter(defaultValue = "true", property = "jetty.startmodule.excludeDefaultGroupIds")
    private boolean excludeDefaultGroupIds = true;

    private static final List<String> DEFAULT_EXCLUDE_GROUPIDS = Arrays.asList("org.eclipse.jetty",
                                                                               "javax.servlet",
                                                                               "jakarta.servlet");

    /**
     * patterns of mod files which need filtering, others found will be simply copied
     */
    @Parameter(defaultValue = ".*", property = "jetty.startmodule.modFilesPattern")
    private String modFilesPattern = ".*";

    /**
     * classifier used for produced maven artifacts
     */
    @Parameter(defaultValue = "config", property = "jetty.startmodule.configFileClassifier")
    private String configFileClassifier = "config";

    /**
     * Path to happen in the maven file format output (such lib/gcloud)
     * maven://org.slf4j/slf4j-api/2.0.0-alpha1|lib/gcloud/slf4j-api-2.0.0-alpha1.jar
     */
    @Parameter(property = "jetty.startmodule.filesLibPath")
    private String filesLibPath;

    /**
     * The Jar archiver.
     */
    @Component(hint = "jar")
    private Archiver archiver;

    @Component
    private MavenProjectHelper projectHelper;

    public void execute()
        throws MojoExecutionException
    {
        if (skip)
        {
            getLog().info("Mod file plugin skipped");
            return;
        }
        // default value src/main/config/modules
        if (!Files.exists(modulesDirectory.toPath()))
        {
            getLog().info("Cannot find directory " + modulesDirectory.toString() + " skip execution");
            return;
        }

        Path f = modFilesOutputDirectory.toPath();

        if (!Files.exists(f))
        {
            try
            {
                Files.createDirectories(f);
            }
            catch (IOException e)
            {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        try
        {
            Files.list(modulesDirectory.toPath())
                .filter(path -> path.toString().endsWith(".mod") && path.getFileName().toString().matches(modFilesPattern))
                .forEach(Errors.rethrow().wrap((Throwing.Consumer<Path>)path -> manageModFile(path, f)));
            // now we build the jar with -config as classifier
            StringBuilder fileName = new StringBuilder(finalName);
            fileName.append("-").append(configFileClassifier);
            fileName.append(".jar");
            File jarFile = new File(outputDirectory, fileName.toString());
            Files.deleteIfExists(jarFile.toPath());
            archiver.setDestFile(jarFile);
            archiver.addFileSet(new DefaultFileSet(configDirectory));
            archiver.addFileSet(new DefaultFileSet(modFilesOutputDirectory.getParentFile()));
            archiver.createArchive();

            projectHelper.attachArtifact(project, "jar", configFileClassifier, jarFile);
        }
        catch (Exception e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    public void copyFolder(Path source, Path target, CopyOption... options)
        throws IOException
    {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>()
        {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException
            {
                Files.copy(file, target.resolve(source.relativize(file)), options);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    protected void manageModFile(Path path, Path output)
        throws IOException
    {
        List<String> lines = Files.readAllLines(path);
        List<String> newContent = new ArrayList<>();
        List<String> filesSectionContent = new ArrayList<>();

        boolean parsingFilesSection = false;
        for (String line : lines)
        {
            if (StringUtils.startsWith(line, "[files]"))
            {
                newContent.add(line);
                parsingFilesSection = true;
            }
            else if (StringUtils.startsWith(line, "[") && parsingFilesSection)
            {
                // end of files
                parsingFilesSection = false;
                boolean emptyLine = false;
                // empty line after files section has been recorded so remove and re add it
                if (newContent.get(newContent.size() - 1).length() == 0)
                {
                    emptyLine = true;
                    newContent.remove(newContent.size() - 1);
                }
                // if files section was empty we add all dependencies
                if (filesSectionContent.isEmpty())
                {
                    List<Artifact> artifacts = project.getArtifacts()
                        .stream()
                        .filter(artifact -> !((excludeDefaultGroupIds && DEFAULT_EXCLUDE_GROUPIDS.contains(artifact.getGroupId())) ||
                                            excludeGroupIds.contains(artifact.getGroupId())))
                        .collect(Collectors.toList());
                    artifacts.forEach(artifact -> newContent.add(artifactOutput(artifact)));
                }
                else
                {
                    // TODO filter line with project.properties and parse maven://
                    newContent.addAll(filesSectionContent);
                }
                if (emptyLine)
                {
                    newContent.add("");
                }
                newContent.add(line);

            }
            else if (parsingFilesSection && StringUtils.isNotEmpty(line))
            {
                filesSectionContent.add(line);
            }
            else
            {
                newContent.add(line);
            }

        }

        Path outputPath = Paths.get(output.toString(), path.getFileName().toString());
        Files.write(outputPath, newContent);
    }

    protected String artifactOutput(Artifact a)
    {
        // maven://org.slf4j/slf4j-api/2.0.0-alpha1|lib/gcloud/slf4j-api-2.0.0-alpha1.jar
        return String.format("maven://%s/%s/%s|%s/%s-%s.%s",
                              a.getGroupId(),
                              a.getArtifactId(),
                              a.getVersion(),
                              StringUtils.isEmpty(filesLibPath) ? "" : filesLibPath,
                              a.getArtifactId(),
                              a.getVersion(),
                              a.getType());
    }
}
