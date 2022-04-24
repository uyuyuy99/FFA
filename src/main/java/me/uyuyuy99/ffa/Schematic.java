package me.uyuyuy99.ffa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.SessionManager;

public class Schematic {
	
	public static void save(Player player, File file) {
		try {
			try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
				SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
				Clipboard clipboard = sessionManager.get(new BukkitPlayer(player)).getClipboard().getClipboard();
				writer.write(clipboard);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void paste(Location pasteLoc, File file) {
		//FIXME NullPointerException for signs (tile entities) (TagCompound size problem?)
		try {
			Clipboard clipboard;
			ClipboardFormat format = ClipboardFormats.findByFile(file);
			
			try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
				clipboard = reader.read();
			}
			
			try (EditSession editSession = WorldEdit.getInstance().newEditSession(new BukkitWorld(pasteLoc.getWorld()))) {
				Operation operation = new ClipboardHolder(clipboard)
						.createPaste(editSession)
						.to(clipboard.getOrigin())
						.build();
				Operations.complete(operation);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
