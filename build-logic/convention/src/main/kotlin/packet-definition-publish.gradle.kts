/*
 * Copyright 2026 Pavel Shoplik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.github.pavelstalone.convention.ArtifactExtension
import io.github.pavelstalone.convention.groupId
import io.github.pavelstalone.convention.versionName

plugins {
    id("com.vanniktech.maven.publish")
}

group = project.groupId
version = project.versionName

val artifact = extensions.create<ArtifactExtension>("artifact")

afterEvaluate {
    mavenPublishing {
        coordinates(
            groupId = project.groupId,
            artifactId = artifact.id.orNull,
            version = project.versionName,
        )

        pom {
            name.set(artifact.name.orNull)
            description.set(artifact.description.orNull)
            inceptionYear.set("2026")
            url.set("https://github.com/PavelStalone/packet-definition")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("PavelStalone")
                    name.set("Pavel Shoplik")
                    email.set("pavel.shoplik@yandex.ru")
                    url.set("https://github.com/PavelStalone")
                }
            }

            scm {
                url.set("https://github.com/PavelStalone/packet-definition/")
                connection.set("scm:git:git://github.com/PavelStalone/packet-definition.git")
                developerConnection.set("scm:git:ssh://github.com/PavelStalone/packet-definition.git")
            }

            issueManagement {
                system.set("GitHub")
                url.set("https://github.com/PavelStalone/packet-definition/issues")
            }
        }

        publishToMavenCentral()
        signAllPublications()
    }
}
