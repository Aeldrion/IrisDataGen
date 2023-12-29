package src;

import java.io.FileWriter;
import java.io.File;
import java.nio.file.Files;
import java.io.PrintStream;
import java.util.List;
import java.lang.reflect.Field;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import net.minecraft.data.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.EmptyBlockGetter;

public class Extract {
    /**
     * Simple method writing a JSON object to a file.
     * @param json_obj JSON to write.
     * @param filename Filename of the File to write into.
     */
    public static void write_json(JsonObject json_obj, String filename) {
        try {
            FileWriter myWriter = new FileWriter(filename);
            myWriter.write(json_obj.toString());
            myWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to call if you want to extract all blocks shapes from the Minecraft client.
     * @return JsonObject containing block shapes.
     */
    public static JsonObject extract_blocks_shapes() {
        // Create an empty JSON object that will hold everything
        JsonObject blocks = new JsonObject();

        Class<?> blocks_class = Blocks.class;
        // For each block from the Blocks class
        for (Field blocks_field : blocks_class.getDeclaredFields()) {
            blocks_field.setAccessible(true);
            if (!Block.class.isAssignableFrom(blocks_field.getType())) {
                continue;
            }
            // Get the block object from the class field
            try {
                Block b = (Block)blocks_field.get(null);
                JsonObject block = new JsonObject();

                {// Get shape for each state of the object
                    JsonArray blockStates = new JsonArray();
                    for (BlockState bs : b.getStateDefinition().getPossibleStates()) {
                        JsonObject state = new JsonObject();
                        state.addProperty("shape",
                                bs.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs().toString());
                        blockStates.add(state);
                    }
                    block.add("states", blockStates);
                }

                // Get the true block name from its description ID
                String[] split_block_name = b.getDescriptionId().toString().split("[.]");// Regex split
                blocks.add("minecraft:"+split_block_name[split_block_name.length-1], block);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return blocks;
    }

    /**
     * Main method.
     * @param args Should contain the Minecraft version we are extracting from.
     */
    public static void main(String[] args) {
        PrintStream out = System.out;// Init a PrintStream that will be overriden by Minecraft's logger

        // Initialize Minecraft Registries and everything needed
        try {
            Main.main(new String[]{ "--validate" });
        } catch (Exception e) {
            out.println("Main call of the Minecraft client's init failed.");
            e.printStackTrace();
            return;
        }

        // Write the JSON Object to a file
        write_json(extract_blocks_shapes(), "../../"+args[0] + "_blocks.json");
        out.println("[+] Successfully exported the blocks hitboxes in " + args[0]+"_blocks.json");
    }
}
