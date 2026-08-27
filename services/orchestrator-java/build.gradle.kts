plugins { java }

group = "com.workcontrol"
version = "0.1.0-SNAPSHOT"

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
repositories { mavenCentral() }
dependencies { testImplementation(platform("org.junit:junit-bom:5.11.4")); testImplementation("org.junit.jupiter:junit-jupiter") }
tasks.test { useJUnitPlatform() }
