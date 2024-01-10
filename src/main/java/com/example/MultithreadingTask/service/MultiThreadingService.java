package com.example.MultithreadingTask.service;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.xml.sax.SAXException;
import org.apache.tika.exception.TikaException;

@ShellComponent
public class MultiThreadingService {
    private final AtomicInteger totalOccurrences = new AtomicInteger(0);
    private static final String LOG_FILE_PREFIX = "logfile_";
    private static final Logger LOGGER = Logger.getLogger(MultiThreadingService.class.getName());

    /**
     * This method is used to search for a string in the files of a folder.
     *
     * @param folderPath       The path of the folder in which the string is to be searched.
     * @param searchString     The string to be searched.
     * @param baseLogDirectory The base directory in which the log directories are to be created.
     */
    @ShellMethod("Search for exact string in the specified folder")
    public void search(@ShellOption String folderPath, String searchString, String baseLogDirectory) {
        File folder = new File(folderPath);
        if (folder.isDirectory()) {
            createLogDirectories(baseLogDirectory);
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            File[] files = folder.listFiles();
            if (files != null) {
                int filesPerThread = files.length / 10;
                for (int i = 0; i <= 10; i++) {
                    int startIndex = i * filesPerThread;
                    int endIndex = (i == 10) ? files.length : (i + 1) * filesPerThread;
                    File[] threadFiles = new File[endIndex - startIndex];
                    System.arraycopy(files, startIndex, threadFiles, 0, endIndex - startIndex);
                    int currentThreadIndex = i + 1;
                    executorService.submit(() -> searchInFiles(threadFiles, searchString, baseLogDirectory, currentThreadIndex));
                }
            }
            if (totalOccurrences.get() == 0) {
                LOGGER.info("No occurrences found for the specified string: " + searchString);
            }
            executorService.shutdown();
            handleExecutorShutdown(executorService);
        } else {
            LOGGER.info("Provided path is not a directory.");
        }
    }


    /**
     * This method is used to create the log directories.
     *
     * @param baseLogDirectory The base directory in which the log directories are to be created.
     */
    private void createLogDirectories(String baseLogDirectory) {
        File logFolder = new File(baseLogDirectory);
        if (logFolder.mkdirs()) {
            LOGGER.info("Log directories created successfully at: " + logFolder);
        } else {
            LOGGER.warning("Failed to create log directories at: " + logFolder);
        }
    }


    /**
     * This method is used to handle the shutdown of the executor service.
     *
     * @param executorService The executor service to be shutdown.
     */
    private void handleExecutorShutdown(ExecutorService executorService) {
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    /**
     * This method is used to search for a string in the files of a folder.
     *
     * @param files              The files in which the string is to be searched.
     * @param searchString       The string to be searched.
     * @param logFolderPath      The path of the folder in which the log files are to be created.
     * @param currentThreadIndex The index of the current thread.
     */
    private void searchInFiles(File[] files, String searchString, String logFolderPath, int currentThreadIndex) {
        String logFileName = LOG_FILE_PREFIX + currentThreadIndex + ".txt";
        String logFilePath = logFolderPath + logFileName;

        for (File file : files) {
            if (file.isFile()) {
                int occurrencesInFile = searchInFile(file, searchString);
                if (occurrencesInFile > 0) {
                    LOGGER.info(file.getAbsolutePath() + ": " + occurrencesInFile + " occurrences");
                    totalOccurrences.addAndGet(occurrencesInFile);
                    logOccurrences(logFilePath, file.getAbsolutePath(), occurrencesInFile);
                }
            } else if (file.isDirectory()) {
                searchInFiles(Objects.requireNonNull(file.listFiles()), searchString, logFolderPath, currentThreadIndex);
            }
        }
    }


    /**
     * This method is used to search for a string in a file.
     *
     * @param file         The file in which the string is to be searched.
     * @param searchString The string to be searched.
     * @return The number of occurrences of the string in the file.
     */
    private int searchInFile(File file, String searchString) {
        int occurrencesInFile = 0;
        try (InputStream stream = new FileInputStream(file)) {
            if (stream.available() > 0) {
                AutoDetectParser parser = new AutoDetectParser();
                BodyContentHandler handler = new BodyContentHandler(-1);
                Metadata metadata = new Metadata();
                ParseContext parseContext = new ParseContext();
                parser.parse(stream, handler, metadata, parseContext);
                String fileContent = handler.toString();
                occurrencesInFile = occurrences(fileContent, searchString);
            }
        } catch (IOException | TikaException | SAXException e) {
            LOGGER.info("Error while parsing file: " + file.getAbsolutePath());
        }
        return occurrencesInFile;
    }


    /**
     * This method is used to log the number of occurrences of a string in a file.
     *
     * @param logFilePath The path of the log file.
     * @param filePath    The path of the file in which the occurrences of the string is to be counted.
     * @param occurrences The number of occurrences of the string in the file.
     */
    private void logOccurrences(String logFilePath, String filePath, int occurrences) {
        try (FileWriter fileWriter = new FileWriter(logFilePath, true); BufferedWriter writer = new BufferedWriter(fileWriter)) {
            writer.write(filePath + ": " + occurrences + " occurrences");
            writer.newLine();
        } catch (IOException e) {
            LOGGER.info("Error while writing to log file");
        }
    }


    /**
     * This method is used to count the number of occurrences of a string in a given text.
     *
     * @param text         The text in which the occurrences of the string is to be counted.
     * @param searchString The string whose occurrences is to be counted.
     * @return The number of occurrences of the string in the text.
     */
    public static int occurrences(String text, String searchString) {
        int count = 0;
        String[] words = text.split("\\W+");
        for (String word : words) {
            if (word.equalsIgnoreCase(searchString)) {
                count++;
            }
        }
        return count;
    }
}

