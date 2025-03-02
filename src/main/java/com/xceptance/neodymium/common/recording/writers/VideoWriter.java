package com.xceptance.neodymium.common.recording.writers;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGB24;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegFrameRecorder.Exception;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.common.recording.TakeScreenshotsThread;
import com.xceptance.neodymium.common.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;

/**
 * Writer to create a video using FFmpeg
 * 
 * @author olha
 */
public class VideoWriter implements Writer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoWriter.class);

    private VideoRecordingConfigurations recordingConfigurations;

    private int screenshots;

    private long testDuration;

    private String videoFileName;

    private ProcessBuilder pb;

    private OutputStream ffmpegInput;

    private Process p;

    private Queue<File> screenshotFrames = new ConcurrentLinkedQueue<>();

    private FFmpegFrameRecorder rec;

    /**
     * Required to instantiate the object in the {@link Writer#instantiate(Class, RecordingConfigurations, String)}
     * method.
     * <p>
     * Prepares the FFmpeg command with {@link ProcessBuilder}.
     * 
     * @param recordingConfigurations
     *            {@link VideoRecordingConfigurations} for the writer
     * @param videoFileName
     *            {@link String} video file name ( including the path)
     * @throws FileNotFoundException
     */
    protected VideoWriter(RecordingConfigurations recordingConfigurations, String videoFileName) throws FileNotFoundException
    {
        this.recordingConfigurations = ((VideoRecordingConfigurations) recordingConfigurations);
        // check if ffmpeg binary is found
        // this.recordingConfigurations = ((VideoRecordingConfigurations) recordingConfigurations);
        double framerate = 1 / ((double) recordingConfigurations.oneImagePerMilliseconds() / 1000);

        rec = new FFmpegFrameRecorder(new File(videoFileName), 1024, 629);
        rec.setFormat("webm");
        // rec.setVideoCodec(13);

        rec.setVideoCodecName("libx264");
        rec.setPixelFormat(AV_PIX_FMT_YUV420P);
        // rec.setPixelFormat(AV_PIX_FMT_RGB32_1);
        rec.setFrameRate(framerate);
        System.out.println("Original framerate " + framerate);
        // String ffmpegBinary = this.recordingConfigurations.ffmpegBinaryPath();
        // try
        // {
        // p = new ProcessBuilder(ffmpegBinary, "-h").start();
        // }
        // catch (Exception e)
        // {
        // throw (FileNotFoundException) new FileNotFoundException("FFmpeg binary not found at " + ffmpegBinary
        // + ", please install FFmpeg and add it to the PATH or enter the correct FFmpeg binary location.
        // ").initCause(e);
        // }
        //
        this.videoFileName = videoFileName;
        // pb = new ProcessBuilder(((VideoRecordingConfigurations) recordingConfigurations).ffmpegBinaryPath(), "-y",
        // "-f", "image2pipe", "-r", Fraction.getFraction(framerate)
        // .toString(), "-i", "pipe:0", "-c:v", "libx264", "-strict", "-2", "-preset", "slow", "-pix_fmt", "yuv420p",
        // "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2", "-f", "mp4", videoFileName);
        // pb.redirectErrorStream(true);
        // pb.redirectOutput(Redirect.appendTo(new File((this.recordingConfigurations).ffmpegLogFile())));
    }

    /**
     * Starts FFmpeg video generation.
     * 
     * @throws IOException
     *             to stop screenshots loop on {@link TakeScreenshotsThread} in case of start failure
     */
    @Override
    public void start() throws IOException
    {
        rec.start();
        // p = pb.start();
        // ffmpegInput = p.getOutputStream();
    }

    /**
     * Writes {@link File} into the FFmpeg {@link Process}
     */
    @Override
    public void write(File image, long duration)
    {
        // if (rec == null)
        // {
        // double framerate = 1 / ((double) recordingConfigurations.oneImagePerMilliseconds() / 1000);
        //
        // rec = new FFmpegFrameRecorder(new File(videoFileName), 1024, 629);
        // rec.setFormat("mp4");
        // rec.setVideoCodecName("libx264");
        // rec.setVideoOption("crf", "0");
        // rec.setPixelFormat(AV_PIX_FMT_YUV420P);
        // rec.setFrameRate(framerate);
        // try
        // {
        // rec.start();
        // }
        // catch (Exception e)
        // {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // }

        screenshotFrames.add(image);
        screenshots++;
        testDuration += duration;

        // try (FileInputStream fileInputStream = new FileInputStream(image))
        // {
        // fileInputStream.read(imageBytes);
        //
        // ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes));
        // BufferedImage img = ImageIO.read(iis);
        // ImageIO.write(img, "PNG", ffmpegInput);
        // screenshots++;
        // testDuration += duration;
        // }
        // catch (IOException e)
        // {
        // throw new RuntimeException(e);
        // }

        // byte[] imageBytes;
        // imageBytes = new byte[(int) image.length()];
        // try (InputStream in = new ByteArrayInputStream(imageBytes))
        // {
        // try (Java2DFrameConverter converter = new Java2DFrameConverter())
        // {
        // BufferedImage img1 = ImageIO.read(image);
        // // rec.record(Java2DFrameUtils.toFrame(img1));
        // var g = AV_PIX_FMT_RGB24;
        // rec.record(converter.getFrame(img1, 1.0, true), AV_PIX_FMT_RGB24);
        // screenshots++;
        // // rec.record(converter.getFrame(ImageIO.read(in), 1.0, true));
        // }
        // }
        // catch (IOException e1)
        // {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }
        // testDuration += duration;
    }

    /**
     * Flushes and closes the input stream of the FFmpeg {@link Process} to make it start processing the video. Waits
     * until the video processing is done and logs to the allure report the time required for the processing
     */
    @Override
    public void stop()
    {
        try
        {
            // int width = 0;
            // int height = 0;
            // byte[] imageBtes;
            // BufferedImage img = ImageIO.read(screenshotFrames.element());
            // width = img.getWidth();
            // height = img.getHeight();
            // double framerate = 1 / ((double) recordingConfigurations.oneImagePerMilliseconds() / 1000);
            //
            // rec = new FFmpegFrameRecorder(new File(videoFileName), width, height);
            // rec.setFormat("mp4");
            // rec.setVideoCodecName("libx264");
            // rec.setVideoOption("crf", "0");
            // rec.setPixelFormat(AV_PIX_FMT_YUV420P);
            // rec.setFrameRate(framerate);
            // try
            // {
            // rec.start();
            // }
            // catch (Exception e)
            // {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            // for (File frame : screenshotFrames)
            // {
            // byte[] imageBytes;
            // imageBytes = new byte[(int) frame.length()];
            // try (InputStream in = new ByteArrayInputStream(imageBytes))
            // {
            // try (Java2DFrameConverter converter = new Java2DFrameConverter())
            // {
            // BufferedImage img1 = ImageIO.read(frame);
            // rec.record(converter.getFrame(img1, 1.0, true));
            // // rec.record(converter.getFrame(ImageIO.read(in), 1.0, true));
            // }
            // }
            // catch (IOException e1)
            // {
            // // TODO Auto-generated catch block
            // e1.printStackTrace();
            // }
            // // rec.record(frame);
            // }
            double actualFramerate = screenshots / ((double) testDuration / 1000);
            rec.setFrameRate(actualFramerate);
            for (File image : screenshotFrames)
            {
                byte[] imageBytes;
                imageBytes = new byte[(int) image.length()];
                try (InputStream in = new ByteArrayInputStream(imageBytes))
                {
                    try (Java2DFrameConverter converter = new Java2DFrameConverter())
                    {
                        BufferedImage img1 = ImageIO.read(image);
                        // rec.record(Java2DFrameUtils.toFrame(img1));
                        var g = AV_PIX_FMT_RGB24;
                        rec.record(converter.getFrame(img1, 1.0, true), AV_PIX_FMT_RGB24);
                        // rec.record(converter.getFrame(ImageIO.read(in), 1.0, true));
                    }
                }
                catch (IOException e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            System.out.println("Actual framerate " + actualFramerate);
            rec.stop();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e3)
        {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }

        // try
        // {
        // ffmpegInput.close();
        // ffmpegInput.flush();
        // }
        // catch (IOException e)
        // {
        // throw new RuntimeException(e);
        // }
        //
        // long videoProcessingStart = new Date().getTime();
        // while (p.isAlive())
        // {
        // if (new Date().getTime() - videoProcessingStart > 200000)
        // {
        // LOGGER.error("something went wrong with video processing");
        // break;
        // }
        // LOGGER.info("process video is processing");
        // Selenide.sleep(200);
        // }
        // File tempFile = new File(recordingConfigurations.tempFolderToStoreRecording() + "/" + "temp" +
        // UUID.randomUUID() + ".mp4");
        // new File(videoFileName).renameTo(tempFile);
        // double actualFramerate = screenshots / ((double) testDuration / 1000);
        // pb = new ProcessBuilder(recordingConfigurations.ffmpegBinaryPath(), "-y", "-r", actualFramerate + "", "-i",
        // tempFile.getPath(), videoFileName);
        //
        // pb.redirectErrorStream(true);
        // pb.redirectOutput(Redirect.appendTo(new File((this.recordingConfigurations).ffmpegLogFile())));
        // try
        // {
        // p = pb.start();
        // }
        // catch (IOException e)
        // {
        // throw new RuntimeException("Could not adjust video frame rate", e);
        // }
        // while (p.isAlive())
        // {
        // if (new Date().getTime() - videoProcessingStart > 200000)
        // {
        // LOGGER.error("something went wrong with adjusting frame rate");
        // break;
        // }
        // LOGGER.info("video frame rate adjustment is processing");
        // Selenide.sleep(200);
        // }
        // tempFile.delete();
        // if (recordingConfigurations.logInformationAboutRecording())
        // {
        // AllureAddons.addToReport("video processing took " + (new Date().getTime() - videoProcessingStart + " ms"),
        // "");
        // }
    }
}
