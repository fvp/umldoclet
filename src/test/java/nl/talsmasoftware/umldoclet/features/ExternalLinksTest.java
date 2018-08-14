/*
 * Copyright 2016-2018 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.umldoclet.features;

import nl.talsmasoftware.umldoclet.UMLDoclet;
import nl.talsmasoftware.umldoclet.util.Testing;
import org.junit.Test;

import java.io.File;
import java.io.Serializable;
import java.util.spi.ToolProvider;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;

/**
 * Tests the 'external links' feature
 * <p>
 * This feature is tracked by <a href="https://github.com/talsma-ict/umldoclet/issues/96">issue 96 on github</a>
 *
 * @author Sjoerd Talsma
 */
public class ExternalLinksTest {

    static final File testoutput = new File("target/test-96");
    static final String packageAsPath = ExternalLinksTest.class.getPackageName().replace('.', '/');

    public static class TestClass implements Serializable {
    }

    @Test
    public void testRelativeExternalLink() {
        Testing.write(new File(testoutput, "externalApidocs/package-list"), Serializable.class.getPackageName());
        File outputdir = new File(testoutput, "link-relative");

        File packageUml = new File(outputdir, packageAsPath + "/package.puml");
        ToolProvider.findFirst("javadoc").get().run(
                System.out, System.err,
                "-d", outputdir.getPath(),
                "-doclet", UMLDoclet.class.getName(),
                "-quiet",
                "-createPumlFiles",
                "-link", "../externalApidocs",
                "src/test/java/" + packageAsPath + '/' + getClass().getSimpleName() + ".java"
        );

        String uml = Testing.read(packageUml);
        // Check link to Serializable javadoc
        assertThat(uml, stringContainsInOrder(asList("interface", "Serializable", "externalApidocs/java/io/Serializable.html]]")));
    }

    @Test
    public void testOnlineExternalLink() {
        File outputdir = new File(testoutput, "link-online");

        File packageUml = new File(outputdir, packageAsPath + "/package.puml");
        ToolProvider.findFirst("javadoc").get().run(
                System.out, System.err,
                "-d", outputdir.getPath(),
                "-doclet", UMLDoclet.class.getName(),
                "-quiet",
                "-createPumlFiles",
                "-link", "https://docs.oracle.com/javase/9/docs/api",
                "src/test/java/" + packageAsPath + '/' + getClass().getSimpleName() + ".java"
        );

        String uml = Testing.read(packageUml);
        // Check link to Serializable javadoc
        assertThat(uml, stringContainsInOrder(asList("interface", "Serializable",
                "[[https://docs.oracle.com/javase/9/docs/api/java/io/Serializable.html]]")));
    }

}
