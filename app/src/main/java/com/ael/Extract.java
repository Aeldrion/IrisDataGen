package com.ael;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.Main;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.EmptyBlockGetter;

public class Extract {
    /**
     * Writes a JsonObject to a file
     * 
     * @param object   JSON object to write
     * @param filename Filename of the File to write into
     */
    public static void writeJsonFile(JsonObject object, String filename) throws IOException {
        File file = new File(filename);
        file.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(file);
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        gson.toJson(object, writer);
        writer.close();
        System.out.println("[+] Generated file " + filename);
    }

    /**
     * Returns block shape information for all possible block states of a block
     * 
     * @param block
     * @return JsonArray containing JsonObjects in the form {"properties": {...},
     *         "shape": "[AABB[x, y, z] -> [x, y, z]]"}
     */
    public static JsonArray getBlockShapes(Block block) {
        JsonArray array = new JsonArray();

        for (BlockState blockState : block.getStateDefinition().getPossibleStates()) {
            JsonObject blockStateJsonObject = new JsonObject();

            // Block properties
            JsonObject properties = new JsonObject();
            blockState.getValues().forEach(value -> {
                properties.addProperty(value.property().getName(), value.valueName());
            });
            blockStateJsonObject.add("properties", properties);

            // Block shape
            blockStateJsonObject.addProperty("shape",
                    blockState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs().toString());
            array.add(blockStateJsonObject);
        }

        return array;
    }

    /**
     * Returns block shape information for all blocks from the client's registry
     * 
     * @return JsonObject of the form {"block_id": [...]}
     *         (see getAllBlockShapes(Block block))
     */
    public static JsonObject getAllBlockShapes() {
        JsonObject blocksJsonObject = new JsonObject();
        int numBlocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            numBlocks++;
            String blockName = block.toString().substring(6, block.toString().length() - 1);
            blocksJsonObject.add(blockName, getBlockShapes(block));
        }

        System.out.println("Found " + String.valueOf(numBlocks) + " blocks");
        return blocksJsonObject;
    }

    /**
     * Returns entity width and height information of a given entity type
     * 
     * @param entityType
     * @return JsonObject of the form {"width": x, "height": y}
     */
    public static JsonObject getEntityDimensions(EntityType<?> entityType) {
        JsonObject object = new JsonObject();
        object.add("width", new JsonPrimitive(entityType.getWidth()));
        object.add("height", new JsonPrimitive(entityType.getHeight()));
        return object;
    }

    /**
     * Returns entity width and height information for all entity types from the
     * client's registry
     * 
     * @return JsonObject of the form {"entity_type_id": {"width": x, "height": y}}
     */
    public static JsonObject getAllEntityDimensions() {
        JsonObject entitiesJsonObject = new JsonObject();
        int numEntityTypes = 0;
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            numEntityTypes++;
            String[] splitEntityName = entityType.getDescriptionId().toString().split("[.]");
            String entityTypeName = "minecraft:" + splitEntityName[splitEntityName.length - 1];
            entitiesJsonObject.add(entityTypeName, getEntityDimensions(entityType));
        }

        System.out.println("Found " + String.valueOf(numEntityTypes) + " entity types");
        return entitiesJsonObject;
    }

    public static void main(String[] args) throws IOException {
        Main.main(new String[] { "--validate" });
        writeJsonFile(getAllBlockShapes(), "generated/blocks.json");
        writeJsonFile(getAllEntityDimensions(), "generated/entity_types.json");
    }
}
