# badiff

A pure-java byte-level diffing library for working with large inputs, taking advantage of parallel processing during optimization. badiff is pretty fast:

```
 Test output running on my desktop, diffing two 50MB files with random contents
    Creating random input files
    Mapping input to RAM...
    Starting diff
    Computed parallel graph diff for 52428800 bytes in 54981ms
```

## Development & Issue Tracking
Source is available on [stash.robindps.com](http://stash.robindps.com/projects/BDF/repos/badiff/browse) , and can be cloned with:

```sh
  git clone http://stash.robindps.com/scm/bdf/badiff.git
```

[This github repo](https://github.com/org-badiff/badiff)  is a mirror of the definitive repo hosted on [stash.robindps.com](http://stash.robindps.com/projects/BDF/repos/badiff/browse) .  Please submit issues to [jira.robindps.com](http://jira.robindps.com/browse/BDF/?selectedTab=com.atlassian.jira.jira-projects-plugin:summary-panel) .

badiff has no external dependencies, but requires Java 1.6 or higher.  badiff is released under a BSD license.

## Find it on maven:

```xml
  <dependency>
    <groupId>org.badiff</groupId>
    <artifactId>badiff</artifactId>
    <version>1.1.11</version>
  </dependency>
```
## Example usage


Diffing two files, and storing the result as a file

```java
  File orig, target, diff;
  JFileChooser chooser = new JFileChooser();
  
  // prompt the user for the files to diff and file to save to
  if(chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
  	return;
  orig = chooser.getSelectedFile();
  
  if(chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
  	return;
  target = chooser.getSelectedFile();
  
  if(chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
  	return;
  diff = chooser.getSelectedFile();
  
  // do the diff
  FileDiff computed = FileDiffs.diff(orig, target);
  
  // move the generated diff file to the dest file location
  diff.delete();
  computed.renameTo(diff);
```

Diffing two byte arrays and storing the result as a byte array

```java
  ByteArrayDiffs badiff = new ByteArrayDiffs(); // ByteArrayDiffs can optionally specify a serializer
  
  String orig = "Hello world!";
  String target = "Hellish cruel world!";
  
  byte[] diff = badiff.diff(orig.getBytes(), target.getBytes()); // bidirectional diff
  byte[] udiff = badiff.udiff(orig.getBytes(), target.getBytes()); // unidirectional diff
```
## Algorithm

badiff uses the O(ND) algorithm described in this paper: http://www.xmailserver.org/diff2.pdf‎ 

By default, input is run through the diffing graph in chunks of 1KB.  Increasing chunk size can potentially decrease the size of the resulting diff, but the cost grows with the square of the increase; a chunk size of 2KB takes 4 times longer to compute than a chunk size of 1KB.  After chunked graphing is completed the resultant edit list is post-processed to remove some obvious artifacts of chunking, such as pairs of (INSERT,DELETE) operations with identical data that can potentially occur at chunk boundaries.

Chunked graphing is done in parallel by a thread pool sized to the number of available processor cores.

Post-processing is done serially rather than in parallel, so for large input sizes (>50MB) it may be advantageous to skip the post-processing.

## Copyright

badiff is copyrighted &copy;2013 Robin Kirkman
