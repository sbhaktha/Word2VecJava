package org.allenai.word2vec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.allenai.word2vec.util.Common;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.allenai.word2vec.Searcher.UnknownWordException;
import org.allenai.word2vec.util.Common;
import org.allenai.word2vec.util.ProfilingTimer;

/**
 * Tests converting the binary models into
 * {@link Word2VecModel}s.
 * 
 * @see Word2VecModel#fromBinFile(File)
 * @see Word2VecModel#fromBinFile(File,
 *      java.nio.ByteOrder)
 */
public class Word2VecBinTest {

  /**
   * Tests that the Word2VecModels created from a binary and text
   * representations are equivalent
   */
  @Test
  public void testRead()
      throws IOException, Searcher.UnknownWordException {
    File binFile = Common.getResourceAsFile(
            this.getClass(),
            "/org/allenai/word2vec/tokensModel.bin");
    Word2VecModel binModel = Word2VecModel.fromBinFile(binFile);

    File txtFile = Common.getResourceAsFile(
            this.getClass(),
            "/org/allenai/word2vec/tokensModel.txt");
    Word2VecModel txtModel = Word2VecModel.fromTextFile(txtFile);

    assertEquals(binModel, txtModel);
  }

  private Path tempFile = null;

  /**
   * Tests that a Word2VecModel round-trips through the bin format without changes
   */
  @Test
  public void testRoundTrip() throws IOException, Searcher.UnknownWordException {
    final String filename = "word2vec.c.output.model.txt";
    final Word2VecModel model =
        Word2VecModel.fromTextFile(filename, Common.readResource(Word2VecTest.class, filename));

    tempFile = Files.createTempFile(
            String.format("%s-", Word2VecBinTest.class.getSimpleName()), ".bin");
    try (final OutputStream os = Files.newOutputStream(tempFile)) {
      model.toBinFile(os);
    }

    final Word2VecModel modelCopy = Word2VecModel.fromBinFile(tempFile.toFile());
    assertEquals(model, modelCopy);
  }

	/**
	 * Tests that a Word2VecModel can load properly if the file doesn't fit into one buffer.
	 */
	@Test
	public void testLargeFile() throws IOException, Searcher.UnknownWordException {
    File binFile = Common.getResourceAsFile(
            this.getClass(),
            "/org/allenai/word2vec/tokensModel.bin");
		// The tokens model has 1186 words of 200 doubles each. Make it store 500 vectors per buffer.
    Word2VecModel binModel =
			Word2VecModel.fromBinFile(binFile, ByteOrder.LITTLE_ENDIAN, ProfilingTimer.NONE, 500 * 200);
		Assert.assertEquals("binary file should have been split in 3", binModel.vectors.length, 3);

    File txtFile = Common.getResourceAsFile(
            this.getClass(),
            "/org/allenai/word2vec/tokensModel.txt");
    Word2VecModel txtModel = Word2VecModel.fromTextFile(txtFile);

    assertEquals(binModel, txtModel);
	}

  @After
  public void cleanupTempFile() throws IOException {
    if(tempFile != null)
      Files.delete(tempFile);
  }

  private void assertEquals(
      final Word2VecModel leftModel,
      final Word2VecModel rightModel) throws Searcher.UnknownWordException {
    final Searcher leftSearcher = leftModel.forSearch();
    final Searcher rightSearcher = rightModel.forSearch();

    // test vocab
    for (String vocab : leftModel.getVocab()) {
      assertTrue(rightSearcher.contains(vocab));
    }
    for (String vocab : rightModel.getVocab()) {
      assertTrue(leftSearcher.contains(vocab));
    }
    // test vector
    for (String vocab : leftModel.getVocab()) {
      final List<Double> leftVector = leftSearcher.getRawVector(vocab);
      final List<Double> rightVector = rightSearcher.getRawVector(vocab);
      assertEquals(leftVector, rightVector);
    }
  }

  private void assertEquals(
      final List<Double> leftVector,
      final List<Double> rightVector) {
    Assert.assertEquals(leftVector.size(), rightVector.size());
    for (int i = 0; i < leftVector.size(); i++) {
      double txtD = leftVector.get(i);
      double binD = rightVector.get(i);
      Assert.assertEquals(txtD, binD, 0.0001);
    }
  }
}
