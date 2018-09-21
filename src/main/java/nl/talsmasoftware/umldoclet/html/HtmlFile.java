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
package nl.talsmasoftware.umldoclet.html;

import net.sourceforge.plantuml.FileUtils;
import nl.talsmasoftware.umldoclet.config.UMLDocletConfig;
import nl.talsmasoftware.umldoclet.logging.LogSupport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Abstraction for a single HTML file generated by the Standard doclet.
 *
 * @author Sjoerd Talsma
 */
final class HtmlFile {

    final UMLDocletConfig config;
    final Path path;

    HtmlFile(UMLDocletConfig config, Path path) {
        this.config = requireNonNull(config, "Configuration is <null>.");
        this.path = requireNonNull(path, "HTML file is <null>.").normalize();
    }

    static boolean isHtmlFile(Path path) {
        File file = path == null ? null : path.toFile();
        return file != null && file.isFile() && file.canRead() && file.getName().endsWith(".html");
    }

    boolean process(Collection<UmlDiagram> diagrams) {
        for (UmlDiagram diagram : diagrams) {
            Postprocessor postprocessor = diagram.createPostprocessor(this);
            if (postprocessor != null) return process(postprocessor);
        }
        return skip();
    }

    private boolean skip() {
        LogSupport.debug("Skipping {0}...", path);
        return false;
    }

    private boolean process(Postprocessor postprocessor) {
        try {
            LogSupport.info("Add UML to {0}...", path);
            return postprocessor.call();
        } catch (IOException ioe) {
            throw new IllegalStateException("I/O exception postprocessing " + path, ioe);
        }
    }

    public List<String> readLines() throws IOException {
        return Files.readAllLines(path, config.htmlFileEncoding());
    }

    public void replaceBy(File tempFile) throws IOException {
        File original = path.toFile();
        if (!original.delete()) throw new IllegalStateException("Cannot delete " + original);
        if (tempFile.renameTo(original)) {
            LogSupport.debug("Renamed {0} from {1}.", original, tempFile);
        } else {
            FileUtils.copyToFile(tempFile, original);
            LogSupport.debug("Copied {0} from {1}.", original, tempFile);
            if (!tempFile.delete()) {
                throw new IllegalStateException("Cannot delete " + tempFile + " after postprocessing!");
            }
        }
    }
}