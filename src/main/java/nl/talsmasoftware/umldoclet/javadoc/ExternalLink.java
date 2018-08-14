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
package nl.talsmasoftware.umldoclet.javadoc;

import nl.talsmasoftware.umldoclet.configuration.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

final class ExternalLink {

    private final Configuration config;
    private final URI docUri, packageListUri;
    private Set<String> packages;

    ExternalLink(Configuration config, String apidoc, String packageList) {
        this.config = config;
        this.docUri = createUri(apidoc);
        this.packageListUri = packageList == null ? addPathComponent(this.docUri, "package-list") : createUri(packageList);
    }

    private Set<String> packages() {
        if (packages == null) try {
            synchronized (this) {
                Set<String> pkglist = new HashSet<>();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(makeAbsolute(packageListUri).toURL().openStream(), "UTF-8"))) {
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        line = line.trim();
                        if (!line.isEmpty()) pkglist.add(line);
                    }
                }
                packages = unmodifiableSet(pkglist);
            }
        } catch (IOException ioe) {
            throw new IllegalStateException("Cannot read package-list from: " + packageListUri, ioe);
        }
        return packages;
    }

    private URI makeAbsolute(URI uri) {
        if (uri != null && !uri.isAbsolute())
            uri = new File(config.destinationDirectory(), uri.toASCIIString()).toURI();
        return uri;
    }

    Optional<URI> resolveType(String packagename, String typeName) {
        if (packages().contains(packagename)) {
            String document = packagename.replace('.', '/') + "/" + typeName + ".html";
            return Optional.of(makeAbsolute(addPathComponent(docUri, document)));
        }
        return Optional.empty();
    }

    private static URI createUri(String uri) {
        try {
            return URI.create(uri);
        } catch (IllegalArgumentException iae) {
            if (new File(uri).exists()) return new File(uri).toURI();
            throw iae;
        }
    }

    private static URI addPathComponent(URI uri, String component) {
        try {
            String scheme = uri.getScheme();
            String userInfo = uri.getUserInfo();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            path = path == null ? component
                    : path.endsWith("/") || component.startsWith("/") ? path + component
                    : path + "/" + component;
            String query = uri.getQuery();
            String fragment = uri.getFragment();
            return new URI(scheme, userInfo, host, port, path, query, fragment);
        } catch (URISyntaxException use) {
            throw new IllegalStateException("Could not add path component \"" + component + "\" to " + uri + ": "
                    + use.getMessage(), use);
        }
    }

    private static URI addQueryParam(URI uri, String name, String value) {
        try {
            String scheme = uri.getScheme();
            String userInfo = uri.getUserInfo();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();
            if (query == null || query.isEmpty()) query = name + "=" + value;
            else query = query + "&" + name + "=" + value;
            String fragment = uri.getFragment();
            return new URI(scheme, userInfo, host, port, path, query, fragment);
        } catch (URISyntaxException use) {
            throw new IllegalStateException("Could not add path query parameter \"" + name + "=" + value + "\" to " + uri + ": "
                    + use.getMessage(), use);
        }
    }

}
