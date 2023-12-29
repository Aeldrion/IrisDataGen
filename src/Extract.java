package src;

import java.io.FileWriter;
import java.io.File;
import java.nio.file.Files;
import java.io.PrintStream;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.lang.reflect.Field;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import net.minecraft.data.Main;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.EmptyBlockGetter;

import net.minecraft.world.entity.EntityType;

public class Extract {
    /**
     * Debug flag.
     */
    public static boolean debug_flag = false;
    /**
     * Minecraft version to extract from.
     */
    public static String mc_version = "";
    /**
     * Init a PrintStream that will be overriden by Minecraft's logger
     */
    public static PrintStream out = System.out;

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

                        // Block properties
                        {
                            JsonObject properties = new JsonObject();
                            for (Map.Entry<Property<?>, Comparable<?>> entry : bs.getValues().entrySet()) {
                                Class<?> valClass = entry.getKey().getValueClass();
                                if (valClass.equals(Integer.class)) {
                                    properties.addProperty(entry.getKey().getName(), (Integer) entry.getValue());
                                } else if (valClass.equals(Boolean.class)) {
                                    properties.addProperty(entry.getKey().getName(), (Boolean) entry.getValue());
                                } else {
                                    properties.addProperty(entry.getKey().getName(), String.valueOf(entry.getValue()));
                                }
                            }
                            state.add("properties", properties);
                        }

                        // Block shape
                        state.addProperty("shape",
                                bs.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs().toString());
                        blockStates.add(state);
                    }
                    block.add("states", blockStates);
                }

                {// Get the true block name from its description ID
                    String[] split_block_name = b.getDescriptionId().toString().split("[.]");// Regex split
                    String block_name = "minecraft:"+split_block_name[split_block_name.length-1];
                    blocks.add(block_name, block);
                    if (debug_flag)
                        out.println("> "+block_name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return blocks;
    }

    /**
     * Method to call if you want to extract all entities dimensions.
     * @return JsonObject containing entities dimensions.
     */
    public static JsonObject extract_entities_dimensions() {
        // Create an empty JSON object that will hold everything
        JsonObject entities = new JsonObject();

        Class<?> entity_type_class = EntityType.class;
        // For each EntityType from the Blocks class
        for (Field entity_type_field : entity_type_class.getDeclaredFields()) {
            entity_type_field.setAccessible(true);
            if (!EntityType.class.isAssignableFrom(entity_type_field.getType())) {
                continue;
            }
            // Get the EntityType object from the class field
            try {
                EntityType et = (EntityType)entity_type_field.get(null);
                JsonObject entity = new JsonObject();

                {// Get the Entity's dimensions
                    entity.add("width", new JsonPrimitive(et.getWidth()));
                    entity.add("height", new JsonPrimitive(et.getHeight()));
                }

                {// Get the true block name from its description ID
                    String[] split_entity_name = et.getDescriptionId().toString().split("[.]");// Regex split
                    String entity_name = "minecraft:"+split_entity_name[split_entity_name.length-1];
                    entities.add(entity_name, entity);
                    if (debug_flag)
                        out.println("> "+entity_name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return entities;
    }

    /**
     * Main method.
     * @param args Should contain the Minecraft version we are extracting from.
     */
    public static void main(String[] args) {
        // Parse arguments
        for (String arg : args) {
            // Check for a debug flag
            if (arg.equals("--debug"))
                debug_flag = true;
            else
                mc_version = arg;
        }

        // Initialize Minecraft Registries and everything needed
        try {
            Main.main(new String[]{ "--validate" });
        } catch (Exception e) {
            out.println("Main call of the Minecraft client's init failed.");
            e.printStackTrace();
            return;
        }

        // Write the JSON Object to a file
        write_json(extract_blocks_shapes(), "../../"+ mc_version + "_blocks.json");
        out.println("[+] Successfully exported the blocks hitboxes in " + mc_version +"_blocks.json");
        write_json(extract_entities_dimensions(), "../../"+ mc_version + "_entities.json");
        out.println("[+] Successfully exported the entities dimensions in " + mc_version +"_entities.json");
    }
}
