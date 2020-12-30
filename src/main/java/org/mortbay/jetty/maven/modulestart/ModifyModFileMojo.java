package org.mortbay.jetty.maven.modulestart;


import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modify Jetty module files
 */
@Mojo( name = "modify-mod-file", defaultPhase = LifecyclePhase.PROCESS_RESOURCES )
public class ModifyModFileMojo
    extends AbstractMojo
{

    // TODO extra resources directory??
//    @Parameter
//    private List<Resource> resources;

    @Parameter( defaultValue = "${basedir}/src/main/config/modules")
    private File modulesDirectory;

    /**
     * Directory of the modified files.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "jetty.startmodule.outputdir", required = true )
    private File outputDirectory;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    @Parameter( defaultValue = "false", property = "jetty.startmodule.skip" )
    private boolean skip;

    public void execute()
        throws MojoExecutionException
    {
        if(skip){
            getLog().info("Mod file plugin skipped");
            return;
        }
        // default value src/main/config/modules
        if(!Files.exists(modulesDirectory.toPath())){
            getLog().info("Cannot find directory " + modulesDirectory.toString() + " skip execution");
            return;
        }

        Path f = outputDirectory.toPath();

        if (!Files.exists(f))
        {
            try
            {
                Files.createDirectories(f);
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        try
        {
            Files.list(modulesDirectory.toPath())
                .filter( path -> path.toString().endsWith(".mod"))
                .forEach(this::manageModFile);

        }
        catch ( Exception e )
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    protected void manageModFile(Path path){

    }
}
