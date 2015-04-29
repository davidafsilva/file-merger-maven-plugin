package pt.davidafsilva.maven.filemerger;

/*
 * #%L
 * file-merger-maven-plugin
 * %%
 * Copyright (C) 2015 David Silva
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * The file merger mojo.
 *
 * @author David Silva
 */
@Mojo(
    name = "merge-files",
    defaultPhase = LifecyclePhase.GENERATE_RESOURCES
)
public class FileMergerMojo extends AbstractMojo {

  /**
   * The base directory
   */
  @Parameter(readonly = true, defaultValue = "${project.basedir}")
  private File baseDirectory;

  /**
   * The source directory
   */
  @Parameter(defaultValue = "${project.basedir}/src/main/resources")
  private File sourceDirectory;

  /**
   * The target file
   */
  @Parameter(defaultValue = "${project.basedir}/src/main/resources/merged")
  private File targetFile;

  /**
   * The files to be include (patterns are not supported)
   */
  @Parameter(alias = "files", required = true)
  private String[] includes;

  /**
   * The file encoding charset
   */
  @Parameter(defaultValue = "UTF-8")
  private String encoding;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    // 0. set defaults...
    if (sourceDirectory == null) {
      sourceDirectory = new File(baseDirectory.getAbsolutePath()
                                 + File.pathSeparator + "src"
                                 + File.pathSeparator + "main"
                                 + File.pathSeparator + "resources");
    }
    if (targetFile == null) {
      targetFile = new File(baseDirectory.getAbsolutePath()
                            + File.pathSeparator + "src"
                            + File.pathSeparator + "main"
                            + File.pathSeparator + "resources"
                            + File.pathSeparator + "merged");
    }
    if (encoding == null) {
      encoding = "UTF-8";
    }

    // 1. remove target file if it exists
    if (targetFile.exists()) {
      if (targetFile.isDirectory() || !targetFile.delete()) {
        throw new MojoFailureException("Unable to delete target file");
      }
    }

    // 2. scan the files
    final Collection<File> files = getFilesToMerge();

    // 3. merge the files
    if (files.isEmpty()) {
      getLog().warn("No files to be merged were found.");
    } else {
      merge(files);

      // 4. over and out
      getLog().info("Successfully merged " + files.size() + " files");
    }
  }

  /**
   * Merge the given files to the target file destination
   *
   * @param files the files to be merged
   * @throws MojoExecutionException if any error occurs while merging the files
   */
  private void merge(final Collection<File> files) throws MojoExecutionException {
    FileOutputStream foo = null;
    try {
      if (!targetFile.getParentFile().mkdirs() || !targetFile.createNewFile()) {
        throw new MojoExecutionException("Unable to create output file");
      }

      foo = new FileOutputStream(targetFile, true);
      final Writer writer = new BufferedWriter(new OutputStreamWriter(foo));

      for (final File file : files) {
        getLog().info("Merging file '" + file.getAbsolutePath() + "' into '" +
                      targetFile.getAbsolutePath() + "'");

        FileInputStream fis = null;
        try {
          // create the stream
          fis = new FileInputStream(file);

          // copy the contents
          IOUtils.copy(fis, writer, encoding);
        } catch (final IOException e) {
          throw new MojoExecutionException("Unable to merge file '" + file.getAbsolutePath() + "'",
                                           e);
        } finally {
          // close the stream
          IOUtils.closeQuietly(fis);
        }
      }

      writer.close();
    } catch (final IOException e) {
      throw new MojoExecutionException("Unable to create the output stream", e);
    } finally {
      // close the stream
      IOUtils.closeQuietly(foo);
    }
  }

  /**
   * Returns the files to be merged.
   *
   * @return the list of files to be merge
   * @throws MojoExecutionException if any error occurs while getting the files to be included
   */
  @SuppressWarnings("unchecked")
  private Collection<File> getFilesToMerge() throws MojoExecutionException {
    final Collection<File> files = new LinkedHashSet<File>();

    // iterate over the source directories
    if (!validDirectory(sourceDirectory)) {
      getLog().warn("Invalid source directory specified: " + sourceDirectory.toString());
      return files;
    }

    // get the files to be included
    final String fs = System.getProperty("file.separator");
    for (final String f : includes) {
      final File fp = new File(sourceDirectory.getAbsolutePath() + fs + f);
      if (!fp.exists() || !fp.canRead() || !fp.isFile()) {
        getLog().warn("Unable to read file: " + fp.getAbsolutePath());
      } else {
        files.add(fp);
      }
    }

    return files;
  }

  /**
   * Validates the specified directory
   *
   * @param directory the directory to be validated
   * @return {@code true} if the directory is valid, {@code false} otherwise.
   */
  private boolean validDirectory(final File directory) {
    return directory.exists() && directory.canRead() && directory.isDirectory();
  }

  @Override
  public String toString() {
    return "FileMergerMojo{" +
           "baseDirectory=" + baseDirectory +
           ", sourceDirectory=" + sourceDirectory +
           ", targetFile=" + targetFile +
           ", includes=" + Arrays.toString(includes) +
           ", encoding='" + encoding + '\'' +
           '}';
  }
}
