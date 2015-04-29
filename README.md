file-merger-maven-plugin
=============
file-merger-maven-plugin is a (very) simple maven plugin to (orderly) merge files. 
You define a set of files to be merged and (optionally) a file destination and voilà. 

By default the plugin is bounded to the generate-resources phase.

Usage:
------
#### 1. Merge JavaCC files ("modular" definitions)
```xml
<plugin>
    <groupId>com.davidafsilva</groupId>
    <artifactId>file-merger-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <id>merge-files</id>
            <goals>
                <goal>merge-files</goal>
            </goals>
            <configuration>
                <sourceDirectory>${project.basedir}/src/javacc</sourceDirectory>
                <includes>
                    <include>options.jj</include>
                    <include>parser.jj</include>
                    <include>types.jj</include>
                    <include>math.jj</include>
                </includes>
                <targetFile>${project.build.directory}/generated-sources/javacc/astparser.jj</targetFile>
            </configuration>
        </execution>
    </executions>
</plugin>
```
