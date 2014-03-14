/*
 * Copyright (c) 2008 Los Alamos National Security, LLC.
 * 
 * Los Alamos National Laboratory Research Library Digital Library Research &
 * Prototyping Team
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package gov.lanl.adore.djatoka.kdu;

import gov.lanl.adore.djatoka.DjatokaEncodeParam;
import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.ICompress;
import gov.lanl.adore.djatoka.util.IOUtils;
import gov.lanl.adore.djatoka.util.ImageProcessingUtils;
import gov.lanl.adore.djatoka.util.ImageRecord;
import gov.lanl.adore.djatoka.util.ImageRecordUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.martiansoftware.jsap.CommandLineTokenizer;

/**
 * Java bridge for kdu_compress application
 * 
 * @author Ryan Chute
 * @author <a href="mailto:ksclarke@gmail.com">Kevin S. Clarke</a>
 */
public class KduCompressExe implements ICompress {

    private static Logger LOGGER = LoggerFactory.getLogger(KduCompressExe.class);

    private static boolean isWindows = false;

    private static String env;

    private static String exe;

    private static String[] envParams;

    /** The name of the compressing executable */
    public static final String KDU_COMPRESS_EXE = "kdu_compress";

    public static final String STDOUT = "/dev/stdout";

    static {
        env = System.getProperty("kakadu.home") + System.getProperty("file.separator");
        exe =
                env +
                        ((System.getProperty("os.name").startsWith("Win")) ? KDU_COMPRESS_EXE + ".exe"
                                : KDU_COMPRESS_EXE);

        if (System.getProperty("os.name").startsWith("Mac")) {
            envParams = new String[] { "DYLD_LIBRARY_PATH=" + System.getProperty("DYLD_LIBRARY_PATH") };
        } else if (System.getProperty("os.name").startsWith("Win")) {
            isWindows = true;
        } else if (System.getProperty("os.name").startsWith("Linux")) {
            envParams = new String[] { "LD_LIBRARY_PATH=" + System.getProperty("LD_LIBRARY_PATH") };
        } else if (System.getProperty("os.name").startsWith("Solaris")) {
            envParams = new String[] { "LD_LIBRARY_PATH=" + System.getProperty("LD_LIBRARY_PATH") };
        }

        LOGGER.debug("envParams: " + ((envParams != null) ? envParams[0] + " | " : "") + exe);
    }

    /**
     * Constructor which expects the following system properties to be defined and exported.
     * <p/>
     * (Win/Linux/UNIX) LD_LIBRARY_PATH=$DJATOKA_HOME/lib/$DJATOKA_OS
     * <p/>
     * (Mac OS-X) DYLD_LIBRARY_PATH=$DJATOKA_HOME/lib/$DJATOKA_OS
     * 
     * @throws Exception
     */
    public KduCompressExe() {
        env = System.getProperty("kakadu.home");

        if (env == null) {
            LOGGER.error("kakadu.home is not defined");
            throw new RuntimeException("kakadu.home is not defined");
        }
    }

