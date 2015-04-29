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
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

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
   * The source directories
   */
  @Parameter(defaultValue = "src/main/resources")
  private File[] sourceDirectories;

  /**
   * The target file
   */
  @Parameter(defaultValue = "src/main/resources/merged")
  private File targetFile;

  /**
   * Either the files to include or some arbitrary include patterns
   */
  @Parameter(required = true)
  private File[] includes;

  /**
   * Either the files to exclude or some arbitrary exclude patterns. Excludes have more precedence
   * over includes.
   */
  @Parameter
  private File[] excludes;

  /**
   * The file encoding charset
   */
  @Parameter(defaultValue = "UTF-8")
  private String encoding;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    // 1. remove target file if it exists
    if (targetFile.exists()) {
      if (!targetFile.delete()) {
        throw new MojoFailureException("unable to delete target file");
      }
    }

    // 2. scan the files
    final Collection<File> files = getFilesToMerge();

    // 3. merge the files
    merge(files);

    // 4. over and out
    getLog().info("successfully merged " + files.size() + " files");
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
      foo = new FileOutputStream(targetFile, true);
      final Writer writer = new BufferedWriter(new OutputStreamWriter(foo));

      for (final File file : files) {
        getLog().info("merging file '" + file.getAbsolutePath() + "' into '" +
                      targetFile.getAbsolutePath() + "'");

        FileInputStream fis = null;
        try {
          // create the stream
          fis = new FileInputStream(file);

          // copy the contents
          IOUtils.copy(fis, writer, encoding);
        } catch (final IOException e) {
          throw new MojoExecutionException("unable to merge file '" + file.getAbsolutePath() + "'",
                                           e);
        } finally {
          // close the stream
          IOUtils.closeQuietly(fis);
        }
      }
    } catch (final IOException e) {
      throw new MojoExecutionException("unable to create the output stream", e);
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

    // get the include and exclude rules formatted as a string
    final String includeStr = StringUtils.join(Arrays.asList(includes).iterator(), ",");
    final String excludesStr = StringUtils.join(Arrays.asList(excludes).iterator(), ",");

    // iterate over the source directories
    for (final File directory : sourceDirectories) {
      if (!validDirectory(directory)) {
        getLog().warn("invalid directory specified: " + directory.toString());
        continue;
      }

      // get the files to be included
      try {
        files.addAll(FileUtils.getFiles(directory, includeStr, excludesStr));
      } catch (final IOException e) {
        throw new MojoExecutionException(
            "unable to get files to be merged at " + directory.getAbsolutePath(), e);
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
           "sourceDirectories=" + Arrays.toString(sourceDirectories) +
           ", targetFile=" + targetFile +
           ", includes=" + Arrays.toString(includes) +
           ", excludes=" + Arrays.toString(excludes) +
           '}';
  }
}
