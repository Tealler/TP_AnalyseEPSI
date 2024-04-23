package com.simplecity.amp_library.http;

import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpServer {

    private static final String TAG = "HttpServer";

    private static HttpServer sHttpServer;

    private NanoServer server;

    private String audioFileToServe;
    private byte[] imageBytesToServe;

    private FileInputStream audioInputStream;
    private ByteArrayInputStream imageInputStream;

    private boolean isStarted = false;

    public static HttpServer getInstance() {
        if (sHttpServer == null) {
            sHttpServer = new HttpServer();
        }
        return sHttpServer;
    }

    private HttpServer() {
        server = new NanoServer();
    }

    public void serveAudio(String audioUri) {
        if (audioUri != null) {
            audioFileToServe = audioUri;
        }
    }

    public void serveImage(byte[] imageBytes) {
        if (imageBytes != null) {
            imageBytesToServe = imageBytes;
        }
    }

    public void clearImage() {
        imageBytesToServe = null;
    }

    public void start() {
        if (!isStarted) {
            try {
                server.start();
                isStarted = true;
            } catch (IOException e) {
                Log.e(TAG, "Error starting server: " + e.getMessage());
            }
        }
    }

    public void stop() {
        if (isStarted) {
            server.stop();
            isStarted = false;
            cleanupAudioStream();
            cleanupImageStream();
        }
    }

    private class NanoServer extends NanoHTTPD {

        NanoServer() {
            super(5000);
        }

        @Override
        public Response serve(IHTTPSession session) {

            if (audioFileToServe == null) {
                Log.e(TAG, "Audio file to serve null");
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "File not found");
            }

            String uri = session.getUri();
            if (uri.contains("audio")) {
                try {
                    File file = new File(audioFileToServe);

                    Map<String, String> headers = session.getHeaders();
                    String range = null;
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        if ("range".equals(entry.getKey())) {
                            range = entry.getValue();
                        }
                    }

                    if (range == null) {
                        range = "bytes=0-";
                        session.getHeaders().put("range", range);
                    }

                    long start;
                    long end;
                    long fileLength = file.length();

                    String rangeValue = range.trim().substring("bytes=".length());

                    if (rangeValue.startsWith("-")) {
                        end = fileLength - 1;
                        start = fileLength - 1 - Long.parseLong(rangeValue.substring("-".length()));
                    } else {
                        String[] ranges = rangeValue.split("-");
                        start = Long.parseLong(ranges[0]);
                        end = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileLength - 1;
                    }
                    if (end > fileLength - 1) {
                        end = fileLength - 1;
                    }

                    if (start <= end) {
                        long contentLength = end - start + 1;
                        cleanupAudioStream();
                        audioInputStream = new FileInputStream(file);
                        long skippedBytes = audioInputStream.skip(start);
                        System.out.println("Skipped " + skippedBytes + " bytes");
                        Response response = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, getMimeType(audioFileToServe), audioInputStream, contentLength);
                        response.addHeader("Content-Length", contentLength + "");
                        response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                        response.addHeader("Content-Type", getMimeType(audioFileToServe));
                        return response;
                    } else {
                        return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/html", range);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error serving audio: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (uri.contains("image")) {
                if (imageBytesToServe == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "Image bytes null");
                }
                cleanupImageStream();
                imageInputStream = new ByteArrayInputStream(imageBytesToServe);
                Log.i(TAG, "Serving image bytes: " + imageBytesToServe.length);
                return newFixedLengthResponse(Response.Status.OK, "image/png", imageInputStream, imageBytesToServe.length);
            }
            Log.e(TAG, "Returning NOT_FOUND response");
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "File not found");
        }
    }

    void cleanupAudioStream() {
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    void cleanupImageStream() {
        if (imageInputStream != null) {
            try {
                imageInputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private final Map<String, String> MIME_TYPES = createMimeTypesMap();

    private Map<String, String> createMimeTypesMap() {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("css", "text/css");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("xml", "text/xml");
        mimeTypes.put("java", "text/x-java-source, text/java");
        mimeTypes.put("md", "text/plain");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("asc", "text/plain");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("mp3", "audio/mpeg");
        mimeTypes.put("m3u", "audio/mpeg-url");
        mimeTypes.put("mp4", "video/mp4");
        mimeTypes.put("ogv", "video/ogg");
        mimeTypes.put("flv", "video/x-flv");
        mimeTypes.put("mov", "video/quicktime");
        mimeTypes.put("swf", "application/x-shockwave-flash");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("doc", "application/msword");
        mimeTypes.put("ogg", "application/x-ogg");
        mimeTypes.put("zip", "application/octet-stream");
        mimeTypes.put("exe", "application/octet-stream");
        mimeTypes.put("class", "application/octet-stream");
        return mimeTypes;
    }

    String getMimeType(String filePath) {
        return MIME_TYPES.get(filePath.substring(filePath.lastIndexOf(".") + 1));
    }
}