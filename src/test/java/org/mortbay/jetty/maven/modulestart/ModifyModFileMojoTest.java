package org.mortbay.jetty.maven.modulestart;

import org.junit.Rule;
import org.junit.Test;
import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModifyModFileMojoTest
{
    @Rule
    public MojoRule rule = new MojoRule()
    {
        @Override
        protected void before() throws Throwable 
        {
        }

        @Override
        protected void after()
        {
        }
    };

    @Test
    public void testSomething()
            throws Exception
    {
        File pom = new File("target/test-classes/project-to-test/");
        assertNotNull(pom);
        assertTrue(pom.exists());

        ModifyModFileMojo modifyModFileMojo = (ModifyModFileMojo)rule.lookupConfiguredMojo(pom, "modify-mod-file");
        assertNotNull(modifyModFileMojo);
        modifyModFileMojo.execute();

        File outputDirectory = (File) rule.getVariableValueFromObject(modifyModFileMojo, "outputDirectory");
        assertNotNull(outputDirectory);
        assertTrue(outputDirectory.exists());

    }

    @WithoutMojo
    @Test
    public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn()
    {
        assertTrue(true);
    }

}

