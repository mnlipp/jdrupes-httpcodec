/*
 * JDrupes HTTP Codec
 * Copyright (C) 2016, 2026 Michael N. Lipp
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

import static org.jdrupes.builder.api.Intent.*;
import java.io.IOException;
import java.nio.file.Path;
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import static org.jdrupes.builder.mvnrepo.MvnProperties.ArtifactId;

public class HttpCodec extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public HttpCodec() throws IOException {
        super(name("org.jdrupes.httpcodec"), directory(Path.of(".")));
        set(ArtifactId, "httpcodec");

        dependency(Expose, new MvnRepoLookup().resolve(
            "jakarta.activation:jakarta.activation-api:[2.1.0,3.0.0)"));
    }

    public static class HttpCodecTest extends AbstractProject
            implements JavaProject, MergedTestProject {

        public HttpCodecTest() {
            super(parent(HttpCodec.class));
            dependency(Consume, project(HttpCodec.class));

            dependency(Consume, new MvnRepoLookup().resolve(
                "org.eclipse.angus:angus-activation:[2.0.0,3.0.0)"));
        }
    }
}
