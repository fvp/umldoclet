/*
 * Copyright (C) 2016 Talsma ICT
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
package nl.talsmasoftware.umldoclet.rendering;

import com.sun.javadoc.*;
import nl.talsmasoftware.umldoclet.rendering.indent.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Renderer to produce PlantUML output for a single class.
 *
 * @author <a href="mailto:info@talsma-software.nl">Sjoerd Talsma</a>
 */
public class ClassRenderer extends Renderer {

    protected final Renderer parent;
    protected final ClassDoc classDoc;
    private final Collection<NoteRenderer> notes = new ArrayList<>();

    protected ClassRenderer(Renderer parent, ClassDoc classDoc) {
        super(requireNonNull(parent, "No parent renderer for class provided.").diagram);
        this.parent = parent;
        this.classDoc = requireNonNull(classDoc, "No class documentation provided.");
        // Enum constants are added first.
        for (FieldDoc enumConstant : classDoc.enumConstants()) {
            children.add(new FieldRenderer(diagram, enumConstant));
        }
        // TODO: Couldn't we make Renderer Comparable and have 'children' become a TreeSet?
        // --> Probably, after more tests are in place!
        List<FieldRenderer> fields = new ArrayList<>(); // static fields come before non-static fields.
        for (FieldDoc field : classDoc.fields(false)) {
            if (field.isStatic()) {
                children.add(new FieldRenderer(diagram, field));
            } else {
                fields.add(new FieldRenderer(diagram, field));
            }
        }
        children.addAll(fields);
        for (ConstructorDoc constructor : classDoc.constructors(false)) {
            children.add(new MethodRenderer(diagram, constructor));
        }
        List<MethodRenderer> abstractMethods = new ArrayList<>();
        for (MethodDoc method : classDoc.methods(false)) {
            if (method.isAbstract()) {
                abstractMethods.add(new MethodRenderer(diagram, method));
            } else {
                children.add(new MethodRenderer(diagram, method));
            }
        }
        children.addAll(abstractMethods); // abstract methods come after regular methods in our UML diagrams.

        // Support for tags defined in legacy doclet.
        // TODO: Depending on the amount of code this generates this should be refactored away (after unit testing).
        addLegacyNoteTag();
    }

    private void addLegacyNoteTag() {
        // for (String tagname : new String[] {"note"}) {
        final String tagname = "note";
        for (Tag notetag : classDoc.tags(tagname)) {
            String note = notetag.text();
            if (note != null) {
                notes.add(new NoteRenderer(diagram, note, name()));
            }
        }
    }

    /**
     * Determines the 'UML' type for the class to be rendered.
     * Currently, this can return one of the following: {@code "enum"}, {@code "interface"}, {@code "abstract class"}
     * or (obviously) {@code "class"}.
     *
     * @return The UML type for the class to be rendered.
     */
    protected String umlType() {
        return classDoc.isEnum() ? "enum"
                : classDoc.isInterface() ? "interface"
                : classDoc.isAbstract() ? "abstract class"
                : "class";
    }

    /**
     * This method writes the 'generic' information to the writer, if available in the class documentation.
     * If data is written, starts with {@code '<'} and ends with {@code '>'}.
     *
     * @param out The writer to write to.
     * @return The writer so more content can easily be written.
     */
    protected IndentingPrintWriter writeGenericsTo(IndentingPrintWriter out) {
        if (classDoc.typeParameters().length > 0) {
            out.append('<');
            String sep = "";
            for (TypeVariable generic : classDoc.typeParameters()) {
                out.append(sep).append(generic.typeName());
                sep = ", ";
            }
            out.append('>');
        }
        return out;
    }

    /**
     * This method writes the notes for this class to the output.
     *
     * @param out The writer to write the notes to.
     * @return The writer so more content can easily be written.
     */
    protected IndentingPrintWriter writeNotesTo(IndentingPrintWriter out) {
        for (NoteRenderer note : notes) {
            note.writeTo(out);
        }
        return out;
    }

    /**
     * Determines the name of the class to be rendered.
     * This method considers whether to use the fully qualified class name (including package denomination etc) or
     * a shorter simple name.
     *
     * @return The name of the class to be rendered.
     */
    protected String name() {
        String name = classDoc.qualifiedName();
        if (parent instanceof UMLDiagram) {
            name = classDoc.name();
        } else if (parent instanceof PackageRenderer && !diagram.config.alwaysUseQualifiedClassnames()) {
            String packagePrefix = classDoc.containingPackage().name() + ".";
            if (name.startsWith(packagePrefix)) {
                name = name.substring(packagePrefix.length());
            }
        }
        return name;
    }

    /**
     * This method writes the name of the class to the output and marks (the fully qualified name of) the class as
     * an 'encountered type'.
     *
     * @param out The writer to write the class name to.
     * @return The writer so more content can easily be written.
     */
    protected IndentingPrintWriter writeNameTo(IndentingPrintWriter out) {
        diagram.encounteredTypes.add(classDoc.qualifiedName());
        return out.append(this.name());
    }

    /**
     * This method renders the class information to the specified output.
     *
     * @param out The writer to write the class name to.
     * @return The writer so more content can easily be written.
     */
    protected IndentingPrintWriter writeTo(IndentingPrintWriter out) {
        writeNameTo(out.append(umlType()).whitespace());
        writeGenericsTo(out);
        if (isDeprecated(classDoc)) {
            out.whitespace().append("<<deprecated>>"); // I don't know how to strikethrough a class name!
        }
        writeChildrenTo(out.whitespace().append('{').newline()).append('}').newline().newline();
        return writeNotesTo(out);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classDoc.qualifiedName());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other != null && ClassRenderer.class.equals(other.getClass())
                && Objects.equals(classDoc.qualifiedName(), ((ClassRenderer) other).classDoc.qualifiedName()));
    }

}
