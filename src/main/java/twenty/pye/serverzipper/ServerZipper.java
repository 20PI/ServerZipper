package twenty.pye.serverzipper;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ServerZipper extends JavaPlugin implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("zip")) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "You must provide a file path!");
                return false;
            }

            File worldContainer = getServer().getWorldContainer();
            String path = worldContainer.getPath() + "/" + args[0];
            File toZip = new File(path);

            if (!toZip.exists()) {
                sender.sendMessage(ChatColor.RED + "The file provided does not exist!");
                return false;
            }

            sender.sendMessage(ChatColor.YELLOW + "Attempting to zip provided file...");
            zipAsync(toZip, toZip.getParentFile().getPath() + "/" + toZip.getName() + ".zip", sender);
        } else if (label.equalsIgnoreCase("zipserver")) {
            sender.sendMessage(ChatColor.YELLOW + "Attempting to zip server...");
            zipAsync(getServer().getWorldContainer(),   ".server.zip", sender);
        }

        return false;
    }

    public void zipAsync(File fileOrDirectoryToZip, String outputPath, CommandSender sender) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                zip(fileOrDirectoryToZip, outputPath, sender);
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Failed to zip file!");
                e.printStackTrace();
            }
        });
    }

    public void zip(File fileOrDirectoryToZip, String outputPath, CommandSender sender) {
        try {
            FileOutputStream fos = new FileOutputStream(outputPath);
            ZipOutputStream zos = new ZipOutputStream(fos);
            if (fileOrDirectoryToZip.isDirectory()) {
                zipDirectory(fileOrDirectoryToZip, fileOrDirectoryToZip.getName(), zos);
            } else {
                zipFile(fileOrDirectoryToZip, fileOrDirectoryToZip.getName(), zos);
            }
            zos.close();
            fos.close();
            sender.sendMessage(ChatColor.GREEN + "Successfully zipped file!");
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Failed to zip file!");
            e.printStackTrace();
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

        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) >= 0) {
            zos.write(buffer, 0, length);
        }
        fis.close();
    }

    @Override
    public void onEnable() {
        getCommand("zip").setExecutor(this);
        getCommand("zipserver").setExecutor(this);

    }

    @Override
    public void onDisable() {

    }

}
