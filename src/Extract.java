package src;

import java.io.FileWriter;
import java.io.File;
import java.nio.file.Files;
import java.io.PrintStream;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.lang.reflect.Field;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
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
    public static boolean debug = false;
    public static boolean prettyPrinting = false;
    public static String gameVersion = "";
    public static PrintStream out = System.out; // Init a PrintStream that will be overriden by Minecraft's logger

    /**
     * Writes a JsonObject to a file
     * @param jsonObject JSON object to write
     * @param filename Filename of the File to write into
     */
    public static void writeJsonFile(JsonObject jsonObject, String filename, boolean prettyPrinting) {
        try {
            FileWriter writer = new FileWriter(filename);
            Gson gson = (prettyPrinting ? new GsonBuilder().setPrettyPrinting() : new GsonBuilder()).disableHtmlEscaping().create();
            gson.toJson(jsonObject, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sorts the keys of a JSON object alphabetically
     * @param jsonObject JSON object to sort
     * @return Sorted JSON object
     */
    public static JsonObject sortJsonKeys(JsonObject jsonObject) {
        JsonObject sortedJsonObject = new JsonObject();
        for (Map.Entry<String, JsonElement> entry: jsonObject.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            sortedJsonObject.add(entry.getKey(), entry.getValue());
        }
        return sortedJsonObject;
    }
    
    /**
     * Extracts all block shapes from the Minecraft client
     * @return JsonObject containing block shapes for every block state
     */
    public static JsonObject extractBlockShapes() {
        JsonObject blocksJsonObject = new JsonObject();

        Class<?> blocksClass = Blocks.class;
        // For each block from the Blocks class
        for (Field blocksField : blocksClass.getDeclaredFields()) {
            blocksField.setAccessible(true);
            if (!Block.class.isAssignableFrom(blocksField.getType()))
                continue;
            
            // Get the block object from the class field
            try {
                Block block = (Block)blocksField.get(null);
                JsonObject blockJsonObject = new JsonObject();

                {
                    // Get shape for each state of the object
                    JsonArray blockStates = new JsonArray();
                    for (BlockState blockState : block.getStateDefinition().getPossibleStates()) {
                        JsonObject blockStateJsonObject = new JsonObject();

                        {
                            // Block properties
                            JsonObject properties = new JsonObject();
                            for (Map.Entry<Property<?>, Comparable<?>> entry : blockState.getValues().entrySet())
                                properties.addProperty(entry.getKey().getName(), String.valueOf(entry.getValue()));
                            blockStateJsonObject.add("properties", properties);
                        }

                        // Block shape
                        blockStateJsonObject.addProperty("shape", blockState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs().toString());
                        blockStates.add(blockStateJsonObject);
                    }
                    blockJsonObject.add("states", blockStates);
                }

                {
                    // Get the true block name, e.g. air instead of Block{air}
                    String blockName = block.toString().substring(6, block.toString().length()-1);
                    blocksJsonObject.add(blockName, blockJsonObject);
                    if (debug)
                        out.println("> "+blockName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return blocksJsonObject;
    }

    /**
     * Method to call if you want to extract all entities dimensions.
     * @return JsonObject containing entities dimensions.
     */
    public static JsonObject extractEntityDimensions() {
        JsonObject entitiesJsonObject = new JsonObject();

        Class<?> entityTypeClass = EntityType.class;
        for (Field entityTypeField: entityTypeClass.getDeclaredFields()) {
            entityTypeField.setAccessible(true);
            if (!EntityType.class.isAssignableFrom(entityTypeField.getType()))
                continue;
            
            // Get the EntityType object from the class field
            try {
                EntityType entityType = (EntityType)entityTypeField.get(null);
                JsonObject entityJsonObject = new JsonObject();

                {
                    // Get the Entity's dimensions
                    entityJsonObject.add("width", new JsonPrimitive(entityType.getWidth()));
                    entityJsonObject.add("height", new JsonPrimitive(entityType.getHeight()));
                }

                {
                    // Get the true block name from its description ID
                    String[] splitEntityName = entityType.getDescriptionId().toString().split("[.]");
                    String entityName = "minecraft:"+splitEntityName[splitEntityName.length-1];
                    entitiesJsonObject.add(entityName, entityJsonObject);
                    if (debug)
                        out.println("> "+entityName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return entitiesJsonObject;
    }

    /**
     * Main method
     * @param args The Minecraft version we are extracting from
     */
    public static void main(String[] args) {
        // Parse arguments
        for (String arg: args) {
            // Check for debug flag
            if (arg.equals("--debug"))
                debug = true;
            else if (arg.equals("--pretty"))
                prettyPrinting = true;
            else
                gameVersion = arg;
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
        writeJsonFile(sortJsonKeys(extractBlockShapes()), "../../generated/"+ gameVersion + "_blocks.json", prettyPrinting);
        out.println("[+] Successfully exported block hitboxes in " + gameVersion +"_blocks.json");
        writeJsonFile(sortJsonKeys(extractEntityDimensions()), "../../generated/"+ gameVersion + "_entities.json", prettyPrinting);
        out.println("[+] Successfully exported entity dimensions in " + gameVersion +"_entities.json");
    }
}