    /**
     * Compress input BufferedImage using provided DjatokaEncodeParam parameters.
     * 
     * @param bi in-memory image to be compressed
     * @param output absolute file path for output file.
     * @param params DjatokaEncodeParam containing compression parameters.
     * @throws DjatokaException
     */
    public void compressImage(BufferedImage bi, String output, DjatokaEncodeParam params) throws DjatokaException {
        if (params == null) {
            params = new DjatokaEncodeParam();
        }
        if (params.getLevels() == 0) {
            params.setLevels(ImageProcessingUtils.getLevelCount(bi.getWidth(), bi.getHeight()));
        }
        File in = null;
        try {
            in = IOUtils.createTempTiff(bi);
            compressImage(in.getAbsolutePath(), output, params);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new DjatokaException(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new DjatokaException(e.getMessage(), e);
        } finally {
            if (in != null) {
                if (!in.delete() && LOGGER.isWarnEnabled()) {
                    LOGGER.warn("File not deleted: {}", in);
                }
            }
        }
    }

    /**
     * Compress input BufferedImage using provided DjatokaEncodeParam parameters.
     * 
     * @param bi in-memory image to be compressed
     * @param output OutputStream to serialize compressed image.
     * @param params DjatokaEncodeParam containing compression parameters.
     * @throws DjatokaException
     */
    public void compressImage(BufferedImage bi, OutputStream output, DjatokaEncodeParam params)
            throws DjatokaException {
        if (params == null) {
            params = new DjatokaEncodeParam();
        }
        if (params.getLevels() == 0) {
            params.setLevels(ImageProcessingUtils.getLevelCount(bi.getWidth(), bi.getHeight()));
        }
        File in = null;
        File out = null;
        try {
            in = IOUtils.createTempTiff(bi);
            out = File.createTempFile("tmp", ".jp2");
            compressImage(in.getAbsolutePath(), out.getAbsolutePath(), params);
            IOUtils.copyStream(new FileInputStream(out), output);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new DjatokaException(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new DjatokaException(e.getMessage(), e);
        }

        if (in != null) {
            if (!in.delete() && LOGGER.isWarnEnabled()) {
                LOGGER.warn("File not deleted: {}", in);
            }
        }
        if (out != null) {
            if (!out.delete() && LOGGER.isWarnEnabled()) {
                LOGGER.warn("File not deleted: {}", out);
            }
        }
    }

    /**
     * Compress input using provided DjatokaEncodeParam parameters.
     * 
     * @param input InputStream containing TIFF image bitstream
     * @param output absolute file path for output file.
     * @param params DjatokaEncodeParam containing compression parameters.
     * @throws DjatokaException
     */
    public void compressImage(InputStream input, String output, DjatokaEncodeParam params) throws DjatokaException {
        if (params == null) {
            params = new DjatokaEncodeParam();
        }
        File inputFile;
        try {
            inputFile = File.createTempFile("tmp", ".tif");
            inputFile.deleteOnExit();
            IOUtils.copyStream(input, new FileOutputStream(inputFile));
            if (params.getLevels() == 0) {
                ImageRecord dim = ImageRecordUtils.getImageDimensions(inputFile.getAbsolutePath());
                params.setLevels(ImageProcessingUtils.getLevelCount(dim.getWidth(), dim.getHeight()));
                dim = null;
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new DjatokaException(e.getMessage(), e);
        }

        compressImage(inputFile.getAbsolutePath(), output, params);

        if (inputFile != null) {
            inputFile.delete();
        }
    }

    /**
     * Compress input using provided DjatokaEncodeParam parameters.
     * 
     * @param input InputStream containing TIFF image bitstream
     * @param output OutputStream to serialize compressed image.
     * @param params DjatokaEncodeParam containing compression parameters.
     * @throws DjatokaException
     */
    public void compressImage(InputStream input, OutputStream output, DjatokaEncodeParam params)
            throws DjatokaException {
        if (params == null) {
            params = new DjatokaEncodeParam();
        }
        File inputFile = null;
        try {
            inputFile = File.createTempFile("tmp", ".tif");
            IOUtils.copyStream(input, new FileOutputStream(inputFile));
            if (params.getLevels() == 0) {
                ImageRecord dim = ImageRecordUtils.getImageDimensions(inputFile.getAbsolutePath());
                params.setLevels(ImageProcessingUtils.getLevelCount(dim.getWidth(), dim.getHeight()));
                dim = null;
            }
        } catch (IOException e1) {
            LOGGER.error("Unexpected file format; expecting uncompressed TIFF", e1);
            throw new DjatokaException("Unexpected file format; expecting uncompressed TIFF");
        }

        String out = STDOUT;
        File winOut = null;
        if (isWindows) {
            try {
                winOut = File.createTempFile("pipe_", ".jp2");
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new DjatokaException(e.getMessage(), e);
            }
            out = winOut.getAbsolutePath();
        }

        String command = getKduCompressCommand(inputFile.getAbsolutePath(), out, params);
        String[] cmdParts = CommandLineTokenizer.tokenize(command);
        Runtime rt = Runtime.getRuntime();
        try {
            final Process process = rt.exec(cmdParts, envParams, new File(env));
            if (out.equals(STDOUT)) {
                IOUtils.copyStream(process.getInputStream(), output);
            } else if (isWindows) {
                FileInputStream fis = new FileInputStream(out);
                IOUtils.copyStream(fis, output);
                fis.close();
            }
            process.waitFor();
            if (process != null) {
                String errorCheck = null;
                try {
                    errorCheck = new String(IOUtils.getByteArray(process.getErrorStream()));
                } catch (Exception e1) {
                    LOGGER.error(e1.getMessage(), e1);
                }
                process.getInputStream().close();
                process.getOutputStream().close();
                process.getErrorStream().close();
                process.destroy();
                if (errorCheck != null) {
                    throw new DjatokaException(errorCheck);
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new DjatokaException(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            throw new DjatokaException(e.getMessage(), e);
        }

        if (inputFile != null) {
            if (!inputFile.delete() && LOGGER.isWarnEnabled()) {
                LOGGER.warn("File not deleted: {}", inputFile);
            }
        }
        if (winOut != null) {
            if (!winOut.delete() && LOGGER.isWarnEnabled()) {
                LOGGER.warn("File not deleted: {}", winOut);
            }
        }
    }

    /**
     * Compress input using provided DjatokaEncodeParam parameters.
     * 
     * @param input absolute file path for input file.
     * @param output absolute file path for output file.
     * @param params DjatokaEncodeParam containing compression parameters.
     * @throws DjatokaException
     */
    public void compressImage(String input, String output, DjatokaEncodeParam params) throws DjatokaException {
        if (params == null) {
            params = new DjatokaEncodeParam();
        }
        boolean tmp = false;
        File inputFile = null;
        if ((input.toLowerCase().endsWith(".tif") || input.toLowerCase().endsWith(".tiff") || ImageProcessingUtils
                .checkIfTiff(input)) &&
                ImageProcessingUtils.isUncompressedTiff(input)) {
            LOGGER.debug("Processing TIFF: " + input);
            inputFile = new File(input);
        } else {
            if (LOGGER.isDebugEnabled() &&
                    (input.toLowerCase().endsWith(".tif") || input.toLowerCase().endsWith(".tiff"))) {
                LOGGER.debug(input + " : Is tiff? {} | Is uncompressed? {}", ImageProcessingUtils.checkIfTiff(input),
                        ImageProcessingUtils.isUncompressedTiff(input));
            }

            try {
                inputFile = IOUtils.createTempTiff(input);
                tmp = true;
                input = inputFile.getAbsolutePath();
            } catch (Exception e) {
                throw new DjatokaException("Unrecognized file format: " + e.getMessage());
            }
        }

        if (params.getLevels() == 0) {
            ImageRecord dim = ImageRecordUtils.getImageDimensions(inputFile.getAbsolutePath());
            params.setLevels(ImageProcessingUtils.getLevelCount(dim.getWidth(), dim.getHeight()));
            dim = null;
        }

        File outFile = new File(output);
        String command = getKduCompressCommand(new File(input).getAbsolutePath(), outFile.getAbsolutePath(), params);
        String[] cmdParts = CommandLineTokenizer.tokenize(command);
        Runtime rt = Runtime.getRuntime();

        try {
            final Process process = rt.exec(cmdParts, envParams, new File(env));
            process.waitFor();
            if (process != null) {
                String errorCheck = null;

                try {
                    InputStream inStream = process.getErrorStream();
                    errorCheck = new String(IOUtils.getByteArray(inStream));
                } catch (Exception e1) {
                }

                process.getInputStream().close();
                process.getOutputStream().close();
                process.getErrorStream().close();
                process.destroy();

                if (errorCheck != null && !errorCheck.equals("")) {
                    throw new DjatokaException(errorCheck);
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new DjatokaException(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            throw new DjatokaException(e.getMessage(), e);
        } finally {
            if (tmp) {
                if (!inputFile.delete() && LOGGER.isWarnEnabled()) {
                    LOGGER.warn("File not deleted: {}", inputFile);
                }
            }
        }

        if (!outFile.getAbsolutePath().equals(STDOUT) && !outFile.exists()) {
            throw new DjatokaException("Unknown error occurred during processing.");
        }
    }

    /**
     * Get kdu_compress command line for specified input, output, params.
     * 
     * @param input absolute file path for input file.
     * @param output absolute file path for output file.
     * @param params DjatokaEncodeParam containing compression parameters.
     * @return kdu_compress command line for specified input, output, params
     */
    public static final String getKduCompressCommand(String input, String output, DjatokaEncodeParam params) {
        StringBuffer command = new StringBuffer(exe);
        command.append(" -quiet -i ").append(escape(new File(input).getAbsolutePath()));
        command.append(" -o ").append(escape(new File(output).getAbsolutePath()));
        command.append(" ").append(toKduCompressArgs(params));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Compress command: {}", command.toString());
        }

        return command.toString();
    }

    private static final String escape(String path) {
        if (path.contains(" ")) {
            return "\"" + path + "\"";
        }

        return path;
    }

    private static String toKduCompressArgs(DjatokaEncodeParam params) {
        StringBuffer sb = new StringBuffer();
        if (params.getRate() != null) {
            sb.append("-rate ").append(params.getRate()).append(" ");
        } else {
            sb.append("-slope ").append(params.getSlope()).append(" ");
        }
        if (params.getLevels() > 0) {
            sb.append("Clevels=").append(params.getLevels()).append(" ");
        }
        if (params.getPrecincts() != null) {
            sb.append("Cprecincts=").append(escape(params.getPrecincts()));
            sb.append(" ");
        }
        if (params.getLayers() > 0) {
            sb.append("Clayers=").append(params.getLayers()).append(" ");
        }
        if (params.getProgressionOrder() != null) {
            sb.append("Corder=").append(params.getProgressionOrder()).append(" ");
        }
        if (params.getPacketDivision() != null) {
            sb.append("ORGtparts=").append(params.getPacketDivision()).append(" ");
        }
        if (params.getCodeBlockSize() != null) {
            sb.append("Cblk=").append(escape(params.getCodeBlockSize())).append(" ");
        }
        sb.append("ORGgen_plt=").append((params.getInsertPLT()) ? "yes" : "no").append(" ");
        sb.append("Creversible=").append((params.getUseReversible()) ? "yes" : "no").append(" ");
        if (params.getJP2ColorSpace() != null && !params.getJP2ColorSpace().isEmpty()) {
            sb.append("-jp2_space ").append(params.getJP2ColorSpace());
            sb.append(' ');
        }

        return sb.toString();
    }
}
