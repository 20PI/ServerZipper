package twenty.pye.serverzipper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ServerZipper extends JavaPlugin implements CommandExecutor {
    private static final String FILEBIN_URL = "https://filebin.net";
    private static final String UPLOAD_ENDPOINT = "/%s/%s";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (label.toLowerCase()) {
            case "upload":
            case "zip":
                if (args.length < 1) {
                    sender.sendMessage(ChatColor.RED + "You must provide a file path!");
                    return true;
                }

                File worldContainer = getServer().getWorldContainer();
                String path = worldContainer.getPath() + "/" + args[0];
                File fileToProcess = new File(path);

                if (!fileToProcess.exists()) {
                    sender.sendMessage(ChatColor.RED + "The file provided does not exist!");
                    return true;
                }

                if (label.equalsIgnoreCase("zip")) {
                    sender.sendMessage(ChatColor.YELLOW + "Attempting to zip provided file...");
                    zipFileAsync(fileToProcess, fileToProcess.getParentFile().getPath() + "/" + fileToProcess.getName() + ".zip", sender);
                } else {
                    if (fileToProcess.isDirectory()) {
                        sender.sendMessage(ChatColor.RED + "You cannot upload a directory!");
                        return false;
                    }
                    sender.sendMessage(ChatColor.YELLOW + "Attempting to upload file...");
                    uploadFileAsync("serverzipper", "serverzipper" + generateRandomString(6), fileToProcess.getName(), fileToProcess, sender);
                }
                break;

            case "zipserver":
                sender.sendMessage(ChatColor.YELLOW + "Attempting to zip server...");
                zipFileAsync(getServer().getWorldContainer(), ".server.zip", sender);
                break;

            default:
                return false;
        }

        return true;
    }

    private void uploadFileAsync(String cid, String bin, String filename, File file, CommandSender sender) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> uploadFile(cid, bin, filename, file, sender));
    }

    private void uploadFile(String cid, String bin, String filename, File file, CommandSender sender) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(String.format(FILEBIN_URL + UPLOAD_ENDPOINT, bin, filename));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            String boundary = Long.toHexString(System.currentTimeMillis());
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.writeBytes("--" + boundary + "\r\n");
                outputStream.writeBytes("Content-Disposition: form-data; name=\"cid\"\r\n\r\n");
                outputStream.writeBytes(cid + "\r\n");
                outputStream.writeBytes("--" + boundary + "\r\n");
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n\r\n");

                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                outputStream.writeBytes("\r\n--" + boundary + "--\r\n");
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                sender.sendMessage(ChatColor.GREEN + "Upload successful: " + String.format("https://filebin.net/%s/%s", bin, filename));
            } else {
                sender.sendMessage(ChatColor.RED + "File upload failed. Response code: " + responseCode);
                Bukkit.getLogger().warning("File upload failed. Response code: " + responseCode);
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("An error occurred while uploading the file: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String generateRandomString(int length) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, length);
    }

    private void zipFileAsync(File fileOrDirectoryToZip, String outputPath, CommandSender sender) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                zipFile(fileOrDirectoryToZip, outputPath);
                sender.sendMessage(ChatColor.GREEN + "Successfully zipped file!");
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "Failed to zip file!");
                Bukkit.getLogger().severe("An error occurred while zipping the file: " + e.getMessage());
            }
        });
    }

    private void zipFile(File fileOrDirectoryToZip, String outputPath) throws IOException {
        try (
                FileOutputStream fos = new FileOutputStream(outputPath);
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            if (fileOrDirectoryToZip.isDirectory()) {
                zipDirectory(fileOrDirectoryToZip, fileOrDirectoryToZip.getName(), zos);
            } else {
                zipFile(fileOrDirectoryToZip, fileOrDirectoryToZip.getName(), zos);
            }
        }
    }

    private void zipDirectory(File directoryToZip, String fileName, ZipOutputStream zos) throws IOException {
        File[] files = directoryToZip.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    zipDirectory(file, fileName + "/" + file.getName(), zos);
                } else {
                    zipFile(file, fileName + "/" + file.getName(), zos);
                }
            }
        }
    }

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.getName().equalsIgnoreCase(".server.zip")) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to zip file " + fileName + ": " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        getCommand("zip").setExecutor(this);
        getCommand("zipserver").setExecutor(this);
        getCommand("upload").setExecutor(this);
    }
}