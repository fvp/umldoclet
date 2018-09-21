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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 * @author Sjoerd Talsma
 */
abstract class UmlDiagram {

    /**
     * Creates a postprocessor <strong>if</strong> this diagram corresponds to the HTML file.
     *
     * @param htmlFile
     * @return
     */
    Postprocessor createPostprocessor(HtmlFile htmlFile) {
        return null;
    }

    public abstract Postprocessor.Inserter newInserter(String relativePathToDiagram);

    /**
     * Returns the relative path from one file to another.
     *
     * @param from The source file.
     * @param to   The target file.
     * @return The relative path from the source to the target file.
     */
    protected static String relativePath(File from, File to) {
        if (from == null || to == null) return null;
        try {
            if (from.isFile()) from = from.getParentFile();
            if (!from.isDirectory()) throw new IllegalArgumentException("Not a directory: " + from);

            final String[] fromParts = from.getCanonicalPath().split(Pattern.quote(File.separator));
            List<String> toParts = new ArrayList<>(asList(to.getCanonicalPath().split(Pattern.quote(File.separator))));

            int skip = 0; // Skip the common base path
            while (skip < fromParts.length && skip < toParts.size() && fromParts[skip].equals(toParts.get(skip))) {
                skip++;
            }
            if (skip > 0) toParts = toParts.subList(skip, toParts.size());

            // Replace each remaining directory in 'from' by a preceding "../"
            for (int i = fromParts.length; i > skip; i--) toParts.add(0, "..");

            // Return the resulting path, joined by seprators.
            StringBuilder result = new StringBuilder();
            String sep = "";
            for (String part : toParts) {
                result.append(sep).append(part);
                sep = "/";
            }
            return result.toString();
        } catch (IOException ioe) {
            throw new IllegalStateException("I/O exception calculating relative path from \""
                    + from + "\" to \"" + to + "\": " + ioe.getMessage(), ioe);
        }
    }

}